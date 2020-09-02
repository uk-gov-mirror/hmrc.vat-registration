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
import models.api.{Address, VatSubmission}
import play.api.libs.json.{JsSuccess, JsValue, Json}

class VatSubmissionSpec extends BaseSpec with JsonFormatValidation {

  val testMessageType = "SubmissionCreate"
  val testCustomerStatus = "3"
  val testTradersPartyType = "50"
  val testSafeID = "12345678901234567890"
  val testLine1 = "line1"
  val testLine2 = "line2"
  val testPostCode = "A11 11A"
  val testCountry = "GB"
  val testAddress: Address = Address(
    line1 = testLine1,
    line2 = testLine2,
    postcode = Some(testPostCode),
    country = Some(testCountry)
  )
  val testVatSubmission: VatSubmission = VatSubmission(
    testMessageType,
    Some(testCustomerStatus),
    Some(testTradersPartyType),
    Some(testSafeID),
    Some(testAddress),
    Some(true)
  )

  val submissionJson: JsValue = Json.obj(
    "messageType" -> testMessageType,
    "admin" -> Json.obj(
      "additionalInformation" -> Json.obj(
        "customerStatus" -> testCustomerStatus
      )
    ),
    "customerIdentification" -> Json.obj(
      "tradersPartyType" -> testTradersPartyType,
      "primeBPSafeId" -> testSafeID
    ),
    "contact" -> Json.obj(
      "address" -> Json.obj(
        "line1" -> testLine1,
        "line2" -> testLine2,
        "postCode" -> testPostCode,
        "countryCode" -> testCountry
      )
    ),
    "declaration" -> Json.obj(
      "declarationSigning" -> Json.obj(
        "confirmInformationDeclaration" -> true
      )
    )
  )

  val mongoJson: JsValue = Json.obj(
    "messageType" -> testMessageType,
    "customerStatus" -> testCustomerStatus,
    "tradersPartyType" -> testTradersPartyType,
    "primeBPSafeId" -> testSafeID,
    "address" -> testAddress,
    "confirmInformationDeclaration" -> true
  )

  "converting a VatSubmission model into Json" should {
    "produce a valid Json for a DES submission" in {
      val json = Json.toJson(testVatSubmission)(VatSubmission.submissionFormat)

      json mustBe submissionJson
    }

    "produce a Json to store it in a Mongo DB" in {
      val json = Json.toJson(testVatSubmission)

      json mustBe mongoJson
    }
  }

  "converting a Json from the Mongo DB" should {
    "produce a valid VatSubmission model" in {
      val model = Json.fromJson[VatSubmission](mongoJson)

      model mustBe JsSuccess(testVatSubmission)
    }
  }
}
