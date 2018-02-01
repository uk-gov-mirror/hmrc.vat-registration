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

import auth.CryptoImpl
import helpers.VatRegSpec
import models.api.{BankAccount, BankAccountDetails, BankAccountMongoFormat}
import play.api.Configuration
import play.api.data.validation.ValidationError
import play.api.libs.json._
import org.mockito.Mockito._
import org.mockito.ArgumentMatchers.{any, eq => eqTo}

class BankAccountSpec extends VatRegSpec with JsonFormatValidation {

  "Creating a BankAccount model from Json" should {

    implicit val format: OFormat[BankAccount] = BankAccount.format

    "complete successfully from full Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "name":"Test Account name",
           |    "sortCode":"00-99-22",
           |    "number":"12345678"
           |  }
           |}
        """.stripMargin)

      val bankAccount = BankAccount(
        isProvided = true,
        details = Some(BankAccountDetails(
          name = "Test Account name",
          sortCode = "00-99-22",
          number = "12345678"
        ))
      )

      Json.fromJson[BankAccount](json) shouldBe JsSuccess(bankAccount)
    }

    "fail from Json with invalid  number" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "name":"Test Account name",
           |    "sortCode":"00-99-22",
           |    "number":"123456789"
           |  }
           |}
        """.stripMargin)

      val result = Json.fromJson[BankAccount](json)
      result shouldHaveErrors (__ \ "details" \ "number" -> ValidationError("error.pattern"))
    }

    "fail from Json with invalid sort code" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "name":"Test Account name",
           |    "sortCode":"00-993-22",
           |    "number":"12345678"
           |  }
           |}
        """.stripMargin)

      val result = Json.fromJson[BankAccount](json)
      result shouldHaveErrors (__ \ "details" \ "sortCode" -> ValidationError("error.pattern"))
    }

    "fail from Json with missing  name" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "sortCode":"00-99-22",
           |    "number":"12345678"
           |  }
           |}
        """.stripMargin)

      val result = Json.fromJson[BankAccount](json)
      result shouldHaveErrors (__ \ "details" \ "name" -> ValidationError("error.path.missing"))
    }

    "fail from Json with missing  number" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "name":"Test Account name",
           |    "sortCode":"00-99-22"
           |  }
           |}
        """.stripMargin)

      val result = Json.fromJson[BankAccount](json)
      result shouldHaveErrors (__ \ "details" \ "number" -> ValidationError("error.path.missing"))
    }

    "fail from Json with missing sort code" in {
      val json = Json.parse(
        s"""
           |{
           |  "isProvided":true,
           |  "details":{
           |    "name":"Test Account name",
           |    "number":"12345678"
           |  }
           |}
        """.stripMargin)

      val result = Json.fromJson[BankAccount](json)
      result shouldHaveErrors (__ \ "details" \ "sortCode" -> ValidationError("error.path.missing"))
    }
  }

  "The BankAccount encryption formatter" should {

    val testEncryptionKey = "ABCDEFGHIJKLMNOPQRSTUV=="

    val mockConfig = mock[Configuration]
    val crypto = new CryptoImpl(mockConfig)

    when(mockConfig.getString(eqTo("mongo-encryption.key"), any())).thenReturn(Some(testEncryptionKey))
    when(mockConfig.getStringSeq(any())).thenReturn(None)

    val encryptionFormat: OFormat[BankAccount] = BankAccountMongoFormat.encryptedFormat(crypto)

    val bankAccount = BankAccount(
      isProvided = true,
      Some(BankAccountDetails(
        name = "Test Account name",
        sortCode = "00-99-22",
        number = "12345678"
      ))
    )

    val encryptedJson = Json.parse(
      """
        |{
        | "isProvided":true,
        | "details":{
        |   "name":"Test Account name",
        |   "sortCode":"00-99-22",
        |   "number":"bzVwZ3hOcjhreHVFby9ZYk5RYUllUT09"
        | }
        |}
      """.stripMargin)

    "write from a BankAccount case class to a correct Json representation with an encrypted account number" in {
      val writeResult = Json.toJson(bankAccount)(encryptionFormat)
      writeResult shouldBe encryptedJson
    }

    "read from a Json object with an encrypted account number to a correct BankAccount case class" in {
      val readResult = Json.fromJson(encryptedJson)(encryptionFormat).get
      readResult shouldBe bankAccount
    }
  }
}
