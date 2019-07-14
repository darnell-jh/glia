package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState

interface DomainEventsExtRepository {

  /**
   * Prepends event to the aggregate
   * @param aggregateRoot Aggregate root to update
   * @param event Event that will be updated in aggregate
   */
  fun prependEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent)

  /**
   * Updates last event in aggregate
   * @param aggregateRoot Aggregate root to update
   * @param event Event that will be updated in aggregate
   */
  fun updateLastEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent, eventState: EventState)

}