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

package org.latestbit.circe.adt.codec

import io.circe.*
import scala.deriving.*
import scala.compiletime.*

/**
 * Auxiliary ADT case classes to JSON object converter
 *
 * @tparam T
 *   A trait type
 */
sealed trait JsonTaggedAdtEncoder[T] extends Encoder.AsObject[T] {
  def tagFor(obj: T): String
}

sealed trait JsonTaggedAdtEncoderWithConfig[T] extends JsonTaggedAdtEncoder[T]

object JsonTaggedAdtEncoder {

  class JsonAdtFieldDef[T](val fieldLabel: String, val encoder: Encoder.AsObject[T]) {
    inline def toJsonObject( obj: T ): JsonObject = encoder.encodeObject(obj)
  }

  private inline final def summonEncoder[A]: Encoder.AsObject[A] = summonFrom {
    case encodeA: Encoder.AsObject[A] => encodeA
    case _: Mirror.Of[A] => Encoder.AsObject.derived[A]
  }

  private inline def summmonAllDefs[T,Fields <: Tuple, Types <: Tuple]: Vector[JsonAdtFieldDef[_]] = {
    inline erasedValue[(Fields, Types)] match {
      case (_: (field *: fields), _: (tpe *: types)) =>
        new JsonAdtFieldDef[tpe](
          fieldLabel = constValue[field].toString(),
          encoder = summonEncoder[tpe]
        ) +: summmonAllDefs[T, fields, types]
      case _ => Vector.empty
    }
  }

  private inline def createJsonTaggedAdtEncoder[T](using m: Mirror.Of[T], adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtEncoder[T] = {
    lazy val allDefs: Vector[JsonAdtFieldDef[_]] = summmonAllDefs[T, m.MirroredElemLabels, m.MirroredElemTypes]

    inline m match {
      case sumOfT: Mirror.SumOf[T] => new JsonTaggedAdtEncoder[T] {
        override def encodeObject(obj: T): JsonObject = {
          val caseClassIdx = sumOfT.ordinal(obj)
          val caseClassDef = allDefs(caseClassIdx).asInstanceOf[JsonAdtFieldDef[T]]
          val caseClassTag = adtConfig.toTag.lift(obj).getOrElse(caseClassDef.fieldLabel)
          val srcJsonObj: JsonObject = caseClassDef.toJsonObject(obj)
          srcJsonObj.add(
            adtConfig.typeFieldName,
            Json.fromString( caseClassTag )
          )
        }

        override def tagFor(obj: T): String = {
          val caseClassIdx = sumOfT.ordinal(obj)
          val caseClassDef = allDefs(caseClassIdx).asInstanceOf[JsonAdtFieldDef[T]]
          adtConfig.toTag.lift(obj).getOrElse(caseClassDef.fieldLabel)
        }
      }

      case productOfT: Mirror.ProductOf[T] => new JsonTaggedAdtEncoder[T] {
        override def encodeObject(obj: T): JsonObject = {
          ???
        }

        override def tagFor(obj: T): String = {
          ???
        }
      }
    }
  }

  implicit inline given derived[T](using m: Mirror.Of[T], adtConfig: JsonTaggedAdt.Config[T] = JsonTaggedAdt.Config.empty[T]): JsonTaggedAdtEncoder[T] = createJsonTaggedAdtEncoder[T]

}

object JsonTaggedAdtEncoderWithConfig {

  implicit inline given derived[T](using m: Mirror.Of[T], adtConfig: JsonTaggedAdt.Config[T]): JsonTaggedAdtEncoderWithConfig[T] = {
    val parent = JsonTaggedAdtEncoder.derived[T]
    new JsonTaggedAdtEncoderWithConfig[T] {
      override def encodeObject(obj: T): JsonObject = {
        parent.encodeObject(obj)
      }
      override def tagFor(obj: T): String = {
        parent.tagFor(obj)
      }
    }
  }

}
