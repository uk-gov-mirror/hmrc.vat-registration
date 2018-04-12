/*
 * Copyright 2018 HM Revenue & Customs
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

import javax.inject.Inject

import auth.{Authorisation, AuthorisationResource}
import cats.instances.FutureInstances
import common.TransactionId
import config.AuthClientConnector
import connectors.test.IncorporationInformationTestConnector
import play.api.mvc.{Action, AnyContent}
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import services.{LodgingOfficerService, VatRegistrationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

class IncorporationInformationTestController @Inject()(val iiTestConnector: IncorporationInformationTestConnector,
                                                       lodgingOfficer: LodgingOfficerService) extends IncorpInfoTestCon {

  val resourceConn: AuthorisationResource                              =  lodgingOfficer.registrationRepository
  override lazy val authConnector: AuthConnector = AuthClientConnector
}
  trait IncorpInfoTestCon extends BaseController with Authorisation with FutureInstances {

    val iiTestConnector:IncorporationInformationTestConnector

  def incorpCompany(transactionId: TransactionId, incorpDate: String): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      iiTestConnector.incorpCompany(transactionId, incorpDate).map(_ => Ok)
    }
  }
}
