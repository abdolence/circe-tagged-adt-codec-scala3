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

import org.latestbit.circe.adt.codec.{ JsonAdt, JsonAdtPassThrough, JsonTaggedAdtDecoder }

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

object JsonTaggedAdtDecoderMacroImpl extends JsonTaggedAdtMacroBase {

  /**
   * ADT / JSON type field based trait encoding and decoding implementation as a macro
   * @tparam T a trait type
   * @return a converter instance of T
   */
  def createAdtDecoderMacro[T : c.WeakTypeTag](
      c: blackbox.Context
  ): c.Expr[JsonTaggedAdtDecoder[T]] = {
    import c.universe._

    def createSingleClassConverterExpr( caseClassConfig: JsonAdtConfig[Symbol] ) = {
      // format: off
          c.Expr[JsonTaggedAdtDecoder[T]] (
              q"""
			   new JsonTaggedAdtDecoder[${caseClassConfig.symbol}] {
	                import io.circe.{ JsonObject, Decoder, ACursor, DecodingFailure }
	
                    protected def implicitDerivedDecoder(
                        implicit decoder: Decoder[${caseClassConfig.symbol}]
                    ): Decoder[${caseClassConfig.symbol}] = decoder
                       
			        override def fromJsonObject(jsonTypeFieldValue : String, cursor: ACursor) : Decoder.Result[${caseClassConfig.symbol}] = {
                        implicit val decoder = implicitDerivedDecoder
                        
                        if(jsonTypeFieldValue != ${caseClassConfig.jsonAdtType}) {
                            Left(DecodingFailure(s"Unknown json type received: '$$jsonTypeFieldValue'.", cursor.history))
                        }
                        else {
                            cursor.as[${caseClassConfig.symbol}]
                        }
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
		c.Expr[JsonTaggedAdtDecoder[T]] (
		q"""
			   new JsonTaggedAdtDecoder[${traitSymbol}] {
	                import io.circe.{ JsonObject, Decoder, ACursor, DecodingFailure }
                    import io.circe.syntax._
		
			        override def fromJsonObject(jsonTypeFieldValue : String, cursor: ACursor) : Decoder.Result[${traitSymbol}] = {
	
			            jsonTypeFieldValue match {
	                        case ..${caseClassesConfig.
                                    map { jsonAdtConfig =>
                                        cq"""${jsonAdtConfig.jsonAdtType} => cursor.as[${jsonAdtConfig.symbol}]"""
                                    }.toList :+
                                        cq"""_ =>
                                            Left(DecodingFailure(s"Unknown json type received: '$$jsonTypeFieldValue'.", cursor.history))
                                        """
							}
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
