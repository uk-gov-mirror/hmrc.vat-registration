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

import helpers.VatRegSpec
import play.api.data.validation.ValidationError
import play.api.libs.json._

class VatBankAccountSpec extends VatRegSpec with JsonFormatValidation {


  "Creating a VatBankAccount model from Json" should {

    implicit val format = VatBankAccount.format

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

    "fail from Json with invalid account number" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountSortCode":"00-99-22",
           |  "accountNumber":"123456789"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatBankAccount](json)
      shouldHaveErrors(result, JsPath() \ "accountNumber", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with invalid sort code" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountSortCode":"00-993-22",
           |  "accountNumber":"12345678"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatBankAccount](json)
      shouldHaveErrors(result, JsPath() \ "accountSortCode", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with missing account name" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountSortCode":"00-99-22",
           |  "accountNumber":"12345678"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatBankAccount](json)
      shouldHaveErrors(result, JsPath() \ "accountName", Seq(ValidationError("error.path.missing")))
    }

    "fail from Json with missing account number" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountSortCode":"00-99-22"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatBankAccount](json)
      shouldHaveErrors(result, JsPath() \ "accountNumber", Seq(ValidationError("error.path.missing")))
    }

    "fail from Json with missing sort code" in {
      val json = Json.parse(
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountNumber":"12345678"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatBankAccount](json)
      shouldHaveErrors(result, JsPath() \ "accountSortCode", Seq(ValidationError("error.path.missing")))
    }

  }


  "Creating a VatBankAccountMongoFormat model from Json" should {

    implicit val mongoFormat = VatBankAccountMongoFormat.encryptedFormat

    "complete successfully from full Json" in {
      val jsonValue =
        s"""
           |{
           |  "accountName":"Test Account Name",
           |  "accountSortCode":"00-99-22",
           |  "accountNumber":"12345678"
           |}
                      """

      val tstVatBankAccount = VatBankAccount(
        accountName = "Test Account Name",
        accountSortCode = "00-99-22",
        accountNumber = "12345678"
      )

      val writeResult = mongoFormat.writes(tstVatBankAccount)
      val readResult = mongoFormat.reads(Json.toJson(writeResult))
      val result: VatBankAccount = readResult.get

      result shouldBe tstVatBankAccount

    }

  }
}