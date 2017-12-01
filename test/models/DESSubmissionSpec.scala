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

import java.time.LocalDate

import models.submission.DESSubmission
import play.api.libs.json.{JsSuccess, Json}

class DESSubmissionSpec extends JsonFormatValidation {

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

  val testDesSubmission = DESSubmission(
    acknowledgementReference = "ackRef",
    companyName = "compName",
    vatStartDate = date,
    incorpDate = date
  )


  "Converting a DESSubmission model into JSON" should {
    "complete successfully from a model" in {
      Json.toJson[DESSubmission](testDesSubmission) shouldBe fullJson
    }
  }


}
