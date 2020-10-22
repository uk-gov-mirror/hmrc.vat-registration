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

import models.submission.{PartyType, UkCompany}
import play.api.libs.functional.syntax._
import play.api.libs.json._

case class VatSubmission(messageType: String = "SubscriptionCreate",
                         tradersPartyType: Option[PartyType],
                         confirmInformationDeclaration: Option[Boolean],
                         companyRegistrationNumber: Option[String],
                         applicantDetails: ApplicantDetails,
                         bankDetails: Option[BankAccount],
                         sicAndCompliance: SicAndCompliance,
                         businessContact: BusinessContact,
                         tradingDetails: TradingDetails,
                         flatRateScheme: Option[FRSDetails],
                         eligibilitySubmissionData: EligibilitySubmissionData,
                         returns: Returns)

object VatSubmission {
  val submissionFormat: OFormat[VatSubmission] = (
    (__ \ "messageType").format[String] and
    (__ \ "customerIdentification" \ "tradersPartyType").formatNullable[PartyType] and
    (__ \ "declaration" \ "declarationSigning" \ "confirmInformationDeclaration").formatNullable[Boolean] and
    (__ \ "subscription" \ "corporateBodyRegistered" \ "companyRegistrationNumber").formatNullable[String] and
    (__).format[ApplicantDetails](ApplicantDetails.submissionFormat) and
    (__ \ "bankDetails").formatNullable[JsValue]
      .inmap[Option[BankAccount]](BankAccount.submissionReads, BankAccount.submissionWrites) and
    (__).format[SicAndCompliance](SicAndCompliance.submissionFormat) and
    (__ \ "contact").format[BusinessContact](BusinessContact.submissionFormat) and
    (__).format[TradingDetails](TradingDetails.submissionFormat) and
    (__ \ "subscription" \ "schemes").formatNullable[FRSDetails](FRSDetails.submissionFormat) and
    (__).format[EligibilitySubmissionData](EligibilitySubmissionData.submissionFormat) and
    (__).format[Returns](Returns.submissionFormat)
  )(VatSubmission.apply, unlift(VatSubmission.unapply))

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]

  private def missingSection(section: String) = throw new IllegalStateException(s"VAT scheme missing $section section")

  def fromVatScheme(scheme: VatScheme): VatSubmission =
    VatSubmission(
      tradersPartyType = Some(UkCompany),
      confirmInformationDeclaration = Some(true),
      companyRegistrationNumber = Some("CRN"),
      applicantDetails = scheme.applicantDetails.getOrElse(missingSection("ApplicantDetails")),
      bankDetails = scheme.bankAccount,
      sicAndCompliance = scheme.sicAndCompliance.getOrElse(missingSection("SIC and Compliance")),
      businessContact = scheme.businessContact.getOrElse(missingSection("Business contact")),
      tradingDetails = scheme.tradingDetails.getOrElse(missingSection("Trading details")),
      flatRateScheme = scheme.flatRateScheme.flatMap(_.frsDetails),
      eligibilitySubmissionData = scheme.eligibilitySubmissionData.getOrElse(missingSection("Eligibility")),
      returns = scheme.returns.getOrElse(missingSection("Returns"))
    )

}
