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
import config.AuthClientConnector
import connectors.test.BusinessRegistrationTestConnector
import connectors.BusinessRegistrationConnector
import play.api.mvc.{Action, AnyContent}
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import services.{LodgingOfficerService, RegistrationService, VatRegistrationService}
import uk.gov.hmrc.auth.core.AuthConnector
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.{Left, Right}

class TestSupportController @Inject()(val brConnector: BusinessRegistrationConnector,
                                      val brTestConnector: BusinessRegistrationTestConnector,
                                      lodgingOfficerService: LodgingOfficerService) extends TestSupportCon {

  val resourceConn: AuthorisationResource                              =  lodgingOfficerService.registrationRepository
  override lazy val authConnector: AuthConnector = AuthClientConnector
}

  trait TestSupportCon extends BaseController with Authorisation{

    val brConnector:BusinessRegistrationConnector
    val brTestConnector: BusinessRegistrationTestConnector
  def currentProfileSetup(): Action[AnyContent] = Action.async { implicit request =>
    isAuthenticated { _ =>
      brConnector.retrieveCurrentProfile flatMap {
        case Left(common.exceptions.ResourceNotFound(_))  => brTestConnector.createCurrentProfileEntry()
        case Right(_)                                     => Future.successful(Ok)
        case Left(_)                                      => Future.successful(ServiceUnavailable)
      }
    }
  }

  // TODO This should be removed - just double check there are no callers in ATs or the FE
  def dropCollection(): Action[AnyContent] = ???

}
