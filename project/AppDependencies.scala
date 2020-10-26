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
  val domainVersion = "5.10.0-play-26"
  val bootstrapVersion = "1.16.0"
  val simpleReactiveMongoVersion = "7.30.0-play-26"
  val catsVersion = "0.9.0"
  private val authClientVersion = "3.2.0-play-26"
  private val playJsonVersion = "2.9.1"

  val compile = Seq(
    "uk.gov.hmrc" %% "simple-reactivemongo" % simpleReactiveMongoVersion,
    "uk.gov.hmrc" %% "bootstrap-play-26" % bootstrapVersion,
    "uk.gov.hmrc" %% "domain" % domainVersion,
    "org.typelevel" %% "cats" % catsVersion,
    "uk.gov.hmrc" %% "auth-client" % authClientVersion,
    "com.typesafe.play" %% "play-json-joda" % playJsonVersion
  )

  def apply(): Seq[ModuleID] = compile
}

object UnitTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = Test

  val mockitoVersion = "3.3.3"

  override val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
    "org.mockito" % "mockito-core" % mockitoVersion % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

object IntegrationTestDependencies extends CommonTestDependencies {
  override val scope: Configuration = IntegrationTest

  val wireMockVersion = "2.26.3"

  override val testDependencies: Seq[ModuleID] = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
    "org.scoverage" %% "scalac-scoverage-runtime" % scoverageVersion % scope,
    "uk.gov.hmrc" %% "reactivemongo-test" % reactiveMongoTestVersion % scope,
    "com.github.tomakehurst" % "wiremock-jre8" % wireMockVersion % scope
  )

  def apply(): Seq[ModuleID] = testDependencies
}

trait CommonTestDependencies {
  val scalaTestPlusVersion = "3.1.3"
  val scoverageVersion = "1.4.1"
  val reactiveMongoTestVersion = "4.21.0-play-26"
  val scope: Configuration
  val testDependencies: Seq[ModuleID]
}
