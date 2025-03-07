/*
 * Copyright 2021 HM Revenue & Customs
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

package featureswitch.core.config

import javax.inject.Singleton
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}
import featureswitch.core.models.FeatureSwitch

@Singleton
class FeatureSwitchingModule extends Module with FeatureSwitchRegistry {

  val switches = Seq(
    StubSubmission,
    CheckYourAnswersNrsSubmission,
    UseSubmissionAuditBuilders
  )

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    Seq(
      bind[FeatureSwitchRegistry].to(this).eagerly()
    )
  }
}

case object StubSubmission extends FeatureSwitch {
  override val configName: String = "feature-switch.submission-stub"
  override val displayName: String = "Use stub for Submission to DES"
}

case object CheckYourAnswersNrsSubmission extends FeatureSwitch {
  override val configName: String = "feature-switch.check-your-answers-nrs-submission"
  override val displayName: String = "Use CYA HTML as payload for NRS"
}

case object UseSubmissionAuditBuilders extends FeatureSwitch {
  override val configName: String = "feature-switch.use-submission-audit-builders"
  override val displayName: String = "Use submission audit builders"
}