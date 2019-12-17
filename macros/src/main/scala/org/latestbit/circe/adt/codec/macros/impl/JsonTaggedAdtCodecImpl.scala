package org.latestbit.circe.adt.codec.macros.impl

import io.circe.JsonObject
import org.latestbit.circe.adt.codec.JsonTaggedAdt

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonTaggedAdtCodecImpl {

	def encodeObjImpl[T : c.WeakTypeTag](c : blackbox.Context) : c.Expr[JsonTaggedAdt[T]] = {

		val symbol = c.symbolOf[T]
		print(symbol)
		if(symbol.isClass) {
			c.abort(c.enclosingPosition, s"${symbol.fullName} is a class")
		}
		else {
			c.abort(c.enclosingPosition, s"${symbol.fullName} must be a case class")
		}

		//println(s"ctx = ${c.symbolOf[T]}. ${obj.mirror}")

		//val subclasses: Set[c.universe.Symbol] = c.weakTypeOf[T].typeSymbol.asClass.knownDirectSubclasses
		/*

		q"""

		 """*/

		???
	}
}
