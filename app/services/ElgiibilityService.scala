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

package services

import javax.inject.Inject

import models.api.Eligibility
import play.api.libs.json.{JsObject, JsResultException}
import repositories.{RegistrationMongo, RegistrationMongoRepository}
import utils.EligibilityDataJsonUtils

import scala.concurrent.{ExecutionContext, Future}

class EligibilityService @Inject()(val registrationMongo: RegistrationMongo) extends EligibilitySrv {
  val registrationRepository: RegistrationMongoRepository = registrationMongo.store
}

trait EligibilitySrv {

  val registrationRepository: RegistrationMongoRepository

  @deprecated("Use upsertEligibilityData instead", "SCRS-11579")
  def upsertEligibility(regId: String, eligibility: Eligibility)(implicit ex: ExecutionContext): Future[Eligibility] =
    registrationRepository.updateEligibility(regId, eligibility)

  @deprecated("Use getEligibilityData instead", "SCRS-11579")
  def getEligibility(regId: String)(implicit ex: ExecutionContext): Future[Option[Eligibility]] =
    registrationRepository.getEligibility(regId)

  def getEligibilityData(regId: String)(implicit ex: ExecutionContext): Future[Option[JsObject]] =
    registrationRepository.getEligibilityData(regId)

  def updateEligibilityData(regId: String, eligibilityData: JsObject)(implicit ex: ExecutionContext): Future[JsObject] = {
    eligibilityData.validate[JsObject](EligibilityDataJsonUtils.readsOfFullJson).fold(
      invalid => throw new JsResultException(invalid),
      _ => registrationRepository.updateEligibilityData(regId, eligibilityData)
    )
  }
}
