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

import itutil.{IntegrationSpecBase, IntegrationStubbing}
import play.api.libs.json.{JsObject, Json}
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionPayloadBuilderISpec extends IntegrationSpecBase with IntegrationStubbing {

  val testService: SubmissionPayloadBuilder = app.injector.instanceOf[SubmissionPayloadBuilder]

  class Setup extends SetupHelper

  "buildSubmissionPayload" should {
    "create the correct json when the business partner is registered" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner, repo.insert)

      val res: JsObject = await(testService.buildSubmissionPayload(testRegId))

      res mustBe Json.parse(
        """
          |{
          |  "admin": {
          |    "additionalInformation": {
          |      "customerStatus": "2"
          |    },
          |    "attachments": {
          |      "EORIrequested": true
          |    }
          |  },
          |  "declaration": {
          |    "declarationSigning": {
          |      "confirmInformationDeclaration": true,
          |      "declarationCapacity": "03"
          |    },
          |    "applicantDetails": {
          |      "roleInBusiness": "03",
          |      "name": {
          |        "firstName": "Forename",
          |        "lastName": "Surname"
          |      },
          |      "dateOfBirth": "2017-01-01",
          |      "currAddress": {
          |        "line1": "line1",
          |        "line2": "line2",
          |        "postCode": "XX XX",
          |        "countryCode": "GB",
          |        "addressValidated": true
          |      },
          |      "commDetails": {
          |        "email": "skylake@vilikariet.com"
          |      },
          |      "identifiers": [
          |        {
          |          "idValue": "NB686868C",
          |          "idType": "NINO",
          |          "IDsVerificationStatus": "1",
          |          "date": "2017-01-01"
          |        }
          |      ]
          |    }
          |  },
          |  "customerIdentification": {
          |    "tradersPartyType": "50",
          |    "shortOrgName": "testCompanyName",
          |    "tradingName": "test-name",
          |    "primeBPSafeID": "testBpSafeId"
          |  },
          |  "contact": {
          |    "address": {
          |      "line1": "line1",
          |      "line2": "line2",
          |      "postCode": "XX XX",
          |      "countryCode": "GB",
          |      "addressValidated": true
          |    },
          |    "commDetails": {
          |      "telephone": "12345678910",
          |      "mobileNumber": "12345678910",
          |      "email": "test@test.com",
          |      "commsPreference": "ZEL"
          |    }
          |  },
          |  "subscription": {
          |    "reasonForSubscription": {
          |      "registrationReason": "0016",
          |      "relevantDate": "2017-01-01",
          |      "voluntaryOrEarlierDate": "2017-01-01",
          |      "exemptionOrException": "0"
          |    },
          |    "corporateBodyRegistered": {
          |      "companyRegistrationNumber": "testCrn",
          |      "dateOfIncorporation": "2020-01-02",
          |      "countryOfIncorporation": "GB"
          |    },
          |    "businessActivities": {
          |      "description": "businessDesc",
          |      "SICCodes": {
          |        "primaryMainCode": "12345"
          |      }
          |    },
          |    "yourTurnover": {
          |      "turnoverNext12Months": 123456,
          |      "zeroRatedSupplies": 12.99,
          |      "VATRepaymentExpected": true
          |    }
          |  },
          |  "periods": {
          |    "customerPreferredPeriodicity": "MA"
          |  },
          |  "bankDetails": {
          |    "UK": {
          |      "reasonBankAccNotProvided": "BeingSetup"
          |    }
          |  },
          |  "compliance": {
          |    "numOfWorkersSupplied": 1,
          |    "intermediaryArrangement": true,
          |    "supplyWorkers": true
          |  }
          |}""".stripMargin)
    }
    "create the correct json when the business partner is unregistered" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      val res: JsObject = await(testService.buildSubmissionPayload(testRegId))

      res mustBe Json.parse(
        """
          |{
          |  "admin": {
          |    "additionalInformation": {
          |      "customerStatus": "2"
          |    },
          |    "attachments": {
          |      "EORIrequested": true
          |    }
          |  },
          |  "declaration": {
          |    "declarationSigning": {
          |      "confirmInformationDeclaration": true,
          |      "declarationCapacity": "03"
          |    },
          |    "applicantDetails": {
          |      "roleInBusiness": "03",
          |      "name": {
          |        "firstName": "Forename",
          |        "lastName": "Surname"
          |      },
          |      "prevName": {
          |        "firstName": "Bob",
          |        "lastName": "Smith",
          |        "nameChangeDate": "2017-01-01"
          |      },
          |      "dateOfBirth": "2017-01-01",
          |      "currAddress": {
          |        "line1": "line1",
          |        "line2": "line2",
          |        "postCode": "XX XX",
          |        "countryCode": "GB",
          |        "addressValidated": true
          |      },
          |      "commDetails": {
          |        "email": "skylake@vilikariet.com"
          |      },
          |      "identifiers": [
          |        {
          |          "idValue": "NB686868C",
          |          "idType": "NINO",
          |          "IDsVerificationStatus": "1",
          |          "date": "2017-01-01"
          |        }
          |      ]
          |    }
          |  },
          |  "customerIdentification": {
          |    "tradersPartyType": "50",
          |    "shortOrgName": "testCompanyName",
          |    "tradingName": "test-name",
          |    "customerID": [
          |      {
          |        "idType": "UTR",
          |        "IDsVerificationStatus": "3",
          |        "idValue": "testCtUtr"
          |      },
          |      {
          |        "idType": "CRN",
          |        "idValue": "testCrn",
          |        "date": "2020-01-02",
          |        "IDsVerificationStatus": "3"
          |      }
          |    ]
          |  },
          |  "contact": {
          |    "address": {
          |      "line1": "line1",
          |      "line2": "line2",
          |      "postCode": "XX XX",
          |      "countryCode": "GB",
          |      "addressValidated": true
          |    },
          |    "commDetails": {
          |      "telephone": "12345678910",
          |      "mobileNumber": "12345678910",
          |      "email": "test@test.com",
          |      "commsPreference": "ZEL"
          |    }
          |  },
          |  "subscription": {
          |    "reasonForSubscription": {
          |      "registrationReason": "0016",
          |      "relevantDate": "2017-01-01",
          |      "voluntaryOrEarlierDate": "2017-01-01",
          |      "exemptionOrException": "0"
          |    },
          |    "corporateBodyRegistered": {
          |      "companyRegistrationNumber": "testCrn",
          |      "dateOfIncorporation": "2020-01-02",
          |      "countryOfIncorporation": "GB"
          |    },
          |    "businessActivities": {
          |      "description": "businessDesc",
          |      "SICCodes": {
          |        "primaryMainCode": "12345"
          |      }
          |    },
          |    "yourTurnover": {
          |      "turnoverNext12Months": 123456,
          |      "zeroRatedSupplies": 12.99,
          |      "VATRepaymentExpected": true
          |    },
          |    "schemes": {
          |      "FRSCategory": "testCategory",
          |      "FRSPercentage": 15,
          |      "startDate": "2017-01-01",
          |      "limitedCostTrader": false
          |    }
          |  },
          |  "periods": {
          |    "customerPreferredPeriodicity": "MA"
          |  },
          |  "bankDetails": {
          |    "UK": {
          |      "accountName": "testBankName",
          |       "sortCode": "111111",
          |       "accountNumber": "01234567"
          |     }
          |  },
          |  "compliance": {
          |    "numOfWorkersSupplied": 1,
          |    "intermediaryArrangement": true,
          |    "supplyWorkers": true
          |  }
          |}""".stripMargin)
    }
  }

}
