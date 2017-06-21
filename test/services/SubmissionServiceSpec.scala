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
import org.mockito.Matchers
import org.mockito.Matchers.anyString
import org.mockito.Mockito._
import uk.gov.hmrc.play.http.HeaderCarrier

class SubmissionServiceSpec extends VatRegSpec with VatRegistrationFixture with ApplicativeSyntax with FutureInstances {

  trait Setup {
    val service = new SubmissionService(mockSequenceRepository, mockVatRegistrationService)
  }

  implicit val hc = HeaderCarrier()

  "call to assertOrGenerateAcknowledgementReference" should {

    val vatScheme = VatScheme(RegistrationId("1"), None, None, None)

    "return Success response " in new Setup {

      when(mockVatRegistrationService.retrieveAcknowledgementReference(regId)).
        thenReturn(ServiceMocks.serviceResult(ackRefNumber))
      service.assertOrGenerateAcknowledgementReference(regId) returnsRight ackRefNumber
    }

    "return ResourceNotFound response " in new Setup {
      val sequenceNo = 1
      val formatedRefNumber = f"BRPY$sequenceNo%011d"
      when(mockVatRegistrationService.retrieveAcknowledgementReference(RegistrationId(anyString())))
        .thenReturn(ServiceMocks.serviceError[String](ResourceNotFound("Resource Not Found for regId 1")))
      when(mockSequenceRepository.getNext("AcknowledgementID")).thenReturn(sequenceNo.pure)
      when(mockVatRegistrationService.saveAcknowledgementReference(RegistrationId(anyString()), anyString()))
        .thenReturn(ServiceMocks.serviceResult(formatedRefNumber))

      service.assertOrGenerateAcknowledgementReference(regId) returnsRight formatedRefNumber
    }

  }
}
