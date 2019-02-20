/*
 * Copyright 2019 HM Revenue & Customs
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

import auth.{Authorisation, AuthorisationResource}
import common.TransactionId
import connectors.IncorporationInformationConnector
import javax.inject.Inject
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent}
import services.SubmissionService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BaseController
import scala.concurrent.ExecutionContext.Implicits.global


class IncorporationInformationController @Inject()(val iiConnector: IncorporationInformationConnector,
                                                   val submissionService: SubmissionService,
                                                   val authConnector: AuthConnector) extends BaseController with Authorisation {
  val resourceConn: AuthorisationResource = submissionService.registrationRepository

  private val REGIME = "vat"
  private val SUBSCRIBER = "scrs"

  def getIncorporationInformation(transactionId: TransactionId): Action[AnyContent] = Action.async {
    implicit request =>
      isAuthenticated { user =>
        iiConnector.retrieveIncorporationStatus(None, transactionId, REGIME, SUBSCRIBER).map {status =>
          status.fold(Ok(""))(incorpstatus => Ok(Json.toJson(incorpstatus)))
        }
      }
  }
}
