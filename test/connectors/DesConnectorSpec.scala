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

import config.BackendConfig
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.VatSubmission
import models.submission.{DESSubmission, UkCompany}
import org.mockito.ArgumentMatchers.{any, anyString, contains}
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.Configuration
import play.api.libs.json.Writes
import play.api.test.Helpers._
import uk.gov.hmrc.http._
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

import scala.concurrent.Future

class DesConnectorSpec extends PlaySpec with VatRegSpec with MockitoSugar with HttpErrorFunctions with VatRegistrationFixture {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  override val backendConfig: BackendConfig = new BackendConfig(mock[ServicesConfig], mock[Configuration]) {
    override lazy val desBaseUrl = "testDesUrl"
    override lazy val desStubTopUpUrl = "desStubTopUpURL"
    override lazy val desStubTopUpURI = "testStubTopUpURI"
    override lazy val urlHeaderEnvironment = "env"
    override lazy val urlHeaderAuthorization = "auth"
  }

  class SetupWithProxy {
    val connector: DESConnector = new DESConnector(backendConfig, mockHttpClient)
  }

  val validVatSubmission: VatSubmission = VatSubmission(
    "SubmissionCreate",
    Some(UkCompany),
    Some(true),
    Some("testCrn"),
    validApplicantDetails,
    Some(testBankAccount),
    testSicAndCompliance.get,
    testBusinessContact.get,
    validFullTradingDetails,
    Some(validFullFRSDetails),
    testEligibilitySubmissionData,
    testReturns
  )

  val upstream4xx: Upstream4xxResponse = Upstream4xxResponse("400", 400, 400)

  def mockHttpPOST[I, O](url: String, thenReturn: O): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.POST[I, O](contains(url), any[I](), any())
      (any[Writes[I]](), any[HttpReads[O]](), any[HeaderCarrier](), any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpFailedPOST[I, O](url: String, exception: Exception): OngoingStubbing[Future[O]] = {
    when(mockHttpClient.POST[I, O](anyString(), any[I](), any())
      (any[Writes[I]](), any[HttpReads[O]](), any[HeaderCarrier](), any()))
      .thenReturn(Future.failed(exception))
  }


  "submitToDES with a VAT Submission Model" should {
    "successfully POST" in new SetupWithProxy {
      mockHttpPOST[DESSubmission, HttpResponse](s"${connector.config.desUrl}", HttpResponse(202))

      await(connector.submitToDES(validVatSubmission, "regId")).status mustBe 202
    }

    "handle a failed POST" in new SetupWithProxy {
      mockHttpFailedPOST[DESSubmission, HttpResponse](s"${connector.config.desUrl}", upstream4xx)

      intercept[Upstream4xxResponse](await(connector.submitToDES(validVatSubmission, "regId")))
    }
  }

  "customDesRead" should {
    "successfully convert 409 from DES to 202" in new SetupWithProxy {
      val httpResponse: AnyRef with HttpResponse = HttpResponse(409)
      connector.customDESRead("test", "testUrl", httpResponse).status mustBe 202
    }
  }

  "successfully convert 499 from DES to 502" in new SetupWithProxy {
    val httpResponse: AnyRef with HttpResponse = HttpResponse(499)
    val ex: Upstream5xxResponse = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
    ex mustBe Upstream5xxResponse("Timeout received from DES submission", 499, 502)
  }

  "successfully convert 429 from DES to 503" in new SetupWithProxy {
    val httpResponse: AnyRef with HttpResponse = HttpResponse(429)
    val ex: Upstream5xxResponse = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
    ex mustBe Upstream5xxResponse("429 received fro DES - converted to 503", 429, 503)
  }

  "successfully convert 480 from DES to 400" in new SetupWithProxy {
    val httpResponse: AnyRef with HttpResponse = HttpResponse(480, responseString = Some("foo"))
    val ex: Upstream4xxResponse = intercept[Upstream4xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
    ex mustBe Upstream4xxResponse(upstreamResponseMessage("test", "testUrl", 480, "foo"), 480, 400)
  }

  "successfully convert 500 from DES to 502" in new SetupWithProxy {
    val httpResponse: AnyRef with HttpResponse = HttpResponse(500, responseString = Some("foo"))
    val ex: Upstream5xxResponse = intercept[Upstream5xxResponse](connector.customDESRead("test", "testUrl", httpResponse))
    ex mustBe Upstream5xxResponse(upstreamResponseMessage("test", "testUrl", 500, "foo"), 500, 502)
  }
}
