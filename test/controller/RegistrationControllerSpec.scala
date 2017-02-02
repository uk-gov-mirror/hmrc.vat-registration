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

import akka.util.ByteString
import common.Now
import common.exceptions.ForbiddenException
import connectors.BusinessRegistrationForbiddenResponse
import controllers.VatRegistrationController
import helpers.VatRegSpec
import models.{VatChoice, VatScheme}
import org.joda.time.DateTime
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.libs.streams.Accumulator
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._
import uk.gov.hmrc.play.http.HeaderCarrier
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer

import scala.concurrent.Future


class RegistrationControllerSpec extends VatRegSpec {

  val testId = "testId"


  class Setup {
    val controller = new VatRegistrationController(mockAuthConnector, mockRegistrationService)
  }

  "GET /" should {

    "return 403" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised
      val response: Future[Result] = controller.newVatRegistration(FakeRequest())
      status(response) shouldBe FORBIDDEN
    }

    "return 201" in new Setup {
      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      ServiceMocks.mockSuccessfulCreateNewRegistration(testId)
      val response: Future[Result] = controller.newVatRegistration()(FakeRequest())
      status(response) shouldBe CREATED
    }

    "call updateVatChoice return CREATED" in new Setup {

      implicit val actorSystem: ActorSystem = ActorSystem()
      implicit val materializer = ActorMaterializer()

      AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(testId))
      //ServiceMocks.mockSuccessfulUpdateVatChoice(testId)

      when(mockRegistrationService.updateVatChoice(Matchers.any(), Matchers.any())(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Right(VatChoice.blank(new DateTime(2017, 1, 31, 13, 6)))))

      val response: Accumulator[ByteString, Result] = controller.updateVatChoice(testId)(FakeRequest())
      status(response.run) shouldBe CREATED
    }
  }
}
