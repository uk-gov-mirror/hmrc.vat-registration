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
import common.RegistrationId
import common.LogicalGroup
import common.exceptions._
import models._
import models.api.{VatBankAccountMongoFormat, VatFinancials, VatScheme}
import play.api.Logger
import play.api.libs.json.{OFormat, Writes}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

import scala.concurrent.Future

trait RegistrationRepository {

  def createNewVatScheme(id: RegistrationId): Future[VatScheme]

  def retrieveVatScheme(id: RegistrationId): Future[Option[VatScheme]]

  def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G]): Future[G]

  def deleteVatScheme(id: RegistrationId): Future[Boolean]

  def deleteByElement(id: RegistrationId, elementPath: String): Future[Boolean]

}


object RegistrationMongoFormats extends ReactiveMongoFormats {

  val encryptedFinancials: OFormat[VatFinancials] = VatFinancials.format(VatBankAccountMongoFormat.encryptedFormat)
  val vatSchemeFormat: OFormat[VatScheme] = OFormat(VatScheme.reads(encryptedFinancials), VatScheme.writes(encryptedFinancials))

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

  import cats.instances.future._

  import scala.concurrent.ExecutionContext.Implicits.global

  private[repositories] def ridSelector(id: RegistrationId) = BSONDocument("ID" -> BSONString(id.value))

  override def indexes: Seq[Index] = Seq(Index(
    name = Some("RegId"),
    key = Seq("registrationId" -> IndexType.Ascending),
    unique = true
  ))

  override def createNewVatScheme(id: RegistrationId): Future[VatScheme] = {
    val newReg = VatScheme(id, None, None, None)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        Logger.error(s"Unable to insert new VAT Scheme for registration ID $id, Error: ${e.getMessage}")
        throw InsertFailed(id, "VatScheme")
    }
  }

  override def retrieveVatScheme(id: RegistrationId): Future[Option[VatScheme]] = {
    collection.find(ridSelector(id)).one[VatScheme]
  }

  override def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G]): Future[G] = {
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(logicalGroup.name -> w.writes(group))))
      .map(_.value)).map(_ => group).getOrElse(throw UpdateFailed(id, logicalGroup.name))
  }

  private def unsetElement(id: RegistrationId, element: String): Future[Boolean] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$unset" -> BSONDocument(element -> "")))
      .map(_.value)).map(_ => true).getOrElse(throw UpdateFailed(id, element))


  override def deleteVatScheme(id: RegistrationId): Future[Boolean] = retrieveVatScheme(id) flatMap {
    case Some(ct) => collection.remove(ridSelector(id)) map { _ => true }
    case None => Future.failed(MissingRegDocument(id))
  }


  override def deleteByElement(id: RegistrationId, elementPath: String): Future[Boolean] =
    unsetElement(id, elementPath)

//  override def deleteBankAccountDetails(id: RegistrationId): Future[Boolean] =
//    unsetElement(id, "financials.bankAccount")
//
//  override def deleteZeroRatedTurnover(id: RegistrationId): Future[Boolean] =
//    unsetElement(id, "financials.zeroRatedTurnoverEstimate")
//
//  override def deleteAccountingPeriodStart(id: RegistrationId): Future[Boolean] =
//    unsetElement(id, "financials.accountingPeriods.periodStart")

}
