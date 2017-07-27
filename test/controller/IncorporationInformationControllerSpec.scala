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

package controller

import common.exceptions.ResourceNotFound
import controllers.IncorporationInformationController
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.external.IncorporationStatus
import play.api.libs.json.Json
import play.api.test.FakeRequest
import play.api.test.Helpers._

class IncorporationInformationControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import fakeApplication.materializer

  class Setup {
    val controller = new IncorporationInformationController(mockAuthConnector, mockIIConnector)
    AuthorisationMocks.mockSuccessfulAuthorisation(testAuthority(userId))
  }

  "GET /incorporation-information/:txId" should {

    "return 403 if user not authenticated" in new Setup {
      AuthorisationMocks.mockNotLoggedInOrAuthorised()
      controller.getIncorporationInformation(txId)(FakeRequest()) returnsStatus FORBIDDEN
    }

    "return empty object if incorporation info not found" in new Setup {
      IIMocks.mockIncorporationStatusLeft(ResourceNotFound("incorporation status not known"))
      val res = controller.getIncorporationInformation(txId)(FakeRequest())
      res returnsStatus NOT_FOUND
    }

    "return incorporation status object if found" in new Setup {
      private val status: IncorporationStatus = incorporationStatus("accepted")
      IIMocks.mockIncorporationStatus(status)
      val res = controller.getIncorporationInformation(txId)(FakeRequest())
      res returnsStatus OK
      res returnsJson Json.toJson(status)
    }

  }

}
