
package controllers

import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import itutil.{ITVatSubmissionFixture, IntegrationStubbing}
import models.api.VatScheme
import play.api.libs.json.Json
import play.api.libs.ws.WSResponse
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationControllerISpec extends IntegrationStubbing with FeatureSwitching with ITVatSubmissionFixture {

  class Setup extends SetupHelper

  "GET /new" should {
    "return CREATED if the daily quota has not been met" in new Setup {
      given
        .user.isAuthorised

      val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url)
        .post(Json.obj())
      )

      res.status mustBe CREATED
    }
  }

  "PUT /:regID/submit-registration" should {
    "return OK if the submission is successful with an unregistered business partner" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return OK if the submission is successful where the business partner is already registered" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner, repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return OK if the submission is successful where the business partner is already registered when the frs data is completely missing" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner.copy(flatRateScheme = None), repo.insert)

      stubPost("/vatreg/test-only/vat/subscription", testRegisteredBusinessPartnerSubmissionJson, OK, "")

      val res: WSResponse = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe OK
    }

    "return INTERNAL_SERVER_ERROR if the VAT scheme is missing data" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(VatScheme(testRegId, testInternalid, status = VatRegStatus.draft), repo.insert)
        .subscriptionApi.respondsWith(OK)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API is unavailable" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)
        .subscriptionApi.respondsWith(BAD_GATEWAY)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }

    "return INTERNAL_SERVER_ERROR if the subscription API returns BAD_REQUEST" in new Setup {
      enable(StubSubmission)

      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)
        .subscriptionApi.respondsWith(BAD_REQUEST)

      val res = await(client(controllers.routes.VatRegistrationController.submitVATRegistration(testRegId).url)
        .put(Json.obj())
      )

      res.status mustBe INTERNAL_SERVER_ERROR
    }
  }

  "PATCH  /:regId/honesty-declaration" should {
    "return Ok if the honesty declaration is successfully stored" in new Setup {
      given
        .user.isAuthorised

      insertIntoDb(testEmptyVatScheme("regId"))

      val res = await(client(controllers.routes.VatRegistrationController.storeHonestyDeclaration(testRegId).url)
      .patch(Json.obj("honestyDeclaration" -> true))
      )

      res.status mustBe OK
      await(repo.findAll()).head.confirmInformationDeclaration mustBe Some(true)
    }
  }

}
