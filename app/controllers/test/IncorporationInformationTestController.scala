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

package controllers.test

import auth.{Authorisation, AuthorisationResource}
import cats.instances.FutureInstances
import common.TransactionId
import connectors.test.IncorporationInformationTestConnector
import javax.inject.{Inject, Singleton}
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import services.LodgingOfficerService
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.bootstrap.controller.BackendController

import scala.concurrent.ExecutionContext.Implicits.global

@Singleton
class IncorporationInformationTestController @Inject()(val iiTestConnector: IncorporationInformationTestConnector,
                                                       lodgingOfficer: LodgingOfficerService,
                                                       controllerComponents: ControllerComponents,
                                                       val authConnector: AuthConnector
                                                      )extends BackendController(controllerComponents) with Authorisation with FutureInstances {

  val resourceConn: AuthorisationResource =  lodgingOfficer.registrationRepository

  def incorpCompany(transactionId: TransactionId, incorpDate: String): Action[AnyContent] = Action.async { implicit request =>

    isAuthenticated { _ =>
      iiTestConnector.incorpCompany(transactionId, incorpDate).map(_ => Ok)
    }
  }
}
