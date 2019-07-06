package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.core.ExecutableUpdateOperation
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.query.isEqualTo
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.update
import org.springframework.util.Assert

class DomainEventsExtRepositoryImpl(
    private val cassandraTemplate: CassandraTemplate
) : DomainEventsExtRepository {

  override fun prependEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent): WriteResult {
    Assert.notNull(event, "Event must not be null")

    val (aggregateId, timeUUID) = aggregateRoot.aggregatePrimaryKey

    val update = Update.empty().addTo("events").prepend(event)
    return (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
        .matching(Query.query(
            where("aggregateid").isEqualTo(aggregateId),
            where("timeuuid").isEqualTo(timeUUID)
        ))
        .apply(update)
  }

  override fun updateLastEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent): WriteResult {
    Assert.notNull(event, "Event must not be null")

    val (aggregateId, timeUUID) = aggregateRoot.aggregatePrimaryKey

    val update = Update.empty().set("events").atIndex(0).to(event)
    return (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
        .matching(Query.query(
            where("aggregateid").isEqualTo(aggregateId),
            where("timeuuid").isEqualTo(timeUUID)
        ))
        .apply(update)
  }

}