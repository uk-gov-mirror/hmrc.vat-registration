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

import auth.Authenticated
import connectors.test.BusinessRegistrationTestConnector
import connectors.{AuthConnector, BusinessRegistrationConnector}
import play.api.mvc.{Action, AnyContent}
import repositories.test.{TestOnlyMongo, TestOnlyRepository}
import uk.gov.hmrc.play.microservice.controller.BaseController
import uk.gov.hmrc.play.http.logging.MdcLoggingExecutionContext._

import scala.concurrent.Future
import scala.util.{Left, Right}

class TestSupportController @Inject()(val auth: AuthConnector,
                                      brConnector: BusinessRegistrationConnector,
                                      brTestConnector: BusinessRegistrationTestConnector,
                                      testMongo: TestOnlyMongo) extends BaseController with Authenticated {
  // $COVERAGE-OFF$

  def currentProfileSetup(): Action[AnyContent] = Action.async { implicit request =>
    authenticated { _ =>
      brConnector.retrieveCurrentProfile flatMap {
        case Left(common.exceptions.ResourceNotFound(_))  => brTestConnector.createCurrentProfileEntry()
        case Right(_)                                     => Future.successful(Ok)
        case Left(_)                                      => Future.successful(ServiceUnavailable)
      }
    }
  }

  def dropCollection(): Action[AnyContent] = Action.async { implicit request =>
    authenticated { _ =>
      testMongo.store.dropCollection map {
        _ => Ok("Collection Dropped")
      }
    }
  }

  // $COVERAGE-ON$
}
