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

package connectors.test

import com.google.inject.ImplementedBy
import common.TransactionId
import config.BackendConfig
import javax.inject.Inject
import play.api.mvc.{Result, Results}
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.bootstrap.http.HttpClient
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@ImplementedBy(classOf[VatRegIncorporationInformationTestConnector])
trait IncorporationInformationTestConnector {
  def incorpCompany(transactionId: TransactionId, incorpDate: String)(implicit hc: HeaderCarrier): Future[Result]
}

class VatRegIncorporationInformationTestConnector @Inject()(val backendConfig: BackendConfig, val http: HttpClient) extends IncorporationInformationTestConnector {

  //$COVERAGE-OFF$
  val iiUrl = backendConfig.servicesConfig.baseUrl("incorporation-information")

  def incorpCompany(transactionId: TransactionId, incorpDate: String)(implicit hc: HeaderCarrier): Future[Result] = {
    http.GET(s"$iiUrl/incorporation-information/test-only/add-incorp-update?txId=" +
      s"$transactionId&date=$incorpDate&success=true&crn=12345").map(_ => Results.Ok)
  }
  //$COVERAGE-ON$
}
