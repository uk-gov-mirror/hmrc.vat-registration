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

package repositories

import javax.inject.{Inject, Named}

import auth.AuthorisationResource
import common.exceptions.{InsertFailed, RetrieveFailed}
import helpers.DateTimeHelpers._
import models._
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID, _}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait RegistrationRepository {

  def createNewRegistration(registrationID: String, internalId: String): Future[VatScheme]

  def retrieveRegistration(registrationID: String): Future[Option[VatScheme]]

}

class MongoDBProvider extends Function0[DB] with MongoDbConnection {
  def apply: DB = db()
}

class RegistrationMongoRepository @Inject()(mongoProvider: Function0[DB], @Named("collectionName") collectionName: String)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = collectionName,
    mongo = mongoProvider,
    domainFormat = VatScheme.format
  ) with RegistrationRepository
    with AuthorisationResource[String] {

  override def indexes: Seq[Index] = Seq(
    Index(
      name = Some("RegId"),
      key = Seq("registrationId" -> IndexType.Ascending),
      unique = true
    )
  )

  private[repositories] def registrationIdSelector(registrationID: String) = BSONDocument("ID" -> BSONString(registrationID))

  override def createNewRegistration(registrationId: String, internalId: String): Future[VatScheme] = {
    val newReg = VatScheme.blank(registrationId)
    collection.insert(newReg) map {
      res => newReg
    } recover {
      case e =>
        Logger.warn(s"Unable to insert new VAT Registration for registration ID $registrationId, Error: ${e.getMessage}")
        throw InsertFailed(registrationId, "VatScheme")
    }
  }

  override def getInternalId(id: String): Future[Option[(String, String)]] = retrieveRegistration(id) map {
    case Some(vatScheme) => Some(id -> vatScheme.id)
    case None => None
  }


  override def retrieveRegistration(registrationId: String): Future[Option[VatScheme]] = {
    val selector = registrationIdSelector(registrationId)
    collection.find(selector).one[VatScheme] recover {
      case e: Exception =>
        Logger.error(s"Unable to retrieve VAT Registration for registration ID $registrationId, Error: ${e.getMessage}")
        throw RetrieveFailed(registrationId)
    }
  }

}
