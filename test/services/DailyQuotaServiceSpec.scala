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

package services

import config.BackendConfig
import helpers.VatRegSpec
import mocks.MockDailyQuotaRepository
import uk.gov.hmrc.http.HeaderCarrier
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class DailyQuotaServiceSpec extends VatRegSpec with MockDailyQuotaRepository {

  implicit val hc = HeaderCarrier()
  implicit val config = app.injector.instanceOf[BackendConfig]

  object Service extends DailyQuotaService(mockDailyQuotaRepository)

  "canAllocate" when {
    "the daily count is less than the allowable maximum" must {
      "return true" in {
        mockGetCurrentTotal(9)
        await(Service.canAllocate) mustBe true
      }
    }
    "the daily count equals the allowable maximum" must {
      "return false" in {
        mockGetCurrentTotal(10)
        await(Service.canAllocate) mustBe false
      }
    }
    "the daily count exceeds the allowable maximum" must {
      "return false" in {
        mockGetCurrentTotal(11)
        await(Service.canAllocate) mustBe false
      }
    }
  }

  "incrementTotal" should {
    "return the updated total" in {
      mockUpdateCurrentTotal(2)
      await(Service.incrementTotal) mustBe 2
    }
  }

}
