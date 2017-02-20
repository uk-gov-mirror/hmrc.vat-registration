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

import java.time.LocalDate

import common.exceptions.{InsertFailed, MissingRegDocument}
import models._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  private val date = LocalDate.of(2017, 1, 1)
  private val regId = "AC234321"
  private val vatScheme: VatScheme = VatScheme(regId, None, None, None)
  private val vatChoice: VatChoice = VatChoice(date, "")
  private val tradingDetails: VatTradingDetails = VatTradingDetails("some-trader-name")
  val EstimateValue: Long = 10000000000L
  val zeroRatedTurnoverEstimate : Long = 10000000000L
  val vatFinancials = VatFinancials(Some(VatBankAccount("Reddy", "101010","100000000000")),
                                    EstimateValue,
                                    Some(zeroRatedTurnoverEstimate),
                                    true,
                                    VatAccountingPeriod(None, "monthly")
                                  )


  class Setup {
    val repository = new RegistrationMongoRepository(new MongoDBProvider(), "integration-testing")
    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewVatScheme" should {

    "create a new, blank VatScheme with the correct ID" in new Setup {
      val actual = await(repository.createNewVatScheme(regId))
      actual shouldBe vatScheme
    }

    "throw an InsertFailed exception when creating a new VAT scheme when one already exists" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id))
      an[InsertFailed] shouldBe thrownBy(await(repository.createNewVatScheme(vatScheme.id)))
    }
  }

  "Calling retrieveVatScheme" should {

    "retrieve a VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.retrieveVatScheme(vatScheme.id))
      actual shouldBe Some(vatScheme)
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.retrieveVatScheme("NOT_THERE")) shouldBe None
    }
  }


  "Calling updateVatChoice" should {

    "should update to VatChoice success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateVatChoice(regId, vatChoice))
      result shouldBe vatChoice
    }

    "should throw MissingRegDocument exception when regId not found" in new Setup {
      await(repository.insert(vatScheme))
      an[MissingRegDocument] shouldBe thrownBy(await(repository.updateVatChoice("123", vatChoice)))
    }

  }

  "Calling updateTradingDetails" should {

    "should update VatTradingDetails success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateTradingDetails(regId, tradingDetails))
      result shouldBe tradingDetails
    }

    "should throw MissingRegDocument exception when regId not found" in new Setup {
      await(repository.insert(vatScheme))
      an[MissingRegDocument] shouldBe thrownBy(await(repository.updateTradingDetails("123", tradingDetails)))
    }
  }

  "Calling updateVatFinancials" should {

    "should update updateVatFinancials success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateVatFinancials(regId, vatFinancials))
      result shouldBe vatFinancials
    }

    "should throw MissingRegDocument exception when regId not found" in new Setup {
      await(repository.insert(vatScheme))
      an[MissingRegDocument] shouldBe thrownBy(await(repository.updateVatFinancials("123", vatFinancials)))
    }
  }

  "Calling deleteVatScheme" should {

    "delete a VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.deleteVatScheme(vatScheme.id))
      actual shouldBe true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      an[MissingRegDocument] shouldBe thrownBy(await(repository.deleteVatScheme("123")))
    }
  }

}
