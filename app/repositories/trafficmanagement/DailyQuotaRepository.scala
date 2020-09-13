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

package repositories.trafficmanagement

import java.time.LocalDate

import auth.AuthorisationResource
import javax.inject.Inject
import models.api.DailyQuota
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}


class DailyQuotaRepository @Inject()(mongo: ReactiveMongoComponent, timeMachine: TimeMachine)(implicit ec: ExecutionContext)
  extends ReactiveRepository[DailyQuota, BSONObjectID](
    collectionName = "daily-quota",
    mongo = mongo.mongoConnector.db,
    domainFormat = DailyQuota.format
  ) {

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("quoateDate"),
      key = Seq("date" -> IndexType.Ascending),
      unique = true
    )
  )

  private def today: LocalDate = timeMachine.today

  def getCurrentTotal(implicit hc: HeaderCarrier): Future[Int] = find("date" -> JsString(today.toString))
    .map(_.headOption.getOrElse(DailyQuota(today)).currentTotal)

  def updateCurrentTotal(implicit hc: HeaderCarrier): Future[Int] = {
    val selector = Json.obj("date" -> today.toString)
    val modifier = Json.obj("$inc" -> Json.obj("currentTotal" -> 1))

    findAndUpdate(query = selector, update = modifier, fetchNewObject = true, upsert = true)
      .map(_.result[DailyQuota] match {
        case Some(dailyQuota) => dailyQuota.currentTotal
        case _ => throw new Exception("Unexpected exception while trying to update daily quota")
      })
  }

}
