/*
 * Copyright 2017 HM Revenue & Customs
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

import cats.data.EitherT
import common.Identifiers.RegistrationId
import common.LogicalGroup
import common.exceptions._
import connectors.{AuthConnector, Authority}
import models._
import org.mockito.Matchers
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.Format
import services.{RegistrationService, ServiceResult}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait VatMocks extends WSHTTPMock {

  this: MockitoSugar =>

  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockRegistrationService = mock[RegistrationService]

  object AuthorisationMocks {

    def mockSuccessfulAuthorisation(authority: Authority): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
    }

    def mockNotLoggedInOrAuthorised: OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))
    }

    def mockNotAuthorised(authority: Authority): OngoingStubbing[Future[Option[Authority]]] = {
      when(mockAuthConnector.getCurrentAuthority()(Matchers.any()))
        .thenReturn(Future.successful(Some(authority)))
    }

  }

  object ServiceMocks {

    def serviceResult[B](b: B): ServiceResult[B] = {
      EitherT[Future, LeftState, B](Future.successful(Right(b)))
    }

    def serviceError[B](a: LeftState): ServiceResult[B] = {
      EitherT[Future, LeftState, B](Future.successful(Left(a)))
    }

    def mockRetrieveVatSchemeThrowsException(testId: String): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.retrieveVatScheme(testId))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockRetrieveVatScheme(testId: String, vatScheme: VatScheme): Unit = {
      when(mockRegistrationService.retrieveVatScheme(Matchers.contains(testId)))
        .thenReturn(serviceResult(vatScheme))
    }

    def mockDeleteVatScheme(testId: String): Unit = {
      when(mockRegistrationService.deleteVatScheme(testId))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteVatSchemeThrowsException(testId: String): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteVatScheme(testId))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteBankAccountDetails(testId: String): Unit = {
      when(mockRegistrationService.deleteBankAccountDetails(testId))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteBankAccountDetailsThrowsException(testId: String): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteBankAccountDetails(testId))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteAccountingPeriodStart(testId: String): Unit = {
      when(mockRegistrationService.deleteAccountingPeriodStart(Matchers.contains(testId)))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteAccountingPeriodStartThrowsException(testId: String): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteAccountingPeriodStart(testId))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteZeroRatedTurnover(testId: String): Unit = {
      when(mockRegistrationService.deleteZeroRatedTurnover(Matchers.contains(testId)))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteZeroRatedTurnoverThrowsException(testId: String): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteZeroRatedTurnover(testId))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockSuccessfulCreateNewRegistration(registrationId: RegistrationId): Unit = {
      when(mockRegistrationService.createNewRegistration()(Matchers.any[HeaderCarrier]()))
        .thenReturn(serviceResult(VatScheme(registrationId, None, None, None)))
    }

    def mockFailedCreateNewRegistration(registrationId: RegistrationId): Unit = {
      when(mockRegistrationService.createNewRegistration()(Matchers.any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericError(new RuntimeException("something went wrong"))))
    }

    def mockFailedCreateNewRegistrationWithDbError(registrationId: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.createNewRegistration()(Matchers.any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockSuccessfulUpdateLogicalGroup[G](registrationId: RegistrationId, group: G)
                                           (implicit lg: LogicalGroup[G], fmt: Format[G]): Unit = {
      when(mockRegistrationService.updateLogicalGroup(registrationId, group))
        .thenReturn(serviceResult(group))
    }

    def mockServiceUnavailableUpdateLogicalGroup[G](registrationId: RegistrationId, group: G, exception: Exception)
                                                   (implicit lg: LogicalGroup[G], fmt: Format[G]): Unit = {
      when(mockRegistrationService.updateLogicalGroup(registrationId, group))
        .thenReturn(serviceError[G](GenericError(exception)))
    }

  }

}
