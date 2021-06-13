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

package org.latestbit.circe.adt.codec.tests

import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*
import org.latestbit.circe.adt.codec.*
import org.scalatest.flatspec.AnyFlatSpec

import io.circe.generic.semiauto._

enum TestModelWithDefaults derives JsonTaggedAdtEncoder {
  case Event1
  case Event2( f1: String )
}

given adtConfig: JsonTaggedAdtEncoder.Config[TestModelWithConfig] = JsonTaggedAdtEncoder.Config[TestModelWithConfig] (
  toTag = {
    case TestModelWithConfig.Event1 => "ev1"
  }
)

enum TestModelWithConfig derives JsonTaggedAdtEncoderWithConfig {
  case Event1
  case Event2( f1: String )
}

class JsonTaggedAdtCodecImplTestSuite extends AnyFlatSpec {

  "JsonTaggedAdtCodec" should "be able to serialise ADTs correctly with default config" in {
    val testModel: TestModelWithDefaults = TestModelWithDefaults.Event1
    val json: String = testModel.asJson.dropNullValues.noSpaces

    assert( json.contains( """"type":"Event1"""" ) )
  }

  it should "be able to deserialise ADTs with default config" in {
    implicit val decoder: Decoder[TestModelWithDefaults] = ???

    val testJson = """{"type" : "ev2", "f1" : "test-data"}"""

    decode[TestModelWithDefaults](
      testJson
    ) match {
      case Right( model: TestModelWithDefaults ) =>
        assert( model === TestModelWithDefaults.Event2( "test-data" ) )
      case Left( ex ) => fail( ex )
    }
  }

  it should "be able to serialise ADTs correctly with specified config" in {
    val testModel: TestModelWithConfig = TestModelWithConfig.Event1
    val json: String = testModel.asJson.dropNullValues.noSpaces

    assert( json.contains( """"type":"ev1"""" ) )
  }


}
