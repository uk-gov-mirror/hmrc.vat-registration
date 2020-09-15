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
import javax.inject.{Inject, Singleton}
import models.api.{RegistrationChannel, RegistrationInformation, RegistrationStatus}
import play.api.libs.json.{JsString, Json}
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.indexes.{Index, IndexType}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import utils.TimeMachine

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Success

@Singleton
class TrafficManagementRepository @Inject()(mongo: ReactiveMongoComponent)(implicit ec: ExecutionContext)
  extends ReactiveRepository(
    collectionName = "traffic-management",
    mongo = mongo.mongoConnector.db,
    domainFormat = RegistrationInformation.format
  ) with AuthorisationResource {

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("internalId"),
      key = Seq("internalId" -> IndexType.Ascending),
      unique = true
    )
  )

  def getRegistrationInformation(internalId: String)
                                (implicit hc: HeaderCarrier): Future[Option[RegistrationInformation]] =
    find("internalId" -> JsString(internalId))
      .map(_.headOption)

  def upsertRegistrationInformation(internalId: String,
                                    regId: String,
                                    status: RegistrationStatus,
                                    regStartDate: Option[LocalDate],
                                    channel: RegistrationChannel)
                                   (implicit hc: HeaderCarrier): Future[RegistrationInformation] = {

    val newRecord = RegistrationInformation(
      internalId = internalId,
      registrationId = regId,
      status = status,
      regStartDate = regStartDate,
      channel = channel
    )

    val selector = Json.obj("internalId" -> internalId)
    val modifier = Json.obj("$set" -> Json.toJson(newRecord))

    findAndUpdate(selector, modifier, fetchNewObject = true, upsert = true)
      .map(_.result[RegistrationInformation] match {
        case Some(regInfo) => regInfo
        case _ => throw new Exception("Unexpected error when inserting registration information")
      })
  }

  override def getInternalId(id: String)(implicit hc: HeaderCarrier): Future[Option[String]] =
    find("internalId" -> JsString(id))
      .map(_.headOption.map(_.internalId))

}
