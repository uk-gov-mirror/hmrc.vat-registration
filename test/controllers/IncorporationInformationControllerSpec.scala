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

package controllers

import connectors.IncorporationInformationResponseException
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.auth.core.AuthConnector

class IncorporationInformationControllerSpec extends VatRegSpec with VatRegistrationFixture {


  class Setup {
    val controller = new IncorporationInformationController(mockIIConnector,mockSubmissionService) {
      override val resourceConn: RegistrationMongoRepository = mockRegistrationMongoRepository
      override lazy val authConnector: AuthConnector = mockAuthConnector
    }
  }

  "GET /incorporation-information/:txId" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised(regId.value)

      controller.getIncorporationInformation(txId)(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return an except if the II connection update failed" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      IIMocks.mockIncorporationStatusLeft("incorporation status not known")

      intercept[IncorporationInformationResponseException](await(controller.getIncorporationInformation(txId)(FakeRequest())))
    }

    "return no object if not found in II" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      IIMocks.mockIncorporationStatusNone()

      val res = controller.getIncorporationInformation(txId)(FakeRequest())
      status(res) shouldBe OK
    }

    "return incorporation status object if found" in new Setup {
      AuthorisationMocks.mockAuthorised(regId.value,internalid)
      val iiStatus = incorporationStatus()
      IIMocks.mockIncorporationStatus(iiStatus)

      val res = controller.getIncorporationInformation(txId)(FakeRequest())
      status(res) shouldBe OK
      await(contentAsJson(res)) shouldBe Json.toJson(iiStatus)
    }
  }
}