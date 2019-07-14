package com.dhenry.glia.medium

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import java.util.stream.Collectors
import kotlin.test.Test

class GliaComponentTest : BaseComponentTest() {

  @Test
  fun shouldPersistEventToDatabaseOnAggregateUpdateAndSaveToRepository() {
    // Arrange
    val testAggregate = TestAggregate()
    val testEvent = TestEvent("1")
    val expectedEvent = generateAggregateEvent(event = testEvent)

    // Act
    testAggregate.update("1")
    domainEventsService.save(testAggregate)


    // Assert
    assertEvents()
        .usingElementComparatorOnFields("routingKey", "version", "payload", "state")
        .containsOnly(expectedEvent)
  }

  @Test
  fun shouldLoadSavedAggregate() {
    // Arrange
    val testAggregate = TestAggregate()
    testAggregate.update("1")
    domainEventsService.save(testAggregate)

    // Act
    val actualAggregate = domainEventsService.loadAggregate(testAggregate.id, TestAggregate::class)

    // Assert
    assertThat(actualAggregate).isEqualTo(testAggregate)
  }

  @Test
  fun aggregateShouldHaveAllFieldsPopulated() {
    // Arrange
    val testAggregate = TestAggregate()
    testAggregate.update("1")
    val testEvent = TestEvent("1")
    val expectedEvent = generateAggregateEvent(event = testEvent)

    // Act
    domainEventsService.save(testAggregate)

    // Assert
    val rs = cassandraTemplate.cqlOperations.queryForResultSet("select * from domainevents")
    assertThat(rs.count()).isEqualTo(1)
    for (row in rs) {
      assertThat(row["aggregateid", String::class.java]).isEqualTo(testAggregate.id)
      assertThat(row["active", Boolean::class.java]).isEqualTo(true)
      assertThat(row["sequence", Long::class.java]).isEqualTo(0L)
      val events = row["events", AggregateEvent::class.java]
      assertThat(events.routingKey).isEqualTo(expectedEvent.routingKey)
      assertThat(events.payload).isEqualTo(expectedEvent.payload)
      assertThat(events.eventId).isEqualTo(expectedEvent.eventId)
      //assertThat(events.state).isEqualTo(expectedEvent.state)
      assertThat(events.version).isEqualTo(expectedEvent.version)
    }
  }

  @Test
  fun multipleAggregateUpdateShouldHaveAllFieldsPopulatedAndSequenceIncremented() {
    // Arrange
    val testAggregate = TestAggregate()
    testAggregate.update("1")
    domainEventsService.save(testAggregate)
    val testEvent = TestEvent()
    val expectedEvent = generateAggregateEvent(event = testEvent)
    assertThat(testAggregate.property).isEqualTo("1")
    testAggregate.update("2")

    // Act
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("2")

    // Assert
    val count = cassandraTemplate.count(DomainEvents::class.java)
    assertThat(count).isEqualTo(2)
    val domainEvents = cassandraTemplate.select("select * from domainevents", DomainEvents::class.java)
    var expectedSequence = 1L
    for (domainEvent in domainEvents) {
      assertThat(domainEvent.aggregatePrimaryKey.aggregateId).isEqualTo(testAggregate.id)
      assertThat(domainEvent.active).isEqualTo(true)
      assertThat(domainEvent.aggregatePrimaryKey.sequence).isEqualTo(expectedSequence)
      assertThat(domainEvent.events).hasSize(1)

      val events = domainEvent.events
      assertThat(events).hasSize(1)
      events.forEach {
        val prop = expectedSequence + 1
        assertThat(it.routingKey).isEqualTo(expectedEvent.routingKey)
        assertThat(it.payload).isEqualTo(""" {"property":"$prop"} """.trim())
        assertThat(it.eventId).isNotNull()
        assertThat(it.version).isEqualTo(expectedEvent.version)
      }

      val eventStates = domainEvent.eventState
      assertThat(eventStates).hasSize(1)
      eventStates.forEach {
        assertThat(it.eventId).isNotNull()
        assertThat(it.state).isEqualTo(EventState.SENT)
      }

      expectedSequence--
    }
  }

  private fun assertEvents(): ListAssert<AggregateEvent> {
    val events = cassandraTemplate.query(DomainEvents::class.java).all()
        .stream()
        .flatMap { it.events.stream() }
        .collect(Collectors.toList())

    return assertThat(events)
  }

  private fun generateAggregateEvent(
      routingKey: String = "routingKey", version: String = "1",
      event: Any
  ) = AggregateEvent(routingKey, version, objectMapper.writeValueAsString(event))

}