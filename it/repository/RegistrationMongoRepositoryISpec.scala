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

import common.RegistrationId
import common.exceptions._
import models.VatBankAccountPath
import models.api._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.concurrent.{Eventually, ScalaFutures}
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with BeforeAndAfterEach with ScalaFutures with Eventually with WithFakeApplication {

  private val date = LocalDate.of(2017, 1, 1)
  private val regId = RegistrationId("AC234321")
  private val vatScheme = VatScheme(regId)
  private val vatChoice = VatChoice(
    necessity = "voluntary",
    vatStartDate = VatStartDate(
      selection = "SPECIFIC_DATE",
      startDate = Some(date)))
  private val tradingName = TradingName(selection = true, Some("some-trading-name"))
  private val vatTradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(true, Some(true))
  )
  private val tradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(true, Some(true))
  )
  private val culturalSicAndCompliance =
    VatSicAndCompliance(
      businessDescription = "some-business-description",
      culturalCompliance = Some(VatComplianceCultural(true)),
      labourCompliance = Some(VatComplianceLabour(
        labour = true,
        workers = Some(10),
        temporaryContracts = Some(true),
        skilledWorkers = Some(true))),
      financialCompliance = Some(VatComplianceFinancial(true, true))
    )

  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val vatFinancials = VatFinancials(
    bankAccount = Some(VatBankAccount("Reddy", "101010", "100000000000")),
    turnoverEstimate = EstimateValue,
    zeroRatedTurnoverEstimate = Some(zeroRatedTurnoverEstimate),
    reclaimVatOnMostReturns = true,
    accountingPeriods = VatAccountingPeriod("monthly")
  )

  val vatDigitalContact = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))

  val vatContact = VatContact(vatDigitalContact)

  val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val name = Name(forename = Some("Forename"), surname = Some("Surname"), title = Some("Title"))
  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress, DateOfBirth(1,1,1980), "NB686868C", "director", name)

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
      await(repository.retrieveVatScheme(RegistrationId("NOT_THERE"))) shouldBe None
    }
  }


  "Calling updateLogicalGroup" should {

    "should update to VatTradingDetails success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateLogicalGroup(regId, vatTradingDetails))
      result shouldBe vatTradingDetails
    }

    "should update to VatSicAndCompliance success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateLogicalGroup(regId, culturalSicAndCompliance))
      result shouldBe culturalSicAndCompliance
    }


    "should update to VatFinancials success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateLogicalGroup(regId, vatFinancials))
      result shouldBe vatFinancials
    }


    "should update to VatContact success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateLogicalGroup(regId, vatContact))
      result shouldBe vatContact
    }


    "should update to VatLodgingOfficer success" in new Setup {
      await(repository.insert(vatScheme))
      val result = await(repository.updateLogicalGroup(regId, vatLodgingOfficer))
      result shouldBe vatLodgingOfficer
    }



    "should throw UpdateFailed exception when regId not found" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.updateLogicalGroup(RegistrationId("123"), vatTradingDetails)))
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
      an[MissingRegDocument] shouldBe thrownBy(await(repository.deleteVatScheme(RegistrationId("123"))))
    }
  }

  "Calling deleteByElement" should {

    "delete BankAccountDetails object when one exists" in new Setup {
      val vatSchemeWithBankAccount = vatScheme.copy(financials = Some(vatFinancials))
      await(repository.insert(vatSchemeWithBankAccount))
      val actual = await(repository.deleteByElement(vatSchemeWithBankAccount.id, VatBankAccountPath))
      actual shouldBe true
    }

    "delete BankAccountDetails object when one does not exist" in new Setup {
      await(repository.insert(vatScheme))
      val actual = await(repository.deleteByElement(vatScheme.id, VatBankAccountPath))
      actual shouldBe true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      await(repository.insert(vatScheme))
      an[UpdateFailed] shouldBe thrownBy(await(repository.deleteByElement(RegistrationId("123"), VatBankAccountPath)))
    }
  }

}
