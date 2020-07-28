/*
 * Copyright 2020 HM Revenue & Customs
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

package models.external

import java.time.LocalDate

import helpers.BaseSpec
import models.JsonFormatValidation
import play.api.libs.json.{JsSuccess, Json}

class IncorporationInfoSpec extends BaseSpec with JsonFormatValidation {

  "IncorporationStatus" should {
    "deserialise from full Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"000-434-23"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"http://localhost:9896/TODO-CHANGE-THIS"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "crn":"90000001",
           |      "description": "Some description",
           |      "incorporationDate":1470351600000,
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)

      val tstStatus = IncorporationStatus(
        IncorpSubscription(
          transactionId = "000-434-23",
          regime = "vat",
          subscriber = "scrs",
          callbackUrl = "http://localhost:9896/TODO-CHANGE-THIS"),
        IncorpStatusEvent(
          status = "accepted",
          crn = Some("90000001"),
          incorporationDate = Some(LocalDate.of(2016, 8, 5)),
          description = Some("Some description")))

      Json.fromJson[IncorporationStatus](json)(IncorporationStatus.iiReads) mustBe JsSuccess(tstStatus)
    }

    "deserialise from minimal Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"000-434-23"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"http://localhost:9896/TODO-CHANGE-THIS"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"rejected",
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)

      val tstStatus = IncorporationStatus(
        IncorpSubscription(
          transactionId = "000-434-23",
          regime = "vat",
          subscriber = "scrs",
          callbackUrl = "http://localhost:9896/TODO-CHANGE-THIS"),
        IncorpStatusEvent(
          status = "rejected",
          crn = None,
          incorporationDate = None,
          description = None))

      Json.fromJson[IncorporationStatus](json)(IncorporationStatus.iiReads) mustBe JsSuccess(tstStatus)
    }

  }
}
