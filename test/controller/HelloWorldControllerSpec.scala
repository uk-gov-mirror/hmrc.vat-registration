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

import controllers.HelloWorldController
import helpers.VatRegSpec
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future


class HelloWorldControllerSpec extends VatRegSpec {

  val testId = "testId"

  object TestController extends HelloWorldController {
    override val auth = mockAuthConnector
  }

  "GET /" should {

    "return 403" in {
      val result = HelloWorldController.hello(FakeRequest())
      status(result) shouldBe FORBIDDEN
    }

    "return 200" in {
      AuthorisationMocks.mockSuccessfulAuthorisation(testId, testAuthority(testId))
      val result: Future[Result] = TestController.hello()(FakeRequest())
      status(result) shouldBe OK
    }
  }

}
