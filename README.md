## Circe encoder/decoder Scala 3 implementation for ADT to JSON with a configurable type field.
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/org.latestbit/circe-tagged-adt-codec_2.13/badge.svg)](https://maven-badges.herokuapp.com/maven-central/org.latestbit/circe-tagged-adt-codec_2.13/)
![](https://github.com/abdolence/circe-tagged-adt-codec/workflows/tests%20and%20formatting/badge.svg)

This library provides an efficient, type safe and Scala 3 inline based 
ADT to JSON encoder/decoder for Circe, with configurable JSON type field mappings.

This is the rework of [macro-based Scala 2 version here](https://github.com/abdolence/circe-tagged-adt-codec).
This is not the same in terms of functionality, because meta-programming in Scala 3 is completely different.

For example this code doesn't support `@annotations` anymore, and using `given` instances as a replacement.

When you have ADTs defined like this
```scala
enum TestEvent {
  case Event1(anyYourField : String /*, ...*/)
  case Event2(anyOtherField : Long /*, ...*/)
}
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
- Configurable JSON type field values, and their mapping to case classes. They don't have to be Scala class names.
- Avoid writing circe Encoder/Decoder manually.

## Scala support
- Scala 3.3.0+
- Scala.js v1.6+
- Scala Native 0.4.12+

## Getting Started
Add the following to your `build.sbt`:

```scala
libraryDependencies += "org.latestbit" %% "circe-tagged-adt-codec" % "0.11.0"
```

or if you need Scala.js or Native support:

```scala
libraryDependencies += "org.latestbit" %%% "circe-tagged-adt-codec" % "0.11.0"
```

## Usage

```scala
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

// One import for this ADT/JSON codec
import org.latestbit.circe.adt.codec.*

enum TestEvent derives JsonTaggedAdt.Codec {
  case Event1(anyYourField : String /*, ...*/)
  case Event2(anyOtherField : Long /*, ...*/)
  case Event3 // No fields  
}

val testEvent : TestEvent = TestEvent.TestEvent1("test")
val testJsonString : String = testEvent.asJson.dropNullValues.noSpaces

// Decoding
decode[TestEvent] (testJsonString) match {
   case Right(model : TestEvent) => // ...
}
``` 

### Tag configurations
Now we want to provide mapping between tag in JSON and case classes:
```scala
import io.circe.*
import io.circe.parser.*
import io.circe.syntax.*

// One import for this ADT/JSON codec
import org.latestbit.circe.adt.codec.*

// You still can use JsonTaggedAdt.Encoder/Decoder here, but `WithConfig` make
// configs explicitly required and without configuration it fails at compile time.
enum TestEvent derives JsonTaggedAdt.CodecWithConfig {
  case Event1(anyYourField : String /*, ...*/)
  case Event2(anyOtherField : Long /*, ...*/)
  case Event3 // No fields  
}

// Configuration
given JsonTaggedAdt.Config[TestEvent] = JsonTaggedAdt.Config.Values[TestEvent] (
  // Mappings between type/tag values and case classes/objects  
  mappings = Map(
    "ev1" -> JsonTaggedAdt.tagged[TestEvent.Event1],
    "ev2" -> JsonTaggedAdt.tagged[TestEvent.Event2],
    "ev3" -> JsonTaggedAdt.tagged[TestEvent.Event3.type]
  ),
  // Check if provided mappings aren't sufficient for all case classes (and throw exception if it is not)
  strict = true, // Default is false
  typeFieldName = "my-type" // Default is 'type'
)

// Encoding
val testEvent : TestEvent = TestEvent.Event1("test")
val testJsonString : String = testEvent.asJson.dropNullValues.noSpaces

// Decoding
decode[TestEvent] (testJsonString) match {
   case Right(model : TestEvent) => // ...
}
``` 

### Pure enum constants / case objects ADT definitions support

Sometimes you just need tags constants themselves for declarations like this, 
without any additional type tags and objects to produce JSON strings for enum constants in json 
(instead of objects).
To help with this scenario, ADT codec provides the specialized encoder and decoder implementations:

```scala
enum MyEnum derives JsonTaggedAdt.PureCodec {
  case Enum1
  case Enum2
}
```

There are `WithConfig` versions of configs accordingly:
```scala
enum MyEnum derives JsonTaggedAdt.PureCodecWithConfig {
  case Enum1
  case Enum2
}

given JsonTaggedAdt.PureConfig[MyEnum] = JsonTaggedAdt.PureConfig.Values[MyEnum] (
  mappings = Map(
    "en1" -> JsonTaggedAdt.tagged[MyEnum.Event1.type],
    "en2" -> JsonTaggedAdt.tagged[MyEnum.Event2.type]
  )
)
```

## Licence
Apache Software License (ASL)

## Author
Abdulla Abdurakhmanov
