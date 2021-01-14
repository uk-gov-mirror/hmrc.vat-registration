/*
 * Copyright 2021 HM Revenue & Customs
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

package services.submission

import models.api.Returns.writePeriod
import play.api.libs.json.{JsObject, JsValue, Json}
import repositories.RegistrationMongoRepository
import uk.gov.hmrc.http.InternalServerException

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PeriodsBlockBuilder @Inject()(registrationMongoRepository: RegistrationMongoRepository)
                                   (implicit ec: ExecutionContext) {

  def buildPeriodsBlock(regId: String): Future[JsObject] = {
    registrationMongoRepository.retrieveVatScheme(regId) map { scheme =>
      scheme.flatMap(_.returns) match {
        case Some(returns) =>
          writePeriod(returns.frequency, returns.staggerStart)
            .map(_ => Json.obj(
              "customerPreferredPeriodicity" -> writePeriod(returns.frequency, returns.staggerStart)
            ))
            .getOrElse(
              throw new InternalServerException("Couldn't build periods section due to either an invalid frequency or stagger start")
            )
        case None =>
          throw new InternalServerException("Couldn't build periods section due to missing returns section in vat scheme")
      }
    }
  }

}
