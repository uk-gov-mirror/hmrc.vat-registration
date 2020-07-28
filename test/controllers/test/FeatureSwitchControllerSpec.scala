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

package controllers.test

import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.PlaySpec
import play.api.mvc.Result
import play.api.test.FakeRequest
import play.api.test.Helpers._

import scala.concurrent.Future

class FeatureSwitchControllerSpec extends PlaySpec with BeforeAndAfterEach {

  override def beforeEach() {
    System.clearProperty("feature.mockSubmission")
  }

  class Setup {
    val controller = new FeatureSwitchController(stubControllerComponents())
  }

  "show" should {

    "return a 200 and display all feature flags and their " in new Setup {
      val result: Future[Result] = controller.show(FakeRequest())
      status(result) mustBe 200
      contentAsString(result) mustBe "mockSubmission false\n"
    }
  }

  "switch" should {

    "return a first mockSubmission feature state set to false when we specify off" in new Setup {
      val featureName = "mockSubmission"
      val featureState = "false"

      val result: Future[Result] = controller.switch(featureName, featureState)(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe "BooleanFeatureSwitch(mockSubmission,false)"
    }

    "return a mockSubmission feature state set to true when we specify on" in new Setup {
      val featureName = "mockSubmission"
      val featureState = "true"

      val result: Future[Result] = controller.switch(featureName, featureState)(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe "BooleanFeatureSwitch(mockSubmission,true)"
    }

    "return a submissionCheck feature state set to false as a default when we specify xxxx" in new Setup {
      val featureName = "mockSubmission"
      val featureState = "xxxx"

      val result: Future[Result] = controller.switch(featureName, featureState)(FakeRequest())
      status(result) mustBe OK
      contentAsString(result) mustBe "BooleanFeatureSwitch(mockSubmission,false)"
    }

    "return a bad request when we specify a non implemented feature name" in new Setup {
      val featureName = "Rubbish"
      val featureState = "on"

      val result: Future[Result] = controller.switch(featureName, featureState)(FakeRequest())

      status(result) mustBe BAD_REQUEST
    }
  }
}
