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

import common.Identifiers.RegistrationId
import common.LogicalGroup
import common.exceptions._
import models._
import play.api.Logger
import play.api.libs.json.{Format, OFormat, Writes}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.Future

trait RegistrationRepository {

  def createNewVatScheme(rid: RegistrationId): Future[VatScheme]

  def retrieveVatScheme(rid: RegistrationId): Future[Option[VatScheme]]

  def updateLogicalGroup[G: LogicalGroup : Writes](rid: RegistrationId, group: G): Future[G]

  def deleteVatScheme(rid: RegistrationId): Future[Boolean]

  def deleteBankAccountDetails(rid: RegistrationId): Future[Boolean]

  def deleteZeroRatedTurnover(rid: RegistrationId): Future[Boolean]

  def deleteAccountingPeriodStart(rid: RegistrationId): Future[Boolean]

}


object RegistrationMongoFormats extends ReactiveMongoFormats {

  implicit val mongoFormat = VatBankAccountMongoFormat.format
  implicit val vatFinancialsFormat = OFormat(VatFinancials.cTReads(mongoFormat), VatFinancials.cTWrites(mongoFormat))
  implicit val vatSchemeFormat: Format[VatScheme] = Format(VatScheme.cTReads(vatFinancialsFormat), VatScheme.cTWrites(vatFinancialsFormat))

}

// this is here for Guice dependency injection of `() => DB`
class MongoDBProvider extends (() => DB) with MongoDbConnection {
  def apply: DB = db()
}

class RegistrationMongoRepository @Inject()(mongoProvider: () => DB, @Named("collectionName") collectionName: String)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = collectionName,
    mongo = mongoProvider,
    domainFormat = RegistrationMongoFormats.vatSchemeFormat
  ) with RegistrationRepository {

  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val vatFinancialsFormat = RegistrationMongoFormats.vatFinancialsFormat
  implicit val format = RegistrationMongoFormats.vatSchemeFormat

  private[repositories] def ridSelector(rid: RegistrationId) = BSONDocument("ID" -> BSONString(rid.id))

  override def indexes: Seq[Index] = Seq(Index(
    name = Some("RegId"),
    key = Seq("registrationId" -> IndexType.Ascending),
    unique = true
  ))

  override def createNewVatScheme(rid: RegistrationId): Future[VatScheme] = {
    val newReg = VatScheme(rid, None, None, None)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        Logger.error(s"Unable to insert new VAT Scheme for registration ID $rid, Error: ${e.getMessage}")
        throw InsertFailed(rid, "VatScheme")
    }
  }

  override def retrieveVatScheme(rid: RegistrationId): Future[Option[VatScheme]] = {
    collection.find(ridSelector(rid)).one[VatScheme]
  }

  private def updateVatScheme[T](rid: RegistrationId, groupToUpdate: (String, T))(implicit writes: Writes[T]): Future[T] = {
    val (groupName, group) = groupToUpdate
    collection.findAndUpdate(
      ridSelector(rid),
      BSONDocument("$set" -> BSONDocument(groupName -> writes.writes(group)))
    ).map {
      _.value match {
        case Some(doc) => group
        case _ => throw UpdateFailed(rid, groupName)
      }
    }
  }

  private def unsetElement(rid: RegistrationId, element: String): Future[Boolean] = {
    collection.findAndUpdate(
      ridSelector(rid),
      BSONDocument("$unset" -> BSONDocument(element -> ""))
    ).map {
      _.value match {
        case Some(_) => true
        case _ => throw UpdateFailed(rid, element)
      }
    }
  }

  override def updateLogicalGroup[G: LogicalGroup : Writes](rid: RegistrationId, group: G): Future[G] =
    updateVatScheme(rid, LogicalGroup[G].name -> group)

  override def deleteVatScheme(rid: RegistrationId): Future[Boolean] = {
    retrieveVatScheme(rid) flatMap {
      case Some(ct) => collection.remove(ridSelector(rid)) map { _ => true }
      case None => Future.failed(MissingRegDocument(rid))
    }
  }

  override def deleteBankAccountDetails(rid: RegistrationId): Future[Boolean] =
    unsetElement(rid, "financials.bankAccount")

  override def deleteZeroRatedTurnover(rid: RegistrationId): Future[Boolean] =
    unsetElement(rid, "financials.zeroRatedTurnoverEstimate")

  override def deleteAccountingPeriodStart(rid: RegistrationId): Future[Boolean] =
    unsetElement(rid, "financials.accountingPeriods.periodStart")

}
