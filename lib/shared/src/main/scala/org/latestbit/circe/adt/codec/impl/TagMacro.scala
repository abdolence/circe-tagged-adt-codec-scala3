package org.latestbit.circe.adt.codec.impl

object TagMacro {
  
  private[codec] inline def tagParameterType[T](): String = {
    ${ getTagParameterType[T]() }
  }

  private[codec] def getTagParameterType[T]()(using quoted.Type[T], quoted.Quotes): quoted.Expr[String] = {
    import quoted.quotes.reflect._
    quoted.Expr(TypeRepr.of[T].classSymbol.get.fullName)
  }

}
