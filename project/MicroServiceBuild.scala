import sbt._

object MicroServiceBuild extends Build with MicroService {

  val appName = "vat-registration"

  override lazy val appDependencies: Seq[ModuleID] = AppDependencies()
}

private object AppDependencies {

  import play.core.PlayVersion
  import play.sbt.PlayImport._

  private val hmrcTestVersion = "2.2.0"
  private val scalaTestVersion_test = "3.0.1"
  private val scalaTestVersion_it = "2.2.6"
  private val scalaTestPlusVersion = "1.5.1"
  private val pegdownVersion = "1.6.0"
  private val scoverageVersion = "1.3.0"


  val compile = Seq(
    ws,
    "uk.gov.hmrc" %% "play-reactivemongo" % "5.1.0",
    "uk.gov.hmrc" %% "microservice-bootstrap" % "5.8.0",
    "uk.gov.hmrc" %% "play-authorisation" % "4.2.0",
    "uk.gov.hmrc" %% "play-health" % "2.0.0",
    "uk.gov.hmrc" %% "play-url-binders" % "2.0.0",
    "uk.gov.hmrc" %% "play-config" % "3.0.0",
    "uk.gov.hmrc" %% "play-json-logger" % "3.0.0",
    "uk.gov.hmrc" %% "domain" % "4.0.0",
    "uk.gov.hmrc" %% "crypto" % "3.1.0",
    "org.typelevel" %% "cats" % "0.9.0"
  )

  trait TestDependencies {
    lazy val scope: String = "test"
    lazy val test: Seq[ModuleID] = ???
  }

  object Test {
    def apply(): Seq[ModuleID] = new TestDependencies {
      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion_test % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.scoverage" % "scalac-scoverage-runtime_2.11" % scoverageVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % "1.6.0" % scope,
        "org.mockito" % "mockito-core" % "1.9.5"
      )
    }.test
  }

  object IntegrationTest {
    def apply(): Seq[ModuleID] = new TestDependencies {

      override lazy val scope: String = "it"

      override lazy val test = Seq(
        "uk.gov.hmrc" %% "hmrctest" % hmrcTestVersion % scope,
        "org.scalatest" %% "scalatest" % scalaTestVersion_it % scope,
        "org.scalatestplus.play" %% "scalatestplus-play" % scalaTestPlusVersion % scope,
        "org.pegdown" % "pegdown" % pegdownVersion % scope,
        "org.scoverage" % "scalac-scoverage-runtime_2.11" % scoverageVersion % scope,
        "com.typesafe.play" %% "play-test" % PlayVersion.current % scope,
        "uk.gov.hmrc" %% "reactivemongo-test" % "1.6.0" % scope,
        "com.github.tomakehurst" % "wiremock" % "2.5.0" % scope
      )
    }.test
  }

  def apply(): Seq[ModuleID] = compile ++ Test() ++ IntegrationTest()
}
