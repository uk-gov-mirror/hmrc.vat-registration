
package repository

import java.time.LocalDate

import itutil.{FakeTimeMachine, IntegrationSpecBase}
import models.api.{Draft, OTRS, RegistrationInformation, Submitted, VatReg}
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._
import repositories.trafficmanagement.TrafficManagementRepository
import uk.gov.hmrc.http.HeaderCarrier
import play.api.inject.bind
import utils.TimeMachine

class TrafficManagementRepositoryISpec extends IntegrationSpecBase {

  class Setup extends SetupHelper

  val internalId1 = "internalId1"
  val internalId2 = "internalId2"
  val regId1 = "regId1"
  val regId2 = "regId2"
  val testDate1 = LocalDate.parse("2020-01-01")
  val testDate2 = LocalDate.parse("2020-01-02")
  val regInfo1 = RegistrationInformation(internalId1, regId1, Draft, Some(testDate1), VatReg)
  val regInfo2 = RegistrationInformation(internalId2, regId2, Draft, Some(testDate2), VatReg)
  implicit val hc = HeaderCarrier()

  "getRegistrationInformation" must {
    "return the correct information for the internal id when it exists" in new Setup {
      await(trafficManagementRepo.bulkInsert(Seq(regInfo1, regInfo2)))

      val res = await(trafficManagementRepo.getRegistrationInformation(internalId1))

      res mustBe Some(regInfo1)
    }
    "return None if a record doesn't exist" in new Setup {
      await(trafficManagementRepo.insert(regInfo1))

      val res = await(trafficManagementRepo.getRegistrationInformation(internalId2))

      res mustBe None
    }
  }

  "upsertRegistrationInformation" must {
    "Update an existing record" in new Setup {
      await(trafficManagementRepo.bulkInsert(Seq(regInfo1, regInfo2)))

      val res = await(trafficManagementRepo.upsertRegistrationInformation(internalId2, regId2, Submitted, Some(testDate2), OTRS))

      res mustBe regInfo2.copy(status = Submitted, channel = OTRS)
    }
    "create a new record where one doesn't exist" in new Setup {
      await(trafficManagementRepo.insert(regInfo1))

      val res = await(trafficManagementRepo.upsertRegistrationInformation(internalId2, regId2, Draft, Some(testDate2), VatReg))

      res mustBe regInfo2
    }
  }

}
