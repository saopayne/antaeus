> :warning: This repository was archived automatically since no ownership was defined :warning:
>
> For details on how to claim stewardship of this repository see:
>
> [How to configure a service in OpsLevel](https://www.notion.so/pleo/How-to-configure-a-service-in-OpsLevel-f6483fcb4fdd4dcc9fc32b7dfe14c262)
>
> To learn more about the automatic process for stewardship which archived this repository see:
>
> [Automatic process for stewardship](https://www.notion.so/pleo/Automatic-process-for-stewardship-43d9def9bc9a4010aba27144ef31e0f2)

## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices for the different markets in which we operate. Your task is to build the logic that will schedule payment of those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

## Instructions

Fork this repo with your solution. Ideally, we'd like to see your progression through commits, and don't forget to update the README.md to explain your thought process.

Please let us know how long the challenge takes you. We're not looking for how speedy or lengthy you are. It's just really to give us a clearer idea of what you've produced in the time you decided to take. Feel free to go as big or as small as you want.

## Developing

Requirements:
- \>= Java 11 environment

Open the project using your favorite text editor. If you are using IntelliJ, you can open the `build.gradle.kts` file and it is gonna setup the project in the IDE for you.

### Building

```
./gradlew build
```

### Running

There are 2 options for running Anteus. You either need libsqlite3 or docker. Docker is easier but requires some docker knowledge. We do recommend docker though.

*Running Natively*

Native java with sqlite (requires libsqlite3):

If you use homebrew on MacOS `brew install sqlite`.

```
./gradlew run
```

*Running through docker*

Install docker for your platform

```
docker build -t antaeus
docker run antaeus
```

### App Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── buildSrc
|  | gradle build scripts and project wide dependency declarations
|  └ src/main/kotlin/utils.kt 
|      Dependencies
|
├── pleo-antaeus-app
|       main() & initialization
|
├── pleo-antaeus-core
|       This is probably where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|       Module interfacing with the database. Contains the database 
|       models, mappings and access layer.
|
├── pleo-antaeus-models
|       Definition of the Internal and API models used throughout the
|       application.
|
└── pleo-antaeus-rest
        Entry point for HTTP REST API. This is where the routes are defined.
```

### Main Libraries and dependencies
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
* [Sqlite3](https://sqlite.org/index.html) - Database storage engine

Happy hacking 😁!

### My Thought Process

The main objective: We have both customers and invoices, and we need to schedule payments such that each customer is billed for any 
invoices linked to them on the first of each month. There is a few approaches that can be taken here, but what I think I will do here
is basically filter out all PAID invoices, then group them all by customerId before preparing each invoice where neccesarry and billing
each customer with their invoices. I will handle the bulk of this logic in the BillingService class.

Additional objectives: Looking at the list of exceptions shows me that there are additional requirements  within the billing cycle
which have not been explicity stated but need to be addressed, mostly with extra logic in CustomerService and InvoiceService.
- CurrencyMismatchException: Both the invoice and customer have a currency field which we can guess need to be the same during billing
  otherwise this exception is thrown. I will need to have additional logic in BillingService to prepare invoices prior to billing which 
  updates the currency in the invoices to match the customers native currency. For the purposes of this challenge we will presume that
  all currencies have a 1:1 exchange rate.
- CustomerNotFoundException: Each invoice has an assigned customer, and during the filtering and grouping of invoices we may find one 
  where the customerId does not match any known customer record. This exception would need to trigger an alert of some kind, as having
  unpaid invoices which is unbillable is a big issue. For the purposes of this challenge I will trigger an "alert" to an imaginary 
  external service which we presume raises a ticket for resolution. I will also add a createCustomer function to the CustomerService
  just to provide a way to resolve the issue, assuming we have all the customers details to create a new record.
- InvoiceNotFoundException: This particular exception is unlikely to happen during the billing cycle, so we can assume where this might
  happen is during a lookup for a specific invoice which exists as a physical document but does not have a record yet. I will add a function
  to InvoiceService to create an invoice which will resolve this, and will use a unit test to demo the handling of the exception.

I will purposely leverage only existing entities and fields, avoiding the creation of new ones as I believe that this increases the 
comlexity of the project beyond the scope of the challenge. I do see places where expanding out fields within some entities would allow
greater control and flexibility within the billing process. Eg. InvoiceStatus having only 2 states, PENDING and PAID, would lead to too
much ambiguity in a real world solution.

I will be using rest endpoints for quickly testing and demoing some functionality on a complete data collection (rather than mocked data 
in unit tests), but this is really just for the purpose of the challenge and may not be something needing to be exposed through an api in reality.