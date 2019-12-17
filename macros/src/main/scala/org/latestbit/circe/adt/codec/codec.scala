package org.latestbit.circe.adt

import org.latestbit.circe.adt.codec.macros.impl.JsonTaggedAdtCodecImpl

package object codec {
	import scala.language.experimental.macros

	implicit def encodeObj[T]: JsonTaggedAdt[T] = macro JsonTaggedAdtCodecImpl.encodeObjImpl[T]
}
