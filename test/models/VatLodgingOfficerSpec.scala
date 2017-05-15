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
import play.api.libs.json.Json

class VatLodgingOfficerSpec extends JsonFormatValidation {

  "Creating a Json from a VatLodgingOfficer model" should {

    implicit val format = VatLodgingOfficer.format

    "complete successfully with currentAddress" in {
      val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
      val vatLodgingOfficer = VatLodgingOfficer(
        scrsAddress,
        DateOfBirth(1,1,1990),
        "NB686868C"
      )

      val writeResult = format.writes(vatLodgingOfficer)
      val readResult = format.reads(Json.toJson(writeResult))
      val result = readResult.get

      result shouldBe vatLodgingOfficer
    }

  }

}