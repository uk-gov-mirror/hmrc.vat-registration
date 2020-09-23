/*
 * Copyright 2020 HM Revenue & Customs
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
import common.TransactionId
import enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(id: String,
                     internalId: String,
                     transactionId: Option[TransactionId] = None,
                     tradingDetails: Option[TradingDetails] = None,
                     returns: Option[Returns] = None,
                     sicAndCompliance: Option[SicAndCompliance] = None,
                     businessContact: Option[BusinessContact] = None,
                     turnoverEstimates: Option[TurnoverEstimates] = None,
                     bankAccount: Option[BankAccount] = None,
                     threshold: Option[Threshold] = None,
                     acknowledgementReference: Option[String] = None,
                     flatRateScheme: Option[FlatRateScheme] = None,
                     status: VatRegStatus.Value,
                     eligibilityData: Option[JsObject] = None,
                     applicantDetails: Option[ApplicantDetails] = None)

object VatScheme {

  val apiWrites : OWrites[VatScheme] = (
    (__ \ "registrationId").write[String] and
    (__ \ "internalId").write[String] and
    (__ \ "transactionId").writeNullable[TransactionId] and
    (__ \ "tradingDetails").writeNullable[TradingDetails] and
    (__ \ "returns").writeNullable[Returns] and
    (__ \ "sicAndCompliance").writeNullable[SicAndCompliance] and
    (__ \ "businessContact").writeNullable[BusinessContact] and
    (__ \ "turnoverEstimates").writeNullable[TurnoverEstimates] and
    (__ \ "bankAccount").writeNullable[BankAccount] and
    (__ \ "threshold").writeNullable[Threshold] and
    (__ \ "acknowledgementReference").writeNullable[String] and
    (__ \ "flatRateScheme").writeNullable[FlatRateScheme] and
    (__ \ "status").write[VatRegStatus.Value] and
    (__ \ "eligibilityData").writeNullable[JsObject] and
    (__ \ "applicantDetails").writeNullable[ApplicantDetails]
  )(unlift(VatScheme.unapply))

  def mongoFormat(crypto: CryptoSCRS): OFormat[VatScheme] = (
      (__ \ "registrationId").format[String] and
      (__ \ "internalId").format[String] and
      (__ \ "transactionId").formatNullable[TransactionId] and
      (__ \ "tradingDetails").formatNullable[TradingDetails] and
      (__ \ "returns").formatNullable[Returns] and
      (__ \ "sicAndCompliance").formatNullable[SicAndCompliance] and
      (__ \ "businessContact").formatNullable[BusinessContact] and
      (__ \ "turnoverEstimates").formatNullable[TurnoverEstimates] and
      (__ \ "bankAccount").formatNullable[BankAccount](BankAccountMongoFormat.encryptedFormat(crypto)) and
      (__ \ "threshold").formatNullable[Threshold] and
      (__ \ "acknowledgementReference").formatNullable[String] and
      (__ \ "flatRateScheme").formatNullable[FlatRateScheme] and
      (__ \ "status").format[VatRegStatus.Value] and
      (__ \ "eligibilityData").formatNullable[JsObject] and
      (__ \ "applicantDetails").formatNullable[ApplicantDetails]
    )(VatScheme.apply, unlift(VatScheme.unapply))
}
