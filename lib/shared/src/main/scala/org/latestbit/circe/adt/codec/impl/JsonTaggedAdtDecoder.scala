/*
 * Copyright 2021 Abdulla Abdurakhmanov (abdulla@latestbit.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.latestbit.circe.adt.codec.impl

import io.circe.*
import org.latestbit.circe.adt.codec.*

import scala.compiletime.*
import scala.deriving.*

sealed trait JsonTaggedAdtDecoder[T] extends Decoder[T]
sealed trait JsonTaggedAdtDecoderWithConfig[T] extends JsonTaggedAdtDecoder[T]

sealed trait JsonPureTaggedAdtDecoder[T] extends JsonTaggedAdtDecoder[T]
sealed trait JsonPureTaggedAdtDecoderWithConfig[T] extends JsonPureTaggedAdtDecoder[T]

object JsonTaggedAdtDecoder {

  class JsonAdtFieldDef[T](val decoder: Decoder[T]) {
    def fromJsonObject( cursor: HCursor ): Decoder.Result[T] = decoder(cursor)
    def fromEmptyObject(): Decoder.Result[T] = decoder.decodeJson(Json.fromJsonObject(JsonObject.empty))
  }

  inline final def summonDecoder[A]: Decoder[A] = summonFrom {
    case decoderA: Decoder[A] => decoderA
    case _: Mirror.Of[A] => Decoder.derived[A]
  }

  inline def summmonAllDefs[T,Fields <: Tuple, Types <: Tuple](using inline adtConfig: JsonTaggedAdt.BaseConfig[T]): Map[String, JsonAdtFieldDef[_]] = {
    inline erasedValue[(Fields, Types)] match {
      case (_: (field *: fields), _: (tpe *: types)) =>
        val tagClassName = JsonTaggedAdt.tagged[tpe](using summonInline[Mirror.Of[tpe]]).tagClassName
        val tagValue: String = adtConfig.mappings
          .find(_._2.tagClassName == tagClassName)
          .map(_._1)
          .getOrElse(
            constValue[field].toString()
          )
        summmonAllDefs[T, fields, types] + (
          tagValue -> JsonAdtFieldDef[tpe](
            decoder = summonDecoder[tpe]
          )
        )
      case _ => Map.empty
    }
  }

  inline def createJsonTaggedAdtDecoder[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtDecoder[T] = {
    lazy val allDefs: Map[String, JsonAdtFieldDef[_]] = summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]

    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonTaggedAdtDecoder[T] {

        override def apply(cursor: HCursor): Decoder.Result[T] = {
          adtConfig.decoderDefinition.decodeTaggedJsonObject(
            cursor,
            adtConfig.typeFieldName
          ).flatMap { case ((tagValue, cursor)) =>
            allDefs.get(tagValue) match {
              case Some(caseClassDef) => {
                caseClassDef.fromJsonObject(cursor).map(_.asInstanceOf[T])
              }
              case _ =>
                Decoder.failedWithMessage[T](
                  s"Received unknown type: '${tagValue}'. Exists only types: ${allDefs.keys.mkString(", ")}."
                )(cursor)
            }
          }
        }
      }

      case productOfT: Mirror.ProductOf[T] =>
          new JsonTaggedAdtDecoder[T] {
            override def apply(c: HCursor): Decoder.Result[T] = {
              scala.compiletime.error("This codec implementation for Scala 3 doesn't support deriving anything except enums")
            }
          }
    }
  }

  implicit inline given derived[T](using m: Mirror.Of[T],
                                   inline adtConfig: JsonTaggedAdt.Config[T] = JsonTaggedAdt.Config.default[T]): JsonTaggedAdtDecoder[T] =
    createJsonTaggedAdtDecoder[T]

}

object JsonTaggedAdtDecoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtDecoderWithConfig[T] = {
    val parent = JsonTaggedAdtDecoder.derived[T]
    new JsonTaggedAdtDecoderWithConfig[T] {
      override def apply(c: HCursor): Decoder.Result[T] = parent.apply(c)
    }
  }

}

object JsonPureTaggedAdtDecoder {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.PureConfig[T] = JsonTaggedAdt.PureConfig.default[T]): JsonPureTaggedAdtDecoder[T] = {
    lazy val allDefs: Map[String, JsonTaggedAdtDecoder.JsonAdtFieldDef[_]] = JsonTaggedAdtDecoder.summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]
    val stringDecoder: Decoder[String] = JsonTaggedAdtDecoder.summonDecoder[String]

    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonPureTaggedAdtDecoder[T] {

        override def apply(cursor: HCursor): Decoder.Result[T] = {
          cursor.as[String](using stringDecoder).flatMap { tagValue =>
            allDefs.get(tagValue) match {
              case Some(caseClassDef) => {
                caseClassDef.fromEmptyObject().map(_.asInstanceOf[T])
              }
              case _ =>
                Decoder.failedWithMessage[T](
                  s"Received unknown type: '${tagValue}'. Exists only types: ${allDefs.keys.mkString(", ")}."
                )(cursor)
            }
          }
        }
      }

      case productOfT: Mirror.ProductOf[T] => new JsonPureTaggedAdtDecoder[T] {
          override def apply(c: HCursor): Decoder.Result[T] = {
            scala.compiletime.error("This codec implementation for Scala 3 doesn't support deriving anything except enums")
          }
        }
    }
  }

}

object JsonPureTaggedAdtDecoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.PureConfig[T]): JsonPureTaggedAdtDecoderWithConfig[T] = {
    val parent = JsonPureTaggedAdtDecoder.derived[T]
    new JsonPureTaggedAdtDecoderWithConfig[T] {
      override def apply(c: HCursor): Decoder.Result[T] = parent.apply(c)
    }
  }

}
