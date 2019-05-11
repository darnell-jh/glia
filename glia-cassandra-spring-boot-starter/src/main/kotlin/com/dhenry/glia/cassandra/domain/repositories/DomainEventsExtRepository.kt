package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent

interface DomainEventsExtRepository {

  /**
   * Prepends event to the aggregate
   * @param aggregateRoot Aggregate root to update
   * @param event Event that will be updated in aggregate
   */
  fun prependEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent)

  /**
   * Updates last event in aggregate
   * @param aggregateRoot Aggregate root to update
   * @param event Event that will be updated in aggregate
   */
  fun updateLastEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent)

}