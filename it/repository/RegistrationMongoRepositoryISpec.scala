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
import common.{LogicalGroup, RegistrationId, TransactionId}
import enums.VatRegStatus
import itutil.{FutureAssertions, ITFixtures, MongoBaseSpec}
import models.api._
import models.{AcknowledgementReferencePath, VatBankAccountPath}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json._
import reactivemongo.api.commands.WriteResult
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec extends UnitSpec with MongoBaseSpec with MongoSpecSupport
  with FutureAssertions with BeforeAndAfterEach with WithFakeApplication with ITFixtures {

  class Setup {
    val mongo: RegistrationMongo = fakeApplication.injector.instanceOf[RegistrationMongo]
    val repository: RegistrationMongoRepository = mongo.store

    def insert(json: JsObject): WriteResult = await(repository.collection.insert(json))
    def count: Int = await(repository.count)
    def fetchAll: Option[JsObject] = await(repository.collection.find(Json.obj()).one[JsObject])

    protected def updateLogicalGroup[G: LogicalGroup : Writes](g: G, rid: RegistrationId = regId): Future[G] =
      repository.updateLogicalGroup(rid, g)

    await(repository.drop)
    await(repository.ensureIndexes)
  }

  val registrationId: String = "reg-12345"
  val otherRegId = "other-reg-12345"

  def vatSchemeJson(regId: String = registrationId): JsObject = Json.parse(
    s"""
       |{
       | "registrationId":"$regId",
       | "status":"draft"
       |}
      """.stripMargin).as[JsObject]

  val otherUsersVatScheme: JsObject = vatSchemeJson(otherRegId)

  val accountNumber = "12345678"
  val encryptedAccountNumber = "V0g2RXVUcUZpSUk4STgvbGNFdlAydz09"
  val sortCode = "12-34-56"
  val bankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
  val bankAccount = BankAccount(isProvided = true, Some(bankAccountDetails))
  val vatSchemeWithBankAccount: JsObject = Json.parse(
    s"""
       |{
       | "registrationId":"$registrationId",
       | "status":"draft",
       | "bankAccount":{
       |   "isProvided":true,
       |   "details":{
       |     "name":"testAccountName",
       |     "sortCode":"$sortCode",
       |     "number":"$encryptedAccountNumber"
       |    }
       |  }
       |}
      """.stripMargin).as[JsObject]

  val vatSchemeWithReturns: JsObject = Json.parse(
    s"""
       |{
       |  "registrationId":"$registrationId",
       |  "status":"draft",
       |  "returns":{
       |    "reclaimVatOnMostReturns":true,
       |    "frequency":"quarterly",
       |    "staggerStart":"jan",
       |    "start":{
       |      "date":"$date"
       |    }
       |  }
       |}
     """.stripMargin).as[JsObject]

  val vatTaxable = 1000L
  val turnoverEstimates: TurnoverEstimates = TurnoverEstimates(vatTaxable)
  def vatSchemeWithTurnoverEstimates(regId: String = registrationId): JsObject = vatSchemeJson(regId) ++ Json.parse(
    """
      |{
      | "turnoverEstimates":{
      |   "vatTaxable":1000
      | }
      |}
    """.stripMargin).as[JsObject]

  "Calling createNewVatScheme" should {

    "create a new, blank VatScheme with the correct ID" in new Setup {
      await(repository.createNewVatScheme(regId)) shouldBe vatScheme
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

    "should throw UpdateFailed exception when regId not found" in new Setup {
      repository.insert(vatScheme).flatMap(_ => updateLogicalGroup(vatTradingDetails, RegistrationId("0"))) failedWith classOf[UpdateFailed]
    }

  }

  "Calling deleteVatScheme" should {

    "delete a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(vatScheme.id.value)) returns true
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

  "Calling prepareRegistrationSubmission" should {
    val testAckRef = "testAckRef"
    "update the vat scheme with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.acknowledgementReference

      await(result).get shouldBe testAckRef
    }
  }

  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.submitted)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) shouldBe VatRegStatus.submitted
    }

    "update the vat scheme to held with the provided ackref" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.held)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) shouldBe VatRegStatus.held
    }
  }

  "Calling saveTransId" should {
    "store the transaction id provided into the specified vat scheme document" in new Setup {
      val testTransId = "testTransId"

      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.transactionId

      await(result).get shouldBe TransactionId(testTransId)
    }
  }

  "Calling fetchRegIdByTxId" should {
    "retrieve the vat scheme by transactionid" in new Setup {
      val testTransId = "testTransId"

      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme)   <- repository.fetchRegByTxId(testTransId)
      } yield updatedScheme

      await(result) shouldBe vatScheme.copy(transactionId = Some(TransactionId(testTransId)))
    }
  }

  "updateTradingDetails" should {

    val tradingName = "testTradingName"

    def vatSchemeWithTradingDetailsJson: JsObject = Json.parse(
      s"""
         |{
         | "registrationId":"$registrationId",
         | "status":"draft",
         | "tradingDetails":{
         |   "tradingName":"$tradingName"
         | }
         |}
      """.stripMargin).as[JsObject]

    val otherRegId = "other-reg-12345"
    val otherUsersVatScheme = vatSchemeJson(otherRegId)

    val tradingDetails: TradingDetails = TradingDetails(Some(tradingName), None)

    "update the registration doc with the provided trading details" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(vatSchemeWithTradingDetailsJson)
    }

    "not update or insert new data into the registration doc if the supplied details already exist on the doc" in new Setup {
      insert(vatSchemeWithTradingDetailsJson)

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(vatSchemeWithTradingDetailsJson)
    }

    "not update or insert trading details if a registration doc doesn't already exist" in new Setup {
      count shouldBe 0

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe None
    }

    "not update or insert trading details if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id shouldBe Some(otherUsersVatScheme)

      await(repository.updateTradingDetails(registrationId, tradingDetails))

      fetchAll without _id shouldBe Some(otherUsersVatScheme)
    }
  }

  "Calling retrieveTradingDetails" should {

    val tradingName = "testTradingName"

    val tradingDetails: TradingDetails = TradingDetails(Some(tradingName), Some(true))

    def vatSchemeWithTradingDetailsJson(regId: String): JsObject = Json.parse(
      s"""
         |{
         | "registrationId":"$regId",
         | "status":"draft",
         | "tradingDetails":{
         |   "tradingName":"$tradingName",
         |   "eoriRequested":true
         | }
         |}
      """.stripMargin).as[JsObject]

    "retrieve a TradingDetails object if it exists for the registration id" in new Setup {
      insert(vatSchemeWithTradingDetailsJson(registrationId))
      await(repository.retrieveTradingDetails(registrationId)).get shouldBe tradingDetails
    }

    "return None if a TradingDetails does not exist for the registration id" in new Setup {
      await(repository.retrieveTradingDetails(registrationId)) shouldBe None
    }

  }

  "fetchBankAccount" should {

    "return a BankAccount case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithBankAccount)

      val fetchedBankAccount: Option[BankAccount] = repository.fetchBankAccount(registrationId)

      fetchedBankAccount shouldBe Some(bankAccount)
    }

    "return a None if a VatScheme already exists but a bank account block does not" in new Setup {
      insert(vatSchemeJson(registrationId))
      val fetchedBankAccount: Option[BankAccount] = repository.fetchBankAccount(registrationId)
      fetchedBankAccount shouldBe None
    }

    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count shouldBe 0

      val fetchedBankAccount: Option[BankAccount] = repository.fetchBankAccount(registrationId)

      fetchedBankAccount shouldBe None
    }

    "return None if other users' data exists but no BankAccount is found in mongo for the supplied regId" in new Setup {
      insert(otherUsersVatScheme)

      val fetchedBankAccount: Option[BankAccount] = repository.fetchBankAccount(registrationId)

      fetchedBankAccount shouldBe None
    }
  }

  "updateBankAccount" should {

    "update the registration doc with the provided bank account details and encrypt the account number" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id shouldBe Some(vatSchemeWithBankAccount)
    }

    "not update or insert new data into the registration doc if the supplied bank account details already exist on the doc" in new Setup {
      insert(vatSchemeWithBankAccount)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id shouldBe Some(vatSchemeWithBankAccount)
    }

    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count shouldBe 0

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id shouldBe None
    }

    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id shouldBe Some(otherUsersVatScheme)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id shouldBe Some(otherUsersVatScheme)
    }
  }

  "fetchTurnoverEstimates" should {

    "return a TurnoverEstimates case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithTurnoverEstimates())

      val fetchedTurnoverEstimates: Option[TurnoverEstimates] = repository.fetchTurnoverEstimates(registrationId)
      fetchedTurnoverEstimates shouldBe Some(turnoverEstimates)
    }

    "return None when a document exists for the user but there is no TurnoverEstimates block" in new Setup {
      insert(vatSchemeJson())

      val fetchedTurnoverEstimates: Option[TurnoverEstimates] = repository.fetchTurnoverEstimates(registrationId)
      fetchedTurnoverEstimates shouldBe None
    }

    "throw a MissingRegDocument exception if a registration document is not found for the provided reg Id" in new Setup {
      val ex: MissingRegDocument = intercept[MissingRegDocument](await(repository.fetchTurnoverEstimates(registrationId)))
      ex.getMessage shouldBe s"No Registration document found for regId: $registrationId"
    }
  }

  "updateTurnoverEstimates" should {

    val vatTaxable = 1000L
    val turnoverEstimate = TurnoverEstimates(vatTaxable)

    val vatSchemeWithTurnoverEstimate = Json.parse(
      s"""
        |{
        | "registrationId":"$registrationId",
        | "status":"draft",
        | "turnoverEstimates":{
        |   "vatTaxable":$vatTaxable
        | }
        |}
        |
      """.stripMargin).as[JsObject]

    "update the registration doc with the provided returns" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateTurnoverEstimates(registrationId, turnoverEstimate))

      fetchAll without _id shouldBe Some(vatSchemeWithTurnoverEstimate)
    }

    "not update or insert new data into the registration doc if the supplied returns already exist on the doc" in new Setup {
      insert(vatSchemeWithTurnoverEstimate)

      await(repository.updateTurnoverEstimates(registrationId, turnoverEstimate))

      fetchAll without _id shouldBe Some(vatSchemeWithTurnoverEstimate)
    }

    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count shouldBe 0

      await(repository.updateTurnoverEstimates(registrationId, turnoverEstimate))

      fetchAll without _id shouldBe None
    }

    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id shouldBe Some(otherUsersVatScheme)

      await(repository.updateTurnoverEstimates(registrationId, turnoverEstimate))

      fetchAll without _id shouldBe Some(otherUsersVatScheme)
    }
  }

  "fetchReturns" should {
    "return a Returns case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithReturns)

      val fetchedReturns: Option[Returns] = repository.fetchReturns(registrationId)

      fetchedReturns shouldBe Some(returns)
    }

    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count shouldBe 0

      val fetchedReturns: Option[Returns] = repository.fetchReturns(registrationId)

      fetchedReturns shouldBe None
    }
  }

  "updateReturns" should {

    val registrationId: String = "reg-12345"

    val otherRegId = "other-reg-12345"
    val otherUsersVatScheme = vatSchemeJson(otherRegId)

    val MONTHLY = "monthly"
    val JAN = "jan"

    val dateValue = LocalDate of (1990, 10, 10)
    val startDate = StartDate(Some(dateValue))

    val returns: Returns = Returns(reclaimVatOnMostReturns = true, MONTHLY, Some(JAN), startDate)

    val vatSchemeWithReturns = Json.parse(
      s"""
        |{
        | "registrationId":"$registrationId",
        | "status":"draft",
        | "returns":{
        |   "reclaimVatOnMostReturns":true,
        |   "frequency":"$MONTHLY",
        |   "staggerStart":"$JAN",
        |   "start":{
        |     "date":"$dateValue"
        |   }
        | }
        |}
      """.stripMargin).as[JsObject]

    "update the registration doc with the provided returns" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id shouldBe Some(vatSchemeWithReturns)
    }

    "not update or insert new data into the registration doc if the supplied returns already exist on the doc" in new Setup {
      insert(vatSchemeWithReturns)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id shouldBe Some(vatSchemeWithReturns)
    }

    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count shouldBe 0

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id shouldBe None
    }

    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id shouldBe Some(otherUsersVatScheme)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id shouldBe Some(otherUsersVatScheme)
    }
  }

  "Calling getEligibility" should {
    val eligibility = Eligibility(version = 1, result = "testResult")

    "return eligibility data from an existing registration containing data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(eligibility = Some(eligibility)))
        res <- repository.getEligibility(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(eligibility)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme)
        res <- repository.getEligibility(vatScheme.id.value)
      } yield res

      await(result) shouldBe None
    }

    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(eligibility = Some(eligibility)))
        res <- repository.getEligibility("wrongRegId")
      } yield res

      a[MissingRegDocument] shouldBe thrownBy(await(result))
    }

    "return an exception if there is no version in eligibility block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "eligibility": {
           |    "result": "test result"
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getEligibility(regId)))
    }
  }

  "Calling updateEligibility" should {
    val eligibility = Eligibility(version = 1, result = "testResult")

    "update eligibility block in registration when there is no eligibility data" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme)
        _                   <- repository.updateEligibility(vatScheme.id.value, eligibility)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.eligibility

      await(result) shouldBe Some(eligibility)
    }

    "update eligibility block in registration when there is already eligibility data" in new Setup {
      val otherEligibility = Eligibility(version = 2, result = "oldResult")
      val result = for {
        _                   <- repository.insert(vatScheme.copy(eligibility = Some(otherEligibility)))
        _                   <- repository.updateEligibility(vatScheme.id.value, eligibility)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.eligibility

      await(result) shouldBe Some(eligibility)
    }

    "not update or insert eligibility if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateEligibility("wrongRegId", eligibility)))
    }
  }

  "Calling getThreshold" should {
    val threshold = Threshold(mandatoryRegistration = false,
                              voluntaryReason = Some("a reason"),
                              overThresholdDate = Some(LocalDate.of(2017, 12, 28)),
                              expectedOverThresholdDate = Some(LocalDate.of(2018, 1, 31)))

    "return threshold data from an existing registration containing data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
        res <- repository.getThreshold(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(threshold)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme)
        res <- repository.getThreshold(vatScheme.id.value)
      } yield res

      await(result) shouldBe None
    }

    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
        res <- repository.getThreshold("wrongRegId")
      } yield res

      a[MissingRegDocument] shouldBe thrownBy(await(result))
    }

    "return an exception if there is no mandatory registration in threshold block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "threshold": {
           |    "voluntaryReason": "test reason"
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getThreshold(regId)))
    }
  }

  "Calling updateThreshold" should {
    val threshold = Threshold(mandatoryRegistration = false, voluntaryReason = Some("a reason"), overThresholdDate = None, expectedOverThresholdDate = None)

    "update threshold block in registration when there is no threshold data" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme)
        _                   <- repository.updateThreshold(vatScheme.id.value, threshold)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.threshold

      await(result) shouldBe Some(threshold)
    }

    "update threshold block in registration when there is already threshold data" in new Setup {
      val otherThreshold = Threshold(mandatoryRegistration = true, voluntaryReason = None, overThresholdDate = None, expectedOverThresholdDate = None)
      val result = for {
        _                   <- repository.insert(vatScheme.copy(threshold = Some(otherThreshold)))
        _                   <- repository.updateThreshold(vatScheme.id.value, threshold)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.threshold

      await(result) shouldBe Some(threshold)
    }

    "not update or insert threshold if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateThreshold("wrongRegId", threshold)))
    }
  }

  "Calling getLodgingOfficer" should {
    val lodgingOfficerDetails = LodgingOfficerDetails(
      currentAddress = scrsAddress,
      changeOfName = None,
      previousAddress = None,
      contact = VatDigitalContact(
        email = "test@t.com",
        tel = None,
        mobile = None
      )
    )
    val lodgingOfficer = LodgingOfficer(
      dob = LocalDate.of(1990, 1, 30),
      nino = "NB686868C",
      role = "director",
      name = name,
      ivPassed = Some(true),
      details = None
    )

    "return lodgingOfficer data from an existing registration containing data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(lodgingOfficer = Some(lodgingOfficer)))
        res <- repository.getLodgingOfficer(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(lodgingOfficer)
    }

    "return lodgingOfficer with details data from an existing registration containing data" in new Setup {
      val lodgingOfficerWithDetails = lodgingOfficer.copy(details = Some(lodgingOfficerDetails))
      val result = for {
        _   <- repository.insert(vatScheme.copy(lodgingOfficer = Some(lodgingOfficerWithDetails)))
        res <- repository.getLodgingOfficer(vatScheme.id.value)
      } yield res

      await(result) shouldBe Some(lodgingOfficerWithDetails)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme)
        res <- repository.getLodgingOfficer(vatScheme.id.value)
      } yield res

      await(result) shouldBe None
    }

    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result = for {
        _   <- repository.insert(vatScheme.copy(lodgingOfficer = Some(lodgingOfficer)))
        res <- repository.getLodgingOfficer("wrongRegId")
      } yield res

      a[MissingRegDocument] shouldBe thrownBy(await(result))
    }

    "return an exception if there is no name in lodging officer block in repository" in new Setup {
      val regId = "reg-123"
      val json: JsObject = Json.parse(
        s"""
           |{
           |  "registrationId": "$regId",
           |  "status": "${VatRegStatus.draft}",
           |  "lodgingOfficer": {
           |    "nino": "SS111111S",
           |    "dob" : "2011-11-11",
           |    "role" : "director"
           |  }
           |}
         """.stripMargin).as[JsObject]

      insert(json)

      an[Exception] shouldBe thrownBy(await(repository.getLodgingOfficer(regId)))
    }
  }

  "Calling updateLodgingOfficer" should {
    val lodgingOfficer = LodgingOfficer(
      dob = LocalDate.of(1990, 1, 30),
      nino = "NB686868C",
      role = "director",
      name = name,
      ivPassed = None,
      details = None
    )

    "update lodgingOfficer block in registration when there is no lodgingOfficer data" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme)
        _                   <- repository.updateLodgingOfficer(vatScheme.id.value, lodgingOfficer)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.lodgingOfficer

      await(result) shouldBe Some(lodgingOfficer)
    }

    "update lodgingOfficer block in registration when there is already lodgingOfficer data" in new Setup {
      val lodgingOfficerDetails = LodgingOfficerDetails(
        currentAddress = scrsAddress,
        changeOfName = None,
        previousAddress = None,
        contact = VatDigitalContact(
          email = "test@t.com",
          tel = None,
          mobile = None
        )
      )
      val otherLodgingOfficer = LodgingOfficer(
        dob = LocalDate.of(1988, 12, 15),
        nino = "NB535353C",
        role = "secretary",
        name = oldName,
        ivPassed = Some(true),
        details = Some(lodgingOfficerDetails)
      )
      val result = for {
        _                   <- repository.insert(vatScheme.copy(lodgingOfficer = Some(lodgingOfficer)))
        _                   <- repository.updateLodgingOfficer(vatScheme.id.value, otherLodgingOfficer)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.lodgingOfficer

      await(result) shouldBe Some(otherLodgingOfficer)
    }

    "not update or insert lodgingOfficer if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateLodgingOfficer("wrongRegId", lodgingOfficer)))
    }
  }

  "Calling updateIVStatus" should {
    val lodgingOfficer = LodgingOfficer(
      dob = LocalDate.of(1990, 1, 30),
      nino = "NB686868C",
      role = "director",
      name = name,
      ivPassed = None,
      details = None
    )

    "update lodgingOfficer block with ivPassed in registration" in new Setup {
      val result = for {
        _                   <- repository.insert(vatScheme.copy(lodgingOfficer = Some(lodgingOfficer)))
        _                   <- repository.updateIVStatus(vatScheme.id.value, true)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.lodgingOfficer

      await(result) shouldBe Some(lodgingOfficer.copy(ivPassed = Some(true)))
    }

    "not update lodgingOfficer if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateIVStatus("wrongRegId", true)))
    }

    "not update lodgingOfficer if registration is missing a lodgingOfficer block" in new Setup {
      await(repository.insert(vatScheme))

      count shouldBe 1
      await(repository.findAll()).head shouldBe vatScheme

      a[MissingRegDocument] shouldBe thrownBy(await(repository.updateIVStatus(vatScheme.id.value, true)))
    }
  }

  "calling getSicAndCompliance" should {
    val validSicAndCompliance = Some(SicAndCompliance(
      "this is my business description",
      ComplianceLabour(1000,Some(true),Some(true)),
      SicCode("11111111","the flu","sic details")
    ))
    "return a SicAndComplianceModel from existing data based on the reg Id" in new Setup {
      val result = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateSicAndCompliance(vatScheme.id.value,validSicAndCompliance.get)
        _ = count shouldBe 1
        res <- repository.getSicAndCompliance(vatScheme.id.value)
      } yield res

      await(result).get shouldBe validSicAndCompliance.get
    }
    "return None from an existing registration that exists but SicAndCompliance does not exist" in new Setup {
      val result = for {
        _ <- repository.insert(vatScheme)
        res <- repository.getSicAndCompliance(vatScheme.id.value)
      } yield res
      await(result) shouldBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result = repository.getSicAndCompliance("madeUpRegId")

      a[MissingRegDocument] shouldBe thrownBy(await(result))
    }
    "return an exception if the json is incorrect in the repository (an element is missing)" in new Setup {
     val json = Json.toJson(
       Json.obj("registrationId" -> regId.value,
                "status" -> VatRegStatus.draft,
                "sicAndCompliance" -> Json.toJson(validSicAndCompliance).as[JsObject].-( "businessDescription")))
      insert(json.as[JsObject])
      an[Exception] shouldBe thrownBy(await(repository.getSicAndCompliance(vatScheme.id.value)))
    }
  }
  "calling updateSicAndCompliance" should {
    val validSicAndCompliance = Some(SicAndCompliance(
      "this is my business description",
      ComplianceLabour(1000,Some(true),Some(true)),
      SicCode("12345678","the flu","sic details")
    ))
    "return an amended SicAndCompliance Model when an entry already exists in the repo for 1 field" in new Setup {
      val amendedModel = validSicAndCompliance.map(a => a.copy(businessDescription = "fooBarWizz"))
      val result = for {
      _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
      _ = count shouldBe 1
      res <- repository.updateSicAndCompliance(vatScheme.id.value, amendedModel.get)
      } yield res

      await(result) shouldBe amendedModel.get
    }
    "return an amended Option SicAndCompliance Model when an entry already exists and all fields have changed in the model" in new Setup {
      val amendedModel = SicAndCompliance(
        "foo",
        ComplianceLabour(1,None,None),
        SicCode("foo","bar","wizz"))
      val result = for{
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
        res <- repository.updateSicAndCompliance(vatScheme.id.value, amendedModel)
      }yield res
      await(result) shouldBe amendedModel
    }
    "return an SicAndComplance Model when the block did not exist in the existing reg doc" in new Setup {
      val result = for {
        _ <- repository.insert(vatScheme)
        _ = count shouldBe 1
        res <- repository.updateSicAndCompliance(vatScheme.id.value, validSicAndCompliance.get)
      }yield res
          await(result) shouldBe validSicAndCompliance.get
      }

    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result = repository.updateSicAndCompliance("madeUpRegId",validSicAndCompliance.get)
      a[MissingRegDocument] shouldBe thrownBy(await(result))
    }
  }

}
