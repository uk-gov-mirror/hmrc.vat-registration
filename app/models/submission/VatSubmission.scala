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

import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatSubmission(messageType: String = "SubscriptionCreate",
                         customerStatus: Option[String],
                         tradersPartyType: Option[String],
                         primeBPSafeId: Option[String],
                         confirmInformationDeclaration: Option[Boolean],
                         companyRegistrationNumber: Option[String],
                         applicantDetails: ApplicantDetails,
                         bankDetails: Option[BankAccountDetails],
                         sicAndCompliance: SicAndCompliance,
                         businessContact: BusinessContact,
                         tradingDetails: TradingDetails,
                         flatRateScheme: Option[FRSDetails])

object VatSubmission {
  val submissionFormat: OFormat[VatSubmission] = (
    (__ \ "messageType").format[String] and
    (__ \ "admin" \ "additionalInformation" \ "customerStatus").formatNullable[String] and
    (__ \ "customerIdentification" \ "tradersPartyType").formatNullable[String] and
    (__ \ "customerIdentification" \ "primeBPSafeId").formatNullable[String] and
    (__ \ "declaration" \ "declarationSigning" \ "confirmInformationDeclaration").formatNullable[Boolean] and
    (__ \ "subscription" \ "corporateBodyRegistered" \ "companyRegistrationNumber").formatNullable[String] and
    (__).format[ApplicantDetails](ApplicantDetails.submissionFormat) and
    (__ \ "bankDetails").formatNullable[BankAccountDetails](BankAccountDetails.submissionFormat) and
    (__ \ "businessActivities").format[SicAndCompliance](SicAndCompliance.submissionFormat) and
    (__ \ "contact").format[BusinessContact](BusinessContact.submissionFormat) and
    (__).format[TradingDetails](TradingDetails.submissionFormat) and
    (__ \ "subscription" \ "schemes").formatNullable[FRSDetails](FRSDetails.submissionFormat)
  )(VatSubmission.apply, unlift(VatSubmission.unapply))

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]

  def fromVatScheme(scheme: VatScheme): VatSubmission = {
    (scheme.applicantDetails, scheme.bankAccount, scheme.sicAndCompliance, scheme.businessContact, scheme.tradingDetails, scheme.flatRateScheme) match {
      case (Some(applicant), bankAcc, Some(sicAndCompliance), Some(contact), Some(trading), frs) =>
        VatSubmission(
          customerStatus = Some("STATUS"),
          tradersPartyType = None,
          primeBPSafeId = None,
          confirmInformationDeclaration = Some(true),
          companyRegistrationNumber = Some("CRN"),
          applicantDetails = applicant,
          bankDetails = bankAcc.flatMap(_.details),
          sicAndCompliance = sicAndCompliance,
          businessContact = contact,
          tradingDetails = trading,
          flatRateScheme = frs.flatMap(_.frsDetails)
        )
      case _ =>
        throw new IllegalStateException("Vat scheme missing required sections")
    }
  }
}
