# osta-api-fastag-module

Fastag services microservice module.

## Coordinates

- Artifact: `com.dipcoin:osta-api-fastag-module:0.0.1-SNAPSHOT`
- Spring Boot parent: `2.2.4.RELEASE`
- Main class: `com.dipcoin.CoreApiApplication`

## Purpose

This module contains Fastag/toll-related services extracted from the monolith. It also uses bank, partner, API, DB, AMQP, TCP, metrics, and mock/test support packages needed for Fastag flows.

## Source Areas

- `com.dipcoin.api`: Fastag/toll API resources, models, config, utilities, fraud support.
- `com.dipcoin.partner`: partner/toll integration code.
- `com.dipcoin.db`: DB services, DAOs, models.
- `com.dipcoin.bank`: bank service/client/helper code.
- `com.dipcoin.amqp`: message/callback support.
- `com.dipcoin.tcp`: TCP-related integration support.
- `com.dipcoin.client`, `com.dipcoin.mock`, `com.dipcoin.metrics`: client, mock, and metrics support.

## Important Dependencies

- `com.dipcoin:osta-api-common:0.0.1-SNAPSHOT`
- `com.dipcoin.partner:partner-db-services`
- `com.dipcoin:dipcoin-core-services`
- `com.dipcoin:dipcoin-bank-services`

## Runtime Configuration

The main application:

- excludes `ErrorMvcAutoConfiguration` and `HibernateJpaAutoConfiguration`
- scans `com.dipcoin`
- imports bank/core, `dipcoin-db-services-fastag.xml`, partner DB context, partner services context, and API context
- loads application, bank, partner DB, Dipcoin DB, and API profile properties
- enables transaction management, AspectJ auto proxying, and caching

## Build Notes

Build required shared modules first when changed or missing locally:

```powershell
cd C:\WorkSpace\GitHub\osta-api-common
mvn clean install

cd C:\WorkSpace\GitHub\partner-db-services
mvn clean install

cd C:\WorkSpace\GitHub\osta-api-fastag-module
mvn clean install
```

If common and partner DB services have not changed and their correct JARs are already installed locally, rebuild only this module.

## Development Notes

- Fastag-specific API and toll logic belongs here.
- Partner DB model/DAO changes belong in `partner-db-services`; rebuild it before this module.
- Common classes scanned from `osta-api-common` can affect Fastag runtime bean creation.
