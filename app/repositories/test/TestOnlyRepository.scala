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

import javax.inject.{Inject, Named}

import models.api.VatScheme
import reactivemongo.api.DB
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.concurrent.Future

trait TestOnlyRepository {
  def dropCollection(): Future[Unit]
}


class TestOnlyMongoRepository @Inject()(mongoProvider: Function0[DB], @Named("collectionName") collectionName: String)
  extends ReactiveRepository[VatScheme, BSONObjectID](
    collectionName = collectionName,
    mongo = mongoProvider,
    domainFormat = VatScheme.format
  ) with TestOnlyRepository {

  // $COVERAGE-OFF$

  import scala.concurrent.ExecutionContext.Implicits.global

  override def indexes: Seq[Index] = Seq(Index(
    name = Some("RegId"),
    key = Seq("registrationId" -> IndexType.Ascending),
    unique = true
  ))

  override def dropCollection(): Future[Unit] = collection.drop()

  // $COVERAGE-ON$

}

