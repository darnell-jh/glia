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
    val testEvent = TestEvent()
    val expectedEvent = generateAggregateEvent(event = testEvent, state = EventState.SENT)

    // Act
    testAggregate.update()
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
    testAggregate.update()
    domainEventsService.save(testAggregate)

    // Act
    val actualAggregate = domainEventsService.loadAggregate(testAggregate.id, TestAggregate::class)

    // Assert
    assertThat(actualAggregate).isEqualTo(testAggregate)
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
      event: Any, state: EventState
  ) = AggregateEvent(routingKey, version, objectMapper.writeValueAsString(event), state = state)

}