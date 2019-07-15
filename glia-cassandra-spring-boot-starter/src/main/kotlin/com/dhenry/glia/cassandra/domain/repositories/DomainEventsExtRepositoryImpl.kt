package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.querybuilder.QueryBuilder
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.AggregateEventState
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.models.EventStatePrimaryKey
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.util.Assert

class DomainEventsExtRepositoryImpl(
    private val cassandraTemplate: CassandraTemplate,
    private val aggregateEventStateRepository: AggregateEventStateRepository
) : DomainEventsExtRepository {

  override fun prependEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent) {
    Assert.notNull(aggregateRoot, "Aggregate must not be null")
    Assert.notNull(event, "Event must not be null")

    val (aggregateId, sequence) = aggregateRoot.aggregatePrimaryKey
    val eventInCql = cassandraTemplate.converter.convertToColumnType(event)

    val update = QueryBuilder.update(TBL_DOMAIN_EVENTS)
        .where(QueryBuilder.eq(DomainEvents.FIELD_AGGREGATE_ID, aggregateId))
        .and(QueryBuilder.eq(DomainEvents.FIELD_SEQUENCE, sequence))
        .with(QueryBuilder.prepend(DomainEvents.FIELD_EVENTS, eventInCql))
    cassandraTemplate.cqlOperations.execute(update)
  }

  override fun updateEventState(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent,
                                eventState: EventState) {
    Assert.notNull(aggregateRoot, "Aggregate must not be null")
    Assert.notNull(event, "Event must not be null")
    Assert.notNull(eventState, "Event state must not be null")

    aggregateEventStateRepository.save(AggregateEventState(
        EventStatePrimaryKey(aggregateRoot.aggregatePrimaryKey.aggregateId, event.eventId),
        eventState
    ))
  }

}