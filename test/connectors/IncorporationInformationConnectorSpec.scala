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

package connectors

import java.time.LocalDate

import common.TransactionId
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.external.IncorporationStatus
import org.apache.http.client.HttpClient
import play.api.libs.json.{JsValue, Json}
import play.api.test.Helpers._
import uk.gov.hmrc.http.{HeaderCarrier, HttpResponse, NotFoundException}
import uk.gov.hmrc.play.http.ws.WSHttp

class IncorporationInformationConnectorSpec extends VatRegSpec with VatRegistrationFixture {

  trait Setup {
    val connector: IncorporationInformationConnector = new IncorporationInformationConnector(backendConfig, mockHttpClient) {
      override lazy val iiUrl = "anyUrl"
      override lazy val iiUri = "tst-url"
      override lazy val vatRegUri: String = "test"
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "retrieveIncorporationStatus" should {
    "return an IncorporationStatus if one is found in II" in new Setup {
      val returnedFromII: JsValue = Json.parse(
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
      val expectedIIStatus: IncorporationStatus = incorporationStatus(incorpDate = LocalDate.of(2016, 8, 5))
      mockHttpPOST("anyUrl", HttpResponse(200, responseJson = Some(returnedFromII)))
      await(connector.retrieveIncorporationStatus(Some("any"),TransactionId("any"), regime, subscriber)) mustBe Some(expectedIIStatus)
    }

    "returns None if incorporation status not found in II and a new subscription has been setup" in new Setup {
      mockHttpPOST("anyUrl", HttpResponse(202))
      await(connector.retrieveIncorporationStatus(Some("any"),TransactionId("any"), regime, subscriber)) mustBe None
    }

    "returns an exception when failed to get incorporation status or setup a subscription" in new Setup {
      val exMessage = "400 response code returned requesting II for txId: any"
      mockHttpPOST("anyUrl", HttpResponse(400))

      intercept[IncorporationInformationResponseException](await(
        connector.retrieveIncorporationStatus(Some("any"),TransactionId("any"), regime, subscriber)
      ))
    }
  }

  "getCompanyName" should {
    "return the company name if one is found in II" in new Setup {
      val returnedFromII: JsValue = Json.parse(
        s"""
           |{
           |  "company-name":"test"
           |}
        """.stripMargin)

      mockHttpGet("anyUrl", HttpResponse(200, responseJson = Some(returnedFromII)))
      val res: HttpResponse = await(connector.getCompanyName(regId, TransactionId("any")))
      res.json mustBe returnedFromII
      res.status mustBe OK
    }

    "throw an exception if the company could not be found" in new Setup {
      val upstream4xx = new NotFoundException("Bad request")
      mockHttpFailedGET("anyUrl", upstream4xx)

      intercept[NotFoundException](
        await(connector.getCompanyName(regId, TransactionId("any")))
      )
    }
  }

  "IncorpStatusRequest" should {

    "be serialised to JSON" in {
      val expectedJson = """{"SCRSIncorpSubscription":{"callbackUrl":"someUrl/vatreg/incorporation-data"}}"""
      Json.toJson(IncorpStatusRequest("someUrl"))(IncorpStatusRequest.writes).toString() mustBe expectedJson
    }
    
  }
}
