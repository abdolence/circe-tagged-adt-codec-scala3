package org.latestbit.circe.adt.codec.impl

object TagMacro {
  
  private[codec] inline def tagClassName[T](): String = {
    ${ getTagParameterClassName[T]() }
  }

  private[codec] def getTagParameterClassName[T]()(using quoted.Type[T], quoted.Quotes): quoted.Expr[String] = {
    import quoted.quotes.reflect._
    quoted.Expr(TypeRepr.of[T].classSymbol.get.fullName)
  }

}
