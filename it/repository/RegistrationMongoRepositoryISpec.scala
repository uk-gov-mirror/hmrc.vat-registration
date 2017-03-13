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

import common.exceptions._
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
  private val sicAndCompliance: VatSicAndCompliance = VatSicAndCompliance("some-business-description")
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val vatFinancials = VatFinancials(
    bankAccount = Some(VatBankAccount("Reddy", "101010", "100000000000")),
    turnoverEstimate = EstimateValue,
    zeroRatedTurnoverEstimate = Some(zeroRatedTurnoverEstimate),
    reclaimVatOnMostReturns = true,
    vatAccountingPeriod = VatAccountingPeriod(None, "monthly")
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
      val result = await(repository.updateLogicalGroup(regId, vatChoice))
      result shouldBe vatChoice
    }

    "should throw UpdateFailed exception when regId not found" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.updateLogicalGroup("123", vatChoice)))
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

  "Calling deleteBankAccountDetails" should {

    "delete BankAccountDetails object when one exists" in new Setup {
      val vatSchemeWithBankAccount = vatScheme.copy(financials = Some(vatFinancials))
      await(repository.insert(vatSchemeWithBankAccount))
      val actual = await(repository.deleteBankAccountDetails(vatSchemeWithBankAccount.id))
      actual shouldBe true
    }

    "delete BankAccountDetails object when one does not exist" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.deleteBankAccountDetails(vatScheme.id))
      actual shouldBe true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.deleteBankAccountDetails("123")))
    }
  }

  "Calling deleteZeroRatedTurnover" should {

    "delete ZeroRatedTurnover object when one exists" in new Setup {
      val vatSchemeWithBankAccount = vatScheme.copy(financials = Some(vatFinancials))
      await(repository.insert(vatSchemeWithBankAccount))
      val actual = await(repository.deleteZeroRatedTurnover(vatSchemeWithBankAccount.id))
      actual shouldBe true
    }

    "delete ZeroRatedTurnover object when one does not exist" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.deleteZeroRatedTurnover(vatScheme.id))
      actual shouldBe true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.deleteZeroRatedTurnover("123")))
    }
  }

  "Calling deleteAccountingPeriodStart" should {

    "delete AccountingPeriodStart object when one exists" in new Setup {
      val vatFinancialsWithPeriodStart = vatFinancials.copy(vatAccountingPeriod = VatAccountingPeriod(Some("jan_apr_jul_oct"), "quarterly"))
      val vatSchemeWithPeriodStart = vatScheme.copy(financials = Some(vatFinancialsWithPeriodStart))
      await(repository.insert(vatSchemeWithPeriodStart))
      val actual = await(repository.deleteAccountingPeriodStart(vatSchemeWithPeriodStart.id))
      actual shouldBe true
    }

    "delete AccountingPeriodStart object when one does not exist" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.deleteAccountingPeriodStart(vatScheme.id))
      actual shouldBe true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.deleteAccountingPeriodStart("123")))
    }
  }

}
