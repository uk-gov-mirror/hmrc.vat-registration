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

import helpers.VatRegSpec
import models.submission.DESSubmission
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.BeforeAndAfter
import play.api.libs.json.Writes
import uk.gov.hmrc.http.{HeaderCarrier, HttpReads, HttpResponse, Upstream4xxResponse}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

class DesConnectorSpec extends VatRegSpec with BeforeAndAfter {

  implicit val hc = HeaderCarrier()
  val realmockHttp = mock[WSHttp]

  class SetupWithProxy {
    val connector = new DESConnect {
      override val http = realmockHttp
      override val desStubURI      = "testStubURI"
      override val desStubUrl      = "desStubURL"
      override val urlHeaderEnvironment   = "env"
      override val urlHeaderAuthorization = "auth"
    }
  }

  val validDesSubmission = DESSubmission("AckRef", "CompanyName", LocalDate.of(2017, 1, 1), LocalDate.of(2017, 1, 1))
  val upstream4xx = Upstream4xxResponse("400", 400, 400)

  def mockHttpPOST[I, O](url: String, thenReturn: O): OngoingStubbing[Future[O]] = {
    when(realmockHttp.POST[I, O](ArgumentMatchers.contains(url), ArgumentMatchers.any[I](), ArgumentMatchers.any())
      (ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.successful(thenReturn))
  }

  def mockHttpFailedPOST[I, O](url: String, exception: Exception): OngoingStubbing[Future[O]] = {
    when(realmockHttp.POST[I, O](ArgumentMatchers.anyString(), ArgumentMatchers.any[I](), ArgumentMatchers.any())(ArgumentMatchers.any[Writes[I]](), ArgumentMatchers.any[HttpReads[O]](), ArgumentMatchers.any[HeaderCarrier](), ArgumentMatchers.any()))
      .thenReturn(Future.failed(exception))
  }


  "submitToDES with a DES Submission Model" should {
    "successfully POST" in new SetupWithProxy {

      mockHttpPOST[DESSubmission, HttpResponse](s"${connector.desStubUrl}/${connector.desStubURI}", HttpResponse(202))

      await(connector.submitToDES(validDesSubmission, "regId").status) shouldBe 202
    }

    "handle a failed POST" in new SetupWithProxy {
      mockHttpFailedPOST[DESSubmission, HttpResponse](s"${connector.desStubUrl}/${connector.desStubURI}", upstream4xx)

      intercept[Upstream4xxResponse](await(connector.submitToDES(validDesSubmission, "regId")))
    }
  }


}
