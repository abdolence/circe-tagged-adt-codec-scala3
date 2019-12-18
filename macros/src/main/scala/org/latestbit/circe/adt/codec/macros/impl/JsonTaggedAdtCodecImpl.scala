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

package org.latestbit.circe.adt.codec.macros.impl

import io.circe.JsonObject
import org.latestbit.circe.adt.codec.{JsonAdt, JsonTaggedAdtConverter}

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonTaggedAdtCodecImpl {

	/**
	 * ADT / JSON type field based trait encoding and decoding implementation as a macro
	 * @tparam T a trait type
	 * @return a converter instance of T
	 */
	def encodeObjImpl[T : c.WeakTypeTag](c : blackbox.Context) : c.Expr[JsonTaggedAdtConverter[T]] = {
		import c.universe._

		case class JsonAdtConfig(jsonAdtType : String, symbol: Symbol)

		def isJsonAdtAnnotation(annotation : Annotation) =  {
			annotation.tree.tpe =:= typeOf[JsonAdt]
		}

		def readClassJsonAdt(symbol: Symbol) : JsonAdtConfig = {

			val symAnnotations = symbol.asClass.annotations.filter(isJsonAdtAnnotation)

			if(symAnnotations.size > 1) {
				c.abort(symbol.pos, s"Only one @JsonAdt is allowed for ${symbol.fullName}")
			}
			else {
				symAnnotations.headOption.flatMap { headAnnotation =>
					headAnnotation.tree.children.tail.collectFirst {
						case Literal(Constant(jsonAdtType: String)) => JsonAdtConfig(jsonAdtType, symbol)
					}
				}.
					getOrElse(
						JsonAdtConfig(symbol.name.decodedName.toString, symbol)
					)
			}
		}

		def createConverterExpr(traitSymbol : Symbol, caseClassesConfig : Iterable[JsonAdtConfig]) = {
			c.Expr[JsonTaggedAdtConverter[T]] (
			q"""
					   new JsonTaggedAdtConverter[${traitSymbol}] {
	                        import io.circe.{ JsonObject, Decoder, ACursor }

							override def toJsonObject(obj: ${traitSymbol}): (JsonObject, String) = {

								obj match {
	                                case ..${caseClassesConfig.map { jsonAdtConfig =>
										cq"ev : ${jsonAdtConfig.symbol} => (ev.asJsonObject,${jsonAdtConfig.jsonAdtType}) "
									}}
	                            }
	                        }

					        override def fromJsonObject(jsonTypeFieldValue : String, cursor: ACursor) : Decoder.Result[${traitSymbol}] = {

					            jsonTypeFieldValue match {
			                        case ..${caseClassesConfig.map { jsonAdtConfig =>
										cq"""${jsonAdtConfig.jsonAdtType} => cursor.as[${jsonAdtConfig.symbol}]"""
									}.toList :+
										cq"""_ =>
			                                Left(
												DecodingFailure(s"Unknown json type received: '$$jsonTypeFieldValue'.", cursor.history)
			                                )
											"""
									}
			                    }
		                    }
						}
					 """
			)
		}

		val baseSymbol = c.symbolOf[T]

		if(baseSymbol.isClass) {
			val subclasses: Set[c.universe.Symbol] = c.symbolOf[T].asClass.knownDirectSubclasses
			val subclassesMap = subclasses.toList.map(readClassJsonAdt).groupBy(_.jsonAdtType)

			if(subclasses.isEmpty) {
				c.abort(baseSymbol.pos, s"${baseSymbol} defines no sub classes")
			}
			else
			subclassesMap.
				find(_._2.length > 1) match {
				case Some(duplicate) =>
					c.abort(duplicate._2.head.symbol.pos, s"${duplicate._2.map(_.symbol.fullName).mkString(", ")} defined duplicate json type")
				case _ =>
					createConverterExpr(baseSymbol, subclassesMap.flatMap(_._2.headOption))
			}

		}
		else {
			c.abort(c.enclosingPosition, s"${baseSymbol.fullName} must be a trait or base class")
		}
	}
}
