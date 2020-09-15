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

import helpers.VatRegSpec
import utils.FakeTimeMachine
import mocks.{MockDailyQuotaRepository, MockTrafficManagementRepository}
import models.api.{Draft, OTRS, RegistrationInformation, Submitted, VatReg}
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global
import play.api.test.Helpers._

import scala.concurrent.Future

class TrafficManagementServiceSpec extends VatRegSpec
  with MockDailyQuotaRepository
  with MockTrafficManagementRepository {

  val timeMachine = new FakeTimeMachine

  object Service extends TrafficManagementService(
    mockDailyQuotaRepository,
    mockTrafficManagementRepository,
    timeMachine
  )

  val testInternalId = "testInternalId"
  val testRegId = "testRegID"
  implicit val hc = HeaderCarrier()

  val testRegInfo = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = Draft,
    regStartDate = Some(timeMachine.today),
    channel = VatReg
  )

  "getRegistrationInformation" must {
    "return the registration information where it exists" in {
      mockGetRegInfo(testInternalId)(Future.successful(Some(testRegInfo)))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe Some(testRegInfo)
    }
    "return None where a record doesn't exist" in {
      mockGetRegInfo(testInternalId)(Future.successful(None))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe None
    }
  }
  "upsertRegistrationInformation" must {
    "Insert a new record with today's date when one doesn't exist for the internal id" in {
      mockGetRegInfo(testInternalId)(Future.successful(None))
      mockUpsertRegInfo(testInternalId, testRegId, Draft, Some(timeMachine.today), VatReg)(Future.successful(testRegInfo))
      mockIncrementTotal(1)

      val res = await(Service.upsertRegistrationInformation(testInternalId, testRegId, Draft, VatReg))

      res mustBe testRegInfo
    }
    "Update an existing record, without altering the date" in {
      val oldDate = LocalDate.parse("2019-12-12")
      val existingRecord = testRegInfo.copy(regStartDate = Some(oldDate))
      val newRecord = existingRecord.copy(status = Submitted, channel = OTRS)
      mockGetRegInfo(testInternalId)(Future.successful(Some(existingRecord)))
      mockUpsertRegInfo(testInternalId, testRegId, Submitted, Some(oldDate), OTRS)(Future.successful(newRecord))

      val res = await(Service.upsertRegistrationInformation(testInternalId, testRegId, Submitted, OTRS))

      res mustBe newRecord
    }
  }

}
