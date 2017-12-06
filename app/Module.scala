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

import com.google.inject.name.Names
import com.google.inject.{AbstractModule, Provides}
import repositories.test.{TestOnlyMongoRepository, TestOnlyRepository}
import repositories.{MongoDBProvider, RegistrationMongoRepository, RegistrationRepository}

class Module extends AbstractModule {

  @Provides def mongoFunction: Function0[reactivemongo.api.DB] = new MongoDBProvider()

  override def configure(): Unit = {
    bind(classOf[RegistrationRepository]).to(classOf[RegistrationMongoRepository])
    bind(classOf[TestOnlyRepository]).to(classOf[TestOnlyMongoRepository])
    bind(classOf[connectors.AuthConnector]).to(classOf[connectors.VatRegAuthConnector])
    bind(classOf[connectors.BusinessRegistrationConnector]).to(classOf[connectors.VatRegBusinessRegistrationConnector])
    bind(classOf[connectors.IncorporationInformationConnector]).to(classOf[connectors.VatRegIncorporationInformationConnector ])
    bind(classOf[services.RegistrationService]).to(classOf[services.VatRegistrationService])
    bind(classOf[controllers.ProcessIncorporationsController]).to(classOf[controllers.ProcessIncorporationsControllerImp])
    bindConstant().annotatedWith(Names.named("collectionName")).to("registration-information")
  }
}
