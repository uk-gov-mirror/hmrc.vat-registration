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

package services

import java.io.FileInputStream

import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.VatThreshold
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json.{JsValue, Json}

import scala.collection.immutable.Map
import scala.util.parsing.json.JSON

class VatThresholdServiceSpec extends VatRegSpec with VatRegistrationFixture {

  def date(s: String) = DateTimeFormat.forPattern("yyyy-MM-dd").parseDateTime(s)

  class Setup {
    val service = new VatThresholdService {
      val thresholds: List[VatThreshold] =
        List[VatThreshold](
          VatThreshold(date("2001-01-01"), "1"),
          VatThreshold(date("2002-02-02"), "2"),
          VatThreshold(date("2003-03-03"), "3"),
          VatThreshold(date("2004-04-04"), "4")
        )
    }
  }

  "return most recent threshold for given date" in new Setup {
    val result = service.getThresholdForGivenTime(date("2001-05-15"))
    result shouldBe Some(VatThreshold(date("2001-01-01"), "1"))
  }

  "return most updated threshold for same date edge case" in new Setup {
    val result = service.getThresholdForGivenTime(date("2002-02-02"))
    result shouldBe Some(VatThreshold(date("2002-02-02"), "2"))
  }

  "return most recent threshold for future dates" in new Setup {
    val result = service.getThresholdForGivenTime(date("2010-01-01"))
    result shouldBe Some(VatThreshold(date("2004-04-04"), "4"))
  }

  "return none for date before known thresholds" in new Setup {
    val result = service.getThresholdForGivenTime(date("2000-01-01"))
    result shouldBe None
  }
}