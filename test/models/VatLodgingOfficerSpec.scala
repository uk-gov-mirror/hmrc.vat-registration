/*
 * Copyright 2017 HM Revenue & Customs
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
import models.api._
import play.api.data.validation.ValidationError
import play.api.libs.json.{Format, JsPath, Json}

class VatLodgingOfficerSpec extends JsonFormatValidation with VatRegistrationFixture {

  private def writeAndRead[T](t: T)(implicit fmt: Format[T]) = fmt.reads(Json.toJson(fmt.writes(t)))

  implicit val format = VatLodgingOfficer.format

  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress, DateOfBirth(1, 1, 1990), "NB686868C", "director", name, formerName, Some(currentOrPreviousAddress), contact)

  "Creating a Json from a valid VatLodgingOfficer model" should {

    "complete successfully" in {
      writeAndRead(vatLodgingOfficer) resultsIn vatLodgingOfficer
    }

  }

  "Creating a Json from an invalid VatLodgingOfficer model" should {

    "fail with a ValidationError" when {

      "NINO is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(nino = "NB888")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "nino" -> ValidationError("error.pattern"))
      }

      "DOB day is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(41, 1, 1990))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "dob" \ "day" -> ValidationError("error.max", 31))
      }

      "DOB month is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(1, 0, 1990))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "dob" \ "month" -> ValidationError("error.min", 1))
      }

      "DOB year is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(1, 1, 990))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "dob" \ "year" -> ValidationError("error.min", 1000))
      }

      "Role is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(role = "magician")
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "role" -> ValidationError("error.pattern"))
      }

      "Name is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(name = Name(forename = Some("$%@$%^@#%@$^@$^$%@#$%@#$")))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "name" \ "forename" -> ValidationError("error.pattern"))
      }

      "Contact email is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = OfficerContactDetails(Some("£$%^&&*"), None, None))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "email" -> ValidationError("error.pattern"))
      }

      "Contact tel is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = OfficerContactDetails(None, Some("£$%^&&*"), None))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "tel" -> ValidationError("error.pattern"))
      }

      "Contact mob is invalid" in {
        val lodgingOfficer = vatLodgingOfficer.copy(contact = OfficerContactDetails(None, None, Some("£$%^&&*")))
        writeAndRead(lodgingOfficer) shouldHaveErrors (JsPath() \ "contact" \ "mobile" -> ValidationError("error.pattern"))
      }
    }

  }

}
