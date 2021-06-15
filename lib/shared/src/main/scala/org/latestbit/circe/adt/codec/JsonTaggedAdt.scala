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

object JsonTaggedAdt {

  type Encoder[T] = impl.JsonTaggedAdtEncoder[T]
  type EncoderWithConfig[T] = impl.JsonTaggedAdtEncoderWithConfig[T]

  type Decoder[T] = impl.JsonTaggedAdtDecoder[T]
  type DecoderWithConfig[T] = impl.JsonTaggedAdtDecoderWithConfig[T]

  final val DefaultTypeFieldName: String = "type"

  case class Config[E]( typeFieldName: String = DefaultTypeFieldName,
                        mappings: Map[String, TagClass[E]] = Map(),
                        toTag: PartialFunction[E,String] = PartialFunction.empty,
                        fromTag: PartialFunction[String,E] = PartialFunction.empty)

  object Config {
    inline final def empty[E] = Config[E]()
  }

  class TagClass[+E](using clsTag: ClassTag[E]) {
    lazy val tagClassName = clsTag.runtimeClass.getName
  }

}
