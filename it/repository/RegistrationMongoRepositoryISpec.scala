/*
 * Copyright 2016 HM Revenue & Customs
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

package repository

import common.exceptions.InsertFailed
import models.{VatChoice, VatScheme, VatTradingDetails}
import org.joda.time.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  val fixedDate = new DateTime(2017, 1, 31, 13, 53)
  private val internalId = "internalId"
  private val reg = VatScheme(id = "AC123456", tradingDetails = VatTradingDetails("trading name"), VatChoice(fixedDate, "necessity"))

  class Setup {
    val repository = new RegistrationMongoRepository(new MongoDBProvider(), "integration-testing")
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewRegistration" should {

    "create a new, blank VatRegistration with the correct ID" in new Setup {
      val actual = await(repository.createNewRegistration("AC234321", "09876"))
      actual.id shouldBe "AC234321"
    }

    "throw an Insert Failed exception when creating a new VAT reg when one already exists" in new Setup {
      await(repository.createNewRegistration(reg.id, internalId))
      an[InsertFailed] shouldBe thrownBy(await(repository.createNewRegistration(reg.id, internalId)))
    }
  }

  "Calling retrieveRegistration" should {

    "retrieve a registration object" in new Setup {
      await(repository.insert(reg))
      val actual = await(repository.retrieveRegistration(reg.id))
      actual shouldBe Some(reg)
    }

    "return a None when there is no corresponding registration object" in new Setup {
      await(repository.insert(reg))
      await(repository.retrieveRegistration("NOT_THERE")) shouldBe None
    }
  }

}
