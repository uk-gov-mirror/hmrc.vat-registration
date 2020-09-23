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
import models.api.SicAndCompliance
import play.api.libs.json.{JsSuccess, Json}

class SicAndComplianceSpec extends BaseSpec with VatRegistrationFixture {

  val sicAndCompliance = testSicAndCompliance.get

  "Submission reads" must {
    "read successfully when there are no additional SIC codes" in {
      val sicAndComplianceNoCodes = sicAndCompliance.copy(otherBusinessActivities = List())
      val json = Json.toJson(sicAndComplianceNoCodes)(SicAndCompliance.submissionFormat)
      val res = SicAndCompliance.submissionReads.reads(json)

      res.isSuccess mustBe true
    }
    "read successfully when there are additional sic codes" in {

    }
  }

}
