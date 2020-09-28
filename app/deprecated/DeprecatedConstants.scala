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

package deprecated

import java.time.LocalDate

import models.api.{Address, ApplicantDetails, BankAccountDetails, BusinessContact, DigitalContact, DigitalContactOptional, FRSDetails, Name, SicAndCompliance, SicCode, TradingDetails, VatSubmission}
import models.submission.DateOfBirth

object DeprecatedConstants {
  @Deprecated
  val fakeNino = "AA123456A"

  @Deprecated
  val fakeApplicantName: Name = Name(first = Some("FAKE_FIRST_NAME"), middle = Some("FAKE_MIDDLE_NAME"), last = "[OFFICER_NAME_REMOVED]")

  private val fakeName = Name(Some("fakeFirstName"), None, "fakeLastName")
  private val fakeDob = DateOfBirth(LocalDate.parse("2020-01-01"))
  private val fakeAddress = Address("fakeLine1", "fakeLine2")
  private val fakeContact = DigitalContactOptional(Some("fakeEmail@fake.com"))
  private val fakeApplicantDetails = ApplicantDetails(
    nino = "fakeNino",
    role = "fakeRole",
    name = fakeName,
    dateOfBirth = fakeDob,
    currentAddress = fakeAddress,
    contact = fakeContact
  )
  private val fakeBankDetails = BankAccountDetails("fakeBankName", "fakeSortCode", "fakeAccountNumber")
  private val fakeSicDetails = SicAndCompliance("fakeBusinessDescription", None, SicCode("fakeSic", "fakeSicDesc", "fakeSicDisplayName"), List())
  private val fakeBusinessContact = BusinessContact(DigitalContact("fakeEmail@fake.com", Some("01234567890"), Some("07234567890")), Some("fakeWebsite"), fakeAddress)
  private val fakeTradingDetails = TradingDetails(Some("fakeTradingName"), true)
  private val fakeFrsDetails = FRSDetails(None, Some(LocalDate.parse("2020-01-01")), "fakeCategory", BigDecimal(0.1))

  @Deprecated
  val fakeSubmission = VatSubmission(
    messageType = "SubmissionCreate",
    customerStatus = Some("3"),
    tradersPartyType = Some("50"),
    primeBPSafeId = Some("12345678901234567890"),
    confirmInformationDeclaration = Some(true),
    companyRegistrationNumber = Some("fakeCrn"),
    applicantDetails = fakeApplicantDetails,
    bankDetails = Some(fakeBankDetails),
    sicAndCompliance = fakeSicDetails,
    businessContact = fakeBusinessContact,
    tradingDetails = fakeTradingDetails,
    flatRateScheme = Some(fakeFrsDetails)
  )
}
