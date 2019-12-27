/*
 * Copyright 2019 Abdulla Abdurakhmanov (abdulla@latestbit.com)
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

import io.circe.{ Decoder, Encoder, HCursor, Json, JsonObject }

/**
 * Object provides access to the factory methods for circe Encoder/Decoder
 */
object JsonTaggedAdtCodec {

  /**
   * Default implementation of encoding JSON with a type field
   *
   * @param typeFieldName a JSON field name to encode type name
   * @param converter     converter for trait and its case classes
   * @param obj           an object to encode
   * @tparam T A trait type
   * @return Encoded json object with a type field
   */
  protected def defaultJsonTypeFieldEncoder[T](
      typeFieldName: String
  )( converter: JsonTaggedAdtConverter[T], obj: T ): JsonObject = {
    val ( jsonObj, typeFieldValue ) = converter.toJsonObject( obj )
    jsonObj.add( typeFieldName, Json.fromString( typeFieldValue ) )
  }

  /**
   * Default implementation of decoding JSON with a type field
   *
   * @param typeFieldName a JSON field name to decode type name
   * @param converter     converter for trait and its case classes
   * @param cursor        JSON context cursor
   * @tparam T A trait type
   * @return Decode result
   */
  protected def defaultJsonTypeFieldDecoder[T](
      typeFieldName: String
  )(
      converter: JsonTaggedAdtConverter[T],
      cursor: HCursor
  ): Decoder.Result[T] = {

    cursor.get[Option[String]]( typeFieldName ).flatMap {
      case Some( typeFieldValue ) =>
        converter.fromJsonObject(
          jsonTypeFieldValue = typeFieldValue,
          cursor = cursor
        )
      case _ =>
        Decoder.failedWithMessage[T](
          s"'$typeFieldName' isn't specified in json."
        )( cursor )
    }
  }

  /**
   * Create ADT / JSON type field base encoder with a specified type field encoding implementation
   *
   * @param typeFieldEncoder JSON type field encoding implementation
   * @param converter        implicitly created JSON converter for trait and its case classes
   * @tparam T A trait type
   * @return circe Encoder of T
   */
  def createEncoderDefinition[T](
      typeFieldEncoder: ( JsonTaggedAdtConverter[T], T ) => JsonObject
  )( implicit converter: JsonTaggedAdtConverter[T] ): Encoder.AsObject[T] =
    (obj: T) => {
      typeFieldEncoder( converter, obj )
    }

  /**
   * Create ADT / JSON type field base encoder
   *
   * @param typeFieldName a JSON field name to encode type name
   * @param converter     implicitly created JSON converter for trait and its case classes
   * @tparam T A trait type
   * @return circe Encoder of T
   */
  def createEncoder[T](
      typeFieldName: String
  )( implicit converter: JsonTaggedAdtConverter[T] ): Encoder.AsObject[T] =
    createEncoderDefinition[T]( defaultJsonTypeFieldEncoder( typeFieldName ) )

  /**
	* Create ADT / JSON type field base decoder with a specified type field decoding implementation
	*
	* @param typeFieldDecoder JSON type field decoding implementation
	* @param converter        implicitly created JSON converter for trait and its case classes
	* @tparam T A trait type
	* @return circe Decoder of T
	*/
  def createDecoderDefinition[T](
      typeFieldDecoder: (
          JsonTaggedAdtConverter[T],
          HCursor
      ) => Decoder.Result[T]
  )( implicit converter: JsonTaggedAdtConverter[T] ): Decoder[T] =
    (cursor: HCursor) => {
      typeFieldDecoder( converter, cursor )
    }

  /**
	* Create ADT / JSON type field base decoder
	*
	* @param typeFieldName a JSON field name to decode type name
	* @param converter     implicitly created JSON converter for trait and its case classes
	* @tparam T A trait type
	* @return circe Decoder of T
	*/
  def createDecoder[T](
      typeFieldName: String
  )( implicit converter: JsonTaggedAdtConverter[T] ): Decoder[T] =
    createDecoderDefinition[T]( defaultJsonTypeFieldDecoder( typeFieldName ) )

}
