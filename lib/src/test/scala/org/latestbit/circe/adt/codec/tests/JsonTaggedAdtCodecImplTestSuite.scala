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

package org.latestbit.circe.adt.codec.tests

import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.latestbit.circe.adt.codec._
import org.scalatest.flatspec.AnyFlatSpec

sealed trait TestEvent

@JsonAdt("ev1")
case class TestEvent1(f1: String) extends TestEvent
@JsonAdt("ev2")
case class TestEvent2(f1: String) extends TestEvent
case class TestEvent3(f1: String) extends TestEvent

sealed trait DupTagTestEvent
@JsonAdt("dup-tag")
case class InvalidTestEvent1(f1: String) extends DupTagTestEvent
@JsonAdt("dup-tag")
case class InvalidTestEvent2(f1: String) extends DupTagTestEvent

sealed trait EmptyTestEvent

sealed trait InvalidMultiTagTestEvent
@JsonAdt("dup-tag")
@JsonAdt("dup-tag")
case class InvalidMultiTagTestEvent1(f1: String) extends InvalidMultiTagTestEvent


class JsonTaggedAdtCodecImplTestSuite  extends AnyFlatSpec {


	"A codec" should "be able to serialise case classes correctly" in {
		implicit val encoder : Encoder[TestEvent] = JsonTaggedAdtCodec.createEncoder[TestEvent]("type")

		val testEvent : TestEvent = TestEvent1("test")
		val json : String = testEvent.asJson.dropNullValues.noSpaces

		assert( json.contains( """"type":"ev1"""" )  )
	}

	it should "be able to deserialise case classes" in {
		implicit val decoder : Decoder[TestEvent] = JsonTaggedAdtCodec.createDecoder[TestEvent]("type")

		val testJson = """{"type" : "ev2", "f1" : "test-data"}"""

		decode[TestEvent] (
			testJson
		) match {
			case Right(model) => assert(model === TestEvent2("test-data"))
			case Left(ex) => assertThrows(ex)
		}
	}


	it should "check for unknown or absent type field in json in decoder" in {
		implicit val decoder : Decoder[TestEvent] = JsonTaggedAdtCodec.createDecoder[TestEvent]("type")

		val testJson1 = """{"type" : "ev3", "f1" : "test-data"}"""
		val testJson2 = """{"type2" : "ev2", "f1" : "test-data"}"""

		Seq(testJson1,testJson2).map { testJson =>
			decode[TestEvent] (
				testJson
			) match {
				case Right(model) => fail(model.toString)
				case Left(ex) => assert(ex.getMessage.nonEmpty)
			}
		}

	}

	it should "be able to detect duplicate tags in compile time" in {
		assertDoesNotCompile(
			"""
			  | implicit val encoder : Encoder[DupTagTestEvent] = JsonTaggedAdtCodec.createEncoder[DupTagTestEvent]("type")
			  |""".stripMargin
		)
	}

	it should "be able to detect empty sealed traits compile time" in {
		assertDoesNotCompile(
			"""
			  | implicit val encoder : Encoder[EmptyTestEvent] = JsonTaggedAdtCodec.createEncoder[EmptyTestEvent]("type")
			  |""".stripMargin
		)
	}

	it should "be able to detect multiple annotations in compile time" in {
		assertDoesNotCompile(
			"""
			  | implicit val encoder : Encoder[InvalidMultiTagTestEvent] = JsonTaggedAdtCodec.createEncoder[InvalidMultiTagTestEvent]("type")
			  |""".stripMargin
		)
	}
}
