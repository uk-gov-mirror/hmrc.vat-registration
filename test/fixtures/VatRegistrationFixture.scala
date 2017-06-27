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

package fixtures

import java.time.LocalDate

import common.RegistrationId
import models.api._

trait VatRegistrationFixture {
  val regId = RegistrationId("testId")
  val userId = "userId"
  val ackRefNumber = "BRPY000000000001"
  val date = LocalDate.of(2017, 1, 1)
  val vatChoice: VatChoice = VatChoice(
    necessity = "obligatory",
    vatStartDate = VatStartDate(
      selection = "SPECIFIC_DATE",
      startDate = Some(date)))
  val tradingDetails: VatTradingDetails = VatTradingDetails(
    vatChoice = vatChoice,
    tradingName = TradingName(
      selection = true,
      tradingName = Some("some-trader-name")),
    euTrading = VatEuTrading(selection = true, eoriApplication = Some(true))
  )
  val sicAndCompliance: VatSicAndCompliance = VatSicAndCompliance("some-business-description", None, None)
  val vatDigitalContact = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact = VatContact(vatDigitalContact)
  val vatEligibility = VatServiceEligibility(
    haveNino = Some(true),
    doingBusinessAbroad = Some(true),
    doAnyApplyToYou = Some(true),
    applyingForAnyOf = Some(true),
    companyWillDoAnyOf = Some(true)
  )
  val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val name = Name(forename = Some("Forename"), surname = Some("Surname"), title = Some("Title"))
  val formerName = FormerName(true, Some("Bob Smith"))
  val contact = OfficerContactDetails(Some("test@test.com"), None, None)
  val vatScheme: VatScheme = VatScheme(regId)
  val exception = new Exception("Exception")
  val currentOrPreviousAddress = CurrentOrPreviousAddress(false, Some(scrsAddress))
  val vatFlatRateScheme = VatFlatRateScheme(
    joinFrs = Some(true),
    annualCostsInclusive = Some("yesWithin12months"),
    annualCostsLimited = Some(AnnualCostsLimited(Some(1000), Some("yesWithin12months"))),
    doYouWantToUseThisRate = Some(false),
    whenDoYouWantToJoinFrs=  Some("registrationDate"))

}
