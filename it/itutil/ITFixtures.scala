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
package itutil

import java.time.LocalDate

import common.TransactionId
import enums.VatRegStatus
import models.api._
import models.submission.DateOfBirth
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testDate: LocalDate = LocalDate.of(2017, 1, 1)
  val startDate = StartDate(Some(testDate))
  val testRegId = "regId"
  val testInternalid = "INT-123-456-789"
  val testTransactionId = "transId"
  val vatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val testTradingDetails = TradingDetails(Some("test-name"), true)

  val testReturns = Returns(
    reclaimVatOnMostReturns = true,
    frequency = "quarterly",
    staggerStart = Some("jan"),
    start = startDate
  )

  val frsDetails = FRSDetails(
    businessGoods = Some(BusinessGoods(12345678L, true)),
    startDate = Some(testDate),
    categoryOfBusiness = "testCategory",
    percent = 15
  )

  val testFlatRateScheme = FlatRateScheme(joinFrs = true, Some(frsDetails))
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val testAddress = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val testContactDetails = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  val testNino = "NB686868C"
  val testRole = Some("secretary")
  val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testFormerName = FormerName(name = Some(oldName), change = Some(testDate))

  val testApplicantDetails = ApplicantDetails(
    nino = testNino,
    role = testRole,
    name = testName,
    dateOfBirth = DateOfBirth(testDate),
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = None,
    previousAddress = None
  )

  val testBusinessContactDetails = BusinessContact(digitalContact = testContactDetails, website = None, ppob = testAddress)
  val testSicAndCompliance = SicAndCompliance("businessDesc", Some(ComplianceLabour(1, Some(true), Some(true))), SicCode("12345", "sicDesc", "sicDetail"), List(SicCode("12345", "sicDesc", "sicDetail")))
  val testTurnoverEstimates = TurnoverEstimates(12345678L)
  val testBankDetails = BankAccount(true, None)
  val testThreshold = Threshold(true, None, None, None, None)

  val testFullVatScheme =
    VatScheme(
      id = testRegId,
      internalId = testInternalid,
      transactionId = Some(TransactionId(testTransactionId)),
      tradingDetails = Some(testTradingDetails),
      returns = Some(testReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      turnoverEstimates = Some(testTurnoverEstimates),
      bankAccount = Some(testBankDetails),
      threshold = Some(testThreshold),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testApplicantDetails)
    )

  def testEmptyVatScheme(regId: String): VatScheme = VatScheme(
    id = regId,
    internalId = testInternalid,
    status = VatRegStatus.draft
  )
}
