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

import javax.inject.Inject
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.libs.json._

import scala.io.Source._
import java.lang.Thread._

import models.VatThreshold

class VatThresholdServiceImpl @Inject extends VatThresholdService {
  val thresholdsString = fromFile("conf/thresholds.json").mkString
  implicit val ThresholdReads = Json.reads[VatThreshold]
  val thresholds = Json.parse(thresholdsString).as[List[VatThreshold]]
}


trait VatThresholdService {
  val thresholds: List[VatThreshold]

  def getThresholdForGivenTime(givenDate: DateTime): Option[VatThreshold] = {
    thresholds
      .sortWith(descendingDates)
      .find(model => givenDate.isAfter(model.date) || givenDate.isEqual(model.date))
  }

  private def descendingDates(vt1: VatThreshold, vt2: VatThreshold) = {
    vt1.date isAfter vt2.date
  }
}