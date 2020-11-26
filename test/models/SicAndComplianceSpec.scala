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

package models

import helpers.BaseSpec
import models.api.{ComplianceLabour, SicAndCompliance, SicCode}
import play.api.libs.json.{JsValue, Json}

class SicAndComplianceSpec extends BaseSpec {

  lazy val businessActivities: List[SicCode] = List(
    SicCode("00998", "testDesc", "testDetails"),
    SicCode("12345", "testDesc", "testDetails"),
    SicCode("00889", "testDesc", "testDetails")
  )

  lazy val sicAndCompliance: SicAndCompliance = SicAndCompliance(
    "test business description",
    Some(ComplianceLabour(1000, Some(true), Some(true))),
    SicCode("12345", "testDesc", "testDetails"),
    businessActivities
  )

  val validFullSubmissionJson: JsValue = Json.parse(
    """
      |{
      | "subscription": {
      |   "businessActivities": {
      |     "SICCodes": {
      |       "primaryMainCode": "12345",
      |       "mainCode2": "00998",
      |       "mainCode3": "00889"
      |     },
      |     "description": "test business description"
      |   }
      | },
      | "compliance": {
      |   "numOfWorkers": 1000,
      |   "tempWorkers": true,
      |   "provisionOfLabour": true
      | }
      |}""".stripMargin)

  val validSubmissionJson: JsValue = Json.parse(
    """
      |{
      | "subscription": {
      |   "businessActivities": {
      |     "SICCodes": {
      |       "primaryMainCode": "12345"
      |     },
      |     "description": "test business description"
      |   }
      | }
      |}""".stripMargin)

  "Submission writes" must {
    "write successfully when there is more than 1 sic code and labour compliance" in {
      val res = SicAndCompliance.submissionWrites.writes(sicAndCompliance)

      res mustBe validFullSubmissionJson
    }

    "write successfully when there is just one sic code" in {
      val res = SicAndCompliance.submissionWrites.writes(sicAndCompliance.copy(
        labourCompliance = None,
        businessActivities = List(sicAndCompliance.mainBusinessActivity)
      ))

      res mustBe validSubmissionJson
    }
  }

}
