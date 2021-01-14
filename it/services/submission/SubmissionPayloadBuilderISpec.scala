
package services.submission

import itutil.{IntegrationSpecBase, IntegrationStubbing}
import play.api.libs.json.Json
import play.api.test.Helpers._

import scala.concurrent.ExecutionContext.Implicits.global

class SubmissionPayloadBuilderISpec extends IntegrationSpecBase with IntegrationStubbing {

  val testService = app.injector.instanceOf[SubmissionPayloadBuilder]

  class Setup extends SetupHelper

  "buildSubmissionPayload" should {
    "create the correct json when the business partner is registered" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testMinimalVatSchemeWithRegisteredBusinessPartner, repo.insert)

      val res = await(testService.buildSubmissionPayload(testRegId))

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
          |      "turnoverNext12Months": "",
          |      "zeroRatedSupplies": 12.99,
          |      "VATRepaymentExpected": true
          |    },
          |    "schemes": {
          |      "FRSCategory": "",
          |      "FRSPercentage": "",
          |      "startDate": "",
          |      "limitedCostTrader": ""
          |    }
          |  },
          |  "periods": {
          |    "customerPreferredPeriodicity": "MA"
          |  }
          |}""".stripMargin)
    }
    "create the correct json when the business partner is unregistered" in new Setup {
      given
        .user.isAuthorised
        .regRepo.insertIntoDb(testFullVatSchemeWithUnregisteredBusinessPartner, repo.insert)

      val res = await(testService.buildSubmissionPayload(testRegId))

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
          |      "turnoverNext12Months": "",
          |      "zeroRatedSupplies": 12.99,
          |      "VATRepaymentExpected": true
          |    },
          |    "schemes": {
          |      "FRSCategory": "",
          |      "FRSPercentage": "",
          |      "startDate": "",
          |      "limitedCostTrader": ""
          |    }
          |  },
          |  "periods": {
          |    "customerPreferredPeriodicity": "MA"
          |  }
          |}""".stripMargin)
    }
  }

}
