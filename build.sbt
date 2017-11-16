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

import TestPhases.oneForkedJvmPerTest
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, defaultSettings, scalaSettings}
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings

val appName = "vat-registration"

lazy val scoverageSettings = Seq(
  ScoverageKeys.coverageExcludedPackages  := "<empty>;Reverse.*;config.*;.*(AuthService|BuildInfo|Routes).*",
  ScoverageKeys.coverageMinimum           := 100,
  ScoverageKeys.coverageFailOnMinimum     := false,
  ScoverageKeys.coverageHighlighting      := true
)

lazy val microservice = Project(appName, file("."))
  .enablePlugins(Seq(PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin): _*)
  .settings(PlayKeys.playDefaultPort := 9896)
  .settings(scalaSettings: _*)
  .settings(scoverageSettings: _*)
  .settings(publishingSettings: _*)
  .settings(defaultSettings(): _*)
  .configs(IntegrationTest)
  .settings(inConfig(IntegrationTest)(Defaults.itSettings): _*)
  .settings(
    scalaVersion                                  := "2.11.11",
    libraryDependencies                           ++= AppDependencies(),
    retrieveManaged                               := true,
    evictionWarningOptions in update              := EvictionWarningOptions.default.withWarnScalaVersionEviction(false),
    routesImport                                  ++= Seq("config.CustomPathBinder._", "common.{RegistrationId, TransactionId}", "models.ElementPath"),
    Keys.fork in IntegrationTest                  := false,
    unmanagedSourceDirectories in IntegrationTest := (baseDirectory in IntegrationTest) (base => Seq(base / "it")).value,
    addTestReportOption(IntegrationTest, "int-test-reports"),
    testGrouping in IntegrationTest               := oneForkedJvmPerTest((definedTests in IntegrationTest).value),
    parallelExecution in IntegrationTest          := false,
    resolvers                                     ++= Seq(Resolver.bintrayRepo("hmrc", "releases"), Resolver.jcenterRepo)
  )

