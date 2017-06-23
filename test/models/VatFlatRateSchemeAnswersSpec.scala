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

import models.api.{LimitedCost, VatBankAccount, VatBankAccountMongoFormat, VatFlatRateSchemeAnswers}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class VatFlatRateSchemeAnswersSpec extends JsonFormatValidation {

  "Creating a VatFlatRateSchemeAnswers model from Json" should {
    "complete successfully from full Json " in {
      val json = Json.parse(
        s"""
           |{
           |  "interestedInFRS":true,
           |  "lessThan1000pounds":"yesWithin12months",
           |  "limitedCost" : {
           |      "lessThan" : 1000,
           |      "answer" : "yesWithin12months"
           |      },
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFRS" : "registrationDate"
           |
           |}
        """.stripMargin)

      val tstVatFlatRateSchemeAnswers = VatFlatRateSchemeAnswers(
        interestedInFRS = Some(true),
        lessThan1000pounds = Some("yesWithin12months"),
        limitedCost = Some(LimitedCost(Some(1000), Some("yesWithin12months"))),
        doYouWantToUseThisRate = Some(false),
        whenDoYouWantToJoinFRS=  Some("registrationDate"))

      Json.fromJson[VatFlatRateSchemeAnswers](json) shouldBe JsSuccess(tstVatFlatRateSchemeAnswers)
    }

    "fail from Json with invalid lessThan1000pounds" in {
      val json = Json.parse(
        s"""
           |{
           |  "interestedInFRS":true,
           |  "lessThan1000pounds":"yesWithin32months",
           |  "limitedCost" : {
           |      "lessThan" : 1000,
           |      "answer" : "yesWithin12months"
           |      },
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFRS" : "registrationDate"
           |
           |}
        """.stripMargin)

      val result = Json.fromJson[VatFlatRateSchemeAnswers](json)
      result shouldHaveErrors (JsPath() \ "lessThan1000pounds" -> ValidationError("error.pattern"))
    }

    "fail from Json with invalid whenDoYouWantToJoinFRS" in {
      val json = Json.parse(
        s"""
           |{
           |  "interestedInFRS":true,
           |  "lessThan1000pounds":"yesWithin12months",
           |  "limitedCost" : {
           |      "lessThan" : 1000,
           |      "answer" : "yesWithin12months"
           |      },
           |  "doYouWantToUseThisRate":false,
           |  "whenDoYouWantToJoinFRS" : "Date"
           |
           |}
        """.stripMargin)

      val result = Json.fromJson[VatFlatRateSchemeAnswers](json)
      result shouldHaveErrors (JsPath() \ "whenDoYouWantToJoinFRS" -> ValidationError("error.pattern"))
    }


    "Creating a VatFlatRateSchemeAnswers model from Json" should {

      implicit val formt = VatFlatRateSchemeAnswers.format

      "complete successfully from full Json" in {
        val tstVatFlatRateSchemeAnswers = VatFlatRateSchemeAnswers(
          interestedInFRS = Some(true),
          lessThan1000pounds = Some("yesWithin12months"),
          limitedCost = Some(LimitedCost(Some(1000), Some("yesWithin12months"))),
          doYouWantToUseThisRate = Some(false),
          whenDoYouWantToJoinFRS=  Some("registrationDate"))

        val writeResult = formt.writes(tstVatFlatRateSchemeAnswers)
        val readResult = formt.reads(Json.toJson(writeResult))
        val result: VatFlatRateSchemeAnswers = readResult.get

        result shouldBe tstVatFlatRateSchemeAnswers

      }

    }

  }
}