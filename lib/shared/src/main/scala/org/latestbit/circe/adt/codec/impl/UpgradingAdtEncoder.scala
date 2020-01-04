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
 *
 *
 */

package org.latestbit.circe.adt.codec.impl

import io.circe.{ Encoder, JsonObject }
import org.latestbit.circe.adt.codec
import org.latestbit.circe.adt.codec.JsonTaggedAdtEncoder

class UpgradingAdtEncoder[T]()(
    implicit encoder: Encoder.AsObject[T],
    upgradedEncoder: JsonTaggedAdtEncoder[T]
) extends JsonTaggedAdtEncoder[T] {

  /**
   * Convert a trait to circe JsonObject
   *
   * @param obj an instance of T
   * @return Encoded JSON object and its JSON type field value
   */
  override def toJsonObject( obj: T ): ( JsonObject, String ) = upgradedEncoder.toJsonObject( obj )
}
