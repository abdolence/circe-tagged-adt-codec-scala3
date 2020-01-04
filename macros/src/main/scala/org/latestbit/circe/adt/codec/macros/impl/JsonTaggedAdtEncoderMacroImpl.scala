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

import org.latestbit.circe.adt.codec.{ JsonAdt, JsonAdtPassThrough, JsonTaggedAdtEncoder }

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonTaggedAdtEncoderMacroImpl extends JsonTaggedAdtMacroBase {

  /**
   * ADT / JSON type field based trait encoding and decoding implementation as a macro
   * @tparam T a trait type
   * @return a converter instance of T
   */
  def createAdtEncoderMacro[T : c.WeakTypeTag](
      c: blackbox.Context
  ): c.Expr[JsonTaggedAdtEncoder[T]] = {
    import c.universe._

    def createSingleClassConverterExpr( caseClassConfig: JsonAdtConfig[Symbol] ) = {
      // format: off
          c.Expr[JsonTaggedAdtEncoder[T]] (
              q"""
			   new JsonTaggedAdtEncoder[${caseClassConfig.symbol}] {
	                import io.circe.{ JsonObject, Encoder }
                    import io.circe.generic.semiauto._
                       
					override def toJsonObject(obj: ${caseClassConfig.symbol}): (JsonObject, String) = {
                        val encoder = deriveEncoder[${caseClassConfig.symbol}]
                        (encoder.encodeObject(obj),${caseClassConfig.jsonAdtType})
	                }

				}
			 """
          )
          // format: on
    }

    def createConverterExpr(
        traitSymbol: Symbol,
        caseClassesConfig: Iterable[JsonAdtConfig[Symbol]]
    ) = {
      // format: off
		c.Expr[JsonTaggedAdtEncoder[T]] (
		q"""
			   new JsonTaggedAdtEncoder[${traitSymbol}] {
	                import io.circe.{ JsonObject, Encoder }
                    import io.circe.syntax._
	
					override def toJsonObject(obj: ${traitSymbol}): (JsonObject, String) = {
	
						obj match {
	                        case ..${caseClassesConfig.
                                    map { jsonAdtConfig =>
                                    if(jsonAdtConfig.hasDataToEncode) {
                                        cq"ev : ${jsonAdtConfig.symbol} => (ev.asJsonObject,${jsonAdtConfig.jsonAdtType}) "
                                    }
                                    else {
                                        cq"ev : ${jsonAdtConfig.symbol} => (JsonObject(),${jsonAdtConfig.jsonAdtType}) "
                                    }
							}}
	                    }
	                }
	
				}
			 """
		) 
	    // format: on
    }

    generateCodec( c )(
      createSingleClassConverterExpr( _ )
    )( createConverterExpr( _, _ ) )
  }
}
