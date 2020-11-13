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

import java.time.{LocalDate, LocalDateTime, LocalTime}

import common.TransactionId
import enums.VatRegStatus
import models.api._
import models.submission.{DateOfBirth, OwnerProprietor}
import uk.gov.hmrc.auth.core.AffinityGroup
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val testDate: LocalDate = LocalDate.of(2017, 1, 1)
  val testDateTime: LocalDateTime = LocalDateTime.of(testDate, LocalTime.of(0, 0))
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
    start = startDate,
    zeroRatedSupplies = Some(12.99)
  )

  val frsDetails = FRSDetails(
    businessGoods = Some(BusinessGoods(12345678L, true)),
    startDate = Some(testDate),
    categoryOfBusiness = "testCategory",
    percent = 15,
    limitedCostTrader = Some(false)
  )

  val testFlatRateScheme = FlatRateScheme(joinFrs = true, Some(frsDetails))
  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L
  val testCountry = Country(Some("UK"), None)
  val testAddress = Address("line1", "line2", None, None, Some("XX XX"), Some(testCountry))
  val testContactDetails = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val testDigitalContactOptional = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  val testNino = "NB686868C"
  val testRole = Some("secretary")
  val testName = Name(first = Some("Forename"), middle = None, last = "Surname")
  val testFormerName = FormerName(name = Some(oldName), change = Some(testDate))
  val testCompanyName = "testCompanyName"
  val testCrn = "testCrn"
  val testCtUtr = Some("testCtUtr")
  val testDateOFIncorp = LocalDate.of(2020, 1, 2)
  val testBpSafeId = "testBpSafeId"

  val testApplicantDetails = ApplicantDetails(
    nino = testNino,
    role = testRole,
    name = testName,
    dateOfBirth = DateOfBirth(testDate),
    companyName = testCompanyName,
    companyNumber = Some(testCrn),
    dateOfIncorporation = testDateOFIncorp,
    ctutr = testCtUtr,
    currentAddress = testAddress,
    contact = testDigitalContactOptional,
    changeOfName = None,
    previousAddress = None,
    businessVerification = Some(BvPass),
    bpSafeId = Some(testBpSafeId)
  )

  val testBusinessContactDetails = BusinessContact(digitalContact = testContactDetails, website = None, ppob = testAddress, commsPreference = Email)
  val testSicAndCompliance = SicAndCompliance("businessDesc", Some(ComplianceLabour(1, Some(true), Some(true))), SicCode("12345", "sicDesc", "sicDetail"), List(SicCode("12345", "sicDesc", "sicDetail")))
  val testTurnoverEstimates = TurnoverEstimates(12345678L)
  val testBankDetails = BankAccount(false, None)
  val testThreshold = Threshold(mandatoryRegistration = true, Some(LocalDate.now()), Some(LocalDate.now()), Some(LocalDate.now()))

  val testEligibilitySubmissionData: EligibilitySubmissionData = EligibilitySubmissionData(
    threshold = testThreshold,
    exceptionOrExemption = "0",
    estimates = TurnoverEstimates(123456),
    customerStatus = MTDfB,
    completionCapacity = OwnerProprietor
  )

  val testFullVatScheme: VatScheme =
    VatScheme(
      id = testRegId,
      internalId = testInternalid,
      transactionId = Some(TransactionId(testTransactionId)),
      tradingDetails = Some(testTradingDetails),
      returns = Some(testReturns),
      sicAndCompliance = Some(testSicAndCompliance),
      businessContact = Some(testBusinessContactDetails),
      bankAccount = Some(testBankDetails),
      acknowledgementReference = Some("ackRef"),
      flatRateScheme = Some(testFlatRateScheme),
      status = VatRegStatus.draft,
      applicantDetails = Some(testApplicantDetails),
      eligibilitySubmissionData = Some(testEligibilitySubmissionData)
    )

  def testEmptyVatScheme(regId: String): VatScheme = VatScheme(
    id = regId,
    internalId = testInternalid,
    status = VatRegStatus.draft
  )

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
    lazy val testAffinityGroup: AffinityGroup = AffinityGroup.Organisation
    lazy val testProviderId: String = "testProviderID"
    lazy val testProviderType: String = "GovernmentGateway"
    lazy val testCredentials: Credentials = Credentials(testProviderId, testProviderType)

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
