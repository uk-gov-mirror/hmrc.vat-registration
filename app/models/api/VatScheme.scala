/*
 * Copyright 2021 HM Revenue & Customs
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

import auth.CryptoSCRS
import enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(id: String,
                     internalId: String,
                     tradingDetails: Option[TradingDetails] = None,
                     returns: Option[Returns] = None,
                     sicAndCompliance: Option[SicAndCompliance] = None,
                     businessContact: Option[BusinessContact] = None,
                     bankAccount: Option[BankAccount] = None,
                     acknowledgementReference: Option[String] = None,
                     flatRateScheme: Option[FlatRateScheme] = None,
                     status: VatRegStatus.Value,
                     eligibilityData: Option[JsObject] = None,
                     eligibilitySubmissionData: Option[EligibilitySubmissionData] = None,
                     applicantDetails: Option[ApplicantDetails] = None,
                     confirmInformationDeclaration: Option[Boolean] = None,
                     nrsSubmissionPayload: Option[String] = None)

object VatScheme {

  implicit val apiFormat: OFormat[VatScheme] = (
    (__ \ "registrationId").format[String] and
      (__ \ "internalId").format[String] and
      (__ \ "tradingDetails").formatNullable[TradingDetails] and
      (__ \ "returns").formatNullable[Returns] and
      (__ \ "sicAndCompliance").formatNullable[SicAndCompliance] and
      (__ \ "businessContact").formatNullable[BusinessContact] and
      (__ \ "bankAccount").formatNullable[BankAccount] and
      (__ \ "acknowledgementReference").formatNullable[String] and
      (__ \ "flatRateScheme").formatNullable[FlatRateScheme] and
      (__ \ "status").format[VatRegStatus.Value] and
      (__ \ "eligibilityData").formatNullable[JsObject] and
      (__ \ "eligibilitySubmissionData").formatNullable[EligibilitySubmissionData] and
      (__ \ "applicantDetails").formatNullable[ApplicantDetails] and
      (__ \ "confirmInformationDeclaration").formatNullable[Boolean] and
      (__ \ "nrsSubmissionPayload").formatNullable[String]
    ) (VatScheme.apply, unlift(VatScheme.unapply))

  def mongoFormat(crypto: CryptoSCRS): OFormat[VatScheme] = (
    (__ \ "registrationId").format[String] and
      (__ \ "internalId").format[String] and
      (__ \ "tradingDetails").formatNullable[TradingDetails] and
      (__ \ "returns").formatNullable[Returns] and
      (__ \ "sicAndCompliance").formatNullable[SicAndCompliance] and
      (__ \ "businessContact").formatNullable[BusinessContact] and
      (__ \ "bankAccount").formatNullable[BankAccount](BankAccountMongoFormat.encryptedFormat(crypto)) and
      (__ \ "acknowledgementReference").formatNullable[String] and
      (__ \ "flatRateScheme").formatNullable[FlatRateScheme] and
      (__ \ "status").format[VatRegStatus.Value] and
      (__ \ "eligibilityData").formatNullable[JsObject] and
      (__ \ "eligibilitySubmissionData").formatNullable[EligibilitySubmissionData] and
      (__ \ "applicantDetails").formatNullable[ApplicantDetails] and
      (__ \ "confirmInformationDeclaration").formatNullable[Boolean] and
      (__ \ "nrsSubmissionPayload").formatNullable[String]
    ) (VatScheme.apply, unlift(VatScheme.unapply))

}
