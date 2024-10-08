/*
 * Copyright 2020 Crown Copyright (Office for National Statistics)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package csvwcheck.numberformatparser

/**
  * Part of a parsed number.
  */
abstract class ParsedNumberPart

/**
  * Applies a `scalingFactor` to the parsed number. e.g. 50% can be parsed as 0.5
  */
abstract class ScalingFactorPart extends ParsedNumberPart {
  val scalingFactor: Float
}

case class PercentagePart() extends ScalingFactorPart {
  override val scalingFactor: Float = 0.01f
}

case class PerMillePart() extends ScalingFactorPart {
  override val scalingFactor: Float = 0.001f
}

/**
  * Indicates whether the parsed number is positive or negative.
  *
  * @param isPositive - Whether or not the sign is positive.
  */
case class SignPart(isPositive: Boolean) extends ParsedNumberPart

abstract class DigitsPart extends ParsedNumberPart {
  val digits: String
}

case class IntegerDigitsPart(digits: String) extends DigitsPart

case class FractionalDigitsPart(digits: String) extends DigitsPart

case class ExponentPart(isPositive: Boolean, exponent: DigitsPart)
  extends ParsedNumberPart {
  override def toString: String = {
    val sign = if (isPositive) "+" else "-"
    "E" + sign + exponent.digits
  }
}

/**
  * Brings together the parts of a parsed number and allows conversion to a numeric representation.
  *
  * @param sign             - The sign +/- of the number.
  * @param integerDigits    - The integer digits part of the number.
  * @param fractionalDigits - Fractional digits part of the number.
  * @param exponent         - The exponent part of the number.
  */
case class ParsedNumber(
                         var sign: Option[SignPart] = None,
                         var integerDigits: Option[IntegerDigitsPart] = None,
                         var fractionalDigits: Option[FractionalDigitsPart] = None,
                         var exponent: Option[ExponentPart] = None,
                         var scalingFactor: Option[ScalingFactorPart] = None
                       ) {
  def toBigDecimal: BigDecimal = {
    val signPart = sign.map(s => if (s.isPositive) "+" else "-").getOrElse("+")
    val integerPart = integerDigits.map(i => i.digits).getOrElse("0")
    val fractionalPart = fractionalDigits.map(f => "." + f.digits).getOrElse("")
    val exponentPart = exponent.map(_.toString).getOrElse("")

    val normalisedNumberString =
      signPart + integerPart + fractionalPart + exponentPart

    val factorToScaleNumberBy =
      scalingFactor.map(f => f.scalingFactor).getOrElse(1.0f)

    BigDecimal(normalisedNumberString).*(
      BigDecimal.decimal(factorToScaleNumberBy)
    )
  }
}
