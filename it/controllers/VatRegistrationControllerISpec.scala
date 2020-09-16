
package controllers

import java.time.LocalDate

import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import itutil.IntegrationStubbing
import models.api.DailyQuota
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class VatRegistrationControllerISpec extends IntegrationStubbing with FeatureSwitching {

  class Setup extends SetupHelper

  "GET /new" when {
    "the TrafficManagement feature switch is enabled" must {
      "return CREATED if the daily quota has not been met" in new Setup {
        enable(TrafficManagement)

        given
          .user.isAuthorised
          .dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 2), dailyQuotaRepo.insert)

        val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url)
          .post(Json.obj())
        )

        res.status mustBe CREATED
      }
      "return TOO_MANY_REQUESTS if the daily quota has been met" in new Setup {
        enable(TrafficManagement)

        given
          .user.isAuthorised
          .dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 10), dailyQuotaRepo.insert)

        val res = await(client(controllers.routes.VatRegistrationController.newVatRegistration().url)
          .post(Json.obj())
        )

        res.status mustBe TOO_MANY_REQUESTS
      }
    }
  }

}
