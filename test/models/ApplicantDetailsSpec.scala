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

package models

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api.Returns.JsonUtilities
import models.api._
import models.submission.{IdUnverifiable, IdVerificationFailed, IdVerificationStatus, IdVerified}
import play.api.libs.json._
import uk.gov.hmrc.http.InternalServerException

class ApplicantDetailsSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  val testSafeId = "testSafeId"

  def testJson(idVerificationStatus: Option[IdVerificationStatus] = None,
               safeId: Option[String] = None): JsValue =
    Json.obj(
      "customerIdentification" -> Json.obj(
        "shortOrgName" -> testCompanyName,
        "primeBPSafeID" -> safeId,
        "customerID" -> idVerificationStatus.map(status => Json.arr(
          Json.obj(
            "idValue" -> testCtUtr,
            "idType" -> "UTR",
            "IDsVerificationStatus" -> status
          ),
          Json.obj(
            "idValue" -> testCrn,
            "idType" -> "CRN",
            "IDsVerificationStatus" -> status,
            "date" -> testDateOFIncorp
          )
        ))
      ),
      "subscription" -> Json.obj(
        "corporateBodyRegistered" -> Json.obj(
          "companyRegistrationNumber" -> testCrn,
          "dateOfIncorporation" -> testDateOFIncorp,
          "countryOfIncorporation" -> "GB"
        )
      ),
      "declaration" -> Json.obj(
        "applicantDetails" -> Json.obj(
          "commDetails" -> Json.obj(
            "email" -> "skylake@vilikariet.com"
          ),
          "name" -> Json.obj(
            "firstName" -> "Forename",
            "lastName" -> "Surname"
          ),
          "dateOfBirth" -> testDateOfBirth,
          "roleInBusiness" -> testRole,
          "identifiers" -> Json.arr(
            Json.obj(
              "idValue" -> testNino,
              "idType" -> "NINO",
              "IDsVerificationStatus" -> "1",
              "date" -> testDate
            )
          ),
          "prevName" -> Json.obj(
            "firstName" -> "Forename",
            "lastName" -> "Surname",
            "nameChangeDate" -> testDate
          ),
          "currAddress" -> Json.obj(
            "postCode" -> "XX XX",
            "line1" -> "line1",
            "line2" -> "line2",
            "addressValidated" -> true,
            "countryCode" -> "GB"
          )
        )
      )
    ).filterNullFields

  "Creating a Json from a valid VatApplicantDetails model" should {
    "complete successfully" in {
      writeAndRead(validApplicantDetails) resultsIn validApplicantDetails
    }

    "complete successfully when 'role' is missing" in {
      writeAndRead(validApplicantDetails.copy(role = None)) resultsIn validApplicantDetails.copy(role = None)
    }
  }

  "Creating a Json from a valid VatApplicantDetails model using submission format" should {
    implicit val format: Format[ApplicantDetails] = Format(ApplicantDetails.submissionReads, ApplicantDetails.submissionWrites)

    "produce a valid json when bpSafeId is present" in {
      val applicantDetails = validApplicantDetails.copy(
        bpSafeId = Some(testSafeId),
        businessVerification = None,
        registration = None
      )
      Json.toJson(applicantDetails) mustBe testJson(safeId = Some(testSafeId))
    }

    "produce a valid json with BvPassed and registration failed" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvPass),
        registration = Some(FailedStatus)
      )
      Json.toJson(applicantDetails) mustBe testJson(idVerificationStatus = Some(IdVerified))
    }

    "produce a valid json with BvCtEnrolled and registration failed" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvCtEnrolled),
        registration = Some(FailedStatus)
      )
      Json.toJson(applicantDetails) mustBe testJson(idVerificationStatus = Some(IdVerified))
    }

    "cproduce a valid json with BvFail and registration not called" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvFail),
        registration = Some(NotCalledStatus)
      )
      Json.toJson(applicantDetails) mustBe testJson(idVerificationStatus = Some(IdVerificationFailed))
    }

    "produce a valid json with BvUnchallenged and registration not called" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvUnchallenged),
        registration = Some(NotCalledStatus)
      )
      Json.toJson(applicantDetails) mustBe testJson(idVerificationStatus = Some(IdVerificationFailed))
    }

    "produce a valid json with no Bv response and registration not called" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvUnchallenged),
        registration = Some(NotCalledStatus),
        identifiersMatch = Some(false)
      )
      Json.toJson(applicantDetails) mustBe testJson(idVerificationStatus = Some(IdUnverifiable))
    }

    "fail to produce valid json with an unsupported response from incorpId" in {
      val applicantDetails = validApplicantDetails.copy(
        businessVerification = Some(BvUnchallenged),
        registration = Some(FailedStatus)
      )
      intercept[InternalServerException](Json.toJson(applicantDetails))
    }
  }

  "Creating a Json from an invalid VatApplicantDetails model" ignore {
    "fail with a JsonValidationError" when {
      "NINO is invalid" in {
        val applicantDetails = validApplicantDetails.copy(nino = "NB888")
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "nino" -> JsonValidationError("error.pattern"))
      }

      "Role is invalid" in {
        val applicantDetails = validApplicantDetails.copy(role = Some("magician"))
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "role" -> JsonValidationError("error.pattern"))
      }

      "Name is invalid" in {
        val name = Name(first = Some("$%@$%^@#%@$^@$^$%@#$%@#$"), middle = None, last = "valid name")
        val applicantDetails = validApplicantDetails.copy(name = name)
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "name" \ "first" -> JsonValidationError("error.pattern"))
      }
    }
  }
}