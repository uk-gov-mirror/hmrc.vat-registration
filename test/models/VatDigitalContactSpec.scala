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
import models.api.VatDigitalContact
import play.api.data.validation.ValidationError
import play.api.libs.json._

class VatDigitalContactSpec extends VatRegSpec with JsonFormatValidation {


  "Creating a VatDigitalContact model from Json" should {

    implicit val format = VatDigitalContact.format

    "complete successfully from full Json" in {
      val json = Json.parse(
        s"""
           |{
           |  "email":"test@test.com",
           |  "tel":"12345678910",
           |  "mobile":"12345678910"
           |}
        """.stripMargin)
      val tstVatDigitalContact = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))

      Json.fromJson[VatDigitalContact](json) shouldBe JsSuccess(tstVatDigitalContact)
    }

    "fail from Json with invalid char email" in {
      val json = Json.parse(
        s"""
           |{
           |  "email":"test%£@test.com",
           |  "tel":"12345678910",
           |  "mobile":"12345678910"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatDigitalContact](json)
      shouldHaveErrors(result, JsPath() \ "email", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with invalid email length" in {
      val json = Json.parse(
        s"""
           |{
           |  "email":"testtesttesttesttesttestesttesttesttesttesttettesttesttesttesttesttestteuutttttt@test.com",
           |  "tel":"12345678910",
           |  "mobile":"12345678910"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatDigitalContact](json)
      shouldHaveErrors(result, JsPath() \ "email", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with invalid Telephone" in {
      val json = Json.parse(
        s"""
           |{
           |  "email":"test@test.com",
           |  "tel":"ABC_12345678910",
           |  "mobile":"12345678910"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatDigitalContact](json)
      shouldHaveErrors(result, JsPath() \ "tel", Seq(ValidationError("error.pattern")))
    }

    "fail from Json with invalid Mobile" in {
      val json = Json.parse(
        s"""
           |{
           |  "email":"test@test.com",
           |  "tel":"12345678910",
           |  "mobile":"ABC_12345678910"
           |}
        """.stripMargin)

      val result = Json.fromJson[VatDigitalContact](json)
      shouldHaveErrors(result, JsPath() \ "mobile", Seq(ValidationError("error.pattern")))
    }

  }

}
