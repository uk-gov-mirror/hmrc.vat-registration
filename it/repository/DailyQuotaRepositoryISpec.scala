
package repository

import itutil.IntegrationStubbing
import models.api.DailyQuota
import play.api.libs.json.JsString
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class DailyQuotaRepositoryISpec extends IntegrationStubbing {

  class Setup extends SetupHelper

  val testQuota = DailyQuota(testDate, 10)

  "quotaReached" must {
    "return true if the quota has been reached" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, 10)))

      val res = await(dailyQuotaRepo.quotaReached)

      res mustBe true
    }
    "return false if the quota has not been reached" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.insert(DailyQuota(testDate, 9)))

      val res = await(dailyQuotaRepo.quotaReached)

      res mustBe false
    }
  }

  "incrementTotal" must {
    "increment the quota for the day" in new Setup {
      given.user.isAuthorised
      await(dailyQuotaRepo.bulkInsert(Seq(
        DailyQuota(testDate.minusDays(1), 15),
        DailyQuota(testDate, 9)))
      )

      val res = await(dailyQuotaRepo.incrementTotal)

      res mustBe 10
    }
    "create a new record for the day if one doesn't exist" in new Setup {
      given.user.isAuthorised

      val res = await(dailyQuotaRepo.incrementTotal)
      val data = await(dailyQuotaRepo.find("date" -> JsString(testDate.toString)).map(_.headOption))

      res mustBe 1
      data mustBe Some(DailyQuota(testDate, 1))
    }
  }

}
