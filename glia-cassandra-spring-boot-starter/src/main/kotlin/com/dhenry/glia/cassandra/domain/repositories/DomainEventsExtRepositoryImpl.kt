package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.core.ExecutableUpdateOperation
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.query.isEqualTo
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.update

class DomainEventsExtRepositoryImpl(val cassandraTemplate: CassandraTemplate): DomainEventsExtRepository {

  override fun prependEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent) {
    val (aggregateId, timeUUID) = aggregateRoot.aggregateId

    val update = Update.empty().addTo("events").prepend(event)
    (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
        .matching(Query.query(
            where("aggregateid").isEqualTo(aggregateId),
            where("timeuuid").isEqualTo(timeUUID)
        ))
        .apply(update)
  }

  override fun updateLastEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent) {
    val (aggregateId, timeUUID) = aggregateRoot.aggregateId

    val update = Update.empty().set("events").atIndex(0).to(event)
    (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
        .matching(Query.query(
            where("aggregateid").isEqualTo(aggregateId),
            where("timeuuid").isEqualTo(timeUUID)
        ))
        .apply(update)
  }

}