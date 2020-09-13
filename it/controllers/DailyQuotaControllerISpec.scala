
package controllers

import java.time.LocalDate

import itutil.IntegrationStubbing
import models.api.DailyQuota
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class DailyQuotaControllerISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  "GET /traffic-management/daily-total" must {
    "return NO_CONTENT when the current total is less than the allowed maximum" in new Setup {
      given.dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 9), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.DailyQuotaController.canAllocate().url).get())

      res.status mustBe NO_CONTENT
    }
    "return TOO_MANY_REQUESTS when the current total is equal to the allowed maximum" in new Setup {
      given.dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 10), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.DailyQuotaController.canAllocate().url).get())

      res.status mustBe TOO_MANY_REQUESTS
    }
    "return TOO_MANY_REQUESTS when the current total is more than the allowed maximum" in new Setup {
      given.dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 11), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.DailyQuotaController.canAllocate().url).get())

      res.status mustBe TOO_MANY_REQUESTS
    }
  }

  "PATCH /traffic-management/daily-total" must {
    "return OK with the incremented total" in new Setup {
      given.dailyQuotaRepo.insertIntoDb(DailyQuota(LocalDate.parse(testDate), 11), dailyQuotaRepo.insert)

      val res = await(client(controllers.routes.DailyQuotaController.canAllocate().url).patch(""))

      res.status mustBe OK
      res.body mustBe "12"
    }

    "return OK and return 1 if a tally doesn't exist for the day in question" in new Setup {
      val res = await(client(controllers.routes.DailyQuotaController.canAllocate().url).patch(""))

      res.status mustBe OK
      res.body mustBe "1"
    }
  }

}
