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
import common.{LogicalGroup, RegistrationId}
import itutil.FutureAssertions
import models.{AcknowledgementReferencePath, VatBankAccountPath}
import models.api._
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Writes
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with FutureAssertions with BeforeAndAfterEach with WithFakeApplication {

  private val date = LocalDate.of(2017, 1, 1)
  private val regId = RegistrationId("123")
  private val vatScheme = VatScheme(regId)
  private val vatChoice = VatChoice(necessity = "voluntary", vatStartDate = VatStartDate(selection = "SPECIFIC_DATE", startDate = Some(date)))
  private val tradingName = TradingName(selection = true, Some("some-trading-name"))
  private val vatTradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(
      selection = true,
      eoriApplication = Some(true)
    )
  )
  private val tradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
  )
  private val compliance =
    VatSicAndCompliance(
      businessDescription = "some-business-description",
      culturalCompliance = Some(VatComplianceCultural(true)),
      labourCompliance = Some(VatComplianceLabour(
        labour = true,
        workers = Some(10),
        temporaryContracts = Some(true),
        skilledWorkers = Some(true))),
      financialCompliance = Some(VatComplianceFinancial(adviceOrConsultancyOnly = true, actAsIntermediary = true)),
      mainBusinessActivity = SicCode("88888888", "description", "displayDetails")
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
  val contact = OfficerContactDetails(Some("test@test.com"), None, None)
  val formerName = FormerName(true, Some("Bob Smith"))
  val currentOrPreviousAddress = CurrentOrPreviousAddress(false, Some(scrsAddress))
  val vatLodgingOfficer = VatLodgingOfficer(scrsAddress, DateOfBirth(1, 1, 1980), "NB686868C", "director", name, formerName, currentOrPreviousAddress, contact)

  class Setup {
    val repository = new RegistrationMongoRepository(new MongoDBProvider(), "integration-testing")

    protected def updateLogicalGroup[G: LogicalGroup : Writes](g: G, rid: RegistrationId = regId): Future[G] =
      repository.updateLogicalGroup(rid, g)

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  "Calling createNewVatScheme" should {

    "create a new, blank VatScheme with the correct ID" in new Setup {
      repository.createNewVatScheme(regId) returns vatScheme
    }

    "throw an InsertFailed exception when creating a new VAT scheme when one already exists" in new Setup {
      repository.createNewVatScheme(vatScheme.id).flatMap(_ => repository.createNewVatScheme(vatScheme.id)) failedWith classOf[InsertFailed]
    }

  }

  "Calling retrieveVatScheme" should {

    "retrieve a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme(vatScheme.id)) returns Some(vatScheme)
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.retrieveVatScheme(RegistrationId("NOT_THERE"))) returns None
    }

  }


  "Calling updateLogicalGroup" should {

    "should update to VatTradingDetails success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatTradingDetails)) returns vatTradingDetails
    }

    "should update to VatSicAndCompliance success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(compliance)) returns compliance
    }

    "should update to VatFinancials success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatFinancials)) returns vatFinancials
    }

    "should update to VatContact success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatContact)) returns vatContact
    }

    "should update to VatLodgingOfficer success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatLodgingOfficer)) returns vatLodgingOfficer
    }

    "should update to PPOB success" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(scrsAddress)) returns scrsAddress
    }

    "should throw UpdateFailed exception when regId not found" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatTradingDetails, RegistrationId("0"))) failedWith classOf[UpdateFailed]
    }

  }

  "Calling deleteVatScheme" should {

    "delete a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(vatScheme.id)) returns true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(RegistrationId("0"))) failedWith classOf[MissingRegDocument]
    }
  }

  "Calling deleteByElement" should {

    "delete BankAccountDetails object when one exists" in new Setup {
      val schemeWithAccount = vatScheme.copy(financials = Some(vatFinancials))
      repository.insert(schemeWithAccount).flatMap(_ => repository.deleteByElement(schemeWithAccount.id, VatBankAccountPath)) returns true
    }

    "delete BankAccountDetails object when one does not exist" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteByElement(vatScheme.id, VatBankAccountPath)) returns true
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteByElement(RegistrationId("0"), VatBankAccountPath)) failedWith classOf[UpdateFailed]
    }
  }

  val ACK_REF_NUM = "REF0000001"
  "Calling updateByElement" should {

    "update Element object when one exists" in new Setup {
      val schemeWithAckRefNumber = vatScheme.copy(acknowledgementReference = Some(ACK_REF_NUM))
      repository.insert(schemeWithAckRefNumber).flatMap(_ => repository.updateByElement(vatScheme.id, AcknowledgementReferencePath, ACK_REF_NUM)) returns ACK_REF_NUM
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.updateByElement(RegistrationId("0"), AcknowledgementReferencePath, ACK_REF_NUM)) failedWith classOf[UpdateFailed]
    }
  }


}
