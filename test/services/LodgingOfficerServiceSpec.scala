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

package services

import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{DigitalContactOptional, LodgingOfficer, LodgingOfficerDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class LodgingOfficerServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service: LodgingOfficerService = new LodgingOfficerService(
      registrationRepository = mockRegistrationMongoRepository
    )

    def updateIVPassedToMongo(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.successful(true))

    def updateIVPassedToMongoFail(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def updateIVPassedToMongoNoRegDoc(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(regId)))
  }

  val upsertLodgingOfficerModel: LodgingOfficer = Json.parse(
    s"""
      |{
      | "name": {
      |   "first" : "Skylake",
      |   "last" : "Valiarm"
      | },
      | "nino" : "AB123456A",
      | "role" : "secretary",
      | "details" : {
      |   "currentAddress" : {
      |     "line1" : "12 Lukewarm",
      |     "line2"  : "Oriental lane"
      |   },
      |   "contact" : {
      |     "email" : "skylake@vilikariet.com"
      |   }
      | },
      | "isOfficerApplying": true
      |}
    """.stripMargin).as[LodgingOfficer]

  val validLodgingOfficerModel: LodgingOfficer = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[LodgingOfficer]

  "getLodgingOfficerData" should {
    "return an Lodging Officer if found" in new Setup {
      when(mockRegistrationMongoRepository.getCombinedLodgingOfficer(any())(any()))
        .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

      val result: Option[LodgingOfficer] = await(service.getLodgingOfficerData("regId"))
      result mustBe Some(validLodgingOfficerModel)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.getCombinedLodgingOfficer(any())(any()))
        .thenReturn(Future.successful(None))

      val result: Option[LodgingOfficer] = await(service.getLodgingOfficerData("regId"))
      result mustBe None
    }
  }

  "updateLodgingOfficerData" should {
    val lodgingOfficerDetails = Json.toJson(LodgingOfficerDetails(
      currentAddress = scrsAddress,
      changeOfName = None,
      previousAddress = None,
      contact = DigitalContactOptional(
        email = Some("test@t.com"),
        tel = None,
        mobile = None
      )
    ))
    val lodgeOfficerJson = Json.parse(
      s"""{
         | "details": $lodgingOfficerDetails
         |}
        """.stripMargin).as[JsObject]

    "return the data that is being inputted" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.successful(lodgeOfficerJson))

      val result: JsObject = await(service.updateLodgingOfficerData("regId", lodgeOfficerJson))
      result mustBe lodgeOfficerJson
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateLodgingOfficerData("regId", lodgeOfficerJson)))
    }

    "encounter an MissingRegDocument Exception if no document is found" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(regId)))

      intercept[MissingRegDocument](await(service.updateLodgingOfficerData("regId", lodgeOfficerJson)))
    }
  }
}
