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

import models.api.TradingDetails
import play.api.libs.json.{JsSuccess, JsValue, Json}

class TradingDetailsSpec extends JsonFormatValidation {

  val fullJson: JsValue = Json.parse(
    """
       |{
       |  "tradingName":"test-name",
       |  "eoriRequested":true
       |}
         """.stripMargin
  )
  val fullModel = TradingDetails(Some("test-name"), Some(true))

  val noNameJson: JsValue = Json.parse(
    """
      |{
      |   "eoriRequested":true
      |}
    """.stripMargin
  )
  val noNameModel = TradingDetails(None, Some(true))

  val noEoriJson: JsValue = Json.parse(
    """
      |{
      |   "tradingName":"test-name"
      |}
    """.stripMargin
  )
  val noEoriModel = TradingDetails(Some("test-name"), None)

  val emptyJson: JsValue = Json.parse(
    """
      |{
      |
      |}
    """.stripMargin
  )
  val emptyModel = TradingDetails(None, None)

  "Creating a TradingDetails model from Json" should {
    "complete successfully from full Json" in {
      Json.fromJson[TradingDetails](fullJson) shouldBe JsSuccess(fullModel)
    }
    "complete successfully without a trading name" in {
      Json.fromJson[TradingDetails](noNameJson) shouldBe JsSuccess(noNameModel)
    }
    "complete successfully without eori-requested" in {
      Json.fromJson[TradingDetails](noEoriJson) shouldBe JsSuccess(noEoriModel)
    }
    "complete successfully without any details" in {
      Json.fromJson[TradingDetails](emptyJson) shouldBe JsSuccess(emptyModel)
    }
  }

  "Parsing a TradingDetails model to Json" should {
    "complete successfully with full details" in {
      Json.toJson[TradingDetails](fullModel) shouldBe fullJson
    }
    "complete successfully without a trading name" in {
      Json.toJson[TradingDetails](noNameModel) shouldBe noNameJson
    }
    "complete successfully without a eoriRequired" in {
      Json.toJson[TradingDetails](noEoriModel) shouldBe noEoriJson
    }
    "complete successfully without any details" in {
      Json.toJson[TradingDetails](emptyModel) shouldBe emptyJson
    }
  }

}
