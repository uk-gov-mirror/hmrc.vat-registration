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

import play.api.data.validation.ValidationError
import play.api.libs.json.{JsPath, JsSuccess, Json}
import uk.gov.hmrc.play.test.UnitSpec

class VatBankAccountSpec extends UnitSpec with JsonFormatValidation {

  "Creating a VatBankAccount model from Json" should {
    "complete successfully from full Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountSortCode":"00-99-22",
           |  "accountNumber":"12345678"
           |}
        """.stripMargin)

      val tstVatBankAccount = VatBankAccount(
        accountName = "Test Account Name",
        accountSortCode = "00-99-22",
        accountNumber = "12345678"
      )

      Json.fromJson[VatBankAccount](json) shouldBe JsSuccess(tstVatBankAccount)
    }
  }
}