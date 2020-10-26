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

package services

import java.time.LocalDate

import config.BackendConfig
import enums.VatRegStatus
import featureswitch.core.config.{FeatureSwitching, TrafficManagement}
import helpers.VatRegSpec
import mocks.{MockDailyQuotaRepository, MockRegistrationRepository, MockTrafficManagementService}
import models.api.{Draft, OTRS, RegistrationInformation, VatReg, VatScheme}
import org.scalatest.BeforeAndAfterEach
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._

import scala.concurrent.Future

class NewRegistrationServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementService
  with MockRegistrationRepository
  with FeatureSwitching
  with BeforeAndAfterEach {

  implicit val hc = HeaderCarrier()
  implicit val config = app.injector.instanceOf[BackendConfig]
  val testRegId = "testRegId"
  val testInternalId = "testInternalId"
  val testDate = LocalDate.parse("2020-01-01")

  val testVatScheme = VatScheme(
    id = testRegId,
    internalId = testInternalId,
    status = VatRegStatus.draft
  )

  override def beforeEach(): Unit = {
    disable(TrafficManagement)
  }

  val testRegInfo = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Draft,
    regStartDate = Some(testDate),
    channel = VatReg
  )

  object Service extends NewRegistrationService(
    dailyQuotaRepository = mockDailyQuotaRepository,
    registrationRepository = mockRegistrationRepository,
    trafficManagementService = mockTrafficManagementService
  ) {
    override private[services] def generateRegistrationId(): String = testRegId
  }

  "newRegistration" when {
    "the TrafficManagement feature switch is disabled" should {
      "return a vat scheme" in {
        disable(TrafficManagement)
        mockCreateRegistration(testRegId, testInternalId)(testVatScheme)

        val res = await(Service.newRegistration(testInternalId))

        res mustBe RegistrationCreated(testVatScheme)
      }
    }
    "the TrafficManagement feature switch is enabled" should {
      "return QuotaReached if the daily quota has been exceeded" in {
        enable(TrafficManagement)
        mockQuotaReached(true)
        mockUpsertRegInfo(testInternalId, testRegId, Draft, OTRS)(Future.successful(testRegInfo))

        val res = await(Service.newRegistration(testInternalId))

        res mustBe QuotaReached
      }
      "return a vat scheme if the daily quota has not been met" in {
        enable(TrafficManagement)
        mockQuotaReached(false)
        val e = testRegId
        mockCreateRegistration(e, testInternalId)(testVatScheme)

        val res = await(Service.newRegistration(testInternalId))

        res mustBe RegistrationCreated(testVatScheme)
      }
    }
  }

}
