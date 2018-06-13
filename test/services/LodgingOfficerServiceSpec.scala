/*
 * Copyright 2018 HM Revenue & Customs
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

import java.time.LocalDate

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.{DigitalContactOptional, LodgingOfficer, LodgingOfficerDetails}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.mockito.stubbing.OngoingStubbing
import play.api.libs.json.{JsObject, Json}
import repositories.RegistrationMongoRepository

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class LodgingOfficerServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new LodgingOfficerService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }

    def updateIVPassedToMongo(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.successful(true))

    def updateIVPassedToMongoFail(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.failed(new Exception("")))

    def updateIVPassedToMongoNoRegDoc(): OngoingStubbing[Future[Boolean]] = when(mockRegistrationMongoRepository.updateIVStatus(any(),any())(any()))
      .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))
  }

  val upsertLodgingOfficerModel = Json.parse(
    s"""
      |{
      | "name": {
      |   "first" : "Skylake",
      |   "last" : "Valiarm"
      | },
      | "dob" : "${LocalDate.now()}",
      | "nino" : "AB123456A",
      | "role" : "secretary",
      | "ivPassed" : true,
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

  val validLodgingOfficerModel = Json.parse(
    s"""
       |{
       | "name": {
       |   "first" : "Skylake",
       |   "last" : "Valiarm"
       | },
       | "dob" : "${LocalDate.now()}",
       | "nino" : "AB123456A",
       | "role" : "secretary",
       | "isOfficerApplying": true
       |}
    """.stripMargin).as[LodgingOfficer]

  "updateIVStatus" should {
    "return a boolean" in new Setup {
      updateIVPassedToMongo()
      val result = await(service.updateIVStatus("regId", true))
      result shouldBe true
    }

    "encounter an exception if an error occurs" in new Setup {
      updateIVPassedToMongoFail()
      intercept[Exception](await(service.updateIVStatus("regId", true)))
    }

    "encounter an MissingRegDocument Exception if no docuemnt is found" in new Setup {
      updateIVPassedToMongoNoRegDoc()
      intercept[MissingRegDocument](await(service.updateIVStatus("regId", true)))
    }
  }

  "getLodgingOfficerData" should {
    "return an Lodging Officer if found" in new Setup {
      when(mockRegistrationMongoRepository.getCombinedLodgingOfficer(any())(any()))
        .thenReturn(Future.successful(Some(validLodgingOfficerPreIV)))

      val result = await(service.getLodgingOfficerData("regId"))
      result shouldBe Some(validLodgingOfficerModel)
    }

    "return None if none found matching regId" in new Setup {
      when(mockRegistrationMongoRepository.getCombinedLodgingOfficer(any())(any()))
        .thenReturn(Future.successful(None))

      val result = await(service.getLodgingOfficerData("regId"))
      result shouldBe None
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
         | "ivPassed": true,
         | "dob": "2015-11-20",
         | "details": $lodgingOfficerDetails
         |}
        """.stripMargin).as[JsObject]

    "return the data that is being inputed" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.successful(lodgeOfficerJson))

      val result = await(service.updateLodgingOfficerData("regId", lodgeOfficerJson))
      result shouldBe lodgeOfficerJson
    }

    "encounter an exception if an error occurs" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.failed(new Exception("")))

      intercept[Exception](await(service.updateLodgingOfficerData("regId", lodgeOfficerJson)))
    }

    "encounter an MissingRegDocument Exception if no docuemnt is found" in new Setup {
      when(mockRegistrationMongoRepository.patchLodgingOfficer(any(),any())(any()))
        .thenReturn(Future.failed(MissingRegDocument(RegistrationId("testId"))))

      intercept[MissingRegDocument](await(service.updateLodgingOfficerData("regId", lodgeOfficerJson)))
    }
  }
}
