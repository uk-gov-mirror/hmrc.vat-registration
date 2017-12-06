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
import common.{LogicalGroup, RegistrationId}
import enums.VatRegStatus
import models._
import models.api.{VatBankAccountMongoFormat, VatFinancials, VatScheme}
import play.api.libs.json.{Json, OFormat, Writes}
import play.modules.reactivemongo.MongoDbConnection
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson._
import reactivemongo.json.BSONFormats
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

trait RegistrationRepository {
  def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme]
  def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
  def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], hc: HeaderCarrier): Future[G]
  def deleteVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Boolean]
  def updateByElement(id: RegistrationId, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String]
  def prepareRegistrationSubmission(id: RegistrationId, ackRef : String)(implicit hc: HeaderCarrier): Future[Boolean]
  def finishRegistrationSubmission(id : RegistrationId, status : VatRegStatus.Value)(implicit hc : HeaderCarrier) : Future[VatRegStatus.Value]
  def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Boolean]
  def updateIVStatus(regId: String, ivStatus: Boolean)(implicit hc: HeaderCarrier): Future[Boolean]
  def saveTransId(transId: String, regId: RegistrationId)(implicit hc: HeaderCarrier): Future[String]
  def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]]
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

  private[repositories] def ridSelector(id: RegistrationId) = BSONDocument("registrationId" -> BSONString(id.value))
  private[repositories] def tidSelector(id: String) = BSONDocument("transactionId" -> id)
  private def regIdSelector(regId: String)                  = BSONDocument("registrationId" -> regId)

  override def indexes: Seq[Index] = Seq(
    Index(
      name    = Some("RegId"),
      key     = Seq("registrationId" -> IndexType.Ascending),
      unique  = true
    )
  )

  override def createNewVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[VatScheme] = {
    val newReg = VatScheme(id, None, None, None, None, status = VatRegStatus.draft)
    collection.insert(newReg) map (_ => newReg) recover {
      case e =>
        logger.error(s"[createNewVatScheme] - Unable to insert new VAT Scheme for registration ID $id, Error: ${e.getMessage}")
        throw InsertFailed(id, "VatScheme")
    }
  }

  override def retrieveVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(ridSelector(id)).one[VatScheme]
  }

  override def updateLogicalGroup[G](id: RegistrationId, group: G)(implicit w: Writes[G], logicalGroup: LogicalGroup[G], hc: HeaderCarrier): Future[G] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(logicalGroup.name -> w.writes(group))))
      .map(_.value)).map(_ => group).getOrElse {
      logger.error(s"[updateLogicalGroup] - There was a problem updating logical group ${logicalGroup.name} for regId ${id.value}")
      throw UpdateFailed(id, logicalGroup.name)
    }

  private def unsetElement(id: RegistrationId, element: String)(implicit hc: HeaderCarrier): Future[Boolean] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$unset" -> BSONDocument(element -> "")))
      .map(_.value)).map(_ => true).getOrElse {
      logger.error(s"[unsetElement] - There was a problem unsetting element $element for regId ${id.value}")
      throw UpdateFailed(id, element)
    }

  private def setElement(id: RegistrationId, element: String, value: String)(implicit hc: HeaderCarrier): Future[String] =
    OptionT(collection.findAndUpdate(ridSelector(id), BSONDocument("$set" -> BSONDocument(element -> value)))
      .map(_.value)).map(_ => value).getOrElse {
      logger.error(s"[setElement] - There was a problem setting element $element for regId ${id.value}")
      throw UpdateFailed(id, element)
    }

  override def deleteVatScheme(id: RegistrationId)(implicit hc: HeaderCarrier): Future[Boolean] = retrieveVatScheme(id) flatMap {
    case Some(_) => collection.remove(ridSelector(id)) map(_ => true)
    case None    =>
      logger.error(s"[deleteVatScheme] - No VAT registration could be found for regId ${id.value}")
      Future.failed(MissingRegDocument(id))
  }

  override def deleteByElement(id: RegistrationId, elementPath: ElementPath)(implicit hc: HeaderCarrier): Future[Boolean] =
    unsetElement(id, elementPath.path)

  override def updateByElement(id: RegistrationId, elementPath: ElementPath, value: String)(implicit hc: HeaderCarrier): Future[String] =
    setElement(id, elementPath.path, value)

  override def prepareRegistrationSubmission(id : RegistrationId, ackRef : String)(implicit hc: HeaderCarrier) : Future[Boolean] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      AcknowledgementReferencePath.path -> ackRef
    )).get

    collection.update(ridSelector(id), BSONDocument("$set" -> modifier)).map(_.ok)
  }

  override def finishRegistrationSubmission(id : RegistrationId, status: VatRegStatus.Value)(implicit hc: HeaderCarrier) : Future[VatRegStatus.Value] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      VatStatusPath.path -> status
    )).get

    collection.update(ridSelector(id), BSONDocument("$set" -> modifier)).map(_ => status)
  }

  override def saveTransId(transId: String, regId: RegistrationId)(implicit hc: HeaderCarrier) : Future[String] = {
    val modifier = BSONFormats.toBSON(Json.obj(
      VatTransIdPath.path -> transId
    )).get

    collection.update(ridSelector(regId), BSONDocument("$set" -> modifier)).map(_ => transId)
  }

  override def fetchRegByTxId(transId: String)(implicit hc: HeaderCarrier): Future[Option[VatScheme]] = {
    collection.find(tidSelector(transId)).one[VatScheme]
  }

  override def updateIVStatus(regId: String, ivStatus: Boolean)(implicit hc: HeaderCarrier): Future[Boolean] = {
    val updateDocument = BSONDocument("$set" -> BSONDocument("lodgingOfficer.ivPassed" -> BSONBoolean(ivStatus)))
    collection.find(regIdSelector(regId)).one[VatScheme] flatMap {
      case Some(_) => collection.update(regIdSelector(regId), updateDocument) map { wr =>
        if(wr.ok) {
          ivStatus
        } else {
          logger.error(s"[updateIVStatus] - There was a problem setting the IV status for regId $regId")
          throw UpdateFailed(RegistrationId(regId), "lodgingOfficer.ivPassed")
        }
      }
      case None =>
        logger.error(s"[updateIVStatus] - No VAT registration could be found for regId ${regId}")
        throw MissingRegDocument(RegistrationId(regId))
    }
  }
}
