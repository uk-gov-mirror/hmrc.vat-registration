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

import common.{RegistrationId, TransactionId}
import models.api._
import models.external.{IncorpStatusEvent, IncorpSubscription, IncorporationStatus}
import java.time.LocalDate

trait VatRegistrationFixture {
  val regId = RegistrationId("testId")
  val txId: TransactionId = TransactionId("1")

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
  val scrsAddress = ScrsAddress("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val sicCode = SicCode("88888888", "description", "displayDetails")
  val sicAndCompliance: VatSicAndCompliance = VatSicAndCompliance("some-business-description", None, None, mainBusinessActivity = sicCode)
  val vatDigitalContact = VatDigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))
  val vatContact = VatContact(digitalContact = vatDigitalContact, website = None, ppob = scrsAddress)
  val vatEligibility = VatServiceEligibility(
    haveNino = Some(true),
    doingBusinessAbroad = Some(true),
    doAnyApplyToYou = Some(true),
    applyingForAnyOf = Some(true),
    companyWillDoAnyOf = Some(true)
  )

  val name = Name(forename = Some("Forename"), surname = Some("Surname"), title = Some("Title"))
  val formerName = FormerName("Bob Smith", date)
  val contact = OfficerContactDetails(Some("test@test.com"), None, None)
  val vatScheme: VatScheme = VatScheme(regId)
  val exception = new Exception("Exception")
  val currentOrPreviousAddress = CurrentOrPreviousAddress(false, Some(scrsAddress))
  val vatFlatRateScheme = VatFlatRateScheme(
    joinFrs = true,
    annualCostsInclusive = Some("yesWithin12months"),
    annualCostsLimited = Some("yesWithin12months"),
    doYouWantToUseThisRate = Some(false),
    whenDoYouWantToJoinFrs = Some("VAT_REGISTRATION_DATE"))
  val changeOfName = ChangeOfName(true, Some(FormerName("", LocalDate.now())))

  def incorporationStatus(status: String = "accepted", incorpDate: LocalDate = LocalDate.now()): IncorporationStatus =
    IncorporationStatus(
      subscription = IncorpSubscription(
        transactionId = txId.value,
        regime = "vat",
        subscriber = "scrs",
        callbackUrl = "callbackUrl"
      ),
      statusEvent = IncorpStatusEvent(
        status = status,
        crn = Some("CRN"),
        incorporationDate = Some(incorpDate),
        description = Some("description")
      )
    )

}
