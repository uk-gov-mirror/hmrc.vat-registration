/*
 * Copyright 2019 HM Revenue & Customs
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

import java.time.LocalDate

import helpers.BaseSpec
import models.submission.DESSubmission
import play.api.libs.json.Json

class DESSubmissionSpec extends BaseSpec with JsonFormatValidation {

  val date = LocalDate.of(2017, 1, 1)

  val fullJson = Json.parse(
    s"""
       |{
       |  "acknowledgementReference" : "ackRef",
       |  "companyName" : "compName",
       |  "vatStartDate" : "$date",
       |  "incorpDate" : "$date"
       |}
        """.stripMargin
  )

  val partialJson = Json.parse(
    s"""
       |{
       |  "acknowledgementReference" : "ackRef",
       |  "companyName" : "compName"
       |}
        """.stripMargin
  )

  val testDesSubmission = DESSubmission(
    acknowledgementReference = "ackRef",
    companyName = "compName",
    vatStartDate = Some(date),
    incorpDate = Some(date)
  )

  val testDesPartialSubmission = DESSubmission(
    acknowledgementReference = "ackRef",
    companyName = "compName"
  )

  "Converting a DESSubmission model into JSON" should {
    "complete successfully from a model" in {
      Json.toJson[DESSubmission](testDesSubmission) shouldBe fullJson
    }

    "complete successfully from a partial model" in {
      Json.toJson[DESSubmission](testDesPartialSubmission) shouldBe partialJson
    }
  }
}