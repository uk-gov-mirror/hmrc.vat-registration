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

package connectors

import java.time.LocalDate

import common.TransactionId
import common.exceptions.{GenericError, ResourceNotFound}
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.Json
import uk.gov.hmrc.play.http.{HeaderCarrier, HttpResponse}

class IncorporationInformationConnectorSpec extends VatRegSpec with VatRegistrationFixture {

  trait Setup {
    val connector = new IncorporationInformationConnector {
      override val iiUrl = "anyUrl"
      override val http = mockWSHttp
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
           |      "incorporationDate":1470438000000,
           |      "timestamp":1501061996345
           |    }
           |  }
           |}
        """.stripMargin)
      val expectedIIStatus = incorporationStatus(incorpDate = LocalDate.of(2016, 8, 5))
      mockHttpPOST("anyUrl", HttpResponse(200, responseJson = Some(returnedFromII)))
      connector.retrieveIncorporationStatus(TransactionId("any")) returnsRight expectedIIStatus
    }

    "returns ResourceNotFound if incorporation status not found in II and a new subscription has been setup" in new Setup {
      val resNotFound = ResourceNotFound("Incorporation Status not known. A subscription has been setup")
      mockHttpPOST("anyUrl", HttpResponse(202))
      connector.retrieveIncorporationStatus(TransactionId("any")) returnsLeft resNotFound
    }

    "returns GenericError when failed to get incorporation status or setup a subscription" in new Setup {
      val exMessage = "400 response code returned requesting II for txId: any"
      mockHttpPOST("anyUrl", HttpResponse(400))
      inside(await(connector.retrieveIncorporationStatus(TransactionId("any")).value)) {
        case Left(GenericError(ex)) => ex.getMessage shouldBe exMessage
      }
    }
  }

  "IncorpStatusRequest" should {

    "be serialised to JSON" in {
      val expectedJson = """{"SCRSIncorpSubscription":{"callbackUrl":"someUrl"}}"""
      Json.toJson(IncorpStatusRequest("someUrl"))(IncorpStatusRequest.writes).toString() shouldBe expectedJson
    }
    
  }
}

