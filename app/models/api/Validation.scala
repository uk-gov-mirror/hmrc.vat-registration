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

package models.api

import java.util.regex.Pattern

import play.api.libs.json.Reads.pattern
import play.api.libs.json._

trait Validation {

  protected def readToFmt(rds: Reads[String])(implicit wts: Writes[String]): Format[String] = Format(rds, wts)

  protected def acceptOnly(validStrings: String*)(implicit wts: Writes[String]): Format[String] =
    readToFmt(pattern(validStrings.map(Pattern.quote).mkString("^(?:", "|", ")$").r))

}

trait VatBankAccountValidator extends Validation {

  val accountNumberValidator: Format[String] = readToFmt(pattern("^(\\d){8}$".r))
  val accountSortCodeValidator: Format[String] = readToFmt(pattern("^(\\d){2}-(\\d){2}-(\\d){2}$".r))

}

trait VatAccountingPeriodValidator extends Validation {

  val periodStartValidator: Format[String] =
    acceptOnly("jan_apr_jul_oct", "feb_may_aug_nov", "mar_jun_sep_dec")

  val frequencyValidator: Format[String] =
    acceptOnly("monthly", "quarterly")

}

trait VatChoiceValidator extends Validation {

  val necessityValidator: Format[String] =
    acceptOnly("voluntary", "obligatory")

  val reasonValidator: Format[String] =
    acceptOnly(
      "COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES",
      "COMPANY_INTENDS_TO_SELLS_TAXABLE_GOODS_OR_SERVICES_IN_THE_FUTURE")

}


trait VatStartDateValidator extends Validation {

  val vatStartDateValidator: Format[String] =
    acceptOnly("COMPANY_REGISTRATION_DATE", "BUSINESS_START_DATE", "SPECIFIC_DATE")

}