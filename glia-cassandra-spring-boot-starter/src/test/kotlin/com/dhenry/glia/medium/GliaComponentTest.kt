package com.dhenry.glia.medium

import com.dhenry.glia.cassandra.domain.entities.AggregateEventState
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.data.models.EventState
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import java.util.*
import java.util.stream.Collectors
import kotlin.test.Test
import kotlin.test.assertTrue

class GliaComponentTest : BaseComponentTest() {

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

    // Act
    domainEventsService.save(testAggregate)

    // Assert
    assertDomainEventsAndState(1, 0L, testAggregate.aggregatePrimaryKey.aggregateId, true, 1, "routingKey", "1")
  }

  @Test
  fun multipleAggregateUpdateShouldHaveAllFieldsPopulatedAndSequenceIncremented() {
    // Arrange
    val testAggregate = TestAggregate()

    // Act
    testAggregate.update("1")
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("1")

    testAggregate.update("2")
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("2")

    testAggregate.update("3")
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("3")

    testAggregate.update("4")
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("4")

    testAggregate.update("5")
    domainEventsService.save(testAggregate)
    assertThat(testAggregate.property).isEqualTo("5")

    // Assert
    assertDomainEventsAndState(5, 4L, testAggregate.aggregatePrimaryKey.aggregateId, true, 5, "routingKey", "1")
  }

  private fun assertEvents(): ListAssert<AggregateEvent> {
    val events = cassandraTemplate.query(DomainEvents::class.java).all()
        .stream()
        .flatMap { it.events.stream() }
        .collect(Collectors.toList())

    return assertThat(events)
  }

  private fun assertDomainEventsAndState(domainEventsCount: Long, finalSequence: Long,
                                         aggregateId: String, active: Boolean, eventsCount: Long,
                                         routingKey: String, version: String) {
    val count = cassandraTemplate.count(DomainEvents::class.java)
    assertThat(count).isEqualTo(domainEventsCount)
    val domainEvents = cassandraTemplate.select("select * from domainevents", DomainEvents::class.java)
    var expectedSequence = finalSequence
    val eventIds = mutableListOf<UUID>()
    var actualEventsCount = 0
    for (domainEvent in domainEvents) {
      assertThat(domainEvent.aggregatePrimaryKey.aggregateId).isEqualTo(aggregateId)
      assertThat(domainEvent.active).isEqualTo(active)
      assertThat(domainEvent.aggregatePrimaryKey.sequence).isEqualTo(expectedSequence)
      assertThat(domainEvent.events).hasSize(1)

      val events = domainEvent.events
      events.forEach {
        assertThat(it.routingKey).isEqualTo(routingKey)
        assertThat(it.eventId).isNotNull()
        eventIds.add(it.eventId)
        assertThat(it.version).isEqualTo(version)
        actualEventsCount++
      }

      expectedSequence--
    }
    assertThat(actualEventsCount).isEqualTo(eventsCount)

    val eventStates = cassandraTemplate.select("select * from aggregateeventstate", AggregateEventState::class.java)
    for (eventState in eventStates) {
      assertThat(eventState.eventStatePrimaryKey.aggregateId).isEqualTo(aggregateId)
      assertTrue(eventIds.remove(eventState.eventStatePrimaryKey.eventId))
      assertThat(eventState.state).isEqualTo(EventState.SENT)
    }
    assertThat(eventIds).isEmpty()
  }

}