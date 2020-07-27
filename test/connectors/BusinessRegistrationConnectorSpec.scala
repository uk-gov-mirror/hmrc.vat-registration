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

import common.exceptions._
import fixtures.BusinessRegistrationFixture
import helpers.VatRegSpec
import models.external.CurrentProfile
import org.mockito.ArgumentMatchers
import org.mockito.Mockito._
import play.api.test.Helpers._
import uk.gov.hmrc.http.{ForbiddenException, HeaderCarrier, NotFoundException}
import uk.gov.hmrc.play.http.ws.WSHttp

import scala.concurrent.Future

class BusinessRegistrationConnectorSpec extends VatRegSpec with BusinessRegistrationFixture {

  trait Setup {
    val connector: BusinessRegistrationConnector = new BusinessRegistrationConnector(backendConfig, mockHttpClient) {
      override lazy val businessRegUrl: String = "testBusinessRegUrl"
    }
  }

  implicit val hc: HeaderCarrier = HeaderCarrier()

  "retrieveCurrentProfile" should {
    "return a a CurrentProfile response if one is found in business registration micro-service" in new Setup {
      mockHttpGet[CurrentProfile]("testUrl", validBusinessRegistrationResponse)

      await(connector.retrieveCurrentProfile) mustBe Right(validBusinessRegistrationResponse)
    }

    "return a Not Found response when a CurrentProfile record can not be found" in new Setup {
      when(mockHttpClient.GET[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new NotFoundException("Bad request")))

      await(connector.retrieveCurrentProfile) mustBe Left(ResourceNotFound("Bad request"))
    }

    "return a Forbidden response when a CurrentProfile record can not be accessed by the user" in new Setup {
      when(mockHttpClient.GET[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new ForbiddenException("Forbidden")))

      await(connector.retrieveCurrentProfile) mustBe Left(ForbiddenAccess("Forbidden"))
    }

    "return an Exception response when an unspecified error has occurred" in new Setup {
      val ex = new Exception("exception")
      when(mockHttpClient.GET[CurrentProfile](ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(ex))

      await(connector.retrieveCurrentProfile) mustBe Left(GenericError(ex))
    }
  }
}
