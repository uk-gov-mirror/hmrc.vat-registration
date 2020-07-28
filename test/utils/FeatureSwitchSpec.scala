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

package utils

import org.scalatest.{BeforeAndAfterEach, Matchers}
import org.scalatestplus.play.PlaySpec

class FeatureSwitchSpec extends PlaySpec with BeforeAndAfterEach {

  override def beforeEach() {
    System.clearProperty("feature.mockSubmission")
    System.clearProperty("feature.test")
  }

  "apply" should {

    "return a constructed BooleanFeatureSwitch if the set system property is a boolean" in {
      System.setProperty("feature.test", "true")

      FeatureSwitch("test") mustBe BooleanFeatureSwitch("test", enabled = true)
    }

    "create an instance of BooleanFeatureSwitch which inherits FeatureSwitch" in {
      FeatureSwitch("test") mustBe a[FeatureSwitch]
      FeatureSwitch("test") mustBe a[BooleanFeatureSwitch]
    }
  }

  "unapply" should {

    "deconstruct a given FeatureSwitch into it's name and a false enabled value if undefined as a system property" in {
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) mustBe Some("test" -> false)
    }

    "deconstruct a given FeatureSwitch into its name and true if defined as true as a system property" in {
      System.setProperty("feature.test", "true")
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) mustBe Some("test" -> true)
    }

    "deconstruct a given FeatureSwitch into its name and false if defined as false as a system property" in {
      System.setProperty("feature.test", "false")
      val fs = FeatureSwitch("test")

      FeatureSwitch.unapply(fs) mustBe Some("test" -> false)
    }
  }

  "getProperty" should {

    "return a disabled feature switch if the system property is undefined" in {
      FeatureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return an enabled feature switch if the system property is defined as 'true'" in {
      System.setProperty("feature.test", "true")

      FeatureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = true)
    }

    "return an enabled feature switch if the system property is defined as 'false'" in {
      System.setProperty("feature.test", "false")

      FeatureSwitch.getProperty("test") mustBe BooleanFeatureSwitch("test", enabled = false)
    }
  }

  "systemPropertyName" should {

    "append feature. to the supplied string'" in {
      FeatureSwitch.systemPropertyName("test") mustBe "feature.test"
    }
  }

  "setProperty" should {

    "return a feature switch (testKey, false) when supplied with (testKey, testValue)" in {
      FeatureSwitch.setProperty("test", "testValue") mustBe BooleanFeatureSwitch("test", enabled = false)
    }

    "return a feature switch (testKey, true) when supplied with (testKey, true)" in {
      FeatureSwitch.setProperty("test", "true") mustBe BooleanFeatureSwitch("test", enabled = true)
    }
  }

  "enable" should {
    "set the value for the supplied key to 'true'" in {
      val fs = FeatureSwitch("test")
      System.setProperty("feature.test", "false")

      FeatureSwitch.enable(fs) mustBe BooleanFeatureSwitch("test", enabled = true)
    }
  }

  "disable" should {
    "set the value for the supplied key to 'false'" in {
      val fs = FeatureSwitch("test")
      System.setProperty("feature.test", "true")

      FeatureSwitch.disable(fs) mustBe BooleanFeatureSwitch("test", enabled = false)
    }
  }

  "dynamic toggling should be supported" in {
    val fs = FeatureSwitch("test")

    FeatureSwitch.disable(fs).enabled mustBe false
    FeatureSwitch.enable(fs).enabled mustBe true
  }

  "SCRSFeatureSwitches" should {
    "return a disabled feature when the associated system property doesn't exist" in {
      VATFeatureSwitches.mockSubmission.enabled mustBe false
    }

    "return an enabled feature when the associated system property is true" in {
      FeatureSwitch.enable(VATFeatureSwitches.mockSubmission)

      VATFeatureSwitches.mockSubmission.enabled mustBe true
    }

    "return a disable feature when the associated system property is false" in {
      FeatureSwitch.disable(VATFeatureSwitches.mockSubmission)

      VATFeatureSwitches.mockSubmission.enabled mustBe false
    }

    "return an enabled mockSubmission feature switch if it exists" in {
      System.setProperty("feature.mockSubmission", "true")

      VATFeatureSwitches("mockSubmission") mustBe Some(BooleanFeatureSwitch("mockSubmission", true))
    }

    "return a disable feature switch if the mockSubmission system property doesn't exist when using the apply function" in {
      VATFeatureSwitches("mockSubmission") mustBe Some(BooleanFeatureSwitch("mockSubmission", false))
    }
  }
}
