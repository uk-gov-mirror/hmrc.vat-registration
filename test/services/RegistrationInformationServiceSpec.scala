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
import mocks.MockRegistrationInformationRepository
import models.api.{Draft, IncomingRegistrationInformation, RegistrationChannel, RegistrationInformation, RegistrationStatus, Submitted, VatReg}
import play.api.test.Helpers._
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationInformationServiceSpec extends VatRegSpec with MockRegistrationInformationRepository {

  object Service extends RegistrationInformationService(mockRegistrationInformationRepository)

  val testInternalId = "testInternalId"
  val testRegId = "testRegId"
  val testDate = LocalDate.parse("2020-01-01")
  implicit val hc: HeaderCarrier = HeaderCarrier()

  private def testRegInfo(status: RegistrationStatus, channel: RegistrationChannel) = RegistrationInformation(
    internalId = testInternalId,
    registrationId = testRegId,
    status = status,
    regStartDate = testDate,
    channel = channel
  )

  "getRegistrationInformation" must {
    "return Reg Info when the repo contains a record for the internal ID" in {
      mockGetRegInfo(testInternalId)(Some(testRegInfo(Draft, VatReg)))

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe Some(testRegInfo(Draft, VatReg))
    }

    "return None when the repo doesn't contain a record for the internal ID" in {
      mockGetRegInfo(testInternalId)(None)

      val res = await(Service.getRegistrationInformation(testInternalId))

      res mustBe None
    }
  }

  "upsertRegistrationInformation" must {
    "return reg info for the internal ID" in {
      val updateData = IncomingRegistrationInformation(testRegId, Draft, VatReg)
      mockUpsertRegInfo(testInternalId, updateData)(testRegId, Draft, testDate, VatReg)

      val res = await(Service.upsertRegistrationInformation(testInternalId, updateData))

      res mustBe testRegInfo(Draft, VatReg)
    }
  }

}
