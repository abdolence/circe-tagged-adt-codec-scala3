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

import io.circe.{Encoder, Json, JsonObject}

object JsonTaggedAdtCodec {

	private def defaultJsonTypeFieldEncoder[T](converter: JsonTaggedAdtConverter[T],
	                                           obj: T,
	                                           typeFieldName: String): JsonObject = {
		val (jsonObj, typeFieldValue) = converter.toJsonObject(obj)
		jsonObj.
			add(typeFieldName, Json.fromString(typeFieldValue))
	}

	def createEncoderDefinition[T](typeFieldName: String)
	                              (encodeTypeFieldName: (JsonTaggedAdtConverter[T], T, String) => JsonObject)
	                              (implicit converter: JsonTaggedAdtConverter[T]): Encoder.AsObject[T] = (obj: T) => {

		encodeTypeFieldName(converter, obj, typeFieldName)
	}

	def createEncoder[T](typeFieldName: String)
	                    (implicit converter: JsonTaggedAdtConverter[T]): Encoder.AsObject[T] =
		createEncoderDefinition[T](typeFieldName)(defaultJsonTypeFieldEncoder)

}