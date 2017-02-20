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

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class VatAccountingPeriodSpec extends UnitSpec with JsonFormatValidation {

  "Creating a VatAccountingPeriod model from Json" should {

    val periodStartDate = LocalDate.of(2017, 1, 1)

    "complete successfully from full Json with quarterly frequency" in {
      val json = Json.parse(
        s"""
           |{
           |  "periodStart":"$periodStartDate",
           |  "frequency":"quarterly"
           |}
        """.stripMargin)

      val tstVatAccountingPeriod = VatAccountingPeriod(
        periodStart = Some(periodStartDate),
        frequency = "quarterly"
      )

      Json.fromJson[VatAccountingPeriod](json) shouldBe JsSuccess(tstVatAccountingPeriod)
    }

    "complete successfully from full Json with monthly frequency" in {
      val json = Json.parse(
        s"""
           |{
           |  "periodStart":"$periodStartDate",
           |  "frequency":"monthly"
           |}
        """.stripMargin)

      val tstVatAccountingPeriod = VatAccountingPeriod(
        periodStart = Some(periodStartDate),
        frequency = "monthly"
      )

      Json.fromJson[VatAccountingPeriod](json) shouldBe JsSuccess(tstVatAccountingPeriod)
    }

    "fail from Json with invalid frequency" in {
      val json = Json.parse(
        s"""
           |{
           |  "periodStart":"$periodStartDate",
           |  "frequency":"yearly"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatAccountingPeriod](json)
      shouldHaveErrors(result, JsPath() \ "frequency", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with missing frequency" in {
      val json = Json.parse(
        s"""
           |{
           |  "periodStart":"$periodStartDate"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatAccountingPeriod](json)
      shouldHaveErrors(result, JsPath() \ "frequency", Seq(ValidationError("error.path.missing")))
    }


  }
}