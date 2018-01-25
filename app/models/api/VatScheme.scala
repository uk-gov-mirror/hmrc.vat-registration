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

import common.{RegistrationId, TransactionId}
import enums.VatRegStatus
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatScheme(id: RegistrationId,
                     transactionId: Option[TransactionId] = None,
                     tradingDetails: Option[TradingDetails] = None,
                     lodgingOfficer: Option[LodgingOfficer] = None,
                     financials: Option[VatFinancials] = None,
                     returns: Option[Returns] = None,
                     vatSicAndCompliance: Option[VatSicAndCompliance] = None,
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

  val mongoReads: Reads[VatScheme] = (
    (__ \ "registrationId").read[RegistrationId] and
    (__ \ "transactionId").readNullable[TransactionId] and
    (__ \ "tradingDetails").readNullable[TradingDetails] and
    (__ \ "lodgingOfficer").readNullable[LodgingOfficer] and
    (__ \ "financials").readNullable[VatFinancials] and
    (__ \ "returns").readNullable[Returns] and
    (__ \ "vatSicAndCompliance").readNullable[VatSicAndCompliance] and
    (__ \ "sicAndCompliance").readNullable[SicAndCompliance] and
    (__ \ "vatContact").readNullable[VatContact] and
    (__ \ "businessContact").readNullable[BusinessContact] and
    (__ \ "vatEligibility").readNullable[VatServiceEligibility] and
    (__ \ "eligibility").readNullable[Eligibility] and
    (__ \ "turnoverEstimates").readNullable[TurnoverEstimates] and
    (__ \ "bankAccount").readNullable[BankAccount](BankAccountMongoFormat.encryptedFormat) and
    (__ \ "threshold").readNullable[Threshold] and
    (__ \ "acknowledgementReference").readNullable[String] and
    (__ \ "flatRateScheme").readNullable[FlatRateScheme] and
    (__ \ "status").read[VatRegStatus.Value]
  )(VatScheme.apply _)

  val apiWrites : OWrites[VatScheme] = (
    (__ \ "registrationId").write[RegistrationId] and
    (__ \ "transactionId").writeNullable[TransactionId] and
    (__ \ "tradingDetails").writeNullable[TradingDetails] and
    (__ \ "lodgingOfficer").writeNullable[LodgingOfficer] and
    (__ \ "financials").writeNullable[VatFinancials] and
    (__ \ "returns").writeNullable[Returns] and
    (__ \ "vatSicAndCompliance").writeNullable[VatSicAndCompliance] and
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

  implicit val format: OFormat[VatScheme] = OFormat(mongoReads, apiWrites)
}
