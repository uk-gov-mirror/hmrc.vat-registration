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

package connectors

import java.time.LocalDate

import common.{RegistrationId, TransactionId}
import common.exceptions.{GenericError, ResourceNotFound}
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers.OK
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException, Upstream4xxResponse}

class IncorporationInformationConnectorSpec extends VatRegSpec with VatRegistrationFixture {

  trait Setup {
    val connector = new IncorporationInformationConnector {
      override val iiUrl = "anyUrl"
      override val iiUri = "tst-url"
      override val http = mockWSHttp
      override val vatRegUri: String = "test"
    }
  }

  implicit val hc = HeaderCarrier()

  "retrieveIncorporationStatus" should {
    "return an IncorporationStatus if one is found in II" in new Setup {
      val returnedFromII = Json.parse(
        s"""
           |{
           |  "SCRSIncorpStatus":{
           |    "IncorpSubscriptionKey":{
           |      "subscriber":"scrs",
           |      "discriminator":"vat",
           |      "transactionId":"1"
           |    },
           |    "SCRSIncorpSubscription":{
           |      "callbackUrl":"callbackUrl"
           |    },
           |    "IncorpStatusEvent":{
           |      "status":"accepted",
           |      "crn":"CRN",
           |      "description": "description",
           |      "incorporationDate":1470351600000,
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)
      val expectedIIStatus = incorporationStatus(incorpDate = LocalDate.of(2016, 8, 5))
      mockHttpPOST("anyUrl", HttpResponse(200, responseJson = Some(returnedFromII)))
      await(connector.retrieveIncorporationStatus(TransactionId("any"), regime, subscriber)) shouldBe Some(expectedIIStatus)
    }

    "returns None if incorporation status not found in II and a new subscription has been setup" in new Setup {
      mockHttpPOST("anyUrl", HttpResponse(202))
      await(connector.retrieveIncorporationStatus(TransactionId("any"), regime, subscriber)) shouldBe None
    }

    "returns an exception when failed to get incorporation status or setup a subscription" in new Setup {
      val exMessage = "400 response code returned requesting II for txId: any"
      mockHttpPOST("anyUrl", HttpResponse(400))

      intercept[IncorporationInformationResponseException](await(
        connector.retrieveIncorporationStatus(TransactionId("any"), regime, subscriber)
      ))
    }
  }

  "getCompanyName" should {
    "return the company name if one is found in II" in new Setup {
      val returnedFromII = Json.parse(
        s"""
           |{
           |  "company-name":"test"
           |}
        """.stripMargin)

      mockHttpGet("anyUrl", HttpResponse(200, responseJson = Some(returnedFromII)))
      val res = await(connector.getCompanyName(regId, TransactionId("any")))
      res.json shouldBe returnedFromII
      res.status shouldBe OK
    }

    "throw an exception if the company could not be found" in new Setup {
      val upstream4xx = new NotFoundException("Bad request")
      mockHttpFailedGET("anyUrl", upstream4xx)

      intercept[NotFoundException](await(
        await(connector.getCompanyName(regId, TransactionId("any")))
      ))
    }
  }

  "IncorpStatusRequest" should {

    "be serialised to JSON" in {
      val expectedJson = """{"SCRSIncorpSubscription":{"callbackUrl":"someUrl/vatreg/incorporation-data"}}"""
      Json.toJson(IncorpStatusRequest("someUrl"))(IncorpStatusRequest.writes).toString() shouldBe expectedJson
    }
    
  }
}
