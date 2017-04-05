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

import models.api.{VatChoice, VatStartDate}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class VatChoiceSpec extends JsonFormatValidation {

  "Creating a VatChoice model from Json" should {

    val startDate = LocalDate.of(2017, 1, 1)

    "complete successfully from full Json with obligatory necessity" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "COMPANY_REGISTRATION_DATE",
           |    "startDate": "$startDate"
           |    },
           |  "necessity":"obligatory"
           |}
        """.stripMargin)

      val expectedVatChoice = VatChoice(
        necessity = "obligatory",
        vatStartDate = VatStartDate(
          selection = "COMPANY_REGISTRATION_DATE",
          startDate = Some(startDate)
        )
      )

      Json.fromJson[VatChoice](json) shouldBe JsSuccess(expectedVatChoice)
    }

    "complete successfully from full Json with voluntary necessity" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "SPECIFIC_DATE",
           |    "startDate": "$startDate"
           |    },
           |  "necessity":"voluntary"
           |}
        """.stripMargin)

      val expectedVatChoice = VatChoice(
        necessity = "voluntary",
        vatStartDate = VatStartDate(
          selection = "SPECIFIC_DATE",
          startDate = Some(startDate)
        )
      )

      Json.fromJson[VatChoice](json) shouldBe JsSuccess(expectedVatChoice)
    }

    "complete successfully from full Json with voluntary necessity and a reason" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "SPECIFIC_DATE",
           |    "startDate": "$startDate"
           |    },
           |  "necessity":"voluntary",
           |  "reason": "COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES"
           |}
        """.stripMargin)

      val expectedVatChoice = VatChoice(
        necessity = "voluntary",
        vatStartDate = VatStartDate(
          selection = "SPECIFIC_DATE",
          startDate = Some(startDate)
        ),
        reason = Some("COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES")
      )

      Json.fromJson[VatChoice](json) shouldBe JsSuccess(expectedVatChoice)
    }

    "fail from Json with invalid reason" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "SPECIFIC_DATE",
           |    "startDate": "$startDate"
           |    },
           |  "necessity":"voluntary",
           |  "reason": "COMPANY_LIKES_TO_PAY_TAXES"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatChoice](json)
      shouldHaveErrors(result, JsPath() \ "reason", Seq(ValidationError("error.pattern")))

    }

    "fail from Json with invalid necessity" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |    "selection": "SPECIFIC_DATE",
           |    "startDate": "$startDate"
           |    },
           |  "necessity":"*garbage*"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatChoice](json)
      shouldHaveErrors(result, JsPath() \ "necessity", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with missing necessity" in {
      val json = Json.parse(
        s"""
           |{
           |  "vatStartDate": {
           |   "selection": "SPECIFIC_DATE",
           |   "startDate": "$startDate"
           |   }
           |}
        """.stripMargin)

      val result = Json.fromJson[VatChoice](json)
      shouldHaveErrors(result, JsPath() \ "necessity", Seq(ValidationError("error.path.missing")))
    }

  }
}