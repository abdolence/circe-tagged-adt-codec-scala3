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
import io.circe.Decoder.Result

sealed trait JsonTaggedAdtCodec[T] extends Decoder[T] with Encoder.AsObject[T]
sealed trait JsonTaggedAdtCodecWithConfig[T] extends JsonTaggedAdtCodec[T]

sealed trait JsonPureTaggedAdtCodec[T] extends Decoder[T] with Encoder[T]
sealed trait JsonPureTaggedAdtCodecWithConfig[T] extends JsonPureTaggedAdtCodec[T]

object JsonTaggedAdtCodec {

  inline def createJsonTaggedAdtCodec[T]( using
      m: Mirror.Of[T],
      inline adtConfig: JsonTaggedAdt.Config[T]
  ): JsonTaggedAdtCodec[T] = {
    val decoder = JsonTaggedAdtDecoder.derived[T]
    val encoder = JsonTaggedAdtEncoder.derived[T]

    new JsonTaggedAdtCodec[T] {
      override def encodeObject( obj: T ): JsonObject = encoder.encodeObject( obj )
      override def apply( cursor: HCursor ): Decoder.Result[T] = decoder.apply( cursor )
    }
  }

  implicit inline given derived[T]( using
      m: Mirror.Of[T],
      inline adtConfig: JsonTaggedAdt.Config[T] = JsonTaggedAdt.Config.default[T]
  ): JsonTaggedAdtCodec[T] = createJsonTaggedAdtCodec[T]
}

object JsonTaggedAdtCodecWithConfig {

  implicit inline given derived[T]( using
      m: Mirror.Of[T],
      inline adtConfig: JsonTaggedAdt.Config[T]
  ): JsonTaggedAdtCodecWithConfig[T] = {
    val parent = JsonTaggedAdtCodec.derived[T]
    new JsonTaggedAdtCodecWithConfig[T] {
      override def encodeObject( a: T ): JsonObject = parent.encodeObject( a )
      override def apply( c: HCursor ): Decoder.Result[T] = parent.apply( c )
    }
  }

}

object JsonPureTaggedAdtCodec {

  implicit inline given derived[T]( using
      m: Mirror.Of[T],
      inline adtConfig: JsonTaggedAdt.PureConfig[T] = JsonTaggedAdt.PureConfig.default[T]
  ): JsonPureTaggedAdtCodec[T] = {
    val decoder = JsonPureTaggedAdtDecoder.derived[T]
    val encoder = JsonPureTaggedAdtEncoder.derived[T]
    new JsonPureTaggedAdtCodec[T] {
      override def apply( a: T ): Json = encoder.apply( a )
      override def apply( c: HCursor ): Result[T] = decoder.apply( c )
    }
  }
}

object JsonPureTaggedAdtCodecWithConfig {

  implicit inline given derived[T]( using
      m: Mirror.Of[T],
      inline adtConfig: JsonTaggedAdt.PureConfig[T]
  ): JsonPureTaggedAdtCodecWithConfig[T] = {
    val parent = JsonPureTaggedAdtCodec.derived[T]
    new JsonPureTaggedAdtCodecWithConfig[T] {
      override def apply( c: HCursor ): Result[T] = parent.apply( c )
      override def apply( obj: T ): Json = parent.apply( obj )
    }
  }

}
