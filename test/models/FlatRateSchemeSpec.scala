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

import fixtures.VatRegistrationFixture
import helpers.BaseSpec
import models.api.{FRSDetails, FlatRateScheme}
import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}

class FlatRateSchemeSpec extends BaseSpec with JsonFormatValidation with VatRegistrationFixture {

  "Creating a FlatRateScheme model from Json" should {
    "complete successfully with FRS details" in {
      Json.fromJson[FlatRateScheme](validFullFlatRateSchemeJson) shouldBe JsSuccess(validFullFlatRateScheme)
    }

    "complete successfully without FRS details" in {
      Json.fromJson[FlatRateScheme](validEmptyFlatRateSchemeJson) shouldBe JsSuccess(validEmptyFlatRateScheme)
    }

    "complete successfully with FRS details and joinFrs set to false" in {
      Json.fromJson[FlatRateScheme](detailsPresentJoinFrsFalse) shouldBe JsSuccess(validFullFlatRateScheme.copy(joinFrs = false))
    }

    "fail to read from json if frsDetails is not present when joinFrs is true" in {
      val json = Json.parse("""{"joinFrs":true}""")
      val result = Json.fromJson[FlatRateScheme](json)
      result shouldHaveErrors (JsPath() -> ValidationError("Mismatch between frsDetails presence and joinFrs"))
    }
  }

  "Creating a FRSDetails from Json" should {
    "complete successfully with businessGoods" in {
      Json.fromJson[FRSDetails](validFullFRSDetailsJsonWithBusinessGoods) shouldBe JsSuccess(validFullFRSDetails)
    }
    "complete successfully without businessGoods" in {
      Json.fromJson[FRSDetails](validFRSDetailsJsonWithoutBusinessGoods) shouldBe JsSuccess(validFullFRSDetails.copy(businessGoods = None))
    }

    "fail to read from json if categoryOfBusiness is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "startDate":"$date",
           |  "percent":15
           |}
         """.stripMargin)
      val result = Json.fromJson[FRSDetails](json)
      result shouldHaveErrors (JsPath() \ "categoryOfBusiness" -> ValidationError("error.path.missing"))
    }

    "fail to read from json if percent is missing" in {
      val json = Json.parse(
        s"""
           |{
           |  "startDate":"$date",
           |  "categoryOfBusiness":"testCategory"
           |}
         """.stripMargin)
      val result = Json.fromJson[FRSDetails](json)
      result shouldHaveErrors (JsPath() \ "percent" -> ValidationError("error.path.missing"))
    }
  }

  "Parsing a FlatRateScheme model to Json" should {
    "complete successfully" in {
      Json.toJson[FlatRateScheme](validFullFlatRateScheme) shouldBe validFullFlatRateSchemeJson
    }
  }

}
