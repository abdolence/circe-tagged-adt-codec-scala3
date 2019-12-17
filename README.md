## Circe auto encoder/decoder implementation for ADT to JSON with a type field

This is a tiny (yet macro based) library to avoid boilerplate 
with ADT encoding to JSON when you want to configure JSON type field values for case classes:

When you have case classes defined like this
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
- Avoid JSON type field in Scala case class definitions. It needs only for coding purposes here
- Use any JSON type field values. They shouldn't be class names or anything
- Avoid writing circe Encoder/Decoder manually
- Check in the compile time JSON type field mappings and Scala case classes

This library solves this issue:

### Usage

```scala
import io.circe._
import io.circe.parser._
import io.circe.syntax._
import org.latestbit.circe.adt.codec._


sealed trait TestEvent

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

### Licence
Apache Software License (ASL)

### Author
Abdulla Abdurakhmanov