
package repository

import java.time.{LocalDate, LocalDateTime, LocalTime}
import itutil.{FakeTimeMachine, ITFixtures, IntegrationSpecBase}
import models.api._
import play.api.{Application, Configuration}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier
import utils.TimeMachine

import scala.concurrent.ExecutionContext.Implicits.global
import org.scalatest.concurrent.Eventually._
import reactivemongo.bson.BSONDocument.pretty
import repositories.trafficmanagement.TrafficManagementRepository

import scala.concurrent.duration._

class TrafficManagementRepositoryISpec extends IntegrationSpecBase {

  class Setup(hour: Int = 9) extends SetupHelper with ITFixtures {
    class TimestampMachine extends FakeTimeMachine {
      override val timestamp = LocalDateTime.of(testDate, LocalTime.of(hour, 0))
    }

    implicit lazy val timeApp: Application = new GuiceApplicationBuilder()
      .configure(config)
      .configure("application.router" -> "testOnlyDoNotUseInAppConf.Routes")
      .overrides(bind[TimeMachine].to[TimestampMachine])
      .build()
  }

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
    "remove an expired document" in {
      implicit lazy val tinkerApp: Application = new GuiceApplicationBuilder()
        .configure("cache.expiryInSeconds" -> "1")
        .build()

      val testRepo = tinkerApp.injector.instanceOf[TrafficManagementRepository]
      testRepo.ensureIndexes

      await(testRepo.drop)
      await(testRepo.insert(regInfo1))
      testRepo.ensureIndexes

      eventually(timeout(3 seconds), interval(500 millis)) {
        await(testRepo.getRegistrationInformation(internalId1)) mustBe None
      }
  }

}
