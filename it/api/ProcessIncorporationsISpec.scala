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

package api

import common.RegistrationId
import connectors.stubs.AuditStub._
import connectors.stubs.BusinessRegConnectorStub._
import controllers.routes.ProcessIncorporationsController
import enums.VatRegStatus
import itutil.IntegrationStubbing
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ProcessIncorporationsISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  "/incorporation-data" should {
    val transactionId: String = "transId"
    val incorpDate: DateTime = DateTime.now
    val registrationID = "regId"
    val regIDCase = RegistrationId(registrationID)

    def prepareHeldSubmission(repo : RegistrationMongoRepository): Future[Unit] = {
      for {
        _    <- repo.createNewVatScheme(regIDCase,internalid)
        _    <- repo.updateReturns(registrationID, returns)
        _    <- repo.saveTransId(transactionId, regIDCase)
        _    <- repo.finishRegistrationSubmission(regIDCase, VatRegStatus.held)
      } yield {
        ()
      }
    }

    def topUpJson(status: String): JsObject = Json.obj(
      "SCRSIncorpStatus" -> Json.obj(
        "IncorpSubscriptionKey" -> Json.obj(
          "subscriber" -> "scrs",
          "discriminator" -> "vat",
          "transactionId" -> "transId"
        ),
        "SCRSIncorpSubscription" -> Json.obj(
          "callbackUrl" -> "http://localhost:9896/TODO-CHANGE-THIS"
        ),
        "IncorpStatusEvent" -> Json.obj(
          "status" -> status,
          "timestamp" -> 1501061996345L
        )
      )
    )

    "return an Ok if the accepted top up succeeds" in new Setup() {
      stubBusinessRegVat(ACCEPTED)()
      stubMergedAudit(OK)
      stubAudit(OK)

      await(prepareHeldSubmission(repo))

      val result = await(client(ProcessIncorporationsController.processIncorp().url)
        .post(topUpJson("accepted")))

      result.status shouldBe OK

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.submitted

      await(repo.remove("registrationId" -> registrationID))
    }

    "return an Ok if the rejected top up succeeds" in new Setup() {
      stubBusinessRegVat(ACCEPTED)()
      stubMergedAudit(OK)
      stubAudit(OK)

      await(prepareHeldSubmission(repo))

      val result = await(client(ProcessIncorporationsController.processIncorp().url)
        .post(topUpJson("rejected")))

      result.status shouldBe OK

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.rejected

      await(repo.remove("registrationId" -> registrationID))
    }

    "return a 400 status when DES returns a 4xx" in new Setup() {
      stubBusinessRegVat(EXPECTATION_FAILED)()
      stubMergedAudit(OK)
      stubAudit(OK)

      await(prepareHeldSubmission(repo))

      val result = await(client(ProcessIncorporationsController.processIncorp().url)
        .post(topUpJson("accepted")))

      result.status shouldBe BAD_REQUEST

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.held

      await(repo.remove("registrationId" -> registrationID))
    }

    "return a SERVICE_UNAVAILABLE status when DES returns TOO_MANY_REQUESTS" in new Setup() {
      stubBusinessRegVat(TOO_MANY_REQUESTS)()
      stubMergedAudit(OK)
      stubAudit(OK)

      await(prepareHeldSubmission(repo))

      val result = await(client(ProcessIncorporationsController.processIncorp().url)
        .post(topUpJson("accepted")))

      result.status shouldBe SERVICE_UNAVAILABLE

      val reg = await(repo.retrieveVatScheme(RegistrationId(registrationID)))
      reg.get.status shouldBe VatRegStatus.held

      await(repo.remove("registrationId" -> registrationID))
    }
  }

}
