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

import models.api.{DateOfBirth, ScrsAddress, VatLodgingOfficer}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, Json}

class VatLodgingOfficerSpec extends JsonFormatValidation {

  "Creating a Json from a VatLodgingOfficer model" should {

    implicit val format = VatLodgingOfficer.format

    val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))

    val vatLodgingOfficer = VatLodgingOfficer(
      scrsAddress,
      DateOfBirth(1,1,1990),
      "NB686868C"
    )

    "complete successfully with currentAddress" in {
      val writeResult = format.writes(vatLodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))
      val result = readResult.get

      result shouldBe vatLodgingOfficer
    }

    "fail from Json with invalid NINO" in {
      val lodgingOfficer = vatLodgingOfficer.copy(nino = "NB888")

      val writeResult = format.writes(lodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))

      readResult shouldHaveErrors (JsPath() \ "nino" -> ValidationError("error.pattern"))
    }

    "fail from Json with invalid DOB day" in {
      val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(41,1,1990))

      val writeResult = format.writes(lodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))

      readResult shouldHaveErrors (JsPath() \ "dob" \ "day" -> ValidationError("error.max", 31))
    }

    "fail from Json with invalid DOB month" in {
      val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(1,0,1990))

      val writeResult = format.writes(lodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))

      readResult shouldHaveErrors (JsPath() \ "dob" \ "month" -> ValidationError("error.min", 1))
    }

    "fail from Json with invalid DOB year" in {
      val lodgingOfficer = vatLodgingOfficer.copy(dob = DateOfBirth(1,1,990))

      val writeResult = format.writes(lodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))

      readResult shouldHaveErrors (JsPath() \ "dob" \ "year" -> ValidationError("error.min", 1000))
    }
  }

}