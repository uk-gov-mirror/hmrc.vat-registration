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
import utils.JsonUtilities

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

object VatSubmission extends JsonUtilities {
  def submissionFormat: Format[VatSubmission] = Format(submissionReads, submissionWrites)

  def submissionReads: Reads[VatSubmission] = (
    (__ \ "messageType").read[String] and
      (__ \ "customerIdentification" \ "tradersPartyType").readNullable[PartyType] and
      (__ \ "declaration" \ "declarationSigning" \ "confirmInformationDeclaration").readNullable[Boolean] and
      (__ \ "subscription" \ "corporateBodyRegistered" \ "companyRegistrationNumber").readNullable[String] and
      (__).format[ApplicantDetails](ApplicantDetails.submissionReads) and
      (__ \ "bankDetails").readNullable[JsValue].fmap[Option[BankAccount]](BankAccount.submissionReads) and
      (__).read[SicAndCompliance](SicAndCompliance.submissionReads) and
      (__ \ "contact").read[BusinessContact](BusinessContact.submissionFormat) and
      (__).read[TradingDetails](TradingDetails.submissionFormat) and
      (__ \ "subscription" \ "schemes").readNullable[FRSDetails](FRSDetails.submissionReads) and
      (__).read[EligibilitySubmissionData](EligibilitySubmissionData.submissionFormat) and
      (__).read[Returns](Returns.submissionReads)
    ) (VatSubmission.apply(_, _, _, _, _, _, _, _, _, _, _, _))

  def submissionWrites: Writes[VatSubmission] = Writes { vatSubmission: VatSubmission =>
    Json.obj(
      "messageType" -> vatSubmission.messageType,
      "customerIdentification" -> Json.obj(
        "tradersPartyType" -> Json.toJson(vatSubmission.tradersPartyType)
      ),
      "declaration" -> Json.obj(
        "declarationSigning" -> Json.obj(
          "confirmInformationDeclaration" -> vatSubmission.confirmInformationDeclaration
        )
      ),
      "subscription" -> Json.obj(
        "corporateBodyRegistered" -> Json.obj(
          "companyRegistrationNumber" -> vatSubmission.companyRegistrationNumber
        ),
        "schemes" -> vatSubmission.flatRateScheme.map(Json.toJson(_)(FRSDetails.submissionWrites))
      ),
      "bankDetails" -> Json.toJson(vatSubmission.bankDetails)(BankAccount.submissionWrites),
      "contact" -> Json.toJson(vatSubmission.businessContact)(BusinessContact.submissionFormat)
    ).deepMerge(Json.toJson(vatSubmission.applicantDetails)(ApplicantDetails.submissionWrites).as[JsObject])
      .deepMerge(Json.toJson(vatSubmission.sicAndCompliance)(SicAndCompliance.submissionWrites).as[JsObject])
      .deepMerge(Json.toJson(vatSubmission.tradingDetails)(TradingDetails.submissionFormat).as[JsObject])
      .deepMerge(Json.toJson(vatSubmission.eligibilitySubmissionData)(EligibilitySubmissionData.submissionFormat).as[JsObject])
      .deepMerge(Json.toJson(vatSubmission.returns)(Returns.submissionWrites(
        vatSubmission.eligibilitySubmissionData.threshold.mandatoryRegistration
      )).as[JsObject]) filterNullFields
  }

  implicit val mongoFormat: OFormat[VatSubmission] = Json.format[VatSubmission]

  private def missingSection(section: String) = throw new IllegalStateException(s"VAT scheme missing $section section")

  def fromVatScheme(scheme: VatScheme): VatSubmission =
    VatSubmission(
      tradersPartyType = Some(UkCompany),
      confirmInformationDeclaration = scheme.confirmInformationDeclaration,
      companyRegistrationNumber = Some("CRN"),
      applicantDetails = scheme.applicantDetails.getOrElse(missingSection("ApplicantDetails")),
      bankDetails = scheme.bankAccount,
      sicAndCompliance = scheme.sicAndCompliance.getOrElse(missingSection("SIC and Compliance")),
      businessContact = scheme.businessContact.getOrElse(missingSection("Business contact")),
      tradingDetails = scheme.tradingDetails.getOrElse(missingSection("Trading details")),
      flatRateScheme = scheme.flatRateScheme.flatMap(frs => if (frs.joinFrs) frs.frsDetails else None),
      eligibilitySubmissionData = scheme.eligibilitySubmissionData.getOrElse(missingSection("Eligibility")),
      returns = scheme.returns.getOrElse(missingSection("Returns"))
    )

}
