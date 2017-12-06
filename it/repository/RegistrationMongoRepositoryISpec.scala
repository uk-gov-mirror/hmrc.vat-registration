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

import common.exceptions._
import common.{LogicalGroup, RegistrationId, TransactionId}
import enums.VatRegStatus
import itutil.{FutureAssertions, ITFixtures}
import models.{AcknowledgementReferencePath, VatBankAccountPath}
import org.scalatest.BeforeAndAfterEach
import play.api.libs.json.Writes
import repositories.{MongoDBProvider, RegistrationMongoRepository}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.{UnitSpec, WithFakeApplication}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationMongoRepositoryISpec
  extends UnitSpec with MongoSpecSupport with FutureAssertions with BeforeAndAfterEach with WithFakeApplication with ITFixtures {

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

  "Calling updateIVStatus" should {
    "update the ivStatus in lodging officer" in new Setup {
      val result = for {
        insert                <- repository.insert(vatScheme)
        update                <- repository.updateIVStatus(vatScheme.id.value, true)
        Some(updatedScheme)   <- repository.retrieveVatScheme(vatScheme.id)
      } yield updatedScheme.lodgingOfficer.get.ivPassed

      await(result) shouldBe true
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
}
