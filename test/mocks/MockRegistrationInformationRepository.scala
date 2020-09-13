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

package mocks

import java.time.LocalDate

import models.api.{IncomingRegistrationInformation, RegistrationChannel, RegistrationInformation, RegistrationStatus}
import org.mockito.ArgumentMatchers
import org.scalatestplus.mockito.MockitoSugar
import org.mockito.Mockito._
import repositories.trafficmanagement.RegistrationInformationRepository
import uk.gov.hmrc.http.HeaderCarrier

import scala.concurrent.Future

trait MockRegistrationInformationRepository extends MockitoSugar {

  val mockRegistrationInformationRepository = mock[RegistrationInformationRepository]

  def mockGetRegInfo(internalId: String)(response: Option[RegistrationInformation]) =
    when(mockRegistrationInformationRepository.getRegistrationInformation(
      ArgumentMatchers.eq(internalId)
    )(ArgumentMatchers.any[HeaderCarrier]))
    .thenReturn(Future.successful(response))

  def mockUpsertRegInfo(internalId: String, regiInfo: IncomingRegistrationInformation)
                       (regId: String, status: RegistrationStatus, date: LocalDate, channel: RegistrationChannel) =
    when(mockRegistrationInformationRepository.upsertRegistrationInformation(
      ArgumentMatchers.eq(internalId),
      ArgumentMatchers.eq(regiInfo)
    )(ArgumentMatchers.any[HeaderCarrier]))
    .thenReturn(Future.successful(RegistrationInformation(
      internalId = internalId,
      registrationId = regId,
      status = status,
      regStartDate = date,
      channel = channel
    )))

}
