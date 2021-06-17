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

enum TestModelWithDefaults derives JsonTaggedAdt.Encoder, JsonTaggedAdt.Decoder {
  case Event1
  case Event2( f1: String )
}

enum TestModelWithConfig derives JsonTaggedAdt.EncoderWithConfig, JsonTaggedAdt.DecoderWithConfig {
  case Event1
  case Event2( f1: String )
}

given JsonTaggedAdt.Config[TestModelWithConfig] = JsonTaggedAdt.Config[TestModelWithConfig] (
  mappings = Map(
    "ev1" -> JsonTaggedAdt.tagged[TestModelWithConfig.Event1.type],
    "ev2" -> JsonTaggedAdt.tagged[TestModelWithConfig.Event2]
  )
)

enum TestModelPure derives JsonTaggedAdt.PureEncoder, JsonTaggedAdt.PureDecoder {
  case Event1
  case Event2
}

case class TestModelWithPure(pure: TestModelPure) derives Encoder.AsObject, Decoder

enum TestModelPureConfig derives JsonTaggedAdt.PureEncoderWithConfig, JsonTaggedAdt.PureDecoderWithConfig {
  case Event1
  case Event2
}

given JsonTaggedAdt.Config[TestModelPureConfig] = JsonTaggedAdt.Config[TestModelPureConfig] (
  mappings = Map(
    "ev1" -> JsonTaggedAdt.tagged[TestModelPureConfig.Event1.type],
    "ev2" -> JsonTaggedAdt.tagged[TestModelPureConfig.Event2.type]
  )
)

case class TestModelWithPureConfig(pure: TestModelPureConfig) derives Encoder.AsObject, Decoder

class JsonTaggedAdtCodecImplTestSuite extends AnyFlatSpec {

  "JsonTaggedAdtCodec" should "be able to serialise ADTs correctly with default config" in {
    val testModel1: TestModelWithDefaults = TestModelWithDefaults.Event1
    val json1: String = testModel1.asJson.dropNullValues.noSpaces

    val testModel2: TestModelWithDefaults = TestModelWithDefaults.Event2("test-val")
    val json2: String = testModel2.asJson.dropNullValues.noSpaces

    assert( json1.contains( """"type":"Event1"""" ) )
    assert( json2.contains( """"type":"Event2"""" ) )
    assert( json2.contains( """"f1":"test-val"""" ) )
  }

  it should "be able to deserialise ADTs with default config" in {
    val testJson = """{"type" : "Event2", "f1" : "test-data"}"""

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

  it should "be able to deserialise ADTs with specified config" in {
    val testJson = """{"type" : "ev1", "f1" : "test-data"}"""

    decode[TestModelWithConfig](
      testJson
    ) match {
      case Right( model: TestModelWithConfig ) =>
        assert( model === TestModelWithConfig.Event1 )
      case Left( ex ) => fail( ex )
    }
  }

  "JsonPureTaggedCodec" should "be able to serialise to Json correctly" in {
    val testModelWithPure = TestModelWithPure(pure = TestModelPure.Event1)
    val json1: String = testModelWithPure.asJson.dropNullValues.noSpaces

    val testModelWithPure2 = TestModelWithPure(pure = TestModelPure.Event2)
    val json2: String = testModelWithPure2.asJson.dropNullValues.noSpaces

    assert( json1 contains """"pure":"Event1"""")
    assert( json2 contains """"pure":"Event2"""")
  }

  it should "be able to deserialise from Json correctly" in {
    val testJson = """{ "pure" : "Event2" } """

    decode[TestModelWithPure](
      testJson
    ) match {
      case Right( model: TestModelWithPure ) =>
        assert( model.pure === TestModelPure.Event2 )
      case Left( ex ) => {
        fail( ex )
      }
    }
  }

  it should "be able to serialise to Json correctly with specified config" in {
    val testModelWithPure = TestModelWithPureConfig(pure = TestModelPureConfig.Event1)
    val json1: String = testModelWithPure.asJson.dropNullValues.noSpaces

    val testModelWithPure2 = TestModelWithPureConfig(pure = TestModelPureConfig.Event2)
    val json2: String = testModelWithPure2.asJson.dropNullValues.noSpaces

    assert( json1 contains """"pure":"ev1"""")
    assert( json2 contains """"pure":"ev2"""")
  }

  it should "be able to deserialise from Json correctly with specified config" in {
    val testJson = """{ "pure" : "ev2" } """

    decode[TestModelWithPureConfig](
      testJson
    ) match {
      case Right( model: TestModelWithPureConfig ) =>
        assert( model.pure === TestModelPureConfig.Event2 )
      case Left( ex ) => {
        fail( ex )
      }
    }
  }


}
