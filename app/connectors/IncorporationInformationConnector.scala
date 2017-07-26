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

package connectors

import cats.data.EitherT
import common.exceptions._
import common.TransactionId
import config.WSHttp
import models.external.IncorporationStatus
import play.api.Logger
import play.api.libs.json.{JsString, Json, Writes}
import uk.gov.hmrc.play.config.ServicesConfig
import uk.gov.hmrc.play.http._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

class VatRegIncorporationInformationConnector extends IncorporationInformationConnector with ServicesConfig {
  //$COVERAGE-OFF$
  val iiUrl = baseUrl("incorporation-information")
  val http = WSHttp
  //$COVERAGE-ON$
}

final case class IncorpStatusRequest(callbackUrl: String)

object IncorpStatusRequest {

  implicit val writes: Writes[IncorpStatusRequest] =
    Writes(isr => Json.obj("SCRSIncorpSubscription" -> Json.obj("callbackUrl" -> JsString(isr.callbackUrl))))

}


trait IncorporationInformationConnector {

  val iiUrl: String
  val http: HttpGet with HttpPost

  def retrieveIncorporationStatus(transactionId: TransactionId)
                                 (implicit hc: HeaderCarrier, rds: HttpReads[IncorporationStatus]): EitherT[Future, LeftState, IncorporationStatus] =
    EitherT(http.POST[IncorpStatusRequest, HttpResponse](
      s"$iiUrl/incorporation-information/subscribe/$transactionId/regime/vat/subscriber/scrs",
      IncorpStatusRequest("http://localhost:9896/TODO-CHANGE-THIS") //TODO change this to whatever this will be
    ).map {
      case r if r.status == 200 => Right(r.json.as[IncorporationStatus])
      case r if r.status == 202 => Left(NothingToReturn("")) //TODO revisit
      case r =>
        Logger.error(s"${r.status} response code returned requesting II for txId: $transactionId")
        Left(GenericError(new RuntimeException(s"No joy subscribing to II")))
    })

}
