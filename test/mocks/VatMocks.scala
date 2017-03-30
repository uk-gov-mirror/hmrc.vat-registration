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
import common.{LogicalGroup, RegistrationId}
import common.exceptions._
import connectors.{AuthConnector, Authority}
import models._
import models.api.{VatFinancials, VatScheme}
import org.mockito.Matchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import services.{RegistrationService, ServiceResult}
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future

trait VatMocks extends WSHTTPMock {

  this: MockitoSugar =>

  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockRegistrationService = mock[RegistrationService]

  object AuthorisationMocks {

    def mockSuccessfulAuthorisation(authority: Authority): Unit = {
      when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(authority)))
    }

    def mockNotLoggedInOrAuthorised(): Unit = {
      when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
        .thenReturn(Future.successful(None))
    }

    def mockNotAuthorised(authority: Authority): Unit = {
      when(mockAuthConnector.getCurrentAuthority()(any[HeaderCarrier]()))
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

    def mockRetrieveVatSchemeThrowsException(id: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.retrieveVatScheme(id))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockRetrieveVatScheme(id: RegistrationId, vatScheme: VatScheme): Unit = {
      when(mockRegistrationService.retrieveVatScheme(id))
        .thenReturn(serviceResult(vatScheme))
    }

    def mockDeleteVatScheme(id: RegistrationId): Unit = {
      when(mockRegistrationService.deleteVatScheme(id))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteVatSchemeThrowsException(id: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteVatScheme(id))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteBankAccountDetails(id: RegistrationId): Unit = {
      when(mockRegistrationService.deleteBankAccountDetails(id))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteBankAccountDetailsThrowsException(id: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteBankAccountDetails(id))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteAccountingPeriodStart(id: RegistrationId): Unit = {
      when(mockRegistrationService.deleteAccountingPeriodStart(id))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteAccountingPeriodStartThrowsException(id: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteAccountingPeriodStart(id))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockDeleteZeroRatedTurnover(id: RegistrationId): Unit = {
      when(mockRegistrationService.deleteZeroRatedTurnover(id))
        .thenReturn(serviceResult(true))
    }

    def mockDeleteZeroRatedTurnoverThrowsException(id: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.deleteZeroRatedTurnover(id))
        .thenReturn(serviceError[Boolean](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockSuccessfulCreateNewRegistration(registrationId: RegistrationId): Unit = {
      when(mockRegistrationService.createNewRegistration()(any[HeaderCarrier]()))
        .thenReturn(serviceResult(VatScheme(registrationId, None, None, None)))
    }

    def mockFailedCreateNewRegistration(registrationId: RegistrationId): Unit = {
      when(mockRegistrationService.createNewRegistration()(any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericError(new RuntimeException("something went wrong"))))
    }

    def mockFailedCreateNewRegistrationWithDbError(registrationId: RegistrationId): Unit = {
      val exception = new Exception("Exception")
      when(mockRegistrationService.createNewRegistration()(any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockSuccessfulUpdateLogicalGroup[G](group: G): Unit = {
      // required to do like this because of how Mockito matchers work with Scala Value Classes
      //http://stackoverflow.com/a/34934179/81520
      val idMatcher: RegistrationId = RegistrationId(Matchers.anyString())
      when(mockRegistrationService.updateLogicalGroup(idMatcher, Matchers.any[G]())(Matchers.any(), Matchers.any()))
        .thenReturn(serviceResult(group))
    }

    def mockServiceUnavailableUpdateLogicalGroup[G](group: G, exception: Exception): Unit = {
      // required to do like this because of how Mockito matchers work with Scala Value Classes
      //http://stackoverflow.com/a/34934179/81520

      LogicalGroup[VatFinancials].name

      val idMatcher: RegistrationId = RegistrationId(Matchers.anyString())
      when(mockRegistrationService.updateLogicalGroup(idMatcher, Matchers.any[G]())(Matchers.any(), Matchers.any()))
        .thenReturn(serviceError[G](GenericError(exception)))
    }

  }

}
