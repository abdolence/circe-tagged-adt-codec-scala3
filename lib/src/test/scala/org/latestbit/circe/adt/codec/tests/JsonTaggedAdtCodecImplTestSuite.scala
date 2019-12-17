package org.latestbit.circe.adt.codec

import io.circe.Encoder
import org.scalatest.flatspec.AnyFlatSpec
import io.circe._
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser._


sealed trait TestEvent
case class TestEvent1(f1: String) extends TestEvent
case class TestEvent2(f1: String) extends TestEvent

class JsonTaggedAdtCodecImplTestSuite  extends AnyFlatSpec {


	"A sealed trait" should "be able to be serialised correctly" in {
		import org.latestbit.circe.adt.codec._

		implicit val encoder : Encoder[TestEvent] = JsonTaggedAdtCodec.createEncoder[TestEvent]("type")
		val json = TestEvent1("test").asJson.dropNullValues.noSpaces
	}

}
