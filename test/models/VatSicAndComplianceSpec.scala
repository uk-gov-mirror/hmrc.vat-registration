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

import helpers.BaseSpec
import models.api.{SicCode, VatComplianceCultural, VatComplianceLabour, VatSicAndCompliance}
import play.api.libs.json.{JsSuccess, Json}

class VatSicAndComplianceSpec extends BaseSpec with JsonFormatValidation {

  "Creating a VatSicAndCompliance model from Json" should {

    "complete successfully with culturalCompliance" in {
      val json = Json.parse(
        s"""
           |{
           |  "businessDescription":"some text",
           |  "culturalCompliance": {
           |    "notForProfit": true
           |   },
           |  "labourCompliance": {
           |    "labour": true,
           |    "workers": 10,
           |    "temporaryContracts": true,
           |    "skilledWorkers": true
           |   },
           |  "mainBusinessActivity": {
           |    "id": "88888888",
           |    "description": "description",
           |    "displayDetails": "displayDetails"
           |   }
           |   }
        """.stripMargin)

      val expected = VatSicAndCompliance(
        businessDescription = "some text",
        culturalCompliance = Some(VatComplianceCultural(true)),
        labourCompliance = Some(VatComplianceLabour(true, Some(10), Some(true), Some(true))),
        mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
      )

      Json.fromJson[VatSicAndCompliance](json) shouldBe JsSuccess(expected)
    }
  }

  "complete successfully without culturalCompliance and labourCompliance" in {
    val json = Json.parse(
      s"""
         |{
         |  "businessDescription":"some text",
         |  "mainBusinessActivity": {
         |    "id": "88888888",
         |    "description": "description",
         |    "displayDetails": "displayDetails"
         |   }
         |}
        """.stripMargin)

    val expected = VatSicAndCompliance(
      businessDescription = "some text",
      culturalCompliance = None,
      labourCompliance = None,
      mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
    )

    Json.fromJson[VatSicAndCompliance](json) shouldBe JsSuccess(expected)
  }

  "Creating a Json from a VatSicAndCompliance model" should {

    implicit val format = VatSicAndCompliance.format

    "complete successfully without culturalCompliance and labourCompliance" in {
      val sac = VatSicAndCompliance(
        businessDescription = "some text",
        culturalCompliance = None,
        labourCompliance = None,
        mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
      )

      val writeResult = format.writes(sac)
      val readResult = format.reads(Json.toJson(writeResult))
      val result = readResult.get

      result shouldBe sac
    }

    "complete successfully with culturalCompliance and labourCompliance" in {
      val sac = VatSicAndCompliance(
        businessDescription = "some text",
        culturalCompliance = Some(VatComplianceCultural(true)),
        labourCompliance = Some(VatComplianceLabour(true, Some(10), Some(true), Some(true))),
        mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
      )

      val writeResult = format.writes(sac)
      val readResult = format.reads(Json.toJson(writeResult))
      val result = readResult.get

      result shouldBe sac
    }

  }

}
