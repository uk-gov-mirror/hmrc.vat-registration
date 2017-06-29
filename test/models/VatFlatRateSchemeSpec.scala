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

import models.api.VatFlatRateScheme
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class VatFlatRateSchemeSpec extends JsonFormatValidation {
  val startDate = LocalDate.of(2017, 7, 22)

  "Creating a VatFlatRateScheme" +
    " model from Json" should {
    "complete successfully from full Json " in {
      val json = Json.parse(
        s"""
           |{
           |  "joinFrs":true,
           |  "annualCostsInclusive":"yesWithin12months",
           |  "annualCostsLimited" :"yesWithin12months",
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFrs" : "registrationDate",
           |  "startDate" : "$startDate"
           |
           |}
        """.stripMargin)

      val tstVatFlatRateScheme
      = VatFlatRateScheme(
        joinFrs = true,
        annualCostsInclusive = Some("yesWithin12months"),
        annualCostsLimited = Some("yesWithin12months"),
        doYouWantToUseThisRate = Some(false),
        whenDoYouWantToJoinFrs = Some("registrationDate"),
        startDate = Some(LocalDate.of(2017,7,22)))

      Json.fromJson[VatFlatRateScheme](json) shouldBe JsSuccess(tstVatFlatRateScheme)
    }

    "fail from Json with invalid annualCostsInclusive" in {
      val json = Json.parse(
        s"""
           |{
           |  "joinFrs":true,
           |  "annualCostsInclusive":"yesWithin32months",
           |  "annualCostsLimited" :"yesWithin12months",
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFrs" : "registrationDate"
           |
           |}
        """.stripMargin)

      val result = Json.fromJson[VatFlatRateScheme](json)
      result shouldHaveErrors (JsPath() \ "annualCostsInclusive" -> ValidationError("error.pattern"))
    }

    "fail from Json with invalid whenDoYouWantToJoinFrs" in {
      val json = Json.parse(
        s"""
           |{
           |  "joinFrs":true,
           |  "annualCostsInclusive":"yesWithin12months",
           |  "annualCostsLimited" :"yesWithin12months",
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFrs" : "Date"
           |
           |}
        """.stripMargin)

      val result = Json.fromJson[VatFlatRateScheme](json)
      result shouldHaveErrors (JsPath() \ "whenDoYouWantToJoinFrs" -> ValidationError("error.pattern"))
    }


    "Creating a VatFlatRateScheme" +
      " model from Json" should {

      implicit val formt = VatFlatRateScheme.format

      "complete successfully from full Json" in {
        val tstVatFlatRateScheme
        = VatFlatRateScheme(
          joinFrs = true,
          annualCostsInclusive = Some("yesWithin12months"),
          annualCostsLimited = Some("yesWithin12months"),
          doYouWantToUseThisRate = Some(false),
          whenDoYouWantToJoinFrs = Some("registrationDate"))

        val writeResult = formt.writes(tstVatFlatRateScheme
        )
        val readResult = formt.reads(Json.toJson(writeResult))
        val result: VatFlatRateScheme = readResult.get

        result shouldBe tstVatFlatRateScheme


      }

    }

  }
}