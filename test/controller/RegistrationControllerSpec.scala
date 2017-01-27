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

package controller

import controllers.RegistrationController
import helpers.VatRegSpec
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future


class RegistrationControllerSpec extends VatRegSpec {

  val testId = "testId"

  class Setup {
    val controller = new RegistrationController {
      override val auth = mockAuthConnector
    }
  }

  "GET /" should {

    "return 403" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised
      val response: Future[Result] = controller.newVatRegistration(FakeRequest())
      status(response) shouldBe FORBIDDEN
    }

    "return 200" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testId, testAuthority(testId))
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe OK
    }

  }

}
