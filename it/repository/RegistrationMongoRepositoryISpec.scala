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
import common.{RegistrationId, TransactionId}
import enums.VatRegStatus
import itutil.{FutureAssertions, ITFixtures, MongoBaseSpec}
import models.AcknowledgementReferencePath
import models.api._
import play.api.libs.json._
import play.api.test.Helpers._
import reactivemongo.api.commands.WriteResult
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.mongo.MongoSpecSupport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec extends MongoBaseSpec with MongoSpecSupport with FutureAssertions with ITFixtures {

  class Setup {
    val repository: RegistrationMongoRepository = fakeApplication.injector.instanceOf[RegistrationMongoRepository]

    def insert(json: JsObject): WriteResult = await(repository.collection.insert(json))

    def count: Int = await(repository.count)

    def fetchAll: Option[JsObject] = await(repository.collection.find(Json.obj()).one[JsObject])

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
  val bankAccountDetails: BankAccountDetails = BankAccountDetails("testAccountName", sortCode, accountNumber)
  val bankAccount: BankAccount = BankAccount(isProvided = true, Some(bankAccountDetails))
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

  val vatSchemeWithFlatRateScheme: JsObject = Json.parse(
    s"""
       |{
       |  "registrationId":"$registrationId",
       |  "status":"draft",
       |  "flatRateScheme":{
       |    "joinFrs":false,
       |    "frsDetails":{
       |      "overBusinessGoods":false,
       |      "overBusinessGoodsPercent":true,
       |      "vatInclusiveTurnover":12345678,
       |      "start":{
       |        "date":"$date"
       |      },
       |      "categoryOfBusiness":"testCategory",
       |      "percent":15
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
      await(repository.createNewVatScheme(regId, internalid)) mustBe vatScheme
    }
    "throw an InsertFailed exception when creating a new VAT scheme when one already exists with the same int Id and reg id" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id, internalid))
      intercept[InsertFailed](await(repository.createNewVatScheme(vatScheme.id, internalid)))
    }
    "throw an InsertFailed exception when creating a new VAT scheme where one already exists with the same regId but different Internal id" in new Setup {
      await(repository.createNewVatScheme(vatScheme.id, internalid))
      intercept[InsertFailed](await(repository.createNewVatScheme(vatScheme.id, "fooBarWizz")))
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

  "Calling deleteVatScheme" should {

    "delete a VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.deleteVatScheme(vatScheme.id.value)) returns true
    }
  }


  "Calling clearDownScheme" should {
    "clear any optional data from the vat scheme object" in new Setup {
      await(repository.insert(fullVatScheme))
      await(repository.clearDownDocument(transactionId)) mustBe true
      await(repository.retrieveVatScheme(regId)) mustBe None
    }
    "fail when a already cleared document is cleared" in new Setup {
      await(repository.insert(fullVatScheme))
      await(repository.clearDownDocument(transactionId)) mustBe true
      await(repository.retrieveVatScheme(regId)) mustBe None
      await(repository.clearDownDocument(transactionId)) mustBe true
      await(repository.retrieveVatScheme(regId)) mustBe None
    }
  }


  val ACK_REF_NUM = "REF0000001"
  "Calling updateByElement" should {

    "update Element object when one exists" in new Setup {
      val schemeWithAckRefNumber: VatScheme = vatScheme.copy(acknowledgementReference = Some(ACK_REF_NUM))
      repository.insert(schemeWithAckRefNumber).flatMap(_ => repository.updateByElement(vatScheme.id,
        AcknowledgementReferencePath, ACK_REF_NUM)) returns ACK_REF_NUM
    }

    "return a None when there is no corresponding VatScheme object" in new Setup {
      repository.insert(vatScheme).flatMap(_ => repository.updateByElement(RegistrationId("0"),
        AcknowledgementReferencePath, ACK_REF_NUM)) failedWith classOf[UpdateFailed]
    }
  }

  "Calling prepareRegistrationSubmission" should {
    val testAckRef = "testAckRef"
    "update the vat scheme with the provided ackref" in new Setup {
      val result: Future[(VatRegStatus.Value, Option[String])] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef, VatRegStatus.draft)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield (updatedScheme.status, updatedScheme.acknowledgementReference)

      await(result) mustBe(VatRegStatus.locked, Some(testAckRef))
    }

    "update the vat scheme with the provided ackref on a topup" in new Setup {
      val result: Future[(VatRegStatus.Value, Option[String])] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.prepareRegistrationSubmission(vatScheme.id, testAckRef, VatRegStatus.held)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield (updatedScheme.status, updatedScheme.acknowledgementReference)

      await(result) mustBe(VatRegStatus.held, Some(testAckRef))
    }
  }

  "Calling finishRegistrationSubmission" should {
    "update the vat scheme to submitted with the provided ackref" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.submitted)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.submitted
    }

    "update the vat scheme to held with the provided ackref" in new Setup {
      val result: Future[VatRegStatus.Value] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.finishRegistrationSubmission(vatScheme.id, VatRegStatus.held)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.status

      await(result) mustBe VatRegStatus.held
    }
  }

  "Calling saveTransId" should {
    "store the transaction id provided into the specified vat scheme document" in new Setup {
      val testTransId = "testTransId"

      val result: Future[Option[TransactionId]] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.transactionId

      await(result).get mustBe TransactionId(testTransId)
    }
  }

  "Calling fetchRegIdByTxId" should {
    "retrieve the vat scheme by transactionid" in new Setup {
      val testTransId = "testTransId"

      val result: Future[VatScheme] = for {
        insert <- repository.insert(vatScheme)
        update <- repository.saveTransId(testTransId, vatScheme.id)
        Some(updatedScheme) <- repository.fetchRegByTxId(testTransId)
      } yield updatedScheme

      await(result) mustBe vatScheme.copy(transactionId = Some(TransactionId(testTransId)))
    }
  }

  "updateTradingDetails" should {

    val tradingDetails = TradingDetails(Some("trading-name"), true)

    "update tradingDetails block in registration when there is no tradingDetails data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateTradingDetails(vatScheme.id.value, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }

    "update tradingDetails block in registration when there is already tradingDetails data" in new Setup {
      val otherTradingDetails = TradingDetails(Some("other-trading-name"), true)
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        _ <- repository.updateTradingDetails(vatScheme.id.value, tradingDetails)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.tradingDetails

      await(result) mustBe Some(tradingDetails)
    }

    "not update or insert tradingDetails if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count mustBe 1
      await(repository.findAll()).head mustBe vatScheme

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateTradingDetails("wrongRegId", tradingDetails)))
    }
  }

  "Calling retrieveTradingDetails" should {

    val tradingDetails = TradingDetails(Some("trading-name"), true)

    "return trading details data from an existing registration containing data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails(vatScheme.id.value)
      } yield res

      await(result) mustBe Some(tradingDetails)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.retrieveTradingDetails(vatScheme.id.value)
      } yield res

      await(result) mustBe None
    }

    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[TradingDetails]] = for {
        _ <- repository.insert(vatScheme.copy(tradingDetails = Some(tradingDetails)))
        res <- repository.retrieveTradingDetails("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }

  "fetchBankAccount" should {

    "return a BankAccount case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithBankAccount)

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe Some(bankAccount)
    }

    "return a None if a VatScheme already exists but a bank account block does not" in new Setup {
      insert(vatSchemeJson(registrationId))
      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))
      fetchedBankAccount mustBe None
    }

    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count mustBe 0

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe None
    }

    "return None if other users' data exists but no BankAccount is found in mongo for the supplied regId" in new Setup {
      insert(otherUsersVatScheme)

      val fetchedBankAccount: Option[BankAccount] = await(repository.fetchBankAccount(registrationId))

      fetchedBankAccount mustBe None
    }
  }

  "updateBankAccount" should {

    "update the registration doc with the provided bank account details and encrypt the account number" in new Setup {
      insert(vatSchemeJson())

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(vatSchemeWithBankAccount)
    }

    "not update or insert new data into the registration doc if the supplied bank account details already exist on the doc" in new Setup {
      insert(vatSchemeWithBankAccount)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(vatSchemeWithBankAccount)
    }

    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count mustBe 0

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe None
    }

    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id mustBe Some(otherUsersVatScheme)

      await(repository.updateBankAccount(registrationId, bankAccount))

      fetchAll without _id mustBe Some(otherUsersVatScheme)
    }
  }

  "fetchReturns" should {
    "return a Returns case class if one is found in mongo with the supplied regId" in new Setup {
      insert(vatSchemeWithReturns)

      val fetchedReturns: Option[Returns] = await(repository.fetchReturns(registrationId))

      fetchedReturns mustBe Some(returns)
    }

    "return None if no BankAccount is found in mongo for the supplied regId" in new Setup {
      count mustBe 0

      val fetchedReturns: Option[Returns] = await(repository.fetchReturns(registrationId))

      fetchedReturns mustBe None
    }
  }

  "updateReturns" should {

    val registrationId: String = "reg-12345"

    val otherRegId = "other-reg-12345"
    val otherUsersVatScheme = vatSchemeJson(otherRegId)

    val MONTHLY = "monthly"
    val JAN = "jan"

    val dateValue = LocalDate of(1990, 10, 10)
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

      fetchAll without _id mustBe Some(vatSchemeWithReturns)
    }

    "not update or insert new data into the registration doc if the supplied returns already exist on the doc" in new Setup {
      insert(vatSchemeWithReturns)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe Some(vatSchemeWithReturns)
    }

    "not update or insert returns if a registration doc doesn't already exist" in new Setup {
      count mustBe 0

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe None
    }

    "not update or insert returns if a registration doc associated with the given reg id doesn't already exist" in new Setup {
      insert(otherUsersVatScheme)

      fetchAll without _id mustBe Some(otherUsersVatScheme)

      await(repository.updateReturns(registrationId, returns))

      fetchAll without _id mustBe Some(otherUsersVatScheme)
    }
  }

  //TODO: look at removing the deprecated methods and tests
  //
  //  "Calling getThreshold" should {
  //    val threshold = Threshold(mandatoryRegistration = false,
  //      voluntaryReason = Some("a reason"),
  //      overThresholdDateThirtyDays = Some(LocalDate.of(2017, 12, 28)),
  //      pastOverThresholdDateThirtyDays = Some(LocalDate.of(2017, 6, 15)),
  //      overThresholdOccuredTwelveMonth = Some(LocalDate.of(2018, 1, 31)))
  //
  //    "return threshold data from an existing registration containing data" in new Setup {
  //      val result = for {
  //        _ <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
  //        res <- repository.getThreshold(vatScheme.id.value)
  //      } yield res
  //
  //      await(result) shouldBe Some(threshold)
  //    }
  //
  //    "return None from an existing registration containing no data" in new Setup {
  //      val result = for {
  //        _ <- repository.insert(vatScheme)
  //        res <- repository.getThreshold(vatScheme.id.value)
  //      } yield res
  //
  //      await(result) shouldBe None
  //    }
  //
  //    "throw a MissingRegDocument for a none existing registration" in new Setup {
  //      val result = for {
  //        _ <- repository.insert(vatScheme.copy(threshold = Some(threshold)))
  //        res <- repository.getThreshold("wrongRegId")
  //      } yield res
  //
  //      a[MissingRegDocument] shouldBe thrownBy(await(result))
  //    }
  //
  //    "return an exception if there is no mandatory registration in threshold block in repository" in new Setup {
  //      val regId = "reg-123"
  //      val json: JsObject = Json.parse(
  //        s"""
  //           |{
  //           |  "registrationId": "$regId",
  //           |  "status": "${VatRegStatus.draft}",
  //           |  "threshold": {
  //           |    "voluntaryReason": "test reason"
  //           |  }
  //           |}
  //         """.stripMargin).as[JsObject]
  //
  //      insert(json)
  //
  //      an[Exception] shouldBe thrownBy(await(repository.getThreshold(regId)))
  //    }
  //  }

  "calling getSicAndCompliance" should {
    val validSicAndCompliance = Some(SicAndCompliance(
      "this is my business description",
      Some(ComplianceLabour(1000, Some(true), Some(true))),
      SicCode("11111", "the flu", "sic details"),
      otherBusinessActivities = Nil
    ))
    "return a SicAndComplianceModel from existing data based on the reg Id" in new Setup {
      val result: Future[Option[SicAndCompliance]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateSicAndCompliance(vatScheme.id.value, validSicAndCompliance.get)
        _ = count mustBe 1
        res <- repository.getSicAndCompliance(vatScheme.id.value)
      } yield res

      await(result).get mustBe validSicAndCompliance.get
    }
    "read data to a SicAndCompliance model regardless of whether apivalidation is valid" in new Setup {
      val modelThatDoesNotConformToApiValidation: SicAndCompliance = SicAndCompliance(
        "foo",
        Some(ComplianceLabour(1, None, None)),
        SicCode("fooBARFIZZANDBANG1234", "bar", "wizz"),
        otherBusinessActivities = List(SicCode("11111 FOO BAR WIZZ AND BANG", "barFoo", "amended other foo")))
      val result: Future[Option[SicAndCompliance]] = for {
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = Some(modelThatDoesNotConformToApiValidation)))
        _ = count mustBe 1
        res <- repository.getSicAndCompliance(vatScheme.id.value)
      } yield res
      await(result).get mustBe modelThatDoesNotConformToApiValidation
    }
    "return None from an existing registration that exists but SicAndCompliance does not exist" in new Setup {
      val result: Future[Option[SicAndCompliance]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.getSicAndCompliance(vatScheme.id.value)
      } yield res
      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[SicAndCompliance]] = repository.getSicAndCompliance("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
    "return an exception if the json is incorrect in the repository (an element is missing)" in new Setup {
      val json: JsValue = Json.toJson(
        Json.obj("registrationId" -> regId.value,
          "status" -> VatRegStatus.draft,
          "sicAndCompliance" -> Json.toJson(validSicAndCompliance).as[JsObject].-("businessDescription")))
      insert(json.as[JsObject])
      an[Exception] mustBe thrownBy(await(repository.getSicAndCompliance(vatScheme.id.value)))
    }
  }
  "calling updateSicAndCompliance" should {
    val validSicAndCompliance: Option[SicAndCompliance] = Some(SicAndCompliance(
      "this is my business description",
      Some(ComplianceLabour(1000, Some(true), Some(true))),
      SicCode("12345", "the flu", "sic details"),
      otherBusinessActivities = List(SicCode("99999", "fooBar", "other foo"))
    ))
    "return an amended SicAndCompliance Model when an entry already exists in the repo for 1 field" in new Setup {
      val amendedModel: Option[SicAndCompliance] = validSicAndCompliance.map(a => a.copy(businessDescription = "fooBarWizz"))
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
        _ = count mustBe 1
        res <- repository.updateSicAndCompliance(vatScheme.id.value, amendedModel.get)
      } yield res

      await(result) mustBe amendedModel.get
    }
    "return an amended Option SicAndCompliance Model when an entry already exists and all fields have changed in the model" in new Setup {
      val amendedModel: SicAndCompliance = SicAndCompliance(
        "foo",
        Some(ComplianceLabour(1, None, None)),
        SicCode("foo", "bar", "wizz"),
        otherBusinessActivities = List(SicCode("11111", "barFoo", "amended other foo")))
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme.copy(sicAndCompliance = validSicAndCompliance))
        res <- repository.updateSicAndCompliance(vatScheme.id.value, amendedModel)
      } yield res
      await(result) mustBe amendedModel
    }
    "return an SicAndComplance Model when the block did not exist in the existing reg doc" in new Setup {
      val result: Future[SicAndCompliance] = for {
        _ <- repository.insert(vatScheme)
        _ = count mustBe 1
        res <- repository.updateSicAndCompliance(vatScheme.id.value, validSicAndCompliance.get)
      } yield res
      await(result) mustBe validSicAndCompliance.get
    }

    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[SicAndCompliance] = repository.updateSicAndCompliance("madeUpRegId", validSicAndCompliance.get)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }
  "calling getBusinessContact" should {

    "return a BusinessContact Model from existing data based on the reg Id" in new Setup {
      val result: Future[Option[BusinessContact]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateBusinessContact(vatScheme.id.value, businessContact)
        _ = count mustBe 1
        res <- repository.getBusinessContact(vatScheme.id.value)
      } yield res

      await(result).get mustBe businessContact
    }
    "return None from an existing registration that exists but BusinessContact does not exist" in new Setup {
      val result: Future[Option[BusinessContact]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.getBusinessContact(vatScheme.id.value)
      } yield res
      await(result) mustBe None
    }
    "return a MissingRegDocument when nothing is returned from mongo for the reg id" in new Setup {
      val result: Future[Option[BusinessContact]] = repository.getBusinessContact("madeUpRegId")

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
    "return an exception if the json is incorrect in the repository (an element is missing)" in new Setup {
      val json: JsValue = Json.toJson(
        Json.obj("registrationId" -> regId.value,
          "status" -> VatRegStatus.draft,
          "businessContact" -> Json.toJson(businessContact).as[JsObject].-("digitalContact")))
      insert(json.as[JsObject])
      an[Exception] mustBe thrownBy(await(repository.getBusinessContact(vatScheme.id.value)))
    }
  }
  "calling updateBusinessContact" should {
    "return an amended Business Contact Model when an entry already exists in the repo for 1 field" in new Setup {
      val amendedModel: BusinessContact = businessContact.copy(website = Some("fooBARUpdated"))

      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme.copy(businessContact = Some(businessContact)))
        _ = count mustBe 1
        res <- repository.updateBusinessContact(vatScheme.id.value, amendedModel)
      } yield res

      await(result) mustBe amendedModel
    }
    "return an amended Option BusinessContact Model when an entry already exists and all fields have changed in the model" in new Setup {
      val amendedModel: BusinessContact = businessContact.copy(
        digitalContact = DigitalContact("foozle", Some("2434738"), Some("37483784")),
        website = Some("myLittleWebsite"),
        ppob = Address("lino1", "lino2", None, None, None, Some("Funsville"))
      )
      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme.copy(businessContact = Some(businessContact)))
        res <- repository.updateBusinessContact(vatScheme.id.value, amendedModel)
      } yield res
      await(result) mustBe amendedModel
    }
    "return an BusinessContact Model when the block did not exist in the existing reg doc" in new Setup {
      val result: Future[BusinessContact] = for {
        _ <- repository.insert(vatScheme)
        _ = count mustBe 1
        res <- repository.updateBusinessContact(vatScheme.id.value, businessContact)
      } yield res
      await(result) mustBe businessContact
    }

    "return an MissingRegDocument if registration document does not exist for the registration id" in new Setup {
      val result: Future[BusinessContact] = repository.updateBusinessContact("madeUpRegId", businessContact)
      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }

  "fetchFlatRateScheme" should {
    "return flat rate scheme data from an existing registration containing data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(flatRateScheme)))
        res <- repository.fetchFlatRateScheme(vatScheme.id.value)
      } yield res

      await(result) mustBe Some(flatRateScheme)
    }

    "return None from an existing registration containing no data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme)
        res <- repository.fetchFlatRateScheme(vatScheme.id.value)
      } yield res

      await(result) mustBe None
    }

    "throw a MissingRegDocument for a none existing registration" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(flatRateScheme)))
        res <- repository.fetchFlatRateScheme("wrongRegId")
      } yield res

      a[MissingRegDocument] mustBe thrownBy(await(result))
    }
  }

  "updateFlatRateScheme" should {

    "update flat rate scheme block in registration when there is no flat rate scheme data" in new Setup {
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme)
        _ <- repository.updateFlatRateScheme(vatScheme.id.value, flatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(flatRateScheme)
    }

    "update flat rate scheme block in registration when there is already flat rate scheme data" in new Setup {
      val otherFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = false, None)
      val result: Future[Option[FlatRateScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(flatRateScheme)))
        _ <- repository.updateFlatRateScheme(vatScheme.id.value, flatRateScheme)
        Some(updatedScheme) <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.flatRateScheme

      await(result) mustBe Some(flatRateScheme)
    }

    "not update or insert flat rate scheme if registration does not exist" in new Setup {
      await(repository.insert(vatScheme))

      count mustBe 1
      await(repository.findAll()).head mustBe vatScheme

      a[MissingRegDocument] mustBe thrownBy(await(repository.updateFlatRateScheme("wrongRegId", flatRateScheme)))
    }
  }

  "removeFlatRateScheme" should {
    "remove a flatRateScheme block in the registration document if it exists in the registration doc" in new Setup {
      val result: Future[Option[VatScheme]] = for {
        _ <- repository.insert(vatScheme.copy(flatRateScheme = Some(flatRateScheme)))
        _ <- repository.removeFlatRateScheme(vatScheme.id.value)
        updatedScheme <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme

      await(result) mustBe Some(vatScheme)
    }

    "throw a MissingRegDocument if the vat scheme does not exist for the regId" in new Setup {
      a[MissingRegDocument] mustBe thrownBy(await(repository.removeFlatRateScheme(vatScheme.id.value)))
    }
  }

  "getInternalId" should {
    "return a Future[Option[String]] containing Some(InternalId)" in new Setup {
      val result: Future[Option[String]] = for {
        _ <- repository.insert(vatScheme)
        result <- repository.getInternalId(vatScheme.id.value)

      } yield result
      await(result) mustBe Some(internalid)
    }

    "return a None when no regId document is found" in new Setup {
      await(repository.getInternalId(vatScheme.id.value)) mustBe None
    }
  }
  "getCombinedLodgingOfficer" should {
    val completionCapacity: JsObject = Json.obj("role" -> "director", "name" -> Json.obj(
      "forename" -> "First Name Test",
      "other_forenames" -> "Middle Name Test",
      "surname" -> "Last Name Test"
    ))
    val questions1: Seq[JsObject] = Seq(
      Json.obj("questionId" -> "completionCapacity", "question" -> "Some Question 11", "answer" -> "Some Answer 11", "answerValue" -> completionCapacity),
      Json.obj("questionId" -> "testQId12", "question" -> "Some Question 12", "answer" -> "Some Answer 12", "answerValue" -> "val12")
    )
    val questions2: Seq[JsObject] = Seq(
      Json.obj("questionId" -> "applicantUKNino-optionalData", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "JW778877A"),
      Json.obj("questionId" -> "testQId21", "question" -> "Some Question 21", "answer" -> "Some Answer 21", "answerValue" -> "val21"),
      Json.obj("questionId" -> "testQId22", "question" -> "Some Question 22", "answer" -> "Some Answer 22", "answerValue" -> "val22")
    )
    val section1: JsObject = Json.obj("title" -> "test TITLE 1", "data" -> JsArray(questions1))
    val section2: JsObject = Json.obj("title" -> "test TITLE 2", "data" -> JsArray(questions2))
    val sections: JsArray = JsArray(Seq(section1, section2))
    val eligibilityData = Json.obj("sections" -> sections)


    "return None if both eligibilityData block is missing" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      await(repository.getCombinedLodgingOfficer(vatScheme.id.value)) mustBe None
    }

    "return an exception if no vatscheme doc exists" in new Setup {
      intercept[MissingRegDocument](await(repository.getCombinedLodgingOfficer("1")))
    }
  }

  "patchLodgingOfficer" should {
    val lodgingOfficerDetails: JsValue = Json.toJson(LodgingOfficerDetails(
      currentAddress = scrsAddress,
      changeOfName = None,
      previousAddress = None,
      contact = DigitalContactOptional(
        email = Some("test@t.com"),
        tel = None,
        mobile = None
      )
    ))
    "patch with details only" in new Setup {
      val lodgeOfficerJson: JsObject = Json.parse(
        s"""{
           | "details": $lodgingOfficerDetails
           |}
        """.stripMargin).as[JsObject]


      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1
      val res: JsObject = await(repository.patchLodgingOfficer(vatScheme.id.value, lodgeOfficerJson))
      await(repository.count) mustBe 1
      (fetchAll.get \ "lodgingOfficer").as[JsObject] mustBe lodgeOfficerJson
    }
  }

  val jsonEligiblityData: JsObject = Json.parse(
    """
      | {
      |    "foo" : "bar"
      | }
    """.stripMargin).as[JsObject]

  "getEligibilityData" should {
    "return some of eligibilityData" in new Setup {
      await(repository.insert(vatScheme.copy(eligibilityData = Some(jsonEligiblityData))))
      await(repository.count) mustBe 1

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe Some(jsonEligiblityData)
    }
    "return None of eligibilityData" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe None
    }
  }
  "updateEligibilityData" should {
    "update eligibilityData successfully when no eligibilityData block exists" in new Setup {
      await(repository.insert(vatScheme))
      await(repository.count) mustBe 1

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe None

      val res: JsObject = await(repository.updateEligibilityData(vatScheme.id.value, jsonEligiblityData))
      res mustBe jsonEligiblityData

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe Some(jsonEligiblityData)
    }
    "update eligibilityData successfully when eligibilityData block already exists" in new Setup {
      val newJsonEligiblityData: JsObject = Json.parse(
        """
          | {
          |    "wizz" : "new bar"
          | }
        """.stripMargin).as[JsObject]

      await(repository.insert(vatScheme.copy(eligibilityData = Some(jsonEligiblityData))))
      await(repository.count) mustBe 1

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe Some(jsonEligiblityData)

      val res: JsObject = await(repository.updateEligibilityData(vatScheme.id.value, newJsonEligiblityData))
      res mustBe newJsonEligiblityData

      await(repository.getEligibilityData(vatScheme.id.value)) mustBe Some(newJsonEligiblityData)
    }
  }
}

