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

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.latestbit.circe.adt.codec._
import org.scalatest.flatspec.AnyFlatSpec

object TestModels {

  enum TestEvent {
    case Event1
    case Event2( f1: String )
  }

}

class JsonTaggedAdtCodecImplTestSuite extends AnyFlatSpec {
  import TestModels._

  "A codec" should "be able to serialise case classes correctly" in {

    implicit val encoder: Encoder[TestEvent] = ???

    val testEvent: TestEvent = TestEvent.Event1
    val json: String = testEvent.asJson.dropNullValues.noSpaces

    assert( json.contains( """"type":"ev1"""" ) )
  }

  it should "be able to deserialise case classes" in {
    implicit val decoder: Decoder[TestEvent] = ???

    val testJson = """{"type" : "ev2", "f1" : "test-data"}"""

    decode[TestEvent](
      testJson
    ) match {
      case Right( model: TestEvent ) =>
        assert( model === TestEvent.Event2( "test-data" ) )
      case Left( ex ) => fail( ex )
    }
  }

}
