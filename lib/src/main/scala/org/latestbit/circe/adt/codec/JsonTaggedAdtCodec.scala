package org.latestbit.circe.adt.codec

import io.circe.syntax._
import io.circe.{Encoder, Json, JsonObject}

object JsonTaggedAdtCodec {

	def createEncoder[T](typeField : String) (implicit ta : JsonTaggedAdt[T]) : Encoder[T] = Encoder.instance { obj : T =>
		val jsonObj : JsonObject = ta.toJsonObject(obj)

		jsonObj.
			add("type",Json.fromString(typeField)).
			asJson

	}

}