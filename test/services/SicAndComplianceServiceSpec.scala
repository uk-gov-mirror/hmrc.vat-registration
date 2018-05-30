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

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.SicAndCompliance
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.when
import org.mockito.stubbing.OngoingStubbing
import repositories.RegistrationMongoRepository
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SicAndComplianceServiceSpec extends VatRegSpec with VatRegistrationFixture  {

  class Setup {
    val service = new SicAndComplianceService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }
  def getFromMongo(res:Future[Option[SicAndCompliance]]): OngoingStubbing[Future[Option[SicAndCompliance]]] =
    when(mockRegistrationMongoRepository.getSicAndCompliance(any())(any()))
    .thenReturn(res)

  def updateMongo(res:Future[SicAndCompliance]): OngoingStubbing[Future[SicAndCompliance]] =
    when(mockRegistrationMongoRepository.updateSicAndCompliance(any(),any())(any()))
    .thenReturn(res)

  "getSicAndCompliance" should {
    "return a SicAndCompliance Model when an entry exists in mongo for the specified regId" in new Setup {
      getFromMongo(Future.successful(validSicAndCompliance))
      await(service.getSicAndCompliance("fooBarAndWizz")) shouldBe validSicAndCompliance
    }
    "return None when no entry exists in the dataBase for the specified regId" in new Setup {
      getFromMongo(Future.successful(None))
      await(service.getSicAndCompliance("bangBuzzFang")) shouldBe None
    }
  }
  "updateSiceAndCompliance" should {
    "return an updated SicAndCompliance Model when an update successfully takes place in mongo" in new Setup {
      updateMongo(Future.successful(validSicAndCompliance.get))
      await(service.updateSicAndCompliance("ImARegId",validSicAndCompliance.get)) shouldBe validSicAndCompliance.get
    }
    "return a missingRegDocument when no reg Document exists for the reg id when an update takes place" in new Setup {
      updateMongo(Future.failed(MissingRegDocument(RegistrationId("testId"))))
      intercept[MissingRegDocument](await(
        service.updateSicAndCompliance("testId",validSicAndCompliance.get))) shouldBe MissingRegDocument(RegistrationId("testId"))
    }
    "return new Exception when an exception is returned from the repo during an update" in new Setup {
      updateMongo(Future.failed(new Exception("foo Bar Wizz Bang")))
      intercept[Exception](await(service.updateSicAndCompliance("testId",validSicAndCompliance.get)))
    }
  }

}
