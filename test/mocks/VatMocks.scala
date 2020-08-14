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

import auth.AuthorisationResource
import cats.data.EitherT
import cats.instances.FutureInstances
import cats.syntax.{ApplicativeSyntax, EitherSyntax}
import common.exceptions._
import common.{RegistrationId, TransactionId}
import connectors._
import enums.VatRegStatus
import models.api.VatScheme
import models.external.IncorporationStatus
import org.mockito.{ArgumentMatchers => Matchers}
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import org.scalatestplus.mockito.MockitoSugar
import play.api.libs.json.JsValue
import repositories._
import services._
import uk.gov.hmrc.auth.core.{AuthConnector, InvalidBearerToken}
import uk.gov.hmrc.http.{HeaderCarrier, HttpGet, HttpPost}
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import utils.VATFeatureSwitches

import scala.concurrent.Future

trait VatMocks extends HttpClientMock {

  this: MockitoSugar =>

  lazy val mockAuthConnector: AuthConnector                             = mock[AuthConnector]
  lazy val mockAuthorisationResource: AuthorisationResource             = mock[AuthorisationResource]
  lazy val mockRegistrationMongoRepository: RegistrationMongoRepository = mock[RegistrationMongoRepository]
  lazy val mockSubmissionService: SubmissionService                     = mock[SubmissionService]
  lazy val mockVatRegistrationService: VatRegistrationService           = mock[VatRegistrationService]
  lazy val mockSequenceRepository: SequenceMongoRepository              = mock[SequenceMongoRepository]
  lazy val mockDesConnector: DESConnector                               = mock[DESConnector]
  lazy val mockVatFeatureSwitches: VATFeatureSwitches                   = mock[VATFeatureSwitches]
  lazy val mockEligibilityService: EligibilityService                   = mock[EligibilityService]
  lazy val mockLodgingOfficerService: LodgingOfficerService             = mock[LodgingOfficerService]
  lazy val mockSicAndComplianceService: SicAndComplianceService         = mock[SicAndComplianceService]
  lazy val mockBusinessContactService: BusinessContactService           = mock[BusinessContactService]
  lazy val mockTradingDetailsService: TradingDetailsService             = mock[TradingDetailsService]
  lazy val mockFlatRateSchemeService: FlatRateSchemeService             = mock[FlatRateSchemeService]
  lazy val mockVatThresholdService: VatThresholdService                 = mock[VatThresholdService]

  object AuthorisationMocks {

    def mockAuthenticated(intId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(Some(intId)))
    }

    def mockAuthenticatedLoggedInNoCorrespondingData(): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any())).thenReturn(Future.successful(None))
    }

    def mockNotLoggedInOrAuthenticated(): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.failed(new InvalidBearerToken("Invalid Bearer Token")))
    }

    def mockAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq[String](regId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(internalId)))
    }

    def mockNotAuthorised(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
      when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
        .thenReturn(Future.successful(Some(internalId)))

      when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId))(any[HeaderCarrier]()))
        .thenReturn(Future.successful(Some(internalId + "xxx")))
    }


      def mockAuthMongoResourceNotFound(regId: String, internalId: String): OngoingStubbing[Future[Option[String]]] = {
        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
          .thenReturn(Future.successful(Some(internalId)))

        when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId))(any[HeaderCarrier]()))
          .thenReturn(Future.successful(None))
      }

      def mockNotLoggedInOrAuthorised(regId: String): OngoingStubbing[Future[Option[String]]] = {
        when(mockAuthConnector.authorise[Option[String]](any(), any())(any(), any()))
          .thenReturn(Future.successful(None))

        when(mockRegistrationMongoRepository.getInternalId(Matchers.eq(regId))(any[HeaderCarrier]()))
          .thenReturn(Future.successful(Some("SomeInternalId")))
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
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockVatRegistrationService.retrieveVatScheme(idMatcher)(any()))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockRetrieveVatScheme(id: RegistrationId, vatScheme: VatScheme): Unit = {
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockVatRegistrationService.retrieveVatScheme(idMatcher)(any()))
        .thenReturn(serviceResult(vatScheme))
    }

    def mockDeleteVatScheme(id: String): Unit = {
      when(mockVatRegistrationService.deleteVatScheme(Matchers.eq(id), any())(any()))
        .thenReturn(Future.successful(true))
    }

    def mockDeleteVatSchemeFail(id: String): Unit = {
      when(mockVatRegistrationService.deleteVatScheme(Matchers.eq(id), any())(any()))
        .thenReturn(Future.successful(false))
    }

    def mockDeleteVatSchemeInvalidStatus(id: String): Unit = {
      when(mockVatRegistrationService.deleteVatScheme(Matchers.eq(id), any())(any()))
        .thenReturn(Future.failed(new InvalidSubmissionStatus("")))
    }


    def mockSuccessfulCreateNewRegistration(registrationId: RegistrationId, internalId:String): Unit = {
      when(mockVatRegistrationService.createNewRegistration(Matchers.eq(internalId))(any[HeaderCarrier]()))
        .thenReturn(serviceResult(VatScheme(registrationId,internalId, None, None, None, status = VatRegStatus.draft)))
    }

    def mockFailedCreateNewRegistration(registrationId: RegistrationId, internalId:String): Unit = {
      when(mockVatRegistrationService.createNewRegistration(Matchers.eq(internalId))(any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericError(new RuntimeException("something went wrong"))))
    }

    def mockFailedCreateNewRegistrationWithDbError(registrationId: RegistrationId, internalId:String): Unit = {
      val exception = new Exception("Exception")
      when(mockVatRegistrationService.createNewRegistration(Matchers.eq(internalId))(any[HeaderCarrier]()))
        .thenReturn(serviceError[VatScheme](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockGetAcknowledgementReference(ackRef: String): Unit = {
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(any()))
        .thenReturn(serviceResult(ackRef))
    }

    def mockGetAcknowledgementReferenceServiceUnavailable(exception: Exception): Unit = {
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(any()))
        .thenReturn(serviceError[String](GenericDatabaseError(exception, Some("regId"))))
    }

    def mockGetDocumentStatus(json: JsValue): Unit = {
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockVatRegistrationService.getStatus(idMatcher)(any()))
        .thenReturn(Future.successful(json))
    }

    def mockGetAcknowledgementReferenceExistsError(): Unit = {
      val idMatcher: RegistrationId = RegistrationId(anyString())
      when(mockSubmissionService.getAcknowledgementReference(idMatcher)(any()))
        .thenReturn(serviceError[String](AcknowledgementReferenceExists("regId")))
    }
  }
}
