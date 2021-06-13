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

object JsonTaggedAdt {

  final val DefaultTypeFieldName: String = "type"

  case class Config[E]( typeFieldName: String = DefaultTypeFieldName,
                        toTag: PartialFunction[E,String] = PartialFunction.empty,
                        fromTag: PartialFunction[String,E] = PartialFunction.empty)

  object Config {
    inline final def empty[E] = Config[E]()
  }

  type Encoder[T] = JsonTaggedAdtEncoder[T]
  type EncoderWithConfig[T] = JsonTaggedAdtEncoderWithConfig[T]

  type Decoder[T] = JsonTaggedAdtDecoder[T]
  type DecoderWithConfig[T] = JsonTaggedAdtDecoderWithConfig[T]

  type Codec[T] = Encoder[T] with Decoder[T]
}
