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

sealed trait JsonTaggedAdtEncoder[T] extends Encoder.AsObject[T]
sealed trait JsonTaggedAdtEncoderWithConfig[T] extends JsonTaggedAdtEncoder[T]

sealed trait JsonPureTaggedAdtEncoder[T] extends Encoder[T]
sealed trait JsonPureTaggedAdtEncoderWithConfig[T] extends JsonPureTaggedAdtEncoder[T]

object JsonTaggedAdtEncoder {

  class JsonAdtFieldDef[T](val tagValue: String,
                           val encoder: Encoder.AsObject[T]) {
    def toJsonObject( obj: T ): JsonObject = encoder.encodeObject(obj)
  }

  inline final def summonEncoder[T]: Encoder.AsObject[T] = summonFrom {
    case encodeA: Encoder.AsObject[T] => encodeA
    case _: Mirror.Of[T] => Encoder.AsObject.derived[T]
  }

  inline def summmonAllDefs[T,Fields <: Tuple, Types <: Tuple](using inline adtConfig: JsonTaggedAdt.Config[T]): Vector[JsonAdtFieldDef[_]] = {
    inline erasedValue[(Fields, Types)] match {
      case (_: (field *: fields), _: (tpe *: types)) => {
        val tagClassName = JsonTaggedAdt.tagged[tpe](using summonInline[Mirror.Of[tpe]]).tagClassName
        val tagValue: String = adtConfig.mappings
          .find(_._2.tagClassName == tagClassName)
          .map(_._1)
          .getOrElse(
            constValue[field].toString()
          )
        JsonAdtFieldDef[tpe](
          tagValue = tagValue,
          encoder = summonEncoder[tpe]
        ) +: summmonAllDefs[T, fields, types]
      }
      case _ => Vector.empty
    }
  }

  inline def createJsonTaggedAdtEncoder[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtEncoder[T] = {
    lazy val allDefs: Vector[JsonAdtFieldDef[_]] = summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]


    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonTaggedAdtEncoder[T] {

        override def encodeObject(obj: T): JsonObject = {
          val caseClassIdx = sumOfT.ordinal(obj)
          val caseClassDef = allDefs(caseClassIdx).asInstanceOf[JsonAdtFieldDef[T]]
          val srcJsonObj: JsonObject = caseClassDef.toJsonObject(obj)
          adtConfig.encoderDefinition.encodeTaggedJsonObject(
            adtConfig.typeFieldName,
            caseClassDef.tagValue,
            srcJsonObj
          )
        }
      }

      case productOfT: Mirror.ProductOf[T] => new JsonTaggedAdtEncoder[T] {
        override def encodeObject(obj: T): JsonObject = {
          scala.compiletime.error("This codec implementation for Scala 3 doesn't support deriving anything except enums")
        }
      }
    }
  }

  implicit inline given derived[T](using m: Mirror.Of[T],
                                   inline adtConfig: JsonTaggedAdt.Config[T] =
                                    JsonTaggedAdt.Config.empty[T]): JsonTaggedAdtEncoder[T] =
    createJsonTaggedAdtEncoder[T]

}

object JsonTaggedAdtEncoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtEncoderWithConfig[T] = {
    val parent = JsonTaggedAdtEncoder.derived[T]
    new JsonTaggedAdtEncoderWithConfig[T] {
      override def encodeObject(obj: T): JsonObject = {
        parent.encodeObject(obj)
      }
    }
  }

}

object JsonPureTaggedAdtEncoder {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T] = JsonTaggedAdt.Config.empty[T]): JsonPureTaggedAdtEncoder[T] = {
    lazy val allDefs: Vector[JsonTaggedAdtEncoder.JsonAdtFieldDef[_]] = JsonTaggedAdtEncoder.summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]

    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonPureTaggedAdtEncoder[T] {
        override def apply(obj: T): Json = {
          val caseClassIdx = sumOfT.ordinal(obj)
          val caseClassDef = allDefs(caseClassIdx)
          Json.fromString(caseClassDef.tagValue)
        }
      }

      case productOfT: Mirror.ProductOf[T] => new JsonPureTaggedAdtEncoder[T] {
        override def apply(obj: T): Json = {
          scala.compiletime.error("This codec implementation for Scala 3 doesn't support deriving anything except enums")
        }
      }
    }
  }

}

object JsonPureTaggedAdtEncoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], inline adtConfig: JsonTaggedAdt.Config[T]): JsonPureTaggedAdtEncoderWithConfig[T] = {
    val parent = JsonPureTaggedAdtEncoder.derived[T]
    new JsonPureTaggedAdtEncoderWithConfig[T] {
      override def apply(obj: T): Json = {
        parent.apply(obj)
      }
    }
  }

}
