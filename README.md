## Circe encoder/decoder implementation for ADT to JSON with a configurable type field.
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.latestbit/circe-tagged-adt-codec_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.latestbit/circe-tagged-adt-codec_2.13/)
![](https://github.com/abdolence/circe-tagged-adt-codec/workflows/Scala%20CI/badge.svg)

This library provides an efficient, type safe and macro based 
ADT to JSON encoder/decoder for circe with configurable JSON type field mappings.

When you have ADTs (as trait and case classes) defined like this
```scala
sealed trait TestEvent

case class MyEvent1(anyYourField : String /*, ...*/) extends TestEvent
case class MyEvent2(anyOtherField : Long /*, ...*/) extends TestEvent
// ...
```

and you would like to encode them to JSON like this:

```json
{
  "type" : "my-event-1",
  "anyYourField" : "my-data", 
  "..." : "..."
}
```

The main objectives here are:
- Avoid JSON type field in Scala case class definitions.
- Configurable JSON type field values and their mapping to case classes. They don't have to be Scala class names.
- Avoid writing circe Encoder/Decoder manually.
- Check at the compile time JSON type field mappings and Scala case classes.

### Scala support
- Scala v2.12+
- Scala.js v0.6.28+

### Getting Started
Add the following to your `build.sbt`:

```scala
libraryDependencies += "org.latestbit" %% "circe-tagged-adt-codec" % "0.5.1"
```

or if you need Scala.js support:

```scala
libraryDependencies += "org.latestbit" %%% "circe-tagged-adt-codec" % "0.5.1"
```

### Usage

```scala
import io.circe._
import io.circe.parser._
import io.circe.syntax._

// This example uses auto coding for case classes. 
// You decide here if you need auto/semi/custom coders for your case classes.
import io.circe.generic.auto._ 

// One import for this ADT/JSON codec
import org.latestbit.circe.adt.codec._


sealed trait TestEvent

//@JsonAdt annotation is required only if you'd like to specify JSON type field value yourself. 
// Otherwise it would be the class name  

@JsonAdt("my-event-1") 
case class MyEvent1(anyYourField : String /*, ...*/) extends TestEvent
@JsonAdt("my-event-2")
case class MyEvent2(anyOtherField : Long /*, ...*/) extends TestEvent

// Encoding

implicit val encoder : Encoder[TestEvent] = JsonTaggedAdtCodec.createEncoder[TestEvent]("type")

val testEvent : TestEvent = TestEvent1("test")
val testJsonString : String = testEvent.asJson.dropNullValues.noSpaces

// Decoding
implicit val decoder : Decoder[TestEvent] = JsonTaggedAdtCodec.createDecoder[TestEvent]("type")

decode[TestEvent] (testJsonString) match {
   case Right(model : TestEvent) => // ...
}
``` 
### Configure and customise base ADT Encoder/Decoder implementation
In case you need a slightly different style of coding of your ADT to JSON, there is an API to change it.

Let's assume that you'd like to produce a bit different JSON like this:

```json
{
  "type" : "my-event-1",
  "body" : {
    "anyYourField" : "my-data", 
     "..." : "..."
  }  
}
```

Then you should specify it with your own implementation:

```scala
implicit val encoder: Encoder[TestEvent] =
    JsonTaggedAdtCodec.
        createEncoderDefinition[TestEvent] { case (converter, obj) =>

            // converting our case classes accordingly to obj instance type
            // and receiving JSON type field value from annotation
            val (jsonObj, typeFieldValue) = converter.toJsonObject(obj)

            // Our custom JSON structure
            JsonObject(
                "type" -> Json.fromString(typeFieldValue),
                "body" -> Json.fromJsonObject(jsonObj)
            )
        }

implicit val decoder: Decoder[TestEvent] =
    JsonTaggedAdtCodec.
        createDecoderDefinition[TestEvent] { case (converter, cursor) =>
            
            // Reading JSON type field value
            cursor.get[Option[String]]("type").flatMap {
                case Some(typeFieldValue) =>
                    // Decode a case class from body accordingly to typeFieldValue
                    converter.fromJsonObject(
                        jsonTypeFieldValue = typeFieldValue,
                        cursor = cursor.downField("body")
                    )
                case _ =>
                    Decoder.failedWithMessage(s"'type' isn't specified in json.")(cursor)
            }
        }
```

### Complex ADT definitions and trait inheritance

All the following examples are support for this codec:
```scala

sealed trait MyTrait
case class MyCaseClass() extends MyTrait
// case objects
case object MyCaseObject extends MyTrait 

// trait inheritance with passing through tags - 
// so, direct children of MyTrait and 
// direct children of MyChildTrait now
// share the same tags namespace 
@JsonAdtPassThrough
sealed trait MyChildTrait extends MyTrait 
case class MyChildCaseClass() extends MyChildTrait
case class MyOtherChildCaseClass() extends MyChildTrait

// Now this is has its own decoder/encoder 
// and the children of MyIsolatedChildTrait have their own tags
sealed trait MyIsolatedChildTrait extends MyTrait 
case class MyIsolatedCaseClass() extends MyIsolatedChildTrait
case class MyIsolatedOtherChildCaseClass() extends MyIsolatedChildTrait

// The same like previous, except here we now define our own tag on a child trait 
// (instead of default behaviour where a tag would be a trait name) 
@JsonAdt("isolated-trait-2")
sealed trait MySecondIsolatedChildTrait extends MyTrait 



```

### Licence
Apache Software License (ASL)

### Author
Abdulla Abdurakhmanov
