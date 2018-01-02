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

import models.api.{VatChoice, VatStartDate, VatThresholdPostIncorp}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class VatChoiceSpec extends JsonFormatValidation {

  "Creating a VatChoice model from Json" should {

    val testDate = LocalDate.of(2017, 1, 1)

    "complete successfully from full Json with obligatory necessity" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "COMPANY_REGISTRATION_DATE",
           |    "startDate": "$testDate"
           |  }
           |}
        """.stripMargin)

      val expectedVatChoice = VatChoice(
        vatStartDate = VatStartDate(
          selection = "COMPANY_REGISTRATION_DATE",
          startDate = Some(testDate)
        )
      )

      Json.fromJson[VatChoice](json) shouldBe JsSuccess(expectedVatChoice, __ \ "vatStartDate")
    }
  }
}
