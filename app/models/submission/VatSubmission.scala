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
                         tradersPartyType: Option[String],
                         confirmInformationDeclaration: Option[Boolean],
                         companyRegistrationNumber: Option[String],
                         applicantDetails: ApplicantDetails,
                         bankDetails: Option[BankAccount],
                         sicAndCompliance: SicAndCompliance,
                         businessContact: BusinessContact,
                         tradingDetails: TradingDetails,
                         flatRateScheme: Option[FRSDetails],
                         eligibilitySubmissionData: EligibilitySubmissionData,
                         zeroRatedSupplies: BigDecimal)

object VatSubmission {
  val submissionFormat: OFormat[VatSubmission] = (
    (__ \ "messageType").format[String] and
    (__ \ "customerIdentification" \ "tradersPartyType").formatNullable[String] and
    (__ \ "declaration" \ "declarationSigning" \ "confirmInformationDeclaration").formatNullable[Boolean] and
    (__ \ "subscription" \ "corporateBodyRegistered" \ "companyRegistrationNumber").formatNullable[String] and
    (__).format[ApplicantDetails](ApplicantDetails.submissionFormat) and
    (__ \ "bankDetails").formatNullable[JsValue].inmap[Option[BankAccount]](
      a => BankAccount.submissionReads(a),
      b => BankAccount.submissionWrites(b)
    ) and
    (__ \ "businessActivities").format[SicAndCompliance](SicAndCompliance.submissionFormat) and
    (__ \ "contact").format[BusinessContact](BusinessContact.submissionFormat) and
    (__).format[TradingDetails](TradingDetails.submissionFormat) and
    (__ \ "subscription" \ "schemes").formatNullable[FRSDetails](FRSDetails.submissionFormat) and
    (__).format[EligibilitySubmissionData](EligibilitySubmissionData.submissionFormat) and
    (__ \ "subscription" \ "yourTurnover" \ "zeroRatedSupplies").format[BigDecimal]
    )(VatSubmission.apply, unlift(VatSubmission.unapply))

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]

  def fromVatScheme(scheme: VatScheme): VatSubmission = {
    (scheme.eligibilitySubmissionData, scheme.applicantDetails, scheme.bankAccount,
      scheme.sicAndCompliance, scheme.businessContact, scheme.tradingDetails, scheme.flatRateScheme, scheme.returns.flatMap(_.zeroRatedSupplies)) match {
      case (Some(eligibilityData), Some(applicant), bankAcc, Some(sac), Some(contact), Some(trading), frs, Some(zrs)) =>
        VatSubmission(
          tradersPartyType = None,
          confirmInformationDeclaration = Some(true),
          companyRegistrationNumber = Some("CRN"),
          applicantDetails = applicant,
          bankDetails = bankAcc,
          sicAndCompliance = sac,
          businessContact = contact,
          tradingDetails = trading,
          flatRateScheme = frs.flatMap(_.frsDetails),
          eligibilitySubmissionData = eligibilityData,
          zeroRatedSupplies = zrs
        )
      case _ =>
        throw new IllegalStateException("Vat scheme missing required sections")
    }
  }
}
