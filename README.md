# Glia Framework

This framework is designed to create event driven systems using Spring Boot and your choice of data storage and
messaging queues. This project is still a work in progress and currently only supports event driven architecture through
Cassandra and RabbitMQ. Below are some of the dependencies involved in getting your project started with the event
driven architecture.

## Dependencies:

### Event Stores

Below are the different dependencies needed to hook up a data storage system to be used as your event store.

* glia-cassandra-spring-boot-starter - Uses Cassandra

### Messaging Queues

These are your message queues for delivering event across your microservices

* glia-rabbit-spring-boot-starter - Uses RabbitMQ

### Spring Boot Starters

These are the minimum required dependencies to start using the Glia framework

* glia-spring-boot-starter - A spring boot starter dependency that hooks up your various event stores, message queues,
etc.

### Testing

To perform any type of testing with the Glia framework, these dependencies contain some valuable testing utilities for
ensuring quality in your microservices

* glia-spring-boot-starter-test

## Configuration Properties

Below are some properties you can use to configure different aspects of your microservices.

### Cassandra Properties
* spring.data.cassandra.cluster-name: Name of Cassandra cluster
* spring.data.cassandra.keyspace-name: Configures the keyspace used by Cassandra

* cassandra.contact-points: Configures the contact points for each Cassandra node in your cluster

* glia.consumer.enabled: Enables your microservice as a consumer. This will avoid setting up a domainevents entity.
  * *Note:* If enable-domain-events is set to true, this will create a domainevents entity
* glia.cassandra.entity-base-packages: Specifies packages where entities live
* glia.cassandra.replication.strategy: Specifies replication strategy you wish to use
* glia.cassandra.replication.replication-factor: Specifies replication factor
* glia.cassandra.replication.data-centers: Specifies the different data centers
* glia.cassandra.recreateKeyspace: Specifies whether the keyspace should be recreated
* glia.cassandra.enable-domain-events: Determines whether domain events entity should be created
  * *Note:* Creates domainevents entity even if your microservice is considered a consumer
* glia.cassandra.logging.enabled: Turns on query logging

### Rabbit Properties
* glia.rabbit.event-packages: Informs rabbit where your events can be found
* spring.rabbitmq.username: Rabbit username
* spring.rabbitmq.password: Rabbit password
* spring.rabbitmq.template.exchange: The exchange bus where events pass through

## Logging

Here are some important packages for logging problems in your application:

**Logging Queries**
* com.datastax.driver.core.QueryLogger.NORMAL: TRACE
* com.datastax.driver.core.QueryLogger.SLOW: TRACE
* com.datastax.driver.core.RequestHandler: TRACE

**Logging Glia**
* com.dhenry.glia: DEBUG