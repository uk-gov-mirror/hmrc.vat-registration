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

package fixtures

import play.api.libs.json.{JsValue, Json}

trait VatSubmissionFixture {

  val vatSubmissionJson = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "admin": {
      |    "additionalInformation": {
      |      "customerStatus": "2"
      |    },
      |    "attachments": {
      |      "EORIRequested": true
      |    }
      |  },
      |  "customerIdentification": {
      |    "tradingName": "trading-name",
      |    "tradersPartyType": "50",
      |    "shortOrgName": "testCompanyName",
      |    "name": {
      |      "firstName": "Forename",
      |      "lastName": "Surname"
      |    },
      |    "customerID": [
      |      {
      |        "idValue": "testCtUtr",
      |        "idType": " UTR",
      |        "IDsVerificationStatus": "1"
      |      },
      |      {
      |        "idValue": "testCrn",
      |        "idType": "CRN",
      |        "IDsVerificationStatus": "1",
      |        "date": "2020-01-02"
      |      }
      |    ],
      |    "dateOfBirth": "2018-01-01",
      |    "primeBPSafeID": "testBpSafeId"
      |  },
      |  "declaration": {
      |    "declarationSigning": {
      |      "confirmInformationDeclaration": true
      |    },
      |    "applicantDetails": {
      |      "commDetails": {
      |        "email": "skylake@vilikariet.com"
      |      },
      |      "name": {
      |        "firstName": "Forename",
      |        "lastName": "Surname"
      |      },
      |      "dateOfBirth": "2018-01-01",
      |      "roleInBusiness": "secretary",
      |      "identifiers": [
      |        {
      |          "idValue": "AB123456A",
      |          "idType": "NINO",
      |          "IDsVerificationStatus": "1",
      |          "date": "2018-01-01"
      |        }
      |      ],
      |      "prevName": {
      |        "firstName": "Forename",
      |        "lastName": "Surname",
      |        "nameChangeDate": "2018-01-01"
      |      },
      |      "currAddress": {
      |        "line1": "line1",
      |        "line2": "line2",
      |        "postCode": "XX XX",
      |        "countryCode": "UK"
      |      }
      |    }
      |  },
      |  "subscription": {
      |    "corporateBodyRegistered": {
      |        "companyRegistrationNumber": "testCrn",
      |        "dateOfIncorporation": "2020-01-02",
      |        "countryOfIncorporation": "GB"
      |    },
      |    "reasonForSubscription": {
      |        "exemptionOrException": "0",
      |        "registrationReason": "0016",
      |        "relevantDate": "2020-10-07"
      |    },
      |    "schemes": {
      |        "FRSCategory": "testCategory",
      |        "FRSPercentage": 15,
      |        "startDate": "2018-01-01"
      |    },
      |    "yourTurnover": {
      |        "turnoverNext12Months": 123456,
      |        "zeroRatedSupplies": 12.99
      |    }
      |  },
      |  "bankDetails": {
      |    "UK": {
      |      "accountName": "Test Bank Account",
      |      "accountNumber": "01023456",
      |      "sortCode": "010203"
      |    }
      |  },
      |  "businessActivities": {
      |    "description": "this is my business description",
      |    "SICCodes": {
      |      "mainCode2": "00998",
      |      "primaryMainCode": "12345",
      |      "mainCode3": "00889"
      |    }
      |  },
      |  "contact": {
      |    "commDetails": {
      |      "webAddress": "www.foo.com",
      |      "mobileNumber": "54321",
      |      "telephone": "12345",
      |      "email": "email@email.com",
      |      "commsPreference": "ZEL"
      |    },
      |    "address": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "countryCode": "foo"
      |    }
      |  }
      |}""".stripMargin)

  val mongoJson: JsValue = Json.parse(
    """
      |{
      |  "messageType": "SubmissionCreate",
      |  "tradersPartyType": "50",
      |  "confirmInformationDeclaration": true,
      |  "companyRegistrationNumber": "testCrn",
      |  "applicantDetails": {
      |    "nino": "AB123456A",
      |    "role": "secretary",
      |    "name": {
      |      "first": "Forename",
      |      "last": "Surname"
      |    },
      |    "dateOfBirth": "2018-01-01",
      |    "companyName": "testCompanyName",
      |    "companyNumber": "testCrn",
      |    "dateOfIncorporation": "2020-01-02",
      |    "ctutr": "testCtUtr",
      |    "businessVerification": "PASS",
      |    "bpSafeId": "testBpSafeId",
      |    "currentAddress": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "postcode": "XX XX",
      |      "country": "UK"
      |    },
      |    "contact": {
      |      "email": "skylake@vilikariet.com"
      |    },
      |    "changeOfName": {
      |      "name": {
      |        "first": "Forename",
      |        "last": "Surname"
      |      },
      |      "change": "2018-01-01"
      |    },
      |    "countryOfIncorporation": "GB"
      |  },
      |  "bankDetails": {
      |    "isProvided": true,
      |    "details": {
      |      "name": "Test Bank Account",
      |      "sortCode": "010203",
      |      "number": "01023456"
      |    }
      |  },
      |  "sicAndCompliance": {
      |    "businessDescription": "this is my business description",
      |    "labourCompliance": {
      |      "numberOfWorkers": 1000,
      |      "temporaryContracts": true,
      |      "skilledWorkers": true
      |    },
      |    "mainBusinessActivity": {
      |      "code": "12345",
      |      "desc": "the flu",
      |      "indexes": "sic details"
      |    },
      |    "otherBusinessActivities": [
      |      {
      |        "code": "00998",
      |        "desc": "otherBusiness desc 1",
      |        "indexes": "fooBar 1"
      |      },
      |      {
      |        "code": "00889",
      |        "desc": "otherBusiness desc 2",
      |        "indexes": "fooBar 2"
      |      }
      |    ]
      |  },
      |  "businessContact": {
      |    "digitalContact": {
      |      "email": "email@email.com",
      |      "telephone": "12345",
      |      "mobileNumber": "54321"
      |    },
      |    "website": "www.foo.com",
      |    "ppob": {
      |      "line1": "line1",
      |      "line2": "line2",
      |      "country": "foo"
      |    },
      |    "contactPreference": "Email"
      |  },
      |  "tradingDetails": {
      |    "tradingName": "trading-name",
      |    "eoriRequested": true
      |  },
      |  "flatRateScheme": {
      |    "businessGoods": {
      |      "estimatedTotalSales": 1234567891011,
      |      "overTurnover": true
      |    },
      |    "startDate": "2018-01-01",
      |    "categoryOfBusiness": "testCategory",
      |    "percent": 15
      |  },
      |  "eligibilitySubmissionData": {
      |    "threshold": {
      |      "mandatoryRegistration": true,
      |      "thresholdPreviousThirtyDays": "2020-10-07",
      |      "thresholdInTwelveMonths": "2020-10-07",
      |      "thresholdNextThirtyDays": "2020-10-07"
      |    },
      |    "exceptionOrExemption": "0",
      |    "estimates": {
      |      "turnoverEstimate": 123456
      |    },
      |    "customerStatus": "2"
      |  },
      |  "zeroRatedSupplies": 12.99
      |}""".stripMargin)

}
