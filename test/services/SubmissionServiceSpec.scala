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

package services

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import common.RegistrationId
import common.exceptions._
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api._
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.HeaderCarrier

import scala.concurrent.Future
class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture  with ApplicativeSyntax with FutureInstances{

  trait Setup {
    val service = new SubmissionService(mockSequenceRepository, mockVatRegistrationService)
  }

  implicit val hc = HeaderCarrier()

  "call to assertOrGenerateAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"), None, None, None)

    "return Success response " in new Setup {

      when(mockVatRegistrationService.retrieveAcknowledgementReference(RegistrationId("1"))).
        thenReturn(ServiceMocks.serviceResult(ServiceMocks.ACK_REF_NUMBER))
      service.assertOrGenerateAcknowledgementReference(RegistrationId("1")) returnsRight ServiceMocks.ACK_REF_NUMBER
    }

    "return ResourceNotFound response " in new Setup {

      val sequenceNo = 1
      when(mockVatRegistrationService.retrieveAcknowledgementReference(RegistrationId("1")))
      .thenReturn(ServiceMocks.serviceError[String](ResourceNotFound("Resource Not Found for regId 1")))

      when(mockSequenceRepository.getNext("AcknowledgementID"))
        .thenReturn(Future.successful(sequenceNo))

      val formatedRefNumber = f"BRPY$sequenceNo%011d"
      service.assertOrGenerateAcknowledgementReference(RegistrationId("1")) returnsRight formatedRefNumber
    }
  }
}
