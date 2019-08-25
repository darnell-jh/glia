package com.dhenry.glia.data

import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import com.dhenry.glia.data.aggregate.BaseAbstractAggregateRoot
import kotlin.reflect.KClass

interface DataDomainEventsOperations {

  fun save(aggregateRoot: BaseAbstractAggregateRoot<*, *>)
  fun markDeleted(aggregateRoot: BaseAbstractAggregateRoot<*, *>)
  fun markRestored(aggregateRoot: BaseAbstractAggregateRoot<*, *>)

  fun <T : AbstractAggregateRoot<T, *>> createOrLoadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      initialAggregate: (() -> T)? = null,
      fromOldest: Boolean = false
  ): T

  fun <T : AbstractAggregateRoot<T, *>> loadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      onlyActive: Boolean = true,
      fromOldest: Boolean = false
  ): T?
}