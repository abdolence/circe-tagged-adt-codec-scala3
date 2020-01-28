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

package org.latestbit.circe.adt

import org.latestbit.circe.adt.codec.macros.impl._

/**
 * The main package to import to access to ADT / JSON codec implementation with implicit macros
 */
package object codec {
  import scala.language.experimental.macros

  /**
   * An implicit JSON macro encoder to generate code to convert ADT (trait and case classes) to JSON objects
   * @tparam T A trait or case class type
   * @return JSON ADT encoder
   */
  implicit def createAdtEncoder[T]: JsonTaggedAdtEncoder[T] =
    macro JsonTaggedAdtEncoderMacroImpl.createAdtEncoderMacro[T]

  /**
   * An implicit JSON macro decoder to generate code to decode JSON to ADT
   * @tparam T A trait or case class type
   * @return JSON ADT decoder
   */
  implicit def createAdtDecoder[T]: JsonTaggedAdtDecoder[T] =
    macro JsonTaggedAdtDecoderMacroImpl.createAdtDecoderMacro[T]

}
