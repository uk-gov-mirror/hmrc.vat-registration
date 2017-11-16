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

import common.RegistrationId
import enums.VatRegStatus
import models.api._
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val date = LocalDate.of(2017, 1, 1)
  val regId = RegistrationId("123")
  val vatScheme = VatScheme(regId, status = VatRegStatus.draft)
  val vatChoice = VatChoice(vatStartDate = VatStartDate(selection = "COMPANY_REGISTRATION_DATE", startDate = Some(date)))
  val tradingName = TradingName(selection = true, Some("some-trading-name"))
  val changeOfName = ChangeOfName(true, Some(FormerName("", LocalDate.now())))

  val vatTradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(
      selection = true,
      eoriApplication = Some(true)
    )
  )
  val tradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = tradingName,
    euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
  )
  val compliance =
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

  val scrsAddress               = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val vatDigitalContact         = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact                = VatContact(digitalContact = vatDigitalContact, website = None, ppob = scrsAddress)

  val name                      = Name(forename = Some("Forename"), surname = Some("Surname"), title = Some("Title"))
  val contact                   = OfficerContactDetails(Some("test@test.com"), None, None)
  val formerName                = FormerName("Bob Smith", date)
  val currentOrPreviousAddress  = CurrentOrPreviousAddress(false, Some(scrsAddress))
  val vatLodgingOfficer         = VatLodgingOfficer(
    currentAddress            = Some(scrsAddress),
    dob                       = Some(DateOfBirth(1, 1, 1980)),
    nino                      = Some("NB686868C"),
    role                      = Some("director"),
    name                      = Some(name),
    changeOfName              = Some(changeOfName),
    currentOrPreviousAddress  = Some(currentOrPreviousAddress),
    contact                   = Some(contact))
}
