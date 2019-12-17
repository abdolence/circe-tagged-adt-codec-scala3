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

import io.circe.Encoder
import io.circe.syntax._
import org.latestbit.circe.adt.codec._
import org.scalatest.flatspec.AnyFlatSpec

sealed trait TestEvent

@JsonAdt("ev1")
case class TestEvent1(f1: String) extends TestEvent

@JsonAdt("ev2")
case class TestEvent2(f1: String) extends TestEvent

class JsonTaggedAdtCodecImplTestSuite  extends AnyFlatSpec {


	"A sealed trait" should "be able to be serialised correctly" in {
		implicit val encoder : Encoder[TestEvent] = JsonTaggedAdtCodec.createEncoder[TestEvent]("type")

		val testEvent : TestEvent = TestEvent1("test")
		val json : String = testEvent.asJson.dropNullValues.noSpaces

		assert( json.contains( """"type":"ev1"""" )  )
	}

}
