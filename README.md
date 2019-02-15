## Initial Thoughts

An approach that I suppose won't be easily defeated in a wrestle with Antaeus would be:
1. Have a schedule that's triggered on the first of the month at a hour that then runs a long running service
   to charge invoices with PENDING status.
2. For each pending invoice, attempt a charge for the invoice with:

      Throw exceptions :-
      - Throw CustomerNotFoundException when no customer with id is found.
        + Escalate this to an admin who can manually rectify this but I've decided to mark the invoice as invalid.
      - Throw CurrencyMismatchException when the currencies from invoice and customers don't match.
        + Escalate this to an admin would be ideal for further investigation. However, I've decided to assign the currency 
        on the customer table to the invoice currency. 
      - Throw NetworkException when a network error occurs.
        + Add a retry mechanism for one more time before escalating to an agent.
      
      Return boolean :-
      
      - True if the account was successfully charged.
          + Update the invoice status as PAID 
      - False if the account balance of the customer is not up to to the invoice amount.
          + Leave invoice status as PENDING   
3.           
      
                
## Antaeus

Antaeus (/ænˈtiːəs/), in Greek mythology, a giant of Libya, the son of the sea god Poseidon and the Earth goddess Gaia. He compelled all strangers who were passing through the country to wrestle with him. Whenever Antaeus touched the Earth (his mother), his strength was renewed, so that even if thrown to the ground, he was invincible. Heracles, in combat with him, discovered the source of his strength and, lifting him up from Earth, crushed him to death.

Welcome to our challenge.

## The challenge

As most "Software as a Service" (SaaS) companies, Pleo needs to charge a subscription fee every month. Our database contains a few invoices in the different markets we operate in. Your task is to build the logic that will pay those invoices on the first of the month. While this may seem simple, there is space for some decisions to be taken and you will be expected to justify them.

### Structure
The code given is structured as follows. Feel free however to modify the structure to fit your needs.
```
├── pleo-antaeus-app
|
|       Packages containing the main() application. 
|       This is where all the dependencies are instantiated.
|
├── pleo-antaeus-core
|
|       This is where you will introduce most of your new code.
|       Pay attention to the PaymentProvider and BillingService class.
|
├── pleo-antaeus-data
|
|       Module interfacing with the database. Contains the models, mappings and access layer.
|
├── pleo-antaeus-models
|
|       Definition of models used throughout the application.
|
├── pleo-antaeus-rest
|
|        Entry point for REST API. This is where the routes are defined.
└──
```

## Instructions
Fork this repo with your solution. We want to see your progression through commits (don’t commit the entire solution in 1 step) and don't forget to create a README.md to explain your thought process.

Happy hacking 😁!

## How to run
```
./docker-start.sh
```

## Libraries currently in use
* [Exposed](https://github.com/JetBrains/Exposed) - DSL for type-safe SQL
* [Javalin](https://javalin.io/) - Simple web framework (for REST)
* [kotlin-logging](https://github.com/MicroUtils/kotlin-logging) - Simple logging framework for Kotlin
* [JUnit 5](https://junit.org/junit5/) - Testing framework
* [Mockk](https://mockk.io/) - Mocking library
