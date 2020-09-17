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

import java.time.{LocalDate, ZoneId}

import common.TransactionId
import enums.VatRegStatus
import models.api._
import play.api.libs.json.{JsObject, Json}

trait VatRegistrationFixture {
  val regId = "testRegId"
  val internalid = "INT-123-456-789"
  val txId: TransactionId = TransactionId("1")
  val regime = "vat"
  val subscriber = "scrs"
  val now = LocalDate.now(ZoneId.systemDefault())
  val userId = "userId"
  val ackRefNumber = "BRPY000000000001"
  val date: LocalDate = LocalDate.of(2018, 1, 1)
  val scrsAddress = Address("line1", "line2", None, None, Some("XX XX"), Some("UK"))
  val sicCode = SicCode("88888", "description", "displayDetails")
  val digitalContact = DigitalContact("test@test.com", Some("12345678910"), Some("12345678910"))

  val name = Name(first = Some("Forename"), middle = None, last = "Surname")
  val oldName = Name(first = Some("Bob"), middle = None, last = "Smith")
  val formerName = FormerName(Some("Bob Smith"), Some(date), name = Some(oldName), change = Some(date))
  val vatScheme: VatScheme = VatScheme(regId, internalId = internalid, status = VatRegStatus.draft)
  val exception = new Exception("Exception")

  val voluntaryThreshold = Threshold(false, Some("voluntaryReason"), Some(LocalDate.now()), Some(LocalDate.now()), Some(LocalDate.now()))
  val mandatoryThreshold = Threshold(true, None, Some(LocalDate.now()), Some(LocalDate.now()), Some(LocalDate.now()))
  val currentAddress = Address("12 Lukewarm", "Oriental lane")
  val skylakeValiarm = Name(first = Some("Skylake"), middle = None, last = "Valiarm")
  val skylakeDigitalContact = DigitalContactOptional(Some("skylake@vilikariet.com"), None, None)
  val applicantDetailsDetails = ApplicantDetailsDetails(currentAddress = currentAddress, None, None, contact = skylakeDigitalContact)
  val validApplicantDetailsPreIV = ApplicantDetails(
    nino = "AB123456A",
    role = "secretary",
    name = skylakeValiarm,
    details = None
  )

  val otherBusinessActivitiesSicAndCompiliance =
    SicCode("00998", "otherBusiness desc 1", "fooBar 1") :: SicCode("00889", "otherBusiness desc 2", "fooBar 2") :: Nil

  val validSicAndCompliance = Some(SicAndCompliance(
    "this is my business description",
    Some(ComplianceLabour(1000, Some(true), Some(true))),
    SicCode("12345", "the flu", "sic details"),
    otherBusinessActivitiesSicAndCompiliance
  ))

  val validBusinessContact = Some(BusinessContact(
    digitalContact = DigitalContact("email@email.com", Some("12345"), Some("54321")),
    website = Some("www.foo.com"),
    ppob = Address("line1", "line2", None, None, None, Some("foo"))
  ))

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
       | }
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

  val validFullFRSDetails: FRSDetails =
    FRSDetails(
      businessGoods = Some(BusinessGoods(1234567891011L, true)),
      startDate = Some(date),
      categoryOfBusiness = "testCategory",
      percent = 15
    )
  val validFullFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, Some(validFullFRSDetails))
  val validEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = false, None)
  val invalidEmptyFlatRateScheme: FlatRateScheme = FlatRateScheme(joinFrs = true, None)

  val validFullFRSDetailsJsonWithBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "businessGoods" : {
       |    "overTurnover": true,
       |    "estimatedTotalSales": 1234567891011
       |  },
       |  "startDate": "$date",
       |  "categoryOfBusiness":"testCategory",
       |  "percent":15.00
       |}
     """.stripMargin).as[JsObject]

  val validFRSDetailsJsonWithoutBusinessGoods: JsObject = Json.parse(
    s"""
       |{
       |  "startDate": "$date",
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
       |  "startDate": "$date",
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
