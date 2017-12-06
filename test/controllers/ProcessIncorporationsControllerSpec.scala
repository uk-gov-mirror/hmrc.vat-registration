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

package controllers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import models.external.IncorpStatus
import org.joda.time.DateTime
import org.mockito.ArgumentMatchers
import org.mockito.Matchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import play.api.libs.json.{JsObject, Json}
import play.api.test.FakeRequest
import play.api.test.Helpers._
import services.SubmissionService
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.test.UnitSpec

import scala.concurrent.Future

class ProcessIncorporationsControllerSpec extends UnitSpec with MockitoSugar  {

  implicit val as = ActorSystem()
  implicit val mat = ActorMaterializer()

  val regId = "1234"
  val incDate = DateTime.parse("2000-12-12")
  val transactionId = "trans-12345"
  val crn = "crn-12345"

  val mockSubmissionService = mock[SubmissionService]

  class Setup {
    val controller = new ProcessIncorporationsController {
      override val submissionService = mockSubmissionService
    }
  }

  val rejectedIncorpJson = Json.parse(
    s"""
       |{
       |  "SCRSIncorpStatus":{
       |    "IncorpSubscriptionKey":{
       |      "subscriber":"abc123",
       |      "discriminator":"CT100",
       |      "transactionId":"$transactionId"
       |    },
       |    "SCRSIncorpSubscription":{
       |      "callbackUrl":"www.testUpdate.com"
       |    },
       |    "IncorpStatusEvent":{
       |      "status":"rejected",
       |      "description":"description"
       |    }
       |  }
       |}
    """.stripMargin).as[JsObject]

  val rejectedIncorpStatus = IncorpStatus(transactionId, "rejected", None, Some("description"), None)

  val acceptedIncorpJson = Json.parse(
    s"""
       |{
       |  "SCRSIncorpStatus":{
       |    "IncorpSubscriptionKey":{
       |      "subscriber":"abc123",
       |      "discriminator":"CT100",
       |      "transactionId":"$transactionId"
       |    },
       |    "SCRSIncorpSubscription":{
       |      "callbackUrl":"www.testUpdate.com"
       |    },
       |    "IncorpStatusEvent":{
       |       "status":"accepted",
       |      "crn":"$crn",
       |      "incorporationDate":${incDate.getMillis}
       |    }
       |  }
       |}
    """.stripMargin).as[JsObject]

  val acceptedIncorpStatus = IncorpStatus(transactionId, "accepted", Some(crn), None, Some(incDate))

  "ProcessIncorp" should {

    "read json from request into IncorpStatus case class" in {
      rejectedIncorpJson.as[IncorpStatus](IncorpStatus.reads) shouldBe rejectedIncorpStatus
    }

    "return a 200 response " in new Setup {

      when(mockSubmissionService.submitTopUpVatRegistration(ArgumentMatchers.any[IncorpStatus]())(ArgumentMatchers.any())).thenReturn(Future.successful(true))

      val request = FakeRequest().withBody[JsObject](rejectedIncorpJson)

      val result = await(call(controller.processIncorp, request))

      status(result) shouldBe 200

    }
  }

  "Invalid Data" should {

    "return a 500 response for non admin flow" in new Setup {

      when(mockSubmissionService.submitTopUpVatRegistration(ArgumentMatchers.any[IncorpStatus]())(ArgumentMatchers.any())).thenReturn(Future.successful(false))

      val request = FakeRequest().withBody[JsObject](rejectedIncorpJson)

      val result = await(call(controller.processIncorp, request))

      status(result) shouldBe 400

    }
  }
}
