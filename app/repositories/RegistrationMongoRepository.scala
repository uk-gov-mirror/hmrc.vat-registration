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

import cats.data.OptionT
import common.exceptions._
import models._
import play.api.Logger
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.commands.UpdateWriteResult
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.Future

trait RegistrationRepository {
  def createNewVatScheme(registrationId: String): Future[VatScheme]

  def retrieveVatScheme(registrationId: String): Future[Option[VatScheme]]

  def updateVatChoice(registrationId: String, vatChoice: VatChoice): Future[VatChoice]

  def updateTradingDetails(registrationId: String, tradingDetails: VatTradingDetails): Future[VatTradingDetails]

  def updateVatFinancials(registrationId: String, financials: VatFinancials): Future[VatFinancials]

  def deleteVatScheme(registrationId: String): Future[Boolean]

  def dropCollection: Future[Unit]
}

// this is here for Guice dependency injection of `() => DB`
class MongoDBProvider extends Function0[DB] with MongoDbConnection {
  def apply: DB = db()
}

class RegistrationMongoRepository @Inject()(mongoProvider: Function0[DB], @Named("collectionName") collectionName: String)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = collectionName,
    mongo = mongoProvider,
    domainFormat = VatScheme.format
  ) with RegistrationRepository {

  import cats.implicits._

  import scala.concurrent.ExecutionContext.Implicits.global

  private[repositories] def regIdSelector(registrationID: String) = BSONDocument("ID" -> BSONString(registrationID))

  override def indexes: Seq[Index] = Seq(Index(
    name = Some("RegId"),
    key = Seq("registrationId" -> IndexType.Ascending),
    unique = true
  ))

  override def createNewVatScheme(registrationId: String): Future[VatScheme] = {
    val newReg = VatScheme(registrationId, None, None, None)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        Logger.error(s"Unable to insert new VAT Scheme for registration ID $registrationId, Error: ${e.getMessage}")
        throw InsertFailed(registrationId, "VatScheme")
    }
  }

  override def retrieveVatScheme(regId: String): Future[Option[VatScheme]] = {
    collection.find(regIdSelector(regId)).one[VatScheme]
  }

  private def updateVatScheme[T](regId: String, update: VatScheme => VatScheme, toReturnType: UpdateWriteResult => T): Future[T] = {
    (for {
      vatScheme <- OptionT(retrieveVatScheme(regId))
      res <- OptionT.liftF(collection.update(regIdSelector(regId), update(vatScheme)))
    } yield res).map(toReturnType).getOrElse {
      throw MissingRegDocument(regId)
    }
  }

  override def updateVatFinancials(regId: String, financials: VatFinancials): Future[VatFinancials] =
    updateVatScheme(regId, _.copy(financials = Some(financials)), _ => financials)

  override def updateVatChoice(regId: String, vatChoice: VatChoice): Future[VatChoice] =
    updateVatScheme(regId, _.copy(vatChoice = Some(vatChoice)), _ => vatChoice)

  override def updateTradingDetails(regId: String, tradingDetails: VatTradingDetails): Future[VatTradingDetails] =
    updateVatScheme(regId, _.copy(tradingDetails = Some(tradingDetails)), _ => tradingDetails)

  override def deleteVatScheme(registrationId: String): Future[Boolean] = {
    retrieveVatScheme(registrationId) flatMap {
      case Some(ct) => collection.remove(regIdSelector(registrationId)) map { _ => true }
      case None => Future.failed(MissingRegDocument(registrationId))
    }
  }


  // $COVERAGE-OFF$
  override def dropCollection: Future[Unit] = {
    collection.drop()
  }
  // $COVERAGE-ON$
}
