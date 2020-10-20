/*
 * Copyright 2020 HM Revenue & Customs
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

package fixtures

import java.time.LocalDate

import common.TransactionId
import enums.VatRegStatus
import models.api._
import models.submission.DateOfBirth
import play.api.libs.json.{JsObject, Json}

trait VatRegistrationFixture {
  val testNino = "AB123456A"
  val testRole = Some("secretary")
  val testRegId = "testRegId"
  val testInternalid = "INT-123-456-789"
  val testTxId: TransactionId = TransactionId("1")
  val testAckReference = "BRPY000000000001"
  val testDate: LocalDate = LocalDate.of(2018, 1, 1)
  val testDateOfBirth = DateOfBirth(testDate)
  val testCompanyName = "testCompanyName"
  val testCrn = "testCrn"
  val testCtUtr = Some("testCtUtr")
  val testDateOFIncorp = LocalDate.of(2020, 1, 2)
  val testAddress = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val testSicCode = SicCode("88888", "description", "displayDetails")
  val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testOldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val testPreviousName = FormerName(name = Some(testOldName), change = Some(testDate))
  val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)
  val exception = new Exception("Exception")
  val testVoluntaryThreshold = Threshold(mandatoryRegistration = false, None, None, None)
  val testMandatoryThreshold = Threshold(mandatoryRegistration = true, Some(LocalDate.of(2020, 10, 7)), Some(LocalDate.of(2020, 10, 7)), Some(LocalDate.of(2020, 10, 7)))
  val currentAddress = Address("12 Lukewarm", "Oriental lane")
  val testDigitalContact = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  val testBankDetails = BankAccountDetails("Test Bank Account", "010203", "01023456")
  val testFormerName = FormerName(Some(testName), Some(testDate))
  val testReturns = Returns(false, "", None, StartDate(None), None)
  val zeroRatedSupplies: BigDecimal = 12.99
  val testBpSafeId = "testBpSafeId"

  val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testMandatoryThreshold,
    exceptionOrExemption = "0",
    estimates = TurnoverEstimates(123456),
    customerStatus = MTDfB
  )

  val validApplicantDetails: ApplicantDetails = ApplicantDetails(
    nino = testNino,
    role = testRole,
    name = testName,
    dateOfBirth = DateOfBirth(testDate),
    companyName = testCompanyName,
    companyNumber = testCrn,
    dateOfIncorporation = testDateOFIncorp,
    ctutr = testCtUtr,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = Some(testFormerName),
    previousAddress = None,
    businessVerification = Some(BvPass),
    bpSafeId = Some(testBpSafeId)
  )

  val otherBusinessActivitiesSicAndCompiliance =
    SicCode("00998", "otherBusiness desc 1", "fooBar 1") :: SicCode("00889", "otherBusiness desc 2", "fooBar 2") :: Nil

  val testSicAndCompliance = Some(SicAndCompliance(
    "this is my business description",
    Some(ComplianceLabour(1000, Some(true), Some(true))),
    SicCode("12345", "the flu", "sic details"),
    otherBusinessActivitiesSicAndCompiliance
  ))

  val testBusinessContact = Some(BusinessContact(
    digitalContact = DigitalContact("email@email.com", Some("12345"), Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address("line1", "line2", None, None, None, Some("foo")),
    commsPreference = Email
  ))

  val testBankAccount = BankAccount(true, Some(testBankDetails))
  val testBankAccountNotProvided = BankAccount(false, None)

  val validFullFRSDetails: FRSDetails =
    FRSDetails(
      businessGoods = Some(BusinessGoods(1234567891011L, true)),
      startDate = Some(testDate),
      categoryOfBusiness = "testCategory",
      percent = 15
    )

  val validFullFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, Some(validFullFRSDetails))
  val validEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = false, None)
  val invalidEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, None)

  val testFullVatScheme: VatScheme = testVatScheme.copy(
    tradingDetails = Some(validFullTradingDetails),
    sicAndCompliance = testSicAndCompliance,
    businessContact = testBusinessContact,
    bankAccount = Some(testBankAccount),
    flatRateScheme = Some(validFullFlatRateScheme),
    applicantDetails = Some(validApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    returns = Some(testReturns.copy(zeroRatedSupplies = Some(zeroRatedSupplies)))
  )

  val testFullSubmission: VatSubmission = VatSubmission(
    tradersPartyType = None,
    confirmInformationDeclaration = Some(true),
    companyRegistrationNumber = Some("CRN"),
    applicantDetails = validApplicantDetails,
    bankDetails = Some(testBankAccount),
    sicAndCompliance = testSicAndCompliance.get,
    businessContact = testBusinessContact.get,
    tradingDetails = validFullTradingDetails,
    flatRateScheme = Some(validFullFRSDetails),
    eligibilitySubmissionData = testEligibilitySubmissionData,
    zeroRatedSupplies = zeroRatedSupplies
  )

  val validBusinessContactJson = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email.com",
       |"tel": "12345",
       |"mobile": "54321"
       |},
       |"website": "www.foo.com",
       |"ppob": {
       |"line1": "line1",
       |"line2": "line2",
       |"country": "foo"
       | },
       | "contactPreference": "Email"
       |}
       |
     """.stripMargin
  ).as[JsObject]

  val validSicAndComplianceJson = Json.parse(
    s"""
       |{
       | "businessDescription": "this is my business description",
       | "labourCompliance" : {
       | "numberOfWorkers": 1000,
       | "temporaryContracts":true,
       | "skilledWorkers":true
           },
       "mainBusinessActivity": {
       "code": "12345",
       "desc": "the flu",
       "indexes": "sic details"
           },
       "otherBusinessActivities": [
       |    {  "code": "00998",
       |       "desc": "otherBusiness desc 1",
       |       "indexes": "fooBar 1" },
       |     {  "code": "00889",
       |       "desc": "otherBusiness desc 2",
       |       "indexes": "fooBar 2" }
       ]
       |}
    """.stripMargin).as[JsObject]

  val validFullTradingDetails: TradingDetails = TradingDetails(Some("trading-name"), true)
  val validFullTradingDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "tradingName":"trading-name",
       | "eoriRequested":true
       |}
     """.stripMargin).as[JsObject]

  val invalidTradingDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "tradingName":"trading-name",
       | "eriroREf":true
       |}
     """.stripMargin).as[JsObject]

  val validFullFRSDetailsJsonWithBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "businessGoods" : {
       |    "overTurnover": true,
       |    "estimatedTotalSales": 1234567891011
       |  },
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00
       |}
     """.stripMargin).as[JsObject]

  val validFRSDetailsJsonWithoutBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00
       |}
     """.stripMargin).as[JsObject]

  val validFullFRSDetailsJsonWithOptionals: JsObject = Json.parse(
    s"""
       |{
       |  "businessGoods" : {
       |    "overTurnover": true,
       |    "estimatedTotalSales": 1234567891011
       |  },
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00
       |}
     """.stripMargin).as[JsObject]

  val validFRSDetailsJsonWithoutOptionals: JsObject = Json.parse(
    s"""
       |{
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00
       |}
     """.stripMargin).as[JsObject]


  val validFullFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs": true,
       |  "frsDetails":$validFullFRSDetailsJsonWithBusinessGoods
       |}
     """.stripMargin).as[JsObject]

  val detailsPresentJoinFrsFalse: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs":false,
       |  "frsDetails":$validFullFRSDetailsJsonWithBusinessGoods
       |}
     """.stripMargin).as[JsObject]

  val validEmptyFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs": false
       |}
     """.stripMargin).as[JsObject]
}
