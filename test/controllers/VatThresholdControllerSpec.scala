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

package controllers

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.VatThreshold
import models.api._
import org.joda.time.format.DateTimeFormat
import org.mockito.ArgumentMatchers.{any, anyString}
import org.mockito.Mockito.when
import org.omg.PortableInterceptor.SUCCESSFUL
import play.api.http.Status
import play.api.libs.json.{JsValue, Json}
import play.api.test.FakeRequest
import repositories.RegistrationMongoRepository
import services.{RegistrationService, SubmissionService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.http.HeaderCarrier
import services._

import scala.concurrent.Future

class VatThresholdControllerSpec extends VatRegSpec with VatRegistrationFixture {

  import play.api.test.Helpers._

  class SetupMocks(mockSubmission: Boolean) {
    val controller = new VatThresholdController {
      override val vatThresholdService: VatThresholdService = mockVatThresholdService
    }
  }

  class Setup extends SetupMocks(false)
  class SetupWithMockSubmission extends SetupMocks(true)

  def date(s: String) = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(s)


  "getThresholdForTime" should {
    "returns the correct threshold and since date" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()
      val returnObj = Json.obj("taxable-threshold" -> "73000", "since" -> "2011-04-01")
      when(mockVatThresholdService.getThresholdForGivenDate(any())).thenReturn(Some(VatThreshold(date("2011-04-01"), "73000")))
      val result = controller.getThresholdForDate("2012-03-20")(FakeRequest())
      status(result) shouldBe OK
      await(contentAsJson(result)) shouldBe returnObj
    }

    "return 404 if requested date is before any known thresholds" in new Setup {
      AuthorisationMocks.mockAuthenticatedLoggedInNoCorrespondingData()
      when(mockVatThresholdService.getThresholdForGivenDate(any())).thenReturn(None)
      val result = controller.getThresholdForDate("2012-03-20")(FakeRequest())
      status(result) shouldBe NOT_FOUND
    }
  }
}