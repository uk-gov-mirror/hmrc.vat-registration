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

import java.time.LocalDateTime
import javax.inject.{Inject, Named, Provider}

import auth.AuthorisationResource
import com.google.inject.ImplementedBy
import common.exceptions.DBExceptions._
import helpers.DateHelper._
import models._
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.{BSONDocument, BSONObjectID, _}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[RegistrationMongoRepository])
trait RegistrationRepository {

  def createNewRegistration(registrationID: String, internalId: String): Future[VatRegistration]

  def retrieveRegistration(registrationID: String): Future[Option[VatRegistration]]

}

class MongoDBProvider @Inject() extends Function0[DB] with MongoDbConnection {
  def apply: DB = db()
}

class RegistrationMongoRepository @Inject()( mongoProvider: Function0[DB] ) extends ReactiveRepository[VatRegistration, BSONObjectID](
  collectionName = "registration-information", //TODO confirm name of collection
  mongo = mongoProvider,
  domainFormat = VatRegistration.jsonFormat
) with RegistrationRepository
  with AuthorisationResource[String] {

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("registrationID" -> IndexType.Ascending),
      name = Some("RegId"),
      unique = true,
      sparse = false
    )
  )

  private[repositories] def registrationIDSelector(registrationID: String) = BSONDocument("registrationID" -> registrationID)

  override def createNewRegistration(registrationID: String, internalId: String): Future[VatRegistration] = {
    val newReg = VatRegistration(registrationID, internalId, LocalDateTime.now().toIsoTimestamp)
    collection.insert(newReg) map {
      res => newReg
    } recover {
      case e =>
        Logger.warn(s"Unable to insert new VAT Registration for registration ID $registrationID, Error: ${e.getMessage}")
        throw new InsertFailed(registrationID, "VatRegistration")
    }
  }

  override def getInternalId(id: String): Future[Option[(String, String)]] = retrieveRegistration(id) map {
    case Some(registration) => Some(id -> registration.internalId)
    case None => None
  }

  override def retrieveRegistration(registrationID: String): Future[Option[VatRegistration]] = {
    val selector = registrationIDSelector(registrationID)
    collection.find(selector).one[VatRegistration] recover {
      case e: Exception =>
        Logger.error(s"Unable to retrieve VAT Registration for registration ID $registrationID, Error: ${e.getMessage}")
        throw new RetrieveFailed(registrationID)
    }
  }

}
