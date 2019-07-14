package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregateEventState
import com.dhenry.glia.cassandra.domain.models.EventState
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.util.Assert
import java.time.Instant
import java.time.temporal.ChronoUnit

class DomainEventsExtRepositoryImpl(
    private val cassandraTemplate: CassandraTemplate
) : DomainEventsExtRepository {

  override fun prependEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent) {
    Assert.notNull(event, "Event must not be null")

    val (aggregateId, sequence) = aggregateRoot.aggregatePrimaryKey
    val eventInCql = cassandraTemplate.converter.convertToColumnType(event)

    val microseconds = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now())
    val timestamp = QueryBuilder.timestamp(microseconds)

    val update = QueryBuilder.update(TBL_DOMAIN_EVENTS)
        .where(QueryBuilder.eq(DomainEvents.FIELD_AGGREGATE_ID, aggregateId))
        .and(QueryBuilder.eq(DomainEvents.FIELD_SEQUENCE, sequence))
        .with(QueryBuilder.prepend(DomainEvents.FIELD_EVENTS, eventInCql))
        .using(timestamp)
    cassandraTemplate.cqlOperations.execute(update)
  }

  override fun updateLastEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent,
                               eventState: EventState) {
    Assert.notNull(event, "Event must not be null")

    val (aggregateId, sequence) = aggregateRoot.aggregatePrimaryKey
    val aggregateEventState = AggregateEventState(event.eventId, eventState)
    val eventStateInCql = cassandraTemplate.converter.convertToColumnType(aggregateEventState)

    val microseconds = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now())
    val timestamp = QueryBuilder.timestamp(microseconds)

    val update = QueryBuilder.update(TBL_DOMAIN_EVENTS)
        .where(QueryBuilder.eq(DomainEvents.FIELD_AGGREGATE_ID, aggregateId))
        .and(QueryBuilder.eq(DomainEvents.FIELD_SEQUENCE, sequence))
        .with(QueryBuilder.append(DomainEvents.FIELD_EVENT_STATE, eventStateInCql))
        .using(timestamp)
    cassandraTemplate.cqlOperations.execute(update)
  }

}