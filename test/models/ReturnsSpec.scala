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
import play.api.data.validation.ValidationError
import play.api.libs.json._

class ReturnsSpec extends BaseSpec with JsonFormatValidation {

  val dateValue: LocalDate = LocalDate.of(2017, 1, 1)
  val date = StartDate(Some(dateValue))

  val fullJson: JsObject = Json.parse(
    s"""
       |{
       |  "reclaimVatOnMostReturns" : true,
       |  "frequency" : "quarterly",
       |  "staggerStart" : "jan",
       |  "start" : {
       |    "date" : "$dateValue"
       |  }
       |}
        """.stripMargin).as[JsObject]

  val invalidValidationJson: JsObject = Json.parse(
    s"""
       |{
       |  "reclaimVatOnMostReturns" : true,
       |  "frequency" : "whatever",
       |  "staggerStart" : "month",
       |  "start" : {
       |    "date" : "$dateValue"
       |  }
       |}
        """.stripMargin).as[JsObject]

  val invalidStaggerStartReturns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    Some("month"),
    date
  )

  val invalidFrequencyReturns = Returns(
    reclaimVatOnMostReturns = true,
    "whatever",
    Some("jan"),
    date
  )

  val fullReturns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    Some("jan"),
    date
  )

  val missingOptionsReturns = Returns(
    reclaimVatOnMostReturns = true,
    "quarterly",
    None,
    date
  )

  val invalidValidationReturns = Returns(
    reclaimVatOnMostReturns = true,
    "whatever",
    Some("month"),
    date
  )

  "Parsing Returns" should {
    "succeed" when {
      "full json is present" in {
        Json.fromJson[Returns](fullJson) shouldBe JsSuccess(fullReturns)
      }
      "frequency and staggerStart are present but invalid" in {
        Json.fromJson[Returns](invalidValidationJson) shouldBe JsSuccess(invalidValidationReturns)
      }
      "staggeredStart is missing" in {
        val json = Json.parse(
        s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "frequency" : "quarterly",
             |  "start" : {
             |    "date" : "$dateValue"
             |  }
             |}
        """.stripMargin)
        Json.fromJson[Returns](json) shouldBe JsSuccess(fullReturns.copy(staggerStart = None))
      }
    }
    "fails" when {
      "reclaimVatOnMostReturns is missing" in {
        val json = Json.parse(
        s"""
             |{
             |  "frequency" : "quarterly",
             |  "staggerStart" : "jan",
             |  "start" : {
             |    "date" : "$dateValue"
             |  }
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__ \ "reclaimVatOnMostReturns" -> ValidationError("error.path.missing"))
      }

      "frequency is missing" in {
        val json = Json.parse(
        s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "staggerStart" : "jan",
             |  "start" : {
             |    "date" : "$dateValue"
             |  }
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__ \ "frequency" -> ValidationError("error.path.missing"))
      }

      "start is missing" in {
        val json = Json.parse(
        s"""
             |{
             |  "reclaimVatOnMostReturns" : true,
             |  "frequency" : "quarterly",
             |  "staggerStart" : "jan"
             |}
        """.stripMargin)
        val result = Json.fromJson[Returns](json)
        result shouldHaveErrors (__  \ "start" -> ValidationError("error.path.missing"))
      }
    }
  }

  "Returns model to json" should {
    "succeed" when {
      "everything is present" in {
        Json.toJson[Returns](fullReturns) shouldBe fullJson
      }
      "staggerStart is missing" in {
        Json.toJson[Returns](missingOptionsReturns) shouldBe (fullJson - "staggerStart")
      }
    }
  }
}
