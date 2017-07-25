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

package models.external

import models.JsonFormatValidation
import org.joda.time.DateTime
import play.api.libs.json.{JsSuccess, Json}

class IncorpInfoSpec extends JsonFormatValidation {

  "IncorpStatus" should {
    "deserialise from full Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"SCRS",
           |      "discriminator":"vat",
           |      "transactionId":"123456789"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"/callBackUrl"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "crn":"12345678987654321",
           |      "incorporationDate":"2017-04-26",
           |      "description":"Some description",
           |      "timestamp":"2017-04-25T16:20:10.000+01:00"
           |    }
           |  }
           |}
        """.stripMargin)

      val tstStatus = IncorpStatus(
        IncorpSubscription(
          transactionId = "123456789",
          regime = "vat",
          subscriber = "SCRS",
          callbackUrl = "/callBackUrl"),
        IncorpStatusEvent(
          status = "accepted",
          crn = Some("12345678987654321"),
          incorporationDate = Some(DateTime.parse("2017-04-26")),
          description = Some("Some description"),
          timestamp = DateTime.parse("2017-04-25T16:20:10.000+01:00")))

      Json.fromJson[IncorpStatus](json)(IncorpStatus.iiReads) shouldBe JsSuccess(tstStatus)
    }

    "deserialise from minimal Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"SCRS",
           |      "discriminator":"vat",
           |      "transactionId":"123456789"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"/callBackUrl"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "timestamp":"2017-04-25T16:20:10.000+01:00"
           |    }
           |  }
           |}
        """.stripMargin)

      val tstStatus = IncorpStatus(
        IncorpSubscription(
          transactionId = "123456789",
          regime = "vat",
          subscriber = "SCRS",
          callbackUrl = "/callBackUrl"),
        IncorpStatusEvent(
          status = "accepted",
          crn = None,
          incorporationDate = None,
          description = None,
          timestamp = DateTime.parse("2017-04-25T16:20:10.000+01:00")))

      Json.fromJson[IncorpStatus](json)(IncorpStatus.iiReads) shouldBe JsSuccess(tstStatus)
    }

  }
}
