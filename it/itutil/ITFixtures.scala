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
import uk.gov.hmrc.http.HeaderCarrier

trait ITFixtures {

  implicit val hc: HeaderCarrier = HeaderCarrier()

  val date: LocalDate = LocalDate.of(2017, 1, 1)
  val startDate = StartDate(Some(date))
  val regId = "regId"
  val internalid = "INT-123-456-789"
  val transactionId = "transId"
  val vatScheme = VatScheme(regId, internalId = internalid, status = VatRegStatus.draft)
  val tradingName = TradingName(selection = true, Some("some-trading-name"))
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val tradingDetails = TradingDetails(Some("test-name"), true)
  val returns = Returns(
    reclaimVatOnMostReturns = true,
    frequency = "quarterly",
    staggerStart = Some("jan"),
    start = startDate
  )
  val frsDetails = FRSDetails(
    businessGoods = Some(BusinessGoods(12345678L, true)),
    startDate = Some(date),
    categoryOfBusiness = "testCategory",
    percent = 15
  )


  val flatRateScheme = FlatRateScheme(
    joinFrs = true,
    Some(frsDetails)
  )

  val EstimateValue: Long = 1000L
  val zeroRatedTurnoverEstimate: Long = 1000L

  val scrsAddress = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val digitalContact = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))

  val name = Name(first = Some("Forename"), middle = None, last = "Surname")
  val formerName = FormerName(Some("Bob Smith"), Some(date), name = Some(oldName), change = Some(date))
  val vatApplicantDetails = ApplicantDetails(
    nino = "NB686868C",
    role = "director",
    name = name,
    details = None
  )
  val businessContact = BusinessContact(digitalContact = digitalContact, website = None, ppob = scrsAddress)
  val sicAndCompliance = SicAndCompliance("businessDesc", Some(ComplianceLabour(1, Some(true), Some(true))), SicCode("12345678", "sicDesc", "sicDetail"), List(SicCode("12345678", "sicDesc", "sicDetail")))

  val vatTurnoverEstimates = TurnoverEstimates(12345678L)

  val vatBankAccount = BankAccount(true, None)

  val threshold = Threshold(true, None, None, None, None)

  val fullVatScheme =
    VatScheme(
      regId,
      internalid,
      Some(TransactionId(transactionId)),
      Some(tradingDetails),
      Some(returns),
      Some(sicAndCompliance),
      Some(businessContact),
      Some(vatTurnoverEstimates),
      Some(vatBankAccount),
      Some(threshold),
      Some("ackRef"),
      Some(flatRateScheme),
      VatRegStatus.draft
    )

  def emptyVatScheme(regId: String): VatScheme = VatScheme(
    id = regId,
    internalId = internalid,
    status = VatRegStatus.draft
  )
}
