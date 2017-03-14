/*
 * Copyright 2017 HM Revenue & Customs
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

package models

import play.api.libs.json.Reads.pattern
import play.api.libs.json._

trait Validation {

  def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)
}

trait VatBankAccountValidator extends Validation {

  val accountNumberValidator: Format[String] = readToFmt(pattern("^(\\d){8}$".r))
  val accountSortCodeValidator: Format[String] = readToFmt(pattern("^(\\d){2}-(\\d){2}-(\\d){2}$".r))
}

trait VatAccountingPeriodValidator extends Validation {

  val periodStartValidator: Format[String] = readToFmt(pattern("^(jan_apr_jul_oct|feb_may_aug_nov|mar_jun_sep_dec)$".r))
  val frequencyValidator: Format[String] = readToFmt(pattern("^(monthly|quarterly)$".r))
}

trait VatChoiceValidator extends Validation {

  val necessityValidator: Format[String] = readToFmt(pattern("^(voluntary|obligatory)$".r))
}
