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
 *
 *
 */

package org.latestbit.circe.adt.codec.macros.impl

import org.latestbit.circe.adt.codec.{ JsonAdt, JsonAdtPassThrough }

import scala.language.experimental.macros
import scala.reflect.macros._

trait JsonTaggedAdtMacroBase {

  protected def isJsonAdtAnnotation[T : c.WeakTypeTag](
      c: blackbox.Context
  )( annotation: c.universe.Annotation ) = {
    import c.universe._
    annotation.tree.tpe =:= typeOf[JsonAdt]
  }

  protected def isJsonAdtPassThroughAnnotation[T : c.WeakTypeTag](
      c: blackbox.Context
  )( annotation: c.universe.Annotation ) = {
    import c.universe._
    annotation.tree.tpe =:= typeOf[JsonAdtPassThrough]
  }

  protected def hasPassThroughAnnotation[T : c.WeakTypeTag](
      c: blackbox.Context
  )( symbol: c.universe.Symbol ): Boolean = {
    import c.universe._
    symbol.asClass.annotations.exists( isJsonAdtPassThroughAnnotation( c )( _ ) )
  }

  protected def getAllSubclasses[T : c.WeakTypeTag]( c: blackbox.Context )(
      symbol: c.universe.Symbol
  ): Set[c.universe.Symbol] = {
    if (symbol.isClass && symbol.asClass.isTrait) {
      val directSubclasses: Set[c.universe.Symbol] = symbol.asClass.knownDirectSubclasses
      directSubclasses.foldLeft( Set[c.universe.Symbol]() ) { case ( all, subclass ) =>
        if (subclass.isClass) {
          if (subclass.asClass.isTrait && hasPassThroughAnnotation( c )( subclass )) {
            all ++ getAllSubclasses( c )( subclass )
          } else {
            all + subclass
          }
        } else
          all
      }
    } else
      Set()
  }

  protected case class JsonAdtConfig[T](
      jsonAdtType: String,
      isSuitable: Boolean,
      isIsolated: Boolean,
      hasDataToEncode: Boolean,
      hasPassThroughIndicator: Boolean,
      symbol: T
  )

  protected def readClassJsonAdt(
      c: blackbox.Context
  )( symbol: c.universe.Symbol ): JsonAdtConfig[c.Symbol] = {
    import c.universe._

    val symAnnotations = symbol.asClass.annotations.filter( isJsonAdtAnnotation( c )( _ ) )
    val hasPassThroughAnnotationDefined = hasPassThroughAnnotation( c )( symbol )

    if (symAnnotations.size > 1) {
      c.abort( symbol.pos, s"JsonAdt codec: Only one @JsonAdt is allowed for ${symbol.fullName}" )
    } else if (symAnnotations.nonEmpty && hasPassThroughAnnotationDefined) {
      c.abort(
        symbol.pos,
        s"JsonAdt codec: You can't mix @JsonAdt and @JsonAdtPassThrough for ${symbol.fullName}"
      )
    } else if (hasPassThroughAnnotationDefined && !symbol.asClass.isTrait) {
      c.abort(
        symbol.pos,
        s"JsonAdt codec: You can't use @JsonAdtPassThrough on anything but trait: ${symbol.fullName}"
      )
    } else {
      symAnnotations.headOption
        .flatMap { headAnnotation =>
          headAnnotation.tree.children.tail.collectFirst {
            case Literal( Constant( jsonAdtType: String ) ) =>
              JsonAdtConfig(
                jsonAdtType,
                isSuitable = symbol.isClass,
                isIsolated = symbol.asClass.isTrait && !hasPassThroughAnnotationDefined,
                hasDataToEncode = !symbol.asClass.isModuleClass,
                hasPassThroughIndicator = hasPassThroughAnnotationDefined,
                symbol = symbol
              )
          }
        }
        .getOrElse(
          JsonAdtConfig(
            symbol.name.decodedName.toString,
            isSuitable = symbol.isClass,
            isIsolated = symbol.asClass.isTrait && !hasPassThroughAnnotationDefined,
            hasDataToEncode = !symbol.asClass.isModuleClass,
            hasPassThroughIndicator = hasPassThroughAnnotationDefined,
            symbol = symbol
          )
        )
    }
  }

  protected def generateCodec[T : c.WeakTypeTag, F[_]](
      c: blackbox.Context
  )( singleCaseClassGenerator: JsonAdtConfig[c.Symbol] => c.Expr[F[T]] )(
      traitGenerator: ( c.Symbol, Iterable[JsonAdtConfig[c.Symbol]] ) => c.Expr[F[T]]
  ): c.Expr[F[T]] = {
    import c.universe._

    val baseSymbol = c.symbolOf[T]
    val baseSymbolConfig = readClassJsonAdt( c )( baseSymbol )

    if (baseSymbolConfig.isSuitable) {
      val subclasses = getAllSubclasses( c )( baseSymbol )

      if (subclasses.isEmpty) {
        if (baseSymbol.asClass.isCaseClass) {
          singleCaseClassGenerator( baseSymbolConfig )
        } else {
          c.abort(
            baseSymbol.pos,
            s"JsonAdt codec: ${baseSymbol} defines neither no sub classes nor its defined as a case class itself"
          )
        }
      } else {
        val subclassesMap =
          subclasses.toList
            .map( readClassJsonAdt( c )( _ ) )
            .filter( _.isSuitable )
            .groupBy( _.jsonAdtType )

        subclassesMap.find( _._2.length > 1 ) match {
          case Some( duplicate ) =>
            c.abort(
              duplicate._2.head.symbol.pos,
              s"JsonAdt codec: ${duplicate._2.map( _.symbol.fullName ).mkString( ", " )} defined a duplicate json type: '${duplicate._1}''"
            )
          case _ =>
            traitGenerator( baseSymbol, subclassesMap.flatMap( _._2.headOption ) )
        }
      }

    } else {
      c.abort(
        c.enclosingPosition,
        s"JsonAdt codec: ${baseSymbol.fullName} must be a trait or a case class"
      )
    }
  }

}
