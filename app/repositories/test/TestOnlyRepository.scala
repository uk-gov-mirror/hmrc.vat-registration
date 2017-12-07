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

package repositories.test

import javax.inject.Inject

import models.api.VatScheme
import play.modules.reactivemongo.ReactiveMongoComponent
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.mongo.ReactiveRepository
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future

class TestOnlyMongo @Inject()(mongo: ReactiveMongoComponent) extends ReactiveMongoFormats {
  val store = new TestOnlyMongoRepository(mongo.mongoConnector.db)
}

trait TestOnlyRepository {
  def dropCollection(implicit hc: HeaderCarrier): Future[Unit]
}
class TestOnlyMongoRepository (mongo: () => DB)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = "test",
    mongo = mongo,
    domainFormat = VatScheme.format) with TestOnlyRepository {

  // $COVERAGE-OFF$

  override def indexes: Seq[Index] = Seq(
    Index(
      name     = Some("RegId"),
      key     = Seq("registrationId" -> IndexType.Ascending),
      unique  = true
    )
  )

  override def dropCollection(implicit hc: HeaderCarrier): Future[Unit] = collection.drop()

  // $COVERAGE-ON$
}


