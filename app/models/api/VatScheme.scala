/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.Crypto
import common.{RegistrationId, TransactionId}
import enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.util.control.NoStackTrace

case class VatScheme(id: RegistrationId,
                     internalId: String,
                     transactionId: Option[TransactionId] = None,
                     tradingDetails: Option[TradingDetails] = None,
                     lodgingOfficer: Option[LodgingOfficer] = None,
                     returns: Option[Returns] = None,
                     sicAndCompliance: Option[SicAndCompliance] = None,
                     vatContact: Option[VatContact] = None,
                     businessContact: Option[BusinessContact] = None,
                     vatEligibility: Option[VatServiceEligibility] = None,
                     eligibility: Option[Eligibility] = None,
                     turnoverEstimates: Option[TurnoverEstimates] = None,
                     bankAccount: Option[BankAccount] = None,
                     threshold: Option[Threshold] = None,
                     acknowledgementReference: Option[String] = None,
                     flatRateScheme: Option[FlatRateScheme] = None,
                     status: VatRegStatus.Value)

object VatScheme {

  val apiWrites : OWrites[VatScheme] = (
    (__ \ "registrationId").write[RegistrationId] and
    (__ \ "internalId").write[String] and
    (__ \ "transactionId").writeNullable[TransactionId] and
    (__ \ "tradingDetails").writeNullable[TradingDetails] and
    (__ \ "lodgingOfficer").writeNullable[LodgingOfficer] and
    (__ \ "returns").writeNullable[Returns] and
    (__ \ "sicAndCompliance").writeNullable[SicAndCompliance] and
    (__ \ "vatContact").writeNullable[VatContact] and
    (__ \ "businessContact").writeNullable[BusinessContact] and
    (__ \ "vatEligibility").writeNullable[VatServiceEligibility] and
    (__ \ "eligibility").writeNullable[Eligibility] and
    (__ \ "turnoverEstimates").writeNullable[TurnoverEstimates] and
    (__ \ "bankAccount").writeNullable[BankAccount] and
    (__ \ "threshold").writeNullable[Threshold] and
    (__ \ "acknowledgementReference").writeNullable[String] and
    (__ \ "flatRateScheme").writeNullable[FlatRateScheme] and
    (__ \ "status").write[VatRegStatus.Value]
  )(unlift(VatScheme.unapply))

  def mongoFormat(crypto: Crypto): OFormat[VatScheme] = (
      (__ \ "registrationId").format[RegistrationId] and
      (__ \ "internalId").format[String] and
      (__ \ "transactionId").formatNullable[TransactionId] and
      (__ \ "tradingDetails").formatNullable[TradingDetails] and
      (__ \ "lodgingOfficer").formatNullable[LodgingOfficer] and
      (__ \ "returns").formatNullable[Returns] and
      (__ \ "sicAndCompliance").formatNullable[SicAndCompliance] and
      (__ \ "vatContact").formatNullable[VatContact] and
      (__ \ "businessContact").formatNullable[BusinessContact] and
      (__ \ "vatEligibility").formatNullable[VatServiceEligibility] and
      (__ \ "eligibility").formatNullable[Eligibility] and
      (__ \ "turnoverEstimates").formatNullable[TurnoverEstimates] and
      (__ \ "bankAccount").formatNullable[BankAccount](BankAccountMongoFormat.encryptedFormat(crypto)) and
      (__ \ "threshold").formatNullable[Threshold] and
      (__ \ "acknowledgementReference").formatNullable[String] and
      (__ \ "flatRateScheme").formatNullable[FlatRateScheme] and
      (__ \ "status").format[VatRegStatus.Value]
    )(VatScheme.apply, unlift(VatScheme.unapply))
}
