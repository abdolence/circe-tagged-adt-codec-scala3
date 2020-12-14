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
import shapeless.LabelledGeneric

object TestModels {
  sealed trait TestEvent

  @JsonAdt( "ev1" )
  case class TestEvent1( f1: String ) extends TestEvent

  @JsonAdt( "ev2" )
  case class TestEvent2( f1: String ) extends TestEvent

  case class TestEvent3( f1: String ) extends TestEvent

  sealed trait DupTagTestEvent

  @JsonAdt( "dup-tag" )
  case class InvalidTestEvent1( f1: String ) extends DupTagTestEvent

  @JsonAdt( "dup-tag" )
  case class InvalidTestEvent2( f1: String ) extends DupTagTestEvent

  sealed trait EmptyTestEvent

  sealed trait InvalidMultiTagTestEvent

  @JsonAdt( "dup-tag" )
  @JsonAdt( "dup-tag" )
  case class InvalidMultiTagTestEvent1( f1: String ) extends InvalidMultiTagTestEvent

  sealed trait NotAnnotatedTestEvent
  case class NotAnnotatedTestEvent1( f1: String ) extends NotAnnotatedTestEvent
  case class NotAnnotatedTestEvent2( f1: String ) extends NotAnnotatedTestEvent

  sealed trait InnerSubclassesTestEvent

  object InnerSubclassesTestEvent {

    case class InnerCaseClassTestEvent( test: String ) extends InnerSubclassesTestEvent
    case object InnerObjTestEvent extends InnerSubclassesTestEvent
  }

  sealed trait EmptyCaseClassParentTestEvent
  case class EmptyCaseClassTestEvent() extends EmptyCaseClassParentTestEvent

  sealed trait BaseTestEvent

  @JsonAdt( "test-case-class" )
  case class BaseTestEventCaseClass() extends BaseTestEvent

  @JsonAdt( "test-case-object" )
  case object BaseTestEventCaseObject extends BaseTestEvent

  @JsonAdtPassThrough
  sealed trait ChildTestEvent extends BaseTestEvent

  @JsonAdt( "child-case-class" )
  case class ChildTestEventCaseClass() extends ChildTestEvent

  @JsonAdt( "child-case-object" )
  case object ChildTestEventCaseObject extends ChildTestEvent

  @JsonAdtPassThrough
  sealed trait SecondChildTestEvent extends BaseTestEvent

  @JsonAdt( "second-child-case-class" )
  case class SecondChildTestEventCaseClass() extends ChildTestEvent

  @JsonAdt( "second-child-case-object" )
  case object SecondChildTestEventCaseObject extends ChildTestEvent

  sealed trait NoPassThroughChildTestEvent extends BaseTestEvent

  @JsonAdt( "second-child-case-class" )
  case class NoPassThroughTestEventCaseClass() extends NoPassThroughChildTestEvent

  @JsonAdt( "second-child-case-object" )
  case object NoPassThroughTestEventCaseObject extends NoPassThroughChildTestEvent

  @JsonAdt( "isolated-child-case-class" )
  sealed trait IsolatedChildTestEvent extends BaseTestEvent

  @JsonAdt( "child-case-class" )
  case class IsolatedChildTestEventCaseClass() extends IsolatedChildTestEvent

  @JsonAdt( "child-case-object" )
  case object IsolatedChildTestEventCaseObject extends IsolatedChildTestEvent

  @JsonAdtPassThrough()
  @JsonAdt( "dup-tag2" )
  sealed trait InvalidMixedTagTestEvent
  case class ChildInvalidMixedInheritanceTagTestEvent() extends InvalidMixedTagTestEvent

  @JsonAdtPassThrough
  case class InvalidPassThroughCaseClass()

  sealed trait PureEnum

  @JsonAdt( "en1" )
  case object Enum1 extends PureEnum
  case object Enum2 extends PureEnum

  case class Enum3() extends PureEnum

  object PureEnum {

    @JsonAdt( "en4" )
    case object Enum4 extends PureEnum
  }

  case class WrapperPureEnum( test: PureEnum )

}

class JsonTaggedAdtCodecImplTestSuite extends AnyFlatSpec {
  import TestModels._

  "A codec" should "be able to serialise case classes correctly" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[TestEvent] =
      JsonTaggedAdtCodec.createEncoder[TestEvent]( "type" )

    val testEvent: TestEvent = TestEvent1( "test" )
    val json: String = testEvent.asJson.dropNullValues.noSpaces

    assert( json.contains( """"type":"ev1"""" ) )
  }

  it should "be able to deserialise case classes" in {
    import io.circe.generic.auto._

    implicit val decoder: Decoder[TestEvent] =
      JsonTaggedAdtCodec.createDecoder[TestEvent]( "type" )

    val testJson = """{"type" : "ev2", "f1" : "test-data"}"""

    decode[TestEvent](
      testJson
    ) match {
      case Right( model: TestEvent ) =>
        assert( model === TestEvent2( "test-data" ) )
      case Left( ex ) => fail( ex )
    }
  }

  it should "check for unknown or absent type field in json in decoder" in {
    import io.circe.generic.auto._

    implicit val decoder: Decoder[TestEvent] =
      JsonTaggedAdtCodec.createDecoder[TestEvent]( "type" )

    val testJson1 = """{"type" : "ev3", "f1" : "test-data"}"""
    val testJson2 = """{"type2" : "ev2", "f1" : "test-data"}"""

    Seq( testJson1, testJson2 ).map { testJson =>
      decode[TestEvent](
        testJson
      ) match {
        case Right( model ) => fail( model.toString )
        case Left( ex )     => assert( ex.getMessage.nonEmpty )
      }
    }

  }

  it should "able to serialise and deserialise unannotated case classes" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[NotAnnotatedTestEvent] =
      JsonTaggedAdtCodec.createEncoder[NotAnnotatedTestEvent]( "type" )
    implicit val decoder: Decoder[NotAnnotatedTestEvent] =
      JsonTaggedAdtCodec.createDecoder[NotAnnotatedTestEvent]( "type" )

    val testEvent: NotAnnotatedTestEvent = NotAnnotatedTestEvent1( "test" )
    val testJson: String = testEvent.asJson.dropNullValues.noSpaces

    decode[NotAnnotatedTestEvent](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }

  }

  it should "be able to encode/decode inner case classes and objects" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[InnerSubclassesTestEvent] =
      JsonTaggedAdtCodec.createEncoder[InnerSubclassesTestEvent]( "type" )
    implicit val decoder: Decoder[InnerSubclassesTestEvent] =
      JsonTaggedAdtCodec.createDecoder[InnerSubclassesTestEvent]( "type" )

    val testEvent: InnerSubclassesTestEvent =
      InnerSubclassesTestEvent.InnerObjTestEvent
    val testJson: String = testEvent.asJson.dropNullValues.noSpaces

    decode[InnerSubclassesTestEvent](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }
  }

  it should "be able to encode/decode empty case classes" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[EmptyCaseClassParentTestEvent] =
      JsonTaggedAdtCodec.createEncoder[EmptyCaseClassParentTestEvent]( "type" )
    implicit val decoder: Decoder[EmptyCaseClassParentTestEvent] =
      JsonTaggedAdtCodec.createDecoder[EmptyCaseClassParentTestEvent]( "type" )

    val testEvent: EmptyCaseClassParentTestEvent =
      EmptyCaseClassTestEvent()
    val testJson: String = testEvent.asJson.dropNullValues.noSpaces

    decode[EmptyCaseClassParentTestEvent](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }
  }

  it should "able to serialise and deserialise correctly when specified on a single case class" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[NotAnnotatedTestEvent1] =
      JsonTaggedAdtCodec.createEncoder[NotAnnotatedTestEvent1]( "type" )
    implicit val decoder: Decoder[NotAnnotatedTestEvent1] =
      JsonTaggedAdtCodec.createDecoder[NotAnnotatedTestEvent1]( "type" )

    val testEvent: NotAnnotatedTestEvent1 = NotAnnotatedTestEvent1( "test" )
    val testJson: String = testEvent.asJson.dropNullValues.noSpaces

    decode[NotAnnotatedTestEvent1](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }

  }

  it should "able to serialise and deserialise correctly when specified on a single case class for semiauto mode" in {
    import io.circe.generic.semiauto._

    implicit val encoderEv1: Encoder.AsObject[NotAnnotatedTestEvent1] =
      JsonTaggedAdtCodec.createEncoder[NotAnnotatedTestEvent1]( "type" )
    implicit val decoderEv1: Decoder[NotAnnotatedTestEvent1] =
      JsonTaggedAdtCodec.createDecoder[NotAnnotatedTestEvent1]( "type" )

    implicit val implicitEncoderEv2: Encoder.AsObject[NotAnnotatedTestEvent2] =
      deriveEncoder[NotAnnotatedTestEvent2]
    implicit val implicitDecoderEv2: Decoder[NotAnnotatedTestEvent2] =
      deriveDecoder[NotAnnotatedTestEvent2]

    implicit val encoderTrait: Encoder.AsObject[NotAnnotatedTestEvent] =
      JsonTaggedAdtCodec.createEncoder[NotAnnotatedTestEvent]( "type" )
    implicit val decoderTrait: Decoder[NotAnnotatedTestEvent] =
      JsonTaggedAdtCodec.createDecoder[NotAnnotatedTestEvent]( "type" )

    val testEvent: NotAnnotatedTestEvent1 = NotAnnotatedTestEvent1( "test" )
    val testJson: String = testEvent.asJson.dropNullValues.noSpaces
    val testTrait: NotAnnotatedTestEvent = testEvent
    val testTraitJson: String = testTrait.asJson.dropNullValues.noSpaces

    assert( testJson.contains( """"type"""" ) )
    assert( testJson === testTraitJson )

    decode[NotAnnotatedTestEvent1](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }

    decode[NotAnnotatedTestEvent](
      testJson
    ) match {
      case Right( model ) => assert( model === testEvent )
      case Left( ex )     => fail( ex )
    }

  }

  it should "able to serialise and deserialise correctly traits inheritance" in {
    import io.circe.generic.auto._

    implicit val childEncoder: Encoder[ChildTestEvent] =
      JsonTaggedAdtCodec.createEncoder[ChildTestEvent]( "type" )
    implicit val childDecoder: Decoder[ChildTestEvent] =
      JsonTaggedAdtCodec.createDecoder[ChildTestEvent]( "type" )

    implicit val isolatedEncoder: Encoder[IsolatedChildTestEvent] =
      JsonTaggedAdtCodec.createEncoder[IsolatedChildTestEvent]( "type" )
    implicit val isolatedDecoder: Decoder[IsolatedChildTestEvent] =
      JsonTaggedAdtCodec.createDecoder[IsolatedChildTestEvent]( "type" )

    implicit val baseEncoder: Encoder[BaseTestEvent] =
      JsonTaggedAdtCodec.createEncoder[BaseTestEvent]( "type" )
    implicit val baseDecoder: Decoder[BaseTestEvent] =
      JsonTaggedAdtCodec.createDecoder[BaseTestEvent]( "type" )

    val childTestEvent: ChildTestEvent = ChildTestEventCaseClass()
    val baseTestEvent: BaseTestEvent = BaseTestEventCaseObject
    val childTestJson: String = childTestEvent.asJson.dropNullValues.noSpaces
    val baseTestJson: String = baseTestEvent.asJson.dropNullValues.noSpaces

    val isolatedTestEvent: IsolatedChildTestEvent = IsolatedChildTestEventCaseClass()
    val isolatedTestJson: String = isolatedTestEvent.asJson.dropNullValues.noSpaces

    decode[ChildTestEvent](
      childTestJson
    ) match {
      case Right( model ) => {
        assert( model === childTestEvent )
      }
      case Left( ex ) => fail( ex )
    }

    decode[BaseTestEvent](
      childTestJson
    ) match {
      case Right( model ) => {
        assert( model === childTestEvent )
      }
      case Left( ex ) => fail( ex )
    }

    decode[BaseTestEvent](
      baseTestJson
    ) match {
      case Right( model ) => {
        assert( model === baseTestEvent )
      }
      case Left( ex ) => fail( ex )
    }

    decode[ChildTestEvent](
      baseTestJson
    ) match {
      case Right( model ) => {
        fail( model.toString )
      }
      case Left( ex ) => assert( ex.isInstanceOf[DecodingFailure] )
    }

    decode[IsolatedChildTestEvent](
      isolatedTestJson
    ) match {
      case Right( model ) => {
        assert( model === isolatedTestEvent )
      }
      case Left( ex ) => fail( ex )
    }

  }

  it should "be able to detect duplicate tags at compile time" in {
    assertDoesNotCompile(
      """
      | import io.circe.generic.auto._
      |
      | implicit val encoder : Encoder[DupTagTestEvent] = JsonTaggedAdtCodec.createEncoder[DupTagTestEvent]("type")
      |""".stripMargin
    )
  }

  it should "be able to detect empty sealed traits at compile time" in {
    assertDoesNotCompile(
      """
	  | import io.circe.generic.auto._
	  | implicit val encoder : Encoder[EmptyTestEvent] = JsonTaggedAdtCodec.createEncoder[EmptyTestEvent]("type")
	  |""".stripMargin
    )
  }

  it should "be able to detect multiple JSonAdt annotations at compile time" in {
    assertDoesNotCompile(
      """
      | import io.circe.generic.auto._
      |
      | implicit val encoder : Encoder[InvalidMultiTagTestEvent] = JsonTaggedAdtCodec.createEncoder[InvalidMultiTagTestEvent]("type")
	  |""".stripMargin
    )
  }

  it should "be able to detect mixed JSonAdt/JsonAdtPassThrough annotations at compile time" in {

    assertDoesNotCompile(
      """
      | import io.circe.generic.auto._
      |
      | implicit val encoder : Encoder[InvalidMixedTagTestEvent] = JsonTaggedAdtCodec.createEncoder[InvalidMixedTagTestEvent]("type")
	  |""".stripMargin
    )
  }

  it should "be able to detect incorrect JsonAdtPassThrough on anything but trait at compile time" in {

    assertDoesNotCompile(
      """
      | import io.circe.generic.auto._
      |
      |implicit val encoder : Encoder[InvalidPassThroughCaseClass] = JsonTaggedAdtCodec.createEncoder[InvalidPassThroughCaseClass]("type")
	  |""".stripMargin
    )
  }

  it should "be able to configured with custom implementation of encoder" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[TestEvent] =
      JsonTaggedAdtCodec.createEncoderDefinition[TestEvent] { case ( converter, obj ) =>
        // converting our case classes accordingly to obj instance type
        // and receiving JSON type field value from annotation
        val ( jsonObj, typeFieldValue ) = converter.toJsonObject( obj )

        // Our custom JSON structure
        JsonObject(
          "type" -> Json.fromString( typeFieldValue ),
          "body" -> Json.fromJsonObject( jsonObj )
        )
      }

    val testEvent: TestEvent = TestEvent1( "test" )
    val json: String = testEvent.asJson.dropNullValues.noSpaces

    assert( json.contains( """"type":"ev1"""" ) )
    assert( json.contains( """"body":{""" ) )
  }

  it should "be able to configured with custom implementation of decoder" in {
    import io.circe.generic.auto._

    implicit val decoder: Decoder[TestEvent] =
      JsonTaggedAdtCodec.createDecoderDefinition[TestEvent] { case ( converter, cursor ) =>
        cursor.get[Option[String]]( "type" ).flatMap {
          case Some( typeFieldValue ) =>
            // Decode a case class from body accordingly to typeFieldValue
            converter.fromJsonObject(
              jsonTypeFieldValue = typeFieldValue,
              cursor = cursor.downField( "body" )
            )
          case _ =>
            Decoder.failedWithMessage( s"'type' isn't specified in json." )(
              cursor
            )
        }
      }

    val testJson = """{"type" : "ev2", "body" : { "f1" : "test-data" } }"""

    decode[TestEvent](
      testJson
    ) match {
      case Right( model: TestEvent ) =>
        assert( model === TestEvent2( "test-data" ) )
      case Left( ex ) => fail( ex )
    }
  }

  it should "be able to decode trait as pure enum objects" in {
    import io.circe.generic.auto._

    implicit val decoder: Decoder[PureEnum] =
      JsonTaggedAdtCodec.createPureEnumDecoder[PureEnum]()

    val testJson = """{"test" : "en1" }"""

    decode[WrapperPureEnum](
      testJson
    ) match {
      case Right( model: WrapperPureEnum ) =>
        assert( model.test === Enum1 )
      case Left( ex ) => fail( ex )
    }
  }

  it should "be able to encode trait as pure enum objects" in {
    import io.circe.generic.auto._

    implicit val encoder: Encoder[PureEnum] =
      JsonTaggedAdtCodec.createPureEnumEncoder[PureEnum]()

    val expectedJson = """{"test":"en4"}"""

    val testObject: WrapperPureEnum = WrapperPureEnum( test = PureEnum.Enum4 )
    val json: String = testObject.asJson.dropNullValues.noSpaces

    assert( json === expectedJson )

  }

}
