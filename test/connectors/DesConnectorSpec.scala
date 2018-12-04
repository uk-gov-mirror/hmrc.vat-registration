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

import models.submission.{DESSubmission, TopUpSubmission}
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers.{any, anyString, contains}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Writes
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.http.ws.WSHttp
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class DesConnectorSpec extends UnitSpec with MockitoSugar with HttpErrorFunctions {

  implicit val hc = HeaderCarrier()
  val realmockHttp = mock[WSHttp]

  class SetupWithProxy {
    val connector = new DESConnector {
      override val http = realmockHttp
      override val desStubURI      = "testStubURI"
      override val desStubUrl      = "desStubURL"
      override val desStubTopUpUrl = "desStubTopUpURL"
      override val desStubTopUpURI = "testStubTopUpURI"
      override val urlHeaderEnvironment   = "env"
      override val urlHeaderAuthorization = "auth"
    }
  }

  val validDesSubmission = DESSubmission("AckRef", "CompanyName", Some(LocalDate.of(2017, 1, 1)), Some(LocalDate.of(2017, 1, 1)))
  val validTopUpAcceptedSubmission = TopUpSubmission("AckRef", "accepted", Some(LocalDate.of(2017, 1, 1)), Some(DateTime.now()))
  val validTopUpRejectedSubmission = TopUpSubmission("AckRef", "rejected")
  val upstream4xx = Upstream4xxResponse("400", 400, 400)

  def mockHttpPOST[I, O](url: String, thenReturn: O): OngoingStubbing[Future[O]] = {
    when(realmockHttp.POST[I, O](contains(url), any[I](), any())
      (any[Writes[I]](), any[HttpReads[O]](), any[HeaderCarrier](), any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpFailedPOST[I, O](url: String, exception: Exception): OngoingStubbing[Future[O]] = {
    when(realmockHttp.POST[I, O](anyString(), any[I](), any())
      (any[Writes[I]](), any[HttpReads[O]](), any[HeaderCarrier](), any()))
      .thenReturn(Future.failed(exception))
  }


  "submitToDES with a DES Submission Model" should {
    "successfully POST" in new SetupWithProxy {
      mockHttpPOST[DESSubmission, HttpResponse](s"${connector.desStubUrl}/${connector.desStubURI}", HttpResponse(202))

      connector.submitToDES(validDesSubmission, "regId").status shouldBe 202
    }

    "handle a failed POST" in new SetupWithProxy {
      mockHttpFailedPOST[DESSubmission, HttpResponse](s"${connector.desStubUrl}/${connector.desStubURI}", upstream4xx)

      intercept[Upstream4xxResponse](await(connector.submitToDES(validDesSubmission, "regId")))
    }
  }

  "submitTopUpDES" should {
    "successfully POST with an accepted DES TopUpSubmission Model" in new SetupWithProxy {
      mockHttpPOST[TopUpSubmission, HttpResponse](s"${connector.desStubTopUpUrl}/${connector.desStubTopUpURI}", HttpResponse(202))
      await(connector.submitTopUpToDES(validTopUpAcceptedSubmission, "regId"))
    }

    "successfully POST with a rejected DES TopUpSubmission Model" in new SetupWithProxy {
      mockHttpPOST[TopUpSubmission, HttpResponse](s"${connector.desStubTopUpUrl}/${connector.desStubTopUpURI}", HttpResponse(202))
      await(connector.submitTopUpToDES(validTopUpRejectedSubmission, "regId"))
    }

    "handle a failed POST" in new SetupWithProxy {
      mockHttpFailedPOST[TopUpSubmission, HttpResponse](s"${connector.desStubTopUpUrl}/${connector.desStubTopUpURI}", upstream4xx)
      intercept[Upstream4xxResponse](await(connector.submitTopUpToDES(validTopUpAcceptedSubmission, "regId")))
    }
  }


  "customDesRead" should {
    "successfully convert 409 from DES to 202" in new SetupWithProxy {
      val httpResponse = HttpResponse(409)
      connector.customDESRead("test","testUrl",httpResponse).status shouldBe 202
    }
  }

    "successfully convert 499 from DES to 502" in new SetupWithProxy {
      val httpResponse = HttpResponse(499)
      val ex = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
      ex shouldBe Upstream5xxResponse("Timeout received from DES submission", 499, 502)
    }

    "successfully convert 429 from DES to 503" in new SetupWithProxy {
      val httpResponse = HttpResponse(429)
      val ex = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
      ex shouldBe Upstream5xxResponse("429 received fro DES - converted to 503", 429, 503)
    }

    "successfully convert 480 from DES to 400" in new SetupWithProxy {
      val httpResponse = HttpResponse(480, responseString = Some("foo"))
      val ex = intercept[Upstream4xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
      ex shouldBe Upstream4xxResponse(upstreamResponseMessage("test", "testUrl", 480, "foo"), 480, 400)
    }

    "successfully convert 500 from DES to 502" in new SetupWithProxy {
      val httpResponse = HttpResponse(500, responseString = Some("foo"))
      val ex = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
      ex shouldBe Upstream5xxResponse(upstreamResponseMessage("test", "testUrl", 500, "foo"), 500, 502)
    }
}
