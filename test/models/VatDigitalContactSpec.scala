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

import helpers.VatRegSpec
import models.api.DigitalContact
import play.api.data.validation.ValidationError
import play.api.libs.json._

class VatDigitalContactSpec extends VatRegSpec with JsonFormatValidation {


  "Creating a VatDigitalContact model from Json" should {

    implicit val format = DigitalContact.format

    "complete successfully" when {
      "from full Json" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test@test.com",
             |  "tel":"12345678910",
             |  "mobile":"12345678910"
             |}
        """.stripMargin)
        val tstVatDigitalContact = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))

        Json.fromJson[DigitalContact](json) shouldBe JsSuccess(tstVatDigitalContact)
      }
    }

    "fail to read" when {
      "Json with invalid char email" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test%£@test.com",
             |  "tel":"12345678910",
             |  "mobile":"12345678910"
             |}
        """.stripMargin)

        val result = Json.fromJson[DigitalContact](json)
        result shouldHaveErrors (JsPath() \ "email" -> ValidationError("error.email"))
      }

      "Json with invalid email length" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"testtesttesttesttesttestesttesttesttestteyyyysttettesttesttesttesttesttestteuutttttt@test.com",
             |  "tel":"12345678910",
             |  "mobile":"12345678910"
             |}
        """.stripMargin)

        val result = Json.fromJson[DigitalContact](json)
        result shouldHaveErrors (JsPath() \ "email" -> ValidationError("error.maxLength", 70))
      }

      "Json with invalid Telephone" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test@test.com",
             |  "tel":"ABC_12345678910",
             |  "mobile":"12345678910"
             |}
        """.stripMargin)

        val result = Json.fromJson[DigitalContact](json)
        result shouldHaveErrors (JsPath() \ "tel" -> ValidationError("error.pattern"))
      }

      "Json with invalid Mobile" in {
        val json = Json.parse(
          s"""
             |{
             |  "email":"test@test.com",
             |  "tel":"12345678910",
             |  "mobile":"ABC_12345678910"
             |}
        """.stripMargin)

        val result = Json.fromJson[DigitalContact](json)
        result shouldHaveErrors (JsPath() \ "mobile" -> ValidationError("error.pattern"))
      }
    }
  }
}
