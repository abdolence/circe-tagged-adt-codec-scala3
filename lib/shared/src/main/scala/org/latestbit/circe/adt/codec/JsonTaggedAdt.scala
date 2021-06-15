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

import org.latestbit.circe.adt.codec.impl.*

import scala.reflect.*
import scala.deriving.*
import io.circe.*

object JsonTaggedAdt {

  type Encoder[T] = impl.JsonTaggedAdtEncoder[T]
  type EncoderWithConfig[T] = impl.JsonTaggedAdtEncoderWithConfig[T]

  type Decoder[T] = impl.JsonTaggedAdtDecoder[T]
  type DecoderWithConfig[T] = impl.JsonTaggedAdtDecoderWithConfig[T]

  final val DefaultTypeFieldName: String = "type"

  class Config[E]( val typeFieldName: String = DefaultTypeFieldName,
                   val mappings: Map[String, TagClass[E]] = Map(),
                   val encoderDefinition: EncoderDefinition = EncoderDefinition.Default
                 )

  object Config {
    inline final def empty[E] = Config[E]()
  }

  class TagClass[+E](using clsTag: ClassTag[E]) {
    lazy val tagClassName = clsTag.runtimeClass.getName
  }

  /**
   * Defines encoding implementation using encoded json object, a tag value, and field name
   */
  trait EncoderDefinition {
    def encodeTaggedJsonObject(typeFieldName: String,
                               tagValue: String,
                               tagJsonObject: JsonObject): JsonObject
  }

  object EncoderDefinition {
    /**
     * Default implementation of encoding JSON with a type field for:
     * ```
     * {
     *  'type': 'tagValue',
     *   ...
     * }
     * ```
     */
    object Default extends EncoderDefinition {
      override def encodeTaggedJsonObject(typeFieldName: String,
                                 tagValue: String,
                                 tagJsonObject: JsonObject): JsonObject = {
        tagJsonObject.add(
          typeFieldName,
          Json.fromString( tagValue )
        )
      }
    }
  }

}
