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

import common.exceptions.DBExceptions.InsertFailed
import models.VatRegistration
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  private val reg = VatRegistration(registrationId = "AC123456", internalId = "09876", timestamp = "timestamp")

  class Setup {
    val repository = new RegistrationMongoRepository(new MongoDBProvider())
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewRegistration" should {

    "create a new, blank VatRegistration with the correct ID" in new Setup {
      val actual = await(repository.createNewRegistration("AC234321", "09876"))
      actual.registrationId shouldBe "AC234321"
    }

    "throw an Insert Failed exception when creating a new VAT reg when one already exists" in new Setup {
      await(repository.createNewRegistration(reg.registrationId, reg.internalId))
      an[InsertFailed] shouldBe thrownBy(await(repository.createNewRegistration(reg.registrationId, reg.internalId)))
    }
  }

  "Calling retrieveRegistration" should {

    "retrieve a registration object" in new Setup {
      await(repository.insert(reg))
      val actual = await(repository.retrieveRegistration(reg.registrationId))
      actual shouldBe Some(reg)
    }

    "return a None when there is no corresponding registration object" in new Setup {
      await(repository.insert(reg))
      await(repository.retrieveRegistration("NOT_THERE")) shouldBe None
    }
  }

}
