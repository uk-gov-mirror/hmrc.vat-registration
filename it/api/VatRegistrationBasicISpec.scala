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

package api

import common.RegistrationId
import connectors.stubs.BusinessRegConnectorStub._
import connectors.stubs.VatSubmissionStub._
import controllers.routes.VatRegistrationController
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import itutil.IntegrationStubbing
import models.api.VatScheme
import models.external.CurrentProfile
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationBasicISpec extends IntegrationStubbing with FeatureSwitching {

  class Setup extends SetupHelper

  val validBusinessRegistrationResponse: CurrentProfile = CurrentProfile(
    "12345",
    Some("director"),
    "ENG"
  )

  "/vatreg/new (create registration)" when {
    "the user is authorised" should {
      "Return CREATED" in new Setup {
        given.user.isAuthorised
        stubBusinessReg(OK)(Some(validBusinessRegistrationResponse))

        val res: WSResponse = await(client(VatRegistrationController.newVatRegistration().url).post("test"))

        res.status mustBe CREATED
        //TODO - test body by injecting journey id generator
        //        res.json mustBe Json.obj(
        //          "registrationId" -> "12345",
        //          "internalId" -> "INT-123-456-789",
        //          "status" -> "draft"
        //        )
      }
      "Return NOT FOUND if the registration is missing" in new Setup {
        given.user.isAuthorised
        stubBusinessReg(NOT_FOUND)()

        val res: WSResponse = await(client(s"/12345").post("test"))

        res.status mustBe NOT_FOUND
      }
    }
    "the user is not authorised" should {
      "Return FORBIDDEN" in new Setup {
        given.user.isNotAuthorised
        stubBusinessReg(OK)(Some(validBusinessRegistrationResponse))

        val res: WSResponse = await(client(VatRegistrationController.newVatRegistration().url).post("test"))

        res.status mustBe FORBIDDEN
      }
    }
  }

  "/vatreg/:regId/submit-registration (submit registration)" should {
    val registrationID = "testRegId"
    val regime = "vat"
    val subscriber = "scrs"

    "return an Ok if the submission is successful for the regID" in new Setup() {
      disable(StubSubmission)
      given.user.isAuthorised
      stubVatSubmission(ACCEPTED)()

      repo.createNewVatScheme(RegistrationId(registrationID), internalid).flatMap(_ => repo.updateReturns(registrationID, returns))

      val result: WSResponse = await(client(
        VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )

      result.status mustBe OK

      val reg: Option[VatScheme] = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status mustBe VatRegStatus.submitted

      await(repo.remove("registrationId" -> registrationID))
    }

    "mock the return if the stub submission flag is on" in new Setup {
      enable(StubSubmission)
      given.user.isAuthorised
      stubVatSubmission(ACCEPTED)()

      repo.createNewVatScheme(RegistrationId(registrationID), internalid).flatMap(_ => repo.updateReturns(registrationID, returns))

      val result: WSResponse = await(client(
        VatRegistrationController.submitVATRegistration(RegistrationId(registrationID)).url).put("")
      )

      result.status mustBe OK
    }
  }
}
