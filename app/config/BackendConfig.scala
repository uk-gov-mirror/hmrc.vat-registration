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

package config

import featureswitch.core.config.{FeatureSwitching, StubSubmission}
import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class BackendConfig @Inject()(val servicesConfig: ServicesConfig,
                              val runModeConfiguration: Configuration) extends FeatureSwitching {

  def loadConfig(key: String): String = servicesConfig.getString(key)

  lazy val vatRegistrationUrl: String = servicesConfig.baseUrl("vat-registration")
  lazy val desBaseUrl: String = servicesConfig.getConfString("des.url", "")
  val desEndpoint = "/vat/subscription"

  def desUrl: String = if (isEnabled(StubSubmission)) {
    vatRegistrationUrl + "/vatreg/test-only" + desEndpoint
  }
  else {
    desBaseUrl + desEndpoint
  }

  lazy val desStubTopUpUrl: String = servicesConfig.baseUrl("des-stub")
  lazy val desStubTopUpURI: String = servicesConfig.getConfString("des-stub.uri", "")

  lazy val urlHeaderEnvironment: String = servicesConfig.getConfString("des-service.environment", throw new Exception("could not find config value for des-service.environment"))
  lazy val urlHeaderAuthorization: String = s"Bearer ${
    servicesConfig.getConfString("des-service.authorization-token",
      throw new Exception("could not find config value for des-service.authorization-token"))
  }"

  lazy val dailyQuota: Int = servicesConfig.getConfInt("constants.daily-quota", 10)

}