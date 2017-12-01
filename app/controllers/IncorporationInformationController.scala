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

package controllers

import javax.inject.Inject

import auth.Authenticated
import cats.instances.FutureInstances
import common.TransactionId
import connectors.{AuthConnector, IncorporationInformationConnector}
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class IncorporationInformationController @Inject()(val auth: AuthConnector,
                                                   iiConnector: IncorporationInformationConnector) extends BaseController with Authenticated with FutureInstances {

  private val REGIME = "vat"
  private val SUBSCRIBER = "scrs"

  def getIncorporationInformation(transactionId: TransactionId): Action[AnyContent] = Action.async {
    implicit request =>
      authenticated { user =>
        iiConnector.retrieveIncorporationStatus(transactionId, REGIME, SUBSCRIBER).map {status =>
          status.fold(Ok(""))(incorpstatus => Ok(Json.toJson(incorpstatus)))
        }
      }
  }
}
