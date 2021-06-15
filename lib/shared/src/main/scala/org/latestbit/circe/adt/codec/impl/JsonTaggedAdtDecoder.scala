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

object JsonTaggedAdtDecoder {

  class JsonAdtFieldDef[T](val decoder: Decoder[T]) {
    def fromJsonObject( cursor: HCursor ): Decoder.Result[T] = decoder(cursor)
  }

  private inline final def summonDecoder[A]: Decoder[A] = summonFrom {
    case decoderA: Decoder[A] => decoderA
    case _: Mirror.Of[A] => Decoder.derived[A]
  }

  private inline def summmonAllDefs[T,Fields <: Tuple, Types <: Tuple]: Map[String, JsonAdtFieldDef[_]] = {
    inline erasedValue[(Fields, Types)] match {
      case (_: (field *: fields), _: (tpe *: types)) =>
        val fieldLabel = constValue[field].toString()
        summmonAllDefs[T, fields, types] + (
          fieldLabel -> JsonAdtFieldDef[tpe](
            decoder = summonDecoder[tpe]
          )
        )
      case _ => Map.empty
    }
  }

  private inline def createJsonTaggedAdtDecoder[T](using m: Mirror.Of[T], adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtDecoder[T] = {
    lazy val allDefs: Map[String, JsonAdtFieldDef[_]] = summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]
    val stringDecoder: Decoder[Option[String]] = summonDecoder[Option[String]]

    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonTaggedAdtDecoder[T] {

        override def apply(cursor: HCursor): Decoder.Result[T] = {
          cursor.get[Option[String]](adtConfig.typeFieldName)(using stringDecoder).flatMap {
            case Some(typeFieldValue: String) => {
              allDefs.get(typeFieldValue) match {
                case Some(caseClassDef) => {
                  caseClassDef.fromJsonObject(cursor).map(_.asInstanceOf[T])
                }
                case _ =>
                  Decoder.failedWithMessage[T](
                    s"Received unknown type: '${typeFieldValue}'. Exists only types: ${allDefs.keys.mkString(", ")}."
                  )(cursor)
              }
            }
            case _ =>
              Decoder.failedWithMessage[T](
                s"'${adtConfig.typeFieldName}' isn't specified in json."
              )(cursor)
          }
        }
      }

      case productOfT: Mirror.ProductOf[T] =>
          new JsonTaggedAdtDecoder[T] {
            override def apply(c: HCursor): Decoder.Result[T] = {
              ???
            }
          }
    }
  }

  implicit inline given derived[T](using m: Mirror.Of[T],
                                   adtConfig: JsonTaggedAdt.Config[T] =
                                   JsonTaggedAdt.Config.empty[T]): JsonTaggedAdtDecoder[T] =
    createJsonTaggedAdtDecoder[T]

}

object JsonTaggedAdtDecoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtDecoderWithConfig[T] = {
    val parent = JsonTaggedAdtDecoder.derived[T]
    new JsonTaggedAdtDecoderWithConfig[T] {
      override def apply(c: HCursor): Decoder.Result[T] = parent.apply(c)
    }
  }

}
