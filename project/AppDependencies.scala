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

import sbt._

object AppDependencies {
  def apply(): Seq[ModuleID] = CompileDependencies() ++ UnitTestDependencies() ++ IntegrationTestDependencies()
}

object CompileDependencies {
  val domainVersion                       = "5.6.0-play-25"
  val bootstrapVersion                    = "4.13.0"
  val reactiveMongoVersion                = "7.20.0-play-25"
  val catsVersion                         = "0.9.0"
  private val authClientVersion           = "2.22.0-play-25"

  val compile = Seq(
    "uk.gov.hmrc"   %% "simple-reactivemongo"   % reactiveMongoVersion,
    "uk.gov.hmrc"   %% "bootstrap-play-25"      % bootstrapVersion,
    "uk.gov.hmrc"   %% "domain"                 % domainVersion,
    "org.typelevel" %% "cats"                   % catsVersion,
    "uk.gov.hmrc" %% "auth-client"              % authClientVersion

  )

  def apply(): Seq[ModuleID] = compile
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = Test

  val mockitoVersion = "2.13.0"

  override val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                       % hmrcTestVersion           % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"             % scalaTestPlusVersion      % scope,
    "org.scoverage"           %  "scalac-scoverage-runtime_2.11"  % scoverageVersion          % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"             % reactiveMongoTestVersion  % scope,
    "org.mockito"             %  "mockito-core"                   % mockitoVersion            % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

object IntegrationTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = IntegrationTest

  val wireMockVersion = "2.6.0"

  override val testDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"             %% "hmrctest"                       % hmrcTestVersion           % scope,
    "org.scalatestplus.play"  %% "scalatestplus-play"             % scalaTestPlusVersion      % scope,
    "org.scoverage"           %  "scalac-scoverage-runtime_2.11"  % scoverageVersion          % scope,
    "uk.gov.hmrc"             %% "reactivemongo-test"             % reactiveMongoTestVersion  % scope,
    "com.github.tomakehurst"  %  "wiremock"                       % wireMockVersion           % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

trait CommonTestDependencies {
  val hmrcTestVersion          = "3.9.0-play-25"
  val scalaTestPlusVersion     = "2.0.1"
  val scoverageVersion         = "1.3.1"
  val reactiveMongoTestVersion = "4.15.0-play-25"
  val scope: Configuration
  val testDependencies: Seq[ModuleID]
}
