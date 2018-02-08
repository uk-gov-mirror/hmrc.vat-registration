/*
 * Copyright 2018 HM Revenue & Customs
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

import auth.AuthorisationResource
import cats.data.EitherT
import cats.instances.FutureInstances
import cats.syntax.{ApplicativeSyntax, EitherSyntax}
import common.exceptions._
import common.{RegistrationId, TransactionId}
import connectors._
import enums.VatRegStatus
import models._
import models.api.VatScheme
import models.external.IncorporationStatus
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.JsValue
import repositories._
import services._
import uk.gov.hmrc.auth.core.{AuthConnector, InvalidBearerToken}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import utils.VATFeatureSwitches

import scala.concurrent.Future

trait VatMocks extends WSHTTPMock {

  this: MockitoSugar =>

  lazy val mockAuthConnector = mock[AuthConnector]
  lazy val mockIIConnector = mock[IncorporationInformationConnector]
  lazy val mockRegistrationService = mock[RegistrationService]
  lazy val mockAuthorisationResource = mock[AuthorisationResource]
  lazy val mockBusRegConnector = mock[BusinessRegistrationConnector]
  lazy val mockRegistrationRepository = mock[RegistrationRepository]
  lazy val mockRegistrationMongoRepository = mock[RegistrationMongoRepository]
  lazy val mockRegistrationMongo = mock[RegistrationMongo]
  lazy val mockHttp = mock[HttpGet with HttpPost]
  lazy val mockSubmissionService = mock[SubmissionService]
  lazy val mockVatRegistrationService = mock[VatRegistrationService]
  lazy val mockSequenceMongo = mock[SequenceMongo]
  lazy val mockSequenceRepository = mock[SequenceRepository]
  lazy val mockCompanyRegConnector = mock[CompanyRegistrationConnector]
  lazy val mockDesConnector = mock[DESConnector]
  lazy val mockVatFeatureSwitches = mock[VATFeatureSwitches]
  lazy val mockEligibilityService = mock[EligibilityService]
  lazy val mockThresholdService = mock[ThresholdService]
  lazy val mockLodgingOfficerService = mock[LodgingOfficerService]
  lazy val mockSicAndComplianceService = mock[SicAndComplianceService]
  lazy val mockBusinessContactService = mock[BusinessContactService]
  lazy val mockTradingDetailsService = mock[TradingDetailsService]
  lazy val mockFlatRateSchemeService = mock[FlatRateSchemeService]


  object AuthorisationMocks {

    def mockAuthenticated(intId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(Some(intId)))
    }

    def mockAuthenticatedLoggedInNoCorrespondingData(): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(Future.successful(None))
    }

    def mockNotLoggedInOrAuthenticated(): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.failed(new InvalidBearerToken("Invalid Bearer Token")))
    }


    def mockAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationRepository.getInternalId(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(internalId)))
    }

    def mockNotAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationRepository.getInternalId(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(internalId + "xxx")))
    }


      def mockAuthResourceNotFound(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
        when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(Some(internalId)))

        when(mockRegistrationRepository.getInternalId(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(None))
      }

      def mockNotLoggedInOrAuthorised(regId: String): OngoingStubbing[Future[Option[String]]] = {
        when(mockAuthConnector.authorise[Option[String]](ArgumentMatchers.any(), ArgumentMatchers.any())(ArgumentMatchers.any(), ArgumentMatchers.any()))
          .thenReturn(Future.successful(None))

        when(mockRegistrationRepository.getInternalId(ArgumentMatchers.eq(regId))(ArgumentMatchers.any[HeaderCarrier]()))
          .thenReturn(Future.successful(Some("SomeInternalId")))
      }

  }

  object IIMocks extends FutureInstances with ApplicativeSyntax with EitherSyntax {

    def mockIncorporationStatus(incorporationStatus: IncorporationStatus): Unit =
      when(mockIIConnector.retrieveIncorporationStatus(TransactionId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any()))
        .thenReturn(Future.successful(Some(incorporationStatus)))

    def mockIncorporationStatusNone(): Unit =
      when(mockIIConnector.retrieveIncorporationStatus(TransactionId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any()))
        .thenReturn(Future.successful(None))

    def mockIncorporationStatusLeft(message : String): Unit =
      when(mockIIConnector.retrieveIncorporationStatus(TransactionId(anyString()), ArgumentMatchers.any(), ArgumentMatchers.any())(any(), any()))
        .thenReturn(Future.failed(new IncorporationInformationResponseException(message)))

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
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockRegistrationService.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockRetrieveVatScheme(id: RegistrationId, vatScheme: VatScheme): Unit = {
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockRegistrationService.retrieveVatScheme(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(serviceResult(vatScheme))
    }

    def mockDeleteVatScheme(id: String): Unit = {
      when(mockRegistrationService.deleteVatScheme(ArgumentMatchers.eq(id), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(true))
    }

    def mockDeleteVatSchemeFail(id: String): Unit = {
      when(mockRegistrationService.deleteVatScheme(ArgumentMatchers.eq(id), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.successful(false))
    }

    def mockDeleteVatSchemeInvalidStatus(id: String): Unit = {
      when(mockRegistrationService.deleteVatScheme(ArgumentMatchers.eq(id), ArgumentMatchers.any())(ArgumentMatchers.any()))
        .thenReturn(Future.failed(new InvalidSubmissionStatus("")))
    }


    def mockSuccessfulCreateNewRegistration(registrationId: RegistrationId): Unit = {
      when(mockRegistrationService.createNewRegistration()(any[HeaderCarrier]()))
        .thenReturn(serviceResult(VatScheme(registrationId, None, None, None, status = VatRegStatus.draft)))
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
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockRegistrationService.updateLogicalGroup(idMatcher, ArgumentMatchers.any[G]())(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any()))
        .thenReturn(serviceResult(group))
    }

    def mockGetAcknowledgementReference(ackRef: String): Unit = {
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(serviceResult(ackRef))
    }

    def mockGetAcknowledgementReferenceServiceUnavailable(exception: Exception): Unit = {
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(serviceError[String](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockGetDocumentStatus(json: JsValue): Unit = {
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockRegistrationService.getStatus(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(Future.successful(json))
    }

    def mockGetAcknowledgementReferenceExistsError(): Unit = {
      val idMatcher: RegistrationId = RegistrationId(ArgumentMatchers.anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(ArgumentMatchers.any()))
        .thenReturn(serviceError[String](AcknowledgementReferenceExists("regId")))
    }
  }
}
