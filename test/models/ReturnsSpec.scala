/*
 * Copyright 2018 HM Revenue & Customs
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
import models.api.{Returns, StartDate}
import models.submission.DESSubmission
import play.api.libs.json.{JsSuccess, JsValue, Json}

class ReturnsSpec extends BaseSpec with JsonFormatValidation {

  val dateValue: LocalDate = LocalDate.of(2017, 1, 1)
  val date = StartDate(Some(dateValue))

  val fullJson: JsValue = Json.parse(
    s"""
       |{
       |  "reclaimVatOnMostReturns" : true,
       |  "frequency" : "quarterly",
       |  "staggerStart" : "jan",
       |  "start" : {
       |    "date" : "$dateValue"
       |  }
       |}
        """.stripMargin
  )

  val fullReturns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    Some("jan"),
    date
  )

  "Converting a Returns model into JSON" should {
    "complete successfully from a model" in {
      Json.toJson[Returns](fullReturns) shouldBe fullJson
    }
  }

  "Parsing Json to a Returns model" should {
    "complete successfully from JSON" in {
      Json.fromJson[Returns](fullJson) shouldBe JsSuccess(fullReturns)
    }
  }


}
