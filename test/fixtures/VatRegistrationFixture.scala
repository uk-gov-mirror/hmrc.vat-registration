/*
 * Copyright 2021 HM Revenue & Customs
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

import enums.VatRegStatus
import models.api._
import models.submission.{DateOfBirth, Director, RoleInBusiness, UkCompany, VatSubmission}
import play.api.libs.json.{JsObject, Json}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.auth.core.retrieve.Credentials

import java.nio.charset.StandardCharsets
import java.time.{LocalDate, LocalDateTime, LocalTime}
import java.util.Base64

trait VatRegistrationFixture {
  lazy val testNino = "AB123456A"
  lazy val testRole: RoleInBusiness = Director
  lazy val testRegId = "testRegId"
  lazy val testInternalid = "INT-123-456-789"
  lazy val testAckReference = "BRPY000000000001"
  lazy val testDate: LocalDate = LocalDate.of(2018, 1, 1)
  lazy val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.MIDNIGHT)
  lazy val testDateOfBirth = DateOfBirth(testDate)
  lazy val testCompanyName = "testCompanyName"
  lazy val testCrn = "testCrn"
  lazy val testCtUtr = "testCtUtr"
  lazy val testDateOFIncorp: LocalDate = LocalDate.of(2020, 1, 2)
  lazy val testAddress = Address("line1", "line2", None, None, Some("XX XX"), Some(Country(Some("GB"), None)), addressValidated = Some(true))
  lazy val testPostcode = "ZZ1 1ZZ"
  lazy val testSicCode = SicCode("88888", "description", "displayDetails")
  lazy val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  lazy val testOldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  lazy val testPreviousName = FormerName(name = Some(testOldName), change = Some(testDate))
  lazy val testVatScheme: VatScheme = VatScheme(testRegId, internalId = testInternalid, status = VatRegStatus.draft)
  lazy val exception = new Exception("Exception")
  lazy val testVoluntaryThreshold = Threshold(mandatoryRegistration = false, None, None, None)
  lazy val testMandatoryThreshold = Threshold(
    mandatoryRegistration = true,
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7)),
    Some(LocalDate.of(2020, 10, 7))
  )
  lazy val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  lazy val testBankDetails = BankAccountDetails("Test Bank Account", "010203", "01023456")
  lazy val testFormerName = FormerName(Some(testName), Some(testDate))
  lazy val testReturns = Returns(reclaimVatOnMostReturns = false, "quarterly", Some("jan"), StartDate(Some(testDate)), Some(12.99))
  lazy val zeroRatedSupplies: BigDecimal = 12.99
  lazy val testBpSafeId = "testBpSafeId"

  lazy val testProviderId: String = "testProviderID"
  lazy val testProviderType: String = "GovernmentGateway"
  lazy val testCredentials: Credentials = Credentials(testProviderId, testProviderType)
  lazy val testAffinityGroup: AffinityGroup = AffinityGroup.Organisation
  lazy val testAuthToken = "testAuthToken"

  val testTradingName = "trading-name"
  val testUserHeaders = Map("testKey" -> "testValue")

  lazy val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testMandatoryThreshold,
    exceptionOrExemption = "0",
    estimates = TurnoverEstimates(123456),
    customerStatus = MTDfB
  )

  lazy val validApplicantDetails: ApplicantDetails = ApplicantDetails(
    nino = testNino,
    roleInBusiness = testRole,
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
    businessVerification = BvFail,
    registration = NotCalledStatus,
    identifiersMatch = true,
    bpSafeId = None
  )

  lazy val otherBusinessActivitiesSicAndCompiliance: List[SicCode] =
    SicCode("00998", "otherBusiness desc 1", "fooBar 1") :: SicCode("00889", "otherBusiness desc 2", "fooBar 2") :: Nil

  lazy val testSicAndCompliance = Some(SicAndCompliance(
    "this is my business description",
    Some(ComplianceLabour(numOfWorkersSupplied = Some(1000), intermediaryArrangement = Some(true), supplyWorkers = true)),
    SicCode("12345", "the flu", "sic details"),
    otherBusinessActivitiesSicAndCompiliance
  ))

  lazy val testBusinessContact = Some(BusinessContact(
    digitalContact = DigitalContact("email@email.com", Some("12345"), Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address("line1", "line2", None, None, Some(testPostcode), Some(Country(Some("GB"), None))),
    commsPreference = Email
  ))

  lazy val testBankAccount = BankAccount(isProvided = true, details = Some(testBankDetails), None)
  lazy val testBankAccountNotProvided = BankAccount(isProvided = false, details = None, reason = Some(BeingSetup))

  lazy val validFullFRSDetails: FRSDetails =
    FRSDetails(
      businessGoods = Some(BusinessGoods(1234567891011L, overTurnover = true)),
      startDate = Some(testDate),
      categoryOfBusiness = Some("testCategory"),
      percent = 15,
      limitedCostTrader = Some(false)
    )

  lazy val validFullFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, Some(validFullFRSDetails))
  lazy val validEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = false, None)
  lazy val invalidEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, None)

  lazy val validFullBusinessContact: BusinessContact = BusinessContact(
    digitalContact = DigitalContact(email = "email@email.com", tel = Some("12345"), mobile = Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address(
      line1 = "line1",
      line2 = "line2",
      postcode = Some(testPostcode),
      country = Some(Country(code = Some("GB"), name = Some("UK")))),
    commsPreference = Email)

  val testSubmissionPayload = "testSubmissionPayload"
  val testEncodedPayload: String = Base64.getEncoder.encodeToString(testSubmissionPayload.getBytes(StandardCharsets.UTF_8))

  lazy val testFullVatScheme: VatScheme = testVatScheme.copy(
    tradingDetails = Some(validFullTradingDetails),
    sicAndCompliance = testSicAndCompliance,
    businessContact = testBusinessContact,
    bankAccount = Some(testBankAccount),
    flatRateScheme = Some(validFullFlatRateScheme),
    applicantDetails = Some(validApplicantDetails),
    eligibilitySubmissionData = Some(testEligibilitySubmissionData),
    confirmInformationDeclaration = Some(true),
    returns = Some(testReturns.copy(zeroRatedSupplies = Some(zeroRatedSupplies))),
    nrsSubmissionPayload = Some(testEncodedPayload)
  )

  lazy val testFullSubmission: VatSubmission = VatSubmission(
    tradersPartyType = Some(UkCompany),
    confirmInformationDeclaration = Some(true),
    companyRegistrationNumber = Some("CRN"),
    applicantDetails = validApplicantDetails,
    bankDetails = Some(testBankAccount),
    sicAndCompliance = testSicAndCompliance.get,
    businessContact = testBusinessContact.get,
    tradingDetails = validFullTradingDetails,
    flatRateScheme = Some(validFullFRSDetails),
    eligibilitySubmissionData = testEligibilitySubmissionData,
    returns = testReturns
  )

  lazy val validBusinessContactJson: JsObject = Json.parse(
    s"""{
       |"digitalContact":{
       |"email": "email@email.com",
       |"tel": "12345",
       |"mobile": "54321"
       |},
       |"website": "www.foo.com",
       |"ppob": {
       |  "line1": "line1",
       |  "line2": "line2",
       |  "postcode": "ZZ1 1ZZ",
       |  "country": {
       |    "code": "GB"
       |  }
       | },
       | "contactPreference": "Email"
       |}
       |
     """.stripMargin
  ).as[JsObject]

  lazy val validSicAndComplianceJson: JsObject = Json.parse(
    s"""
       |{
       | "businessDescription": "this is my business description",
       | "labourCompliance" : {
       | "numOfWorkersSupplied": 1000,
       | "intermediaryArrangement":true,
       | "supplyWorkers":true
       |     },
       | "mainBusinessActivity": {
       | "code": "12345",
       | "desc": "the flu",
       | "indexes": "sic details"
       |     },
       | "businessActivities": [
       |    {  "code": "00998",
       |       "desc": "otherBusiness desc 1",
       |       "indexes": "fooBar 1" },
       |     {  "code": "00889",
       |       "desc": "otherBusiness desc 2",
       |       "indexes": "fooBar 2" }
       ]
       |}
    """.stripMargin).as[JsObject]

  lazy val validFullTradingDetails: TradingDetails = TradingDetails(tradingName = Some(testTradingName), eoriRequested = true)
  lazy val validFullTradingDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "tradingName":"trading-name",
       | "eoriRequested":true
       |}
     """.stripMargin).as[JsObject]

  lazy val invalidTradingDetailsJson: JsObject = Json.parse(
    s"""
       |{
       | "tradingName":"trading-name",
       | "eriroREf":true
       |}
     """.stripMargin).as[JsObject]

  lazy val validFullFRSDetailsJsonWithBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "businessGoods" : {
       |    "overTurnover": true,
       |    "estimatedTotalSales": 1234567891011
       |  },
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00,
       |  "limitedCostTrader":false
       |}
     """.stripMargin).as[JsObject]

  lazy val validFRSDetailsJsonWithoutBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00,
       |  "limitedCostTrader":false
       |}
     """.stripMargin).as[JsObject]

  lazy val validFullFRSDetailsJsonWithOptionals: JsObject = Json.parse(
    s"""
       |{
       |  "businessGoods" : {
       |    "overTurnover": true,
       |    "estimatedTotalSales": 1234567891011
       |  },
       |  "startDate": "$testDate",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00,
       |  "limitedCostTrader":false
       |}
     """.stripMargin).as[JsObject]

  lazy val validFRSDetailsJsonWithoutOptionals: JsObject = Json.parse(
    s"""
       |{
       |  "percent":15.00,
       |  "limitedCostTrader":false
       |}
     """.stripMargin).as[JsObject]


  lazy val validFullFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs": true,
       |  "frsDetails":$validFullFRSDetailsJsonWithBusinessGoods
       |}
     """.stripMargin).as[JsObject]

  lazy val detailsPresentJoinFrsFalse: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs":false,
       |  "frsDetails":$validFullFRSDetailsJsonWithBusinessGoods
       |}
     """.stripMargin).as[JsObject]

  lazy val validEmptyFlatRateSchemeJson: JsObject = Json.parse(
    s"""
       |{
       |  "joinFrs": false
       |}
     """.stripMargin).as[JsObject]

  object AuthTestData {

    import models.nonrepudiation.IdentityData
    import services.NonRepudiationService.NonRepudiationIdentityRetrievals
    import uk.gov.hmrc.auth.core.retrieve._
    import uk.gov.hmrc.auth.core.{ConfidenceLevel, CredentialStrength, User}


    val testInternalId = "testInternalId"
    val testExternalId = "testExternalId"
    val testAgentCode = "testAgentCode"
    val testConfidenceLevel = ConfidenceLevel.L200
    val testSautr = "testSautr"
    val testAuthName = uk.gov.hmrc.auth.core.retrieve.Name(Some("testFirstName"), Some("testLastName"))
    val testAuthDateOfBirth = org.joda.time.LocalDate.now()
    val testEmail = "testEmail"
    val testAgentInformation = AgentInformation(Some("testAgentId"), Some("testAgentCode"), Some("testAgentFriendlyName"))
    val testGroupIdentifier = "testGroupIdentifier"
    val testCredentialRole = User
    val testMdtpInformation = MdtpInformation("testDeviceId", "testSessionId")
    val testItmpName = ItmpName(Some("testGivenName"), Some("testMiddleName"), Some("testFamilyName"))
    val testItmpDateOfBirth = org.joda.time.LocalDate.now()
    val testItmpAddress = ItmpAddress(
      Some("testLine1"),
      None,
      None,
      None,
      None,
      Some("testPostcode"),
      None,
      None
    )
    val testCredentialStrength = CredentialStrength.strong
    val testLoginTimes = LoginTimes(org.joda.time.DateTime.now(), Some(org.joda.time.DateTime.now()))

    val testNonRepudiationIdentityData: IdentityData = IdentityData(
      Some(testInternalId),
      Some(testExternalId),
      Some(testAgentCode),
      Some(testCredentials),
      testConfidenceLevel,
      Some(testNino),
      Some(testSautr),
      Some(testAuthName),
      Some(testAuthDateOfBirth),
      Some(testEmail),
      testAgentInformation,
      Some(testGroupIdentifier),
      Some(testCredentialRole),
      Some(testMdtpInformation),
      Some(testItmpName),
      Some(testItmpDateOfBirth),
      Some(testItmpAddress),
      Some(testAffinityGroup),
      Some(testCredentialStrength),
      testLoginTimes
    )

    implicit class RetrievalCombiner[A](a: A) {
      def ~[B](b: B): A ~ B = new ~(a, b)
    }

    val testAuthRetrievals: NonRepudiationIdentityRetrievals =
      Some(testAffinityGroup) ~
        Some(testInternalId) ~
        Some(testExternalId) ~
        Some(testAgentCode) ~
        Some(testCredentials) ~
        testConfidenceLevel ~
        Some(testNino) ~
        Some(testSautr) ~
        Some(testAuthName) ~
        Some(testAuthDateOfBirth) ~
        Some(testEmail) ~
        testAgentInformation ~
        Some(testGroupIdentifier) ~
        Some(testCredentialRole) ~
        Some(testMdtpInformation) ~
        Some(testItmpName) ~
        Some(testItmpDateOfBirth) ~
        Some(testItmpAddress) ~
        Some(testCredentialStrength) ~
        testLoginTimes

  }

}
