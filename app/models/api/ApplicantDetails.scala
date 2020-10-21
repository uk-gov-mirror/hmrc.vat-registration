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

import java.time.LocalDate

import helpers.ApplicantDetailsHelper
import models.submission.DateOfBirth
import play.api.libs.functional.syntax._
import play.api.libs.json.Json.JsValueWrapper
import play.api.libs.json._
import utils.JsonUtilities

import scala.util.{Failure, Success, Try}

case class ApplicantDetails(nino: String,
                            role: Option[String] = None,
                            name: Name,
                            dateOfBirth: DateOfBirth,
                            companyName: String,
                            companyNumber: Option[String] = None,
                            dateOfIncorporation: LocalDate,
                            ctutr: Option[String] = None,
                            businessVerification: Option[BusinessVerificationStatus] = None,
                            bpSafeId: Option[String] = None,
                            currentAddress: Address,
                            contact: DigitalContactOptional,
                            changeOfName: Option[FormerName] = None,
                            previousAddress : Option[Address] = None,
                            countryOfIncorporation: String = "GB")

object ApplicantDetails extends VatApplicantDetailsValidator
  with ApplicantDetailsHelper
  with JsonUtilities {

  private val custInfoSection = JsPath \ "customerIdentification"
  private val appDetailsSection = JsPath \ "declaration" \ "applicantDetails"
  private val corpBodySection = JsPath \ "subscription" \ "corporateBodyRegistered"

  implicit val format: Format[ApplicantDetails] = (
    (__ \ "nino").format[String] and
    (__ \ "role").formatNullable[String] and
    (__ \ "name").format[Name] and
    (__ \ "dateOfBirth").format[DateOfBirth] and
    (__ \ "companyName").format[String] and
    (__ \ "companyNumber").formatNullable[String] and
    (__ \ "dateOfIncorporation").format[LocalDate] and
    (__ \ "ctutr").formatNullable[String] and
    (__ \ "businessVerification").formatNullable[BusinessVerificationStatus] and
    (__ \ "bpSafeId").formatNullable[String] and
    (__ \ "currentAddress").format[Address] and
    (__ \ "contact").format[DigitalContactOptional] and
    (__ \ "changeOfName").formatNullable[FormerName] and
    (__ \ "previousAddress").formatNullable[Address] and
    (__ \ "countryOfIncorporation").format[String]
  )(ApplicantDetails.apply, unlift(ApplicantDetails.unapply))

  val submissionReads: Reads[ApplicantDetails] = Reads[ApplicantDetails] { json =>
    Try {
      ApplicantDetails(
        nino = json.nino,
        role = json.getOptionalField[String](appDetailsSection \ "roleInBusiness"),
        name = json.getField[Name](appDetailsSection \ "name")(Name.submissionFormat),
        dateOfBirth = json.getField[DateOfBirth](appDetailsSection \ "dateOfBirth"),
        companyName = json.getField[String](custInfoSection \ "shortOrgName"),
        companyNumber = json.getOptionalField[String](corpBodySection \ "companyRegistrationNumber").orElse(None),
        dateOfIncorporation = json.getField[LocalDate](corpBodySection \ "dateOfIncorporation"),
        ctutr = json.ctUtr.orElse(None),
        businessVerification = json.businessVerificationStatus,
        bpSafeId = json.getOptionalField[String](custInfoSection \ "primeBPSafeID").orElse(None),
        currentAddress = json.getField[Address](appDetailsSection \ "currAddress")(Address.submissionFormat),
        contact = json.getField[DigitalContactOptional](appDetailsSection \ "commDetails")(DigitalContactOptional.submissionFormat),
        changeOfName = json.getOptionalField[FormerName](appDetailsSection \ "prevName")(FormerName.submissionFormat),
        previousAddress = json.getOptionalField[Address](appDetailsSection \ "prevAddress")(Address.submissionFormat)
      )
    } match {
      case Failure(exception) => JsError(exception.getMessage)
      case Success(value) => JsSuccess(value)
    }
  }

  val submissionWrites: Writes[ApplicantDetails] = Writes[ApplicantDetails] { appDetails =>
    Json.obj(
      "customerIdentification" -> Json.obj(
        "name" -> Json.toJson(appDetails.name)(Name.submissionFormat),
        "dateOfBirth" -> Json.toJson(appDetails.dateOfBirth),
        "shortOrgName" -> appDetails.companyName,
        optionalIds(appDetails)
      ),
      "subscription" -> Json.obj(
        "corporateBodyRegistered" -> Json.obj(
          "companyRegistrationNumber" -> appDetails.companyNumber,
          "dateOfIncorporation" -> appDetails.dateOfIncorporation,
          "countryOfIncorporation" -> "GB"
        )
      ),
      "declaration" -> Json.obj(
        "applicantDetails" -> Json.obj(
          "roleInBusiness" -> appDetails.role,
          "name" -> Json.toJson(appDetails.name)(Name.submissionFormat),
          "prevName" -> Json.toJson(appDetails.changeOfName.map(FormerName.submissionFormat.writes)),
          "dateOfBirth" -> Json.toJson(appDetails.dateOfBirth),
          "currAddress" -> Json.toJson(appDetails.currentAddress)(Address.submissionFormat),
          "prevAddress" -> Json.toJson(appDetails.previousAddress.map(Address.submissionFormat.writes)),
          "commDetails" -> Json.toJson(appDetails.contact)(DigitalContactOptional.submissionFormat),
          "identifiers" -> Json.toJson(appDetails.personalIdentifiers)
        ).filterNullFields
      )
    )
  }

  val submissionFormat: Format[ApplicantDetails] = Format[ApplicantDetails](submissionReads, submissionWrites)

  private def optionalIds(appDetails: ApplicantDetails): (String, JsValueWrapper) =
    if (appDetails.bpSafeId.isDefined) {
      "primeBPSafeID" -> appDetails.bpSafeId.map(JsString)
    }
    else {
      "customerID" -> Json.toJson(appDetails.companyIdentifiers)
    }

}
