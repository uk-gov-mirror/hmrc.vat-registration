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
import models.api.BusinessContact
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.JsObject
import play.api.test.FakeRequest
import services.BusinessContactService

import scala.concurrent.Future

class BusinessContactControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

  class Setup {
    val controller = new BusinessContactControllerImpl(
      businessContactService = mockBusinessContactService,
      auth = mockAuthConnector
    )
  }
    def userIsAuthorised(): Unit = AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
    def userIsNotAuthorised(): Unit = AuthorisationMocks.mockNotLoggedInOrAuthorised()

    def mockGetBusinessContactFromService(res:Future[Option[BusinessContact]]):OngoingStubbing[Future[Option[BusinessContact]]] = when(mockBusinessContactService.getBusinessContact(any())(any())).thenReturn(res)

    def mockUpdateBusinessContactToSoService(res:Future[BusinessContact]) :OngoingStubbing[Future[BusinessContact]] = when(mockBusinessContactService.updateBusinessContact(any(),any())(any())).thenReturn(res)


  "getBusinessContacty" should {
    "return valid Json if record returned from service" in new Setup {
      userIsAuthorised()
      mockGetBusinessContactFromService(Future.successful(validBusinessContact))
      val result = controller.getBusinessContact("fooBarWizzBang")(FakeRequest())

      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validBusinessContactJson

    }
    "return 204 when nothing is returned but document exists" in new Setup {
      userIsAuthorised()
      mockGetBusinessContactFromService(Future.successful(None))
      val result = controller.getBusinessContact("fooBar")(FakeRequest())

      status(result) shouldBe 204
    }
    "returns 404 if none found" in new Setup {
      userIsAuthorised()
      mockGetBusinessContactFromService(Future.failed(MissingRegDocument(RegistrationId("foo"))))
      val result = controller.getBusinessContact("testId")(FakeRequest())

      status(result) shouldBe 404
    }
    "returns 403 if not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.getBusinessContact("testId")(FakeRequest())
      status(result) shouldBe 403
    }
  }
  "updateBusinessContact" should {
    "return 200 and the updated model as json when a record exists and the update is successful" in new Setup {
      userIsAuthorised()
      mockUpdateBusinessContactToSoService(Future.successful(validBusinessContact.get))
      val result = controller.updateBusinessContact("fooBarWizz")(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 200
      jsonBodyOf(await(result)) shouldBe validBusinessContactJson
    }
    "returns 404 if regId not found" in new Setup {
      userIsAuthorised()
      mockUpdateBusinessContactToSoService(Future.failed(MissingRegDocument(RegistrationId("testId"))))
      val result = controller.updateBusinessContact("fooBarWizz")(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 404
    }
    "returns 500 if an error occurs" in new Setup {
      userIsAuthorised()
      mockUpdateBusinessContactToSoService(Future.failed(new Exception))
      val result = controller.updateBusinessContact("fooBarWizz")(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 500
    }
    "returns 403 if the user is not authorised" in new Setup {
      userIsNotAuthorised()
      val result = controller.updateBusinessContact("fooBarWizz")(FakeRequest().withBody[JsObject](validBusinessContactJson))
      status(result) shouldBe 403
    }
  }
}
