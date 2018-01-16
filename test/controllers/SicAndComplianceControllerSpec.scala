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

package controllers

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.SicAndCompliance
import org.mockito.ArgumentMatchers.any
import org.mockito.stubbing.OngoingStubbing

import scala.concurrent.Future
import org.mockito.Mockito._
import play.api.libs.json.{JsBoolean, JsObject}
import play.api.test.FakeRequest
class SicAndComplianceControllerSpec extends VatRegSpec with VatRegistrationFixture {
  import fakeApplication.materializer
  class Setup {
    val controller = new SicAndComplianceControllerImpl(
      sicAndComplianceService = mockSicAndComplianceService,
      auth = mockAuthConnector
    )
  }
  def userIsAuthorised(): Unit = AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
  def userIsNotAuthorised(): Unit = AuthorisationMocks.mockNotLoggedInOrAuthorised()

  def mockGetSicAndComplianceFromService(res:Future[Option[SicAndCompliance]]):OngoingStubbing[Future[Option[SicAndCompliance]]] = when(mockSicAndComplianceService.getSicAndCompliance(any())(any())).thenReturn(res)

  def mockUpdateSickAndComplianceFromService(res:Future[SicAndCompliance]) :OngoingStubbing[Future[SicAndCompliance]] = when(mockSicAndComplianceService.updateSicAndCompliance(any(),any())(any())).thenReturn(res)

  "getSicAndCompliance" should {
    "return valid Json if record returned from service" in new Setup {
      userIsAuthorised()
      mockGetSicAndComplianceFromService(Future.successful(validSicAndCompliance))
      val result = controller.getSicAndCompliance("fooBarWizzBang")(FakeRequest())

      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validSicAndComplianceJson

    }
    "return 204 when nothing is returned but document exists" in new Setup {
      userIsAuthorised()
      mockGetSicAndComplianceFromService(Future.successful(None))
      val result = controller.getSicAndCompliance("fooBar")(FakeRequest())

      status(result) shouldBe 204
    }
    "returns 404 if none found" in new Setup {
      userIsAuthorised()
      mockGetSicAndComplianceFromService(Future.failed(MissingRegDocument(RegistrationId("foo"))))
      val result = controller.getSicAndCompliance("testId")(FakeRequest())

      status(result) shouldBe 404
    }
    "returns 403 if not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.getSicAndCompliance("testId")(FakeRequest())
      status(result) shouldBe 403
    }
  }
  "updateSicAndCompliance" should {
    "return 200 and the updated model as json when a record exists and the update is successful" in new Setup {
      userIsAuthorised()
      mockUpdateSickAndComplianceFromService(Future.successful(validSicAndCompliance.get))
      val result = controller.updateSicAndCompliance("fooBarWizz")(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validSicAndComplianceJson
    }
    "returns 404 if regId not found" in new Setup {
      userIsAuthorised()
      mockUpdateSickAndComplianceFromService(Future.failed(MissingRegDocument(RegistrationId("testId"))))
      val result = controller.updateSicAndCompliance("fooBarWizz")(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 404
    }
    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      mockUpdateSickAndComplianceFromService(Future.failed(new Exception))
      val result = controller.updateSicAndCompliance("fooBarWizz")(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 500
    }
    "returns 403 if the user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateSicAndCompliance("fooBarWizz")(FakeRequest().withBody[JsObject](validSicAndComplianceJson))
      status(result) shouldBe 403
    }
  }

}
