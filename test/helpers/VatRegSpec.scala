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

package helpers

import cats.instances.FutureInstances
import cats.syntax.ApplicativeSyntax
import connectors.{Authority, UserIds}
import mocks.VatMocks
import org.mockito.Mockito.reset
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Inside}
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

abstract class VatRegSpec extends UnitSpec with WithFakeApplication
  with Inside with MockitoSugar with VatMocks with FutureAssertions with BeforeAndAfterEach with FutureInstances with ApplicativeSyntax {

  protected def testAuthority(userId: String): Authority = Authority(userId, userId, userId, UserIds(userId, userId))

  override def beforeEach() {
    reset(mockRegistrationService)
    reset(mockAuthConnector)
    reset(mockIIConnector)
    reset(mockWSHttp)
    reset(mockAuthorisationResource)
    reset(mockBusRegConnector)
    reset(mockRegistrationRepository)
    reset(mockTestOnlyRepo)
    reset(mockHttp)
    reset(mockSubmissionService)
    reset(mockVatRegistrationService)
  }
}
