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

package services

import common.RegistrationId
import common.exceptions.MissingRegDocument
import fixtures.VatRegistrationFixture
import helpers.VatRegSpec
import models.api.BusinessContact
import org.mockito.stubbing.OngoingStubbing
import repositories.RegistrationMongoRepository
import org.mockito.ArgumentMatchers.any

import scala.concurrent.Future
import org.mockito.Mockito._
import scala.concurrent.ExecutionContext.Implicits.global

class BusinessContactServiceSpec extends VatRegSpec with VatRegistrationFixture {

  class Setup {
    val service = new BusinessContactService(
      registrationMongo = mockRegistrationMongo
    ) {
      override val registrationRepository: RegistrationMongoRepository = mockRegistrationMongoRepository
    }
  }

  def getFromMongo(res: Future[Option[BusinessContact]]): OngoingStubbing[Future[Option[BusinessContact]]] = when(mockRegistrationMongoRepository.getBusinessContact(any())(any()))
    .thenReturn(res)

  def updateMongo(res: Future[BusinessContact]): OngoingStubbing[Future[BusinessContact]] = when(mockRegistrationMongoRepository.updateBusinessContact(any(), any())(any()))
    .thenReturn(res)

  "getBusinessContact" should {
    "return a BusinessContact Model when an entry exists in mongo for the specified regId" in new Setup {
      getFromMongo(Future.successful(validBusinessContact))
      await(service.getBusinessContact("fooBarAndWizz")) shouldBe validBusinessContact
    }
    "return None when no entry exists in the dataBase for the specified regId" in new Setup {
      getFromMongo(Future.successful(None))
      await(service.getBusinessContact("bangBuzzFang")) shouldBe None
    }
  }
  "updateBusinessContact" should {
    "return an updated BusinessContact Model when an update successfully takes place in mongo" in new Setup {
      updateMongo(Future.successful(validBusinessContact.get))
      await(service.updateBusinessContact("ImARegId",validBusinessContact.get)) shouldBe validBusinessContact.get
    }
    "return a missingRegDocument when no reg Document exists for the reg id when an update takes place" in new Setup {
      updateMongo(Future.failed(MissingRegDocument(RegistrationId("testId"))))
      intercept[MissingRegDocument](await(service.updateBusinessContact("testId",validBusinessContact.get))) shouldBe MissingRegDocument(RegistrationId("testId"))
    }
    "return new Exception when an exception is returned from the repo during an update" in new Setup {
      updateMongo(Future.failed(new Exception("foo Bar Wizz Bang")))
      intercept[Exception](await(service.updateBusinessContact("testId",validBusinessContact.get)))
    }
  }
}
