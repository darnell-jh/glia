package com.dhenry.glia.data

import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import com.dhenry.glia.data.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.data.models.EventState
import java.util.stream.Stream

/**
 * Performs operations against a DomainEvents collection
 */
interface DomainEventsOperations {

  fun prependEvent(aggregateRoot: AbstractAggregateRoot<*, *>, event: Any)
  fun updateEventState(aggregateRoot: AbstractAggregateRoot<*, *>, eventId: String, eventState: EventState)
  fun insert(entity: BaseAbstractAggregateRoot<*, *>): BaseAbstractAggregateRoot<*, *>
  fun insert(entities: MutableIterable<BaseAbstractAggregateRoot<*, *>>): MutableList<BaseAbstractAggregateRoot<*, *>>
  fun save(entity: BaseAbstractAggregateRoot<*, *>): BaseAbstractAggregateRoot<*, *>
  fun saveAll(entities: MutableIterable<BaseAbstractAggregateRoot<*, *>>): MutableList<BaseAbstractAggregateRoot<*, *>>
  fun streamById(aggregateId: String): Stream<BaseAbstractAggregateRoot<*, *>>
  fun streamByIdFromOldest(aggregateId: String): Stream<BaseAbstractAggregateRoot<*, *>>
  fun countById(aggregateId: String): Long
  fun findOneById(aggregateId: String, sequence: Long): BaseAbstractAggregateRoot<*, *>

}