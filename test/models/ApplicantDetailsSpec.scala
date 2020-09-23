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

package models

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api._
import play.api.libs.json.{Format, JsPath, Json, JsonValidationError}

class ApplicantDetailsSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  "Creating a Json from a valid VatApplicantDetails model" should {
    "complete successfully" in {
      writeAndRead(validApplicantDetails) resultsIn validApplicantDetails
    }
  }

  "Creating a Json from an invalid VatApplicantDetails model" ignore {
    "fail with a JsonValidationError" when {
      "NINO is invalid" in {
        val applicantDetails = validApplicantDetails.copy(nino = "NB888")
        writeAndRead(applicantDetails) shouldHaveErrors (JsPath() \ "nino" -> JsonValidationError("error.pattern"))
      }

      "Role is invalid" in {
        val applicantDetails = validApplicantDetails.copy(role = "magician")
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
