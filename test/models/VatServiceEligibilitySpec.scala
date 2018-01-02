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

import java.time.{LocalDate, ZoneId}

import helpers.VatRegSpec
import models.api.{VatEligibilityChoice, VatExpectedThresholdPostIncorp, VatServiceEligibility, VatThresholdPostIncorp}
import play.api.data.validation.ValidationError
import play.api.libs.json._

class VatServiceEligibilitySpec extends VatRegSpec with JsonFormatValidation {


  "Creating a VatServiceEligibility model from Json" should {

    implicit val format = VatServiceEligibility.format

    val now = LocalDate.now(ZoneId.systemDefault())

    "complete successfully from long Json" in {
      val json = Json.parse(
        s"""
          |{
          |  "haveNino": true,
          |  "doingBusinessAbroad": true,
          |  "doAnyApplyToYou": true,
          |  "applyingForAnyOf": true,
          |  "applyingForVatExemption": true,
          |  "companyWillDoAnyOf": true,
          |  "vatEligibilityChoice" : {
          |     "necessity" : "obligatory",
          |     "reason" : "COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES",
          |     "vatThresholdPostIncorp" : {
          |       "overThresholdSelection" : true,
          |       "overThresholdDate" : "$now"
          |     },
          |     "vatExpectedThresholdPostIncorp" : {
          |       "expectedOverThresholdSelection" : true,
          |       "expectedOverThresholdDate" : "$now"
          |     }
          |  }
          |}
        """.stripMargin)
      val testVatServiceEligibility = VatServiceEligibility(
        haveNino = Some(true),
        doingBusinessAbroad = Some(true),
        doAnyApplyToYou = Some(true),
        applyingForAnyOf = Some(true),
        applyingForVatExemption = Some(true),
        companyWillDoAnyOf = Some(true),
        vatEligibilityChoice = Some(VatEligibilityChoice(
          necessity = "obligatory",
          reason = Some("COMPANY_ALREADY_SELLS_TAXABLE_GOODS_OR_SERVICES"),
          vatThresholdPostIncorp = Some(VatThresholdPostIncorp(
            overThresholdSelection = true,
            overThresholdDate = Some(now)
          )),
          vatExpectedThresholdPostIncorp = Some(VatExpectedThresholdPostIncorp(
            expectedOverThresholdSelection = true,
            expectedOverThresholdDate = Some(now)
          ))
        ))
      )

      Json.fromJson[VatServiceEligibility](json) shouldBe JsSuccess(testVatServiceEligibility)
    }

    "complete successfully from short Json" in {
      val json = Json.parse("""{ "haveNino": true }""")
      val testVatServiceEligibility = VatServiceEligibility(haveNino = Some(true))

      Json.fromJson[VatServiceEligibility](json) shouldBe JsSuccess(testVatServiceEligibility)
    }

    "complete successfully from empty Json" in {
      val json = Json.parse("{}")
      val testVatServiceEligibility = VatServiceEligibility()

      Json.fromJson[VatServiceEligibility](json) shouldBe JsSuccess(testVatServiceEligibility)
    }

    "fail from Json with invalid boolean" in {
      val json = Json.parse("""{ "haveNino": "SomeBadBoolean" }""")
      val result = Json.fromJson[VatServiceEligibility](json)
      result shouldHaveErrors (JsPath() \ "haveNino" -> ValidationError("error.expected.jsboolean"))
    }

  }

}
