# vat-registration

[![Build Status](https://travis-ci.org/hmrc/vat-registration.svg)](https://travis-ci.org/hmrc/vat-registration) [ ![Download](https://api.bintray.com/packages/hmrc/releases/vat-registration/images/download.svg) ](https://bintray.com/hmrc/releases/vat-registration/_latestVersion)

### License

This code is open source software licensed under the [Apache 2.0 License]("http://www.apache.org/licenses/LICENSE-2.0.html")

## Prior to committing
```
sbt clean coverage test scalastyle coverageReport
```

alternatively, create an alias for the above line, and get in the habit of running it before checking in:

```bash
alias precommit="sbt clean coverage test scalastyle coverageReport"
```

### NOTE: Only commit if test coverage report is above or equal to 95%, scalastyle warnings are corrected and tests green.

## Running locally

Ensure to run the following command to start the service managed services required locally:

```
sm --start SCRS_ALL -f
```

alternatively, create an alias for starting the services required for the VAT Registration (Backend)

```bash
alias scrs='sm --start AUTH AUTHENTICATOR GG GG_STUBS USER_DETAILS KEYSTORE DATASTREAM -f'
```

To run the service, just `cd` to cloned directory and execute:

```
sbt run
```

The service should come to life @ http://localhost:9896/
