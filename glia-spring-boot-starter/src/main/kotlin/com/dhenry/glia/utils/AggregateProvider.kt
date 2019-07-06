package com.dhenry.glia.utils

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.config.DomainEventsService
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.full.createInstance

/**
 * Provides abilities to create and/or load aggregates
 */
@Component
class AggregateProvider constructor(val domainEventsService: DomainEventsService) {

  final inline fun <reified T: AbstractAggregateRoot<T>> createAggregate(
      aggregateId: String? = null,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    val aggregate = initialCreator?.invoke() ?: T::class.createInstance()
    Optional.ofNullable(aggregateId).ifPresent { aggregate.aggregatePrimaryKey.aggregateId = it }
    afterCreator?.invoke(aggregate)
    return aggregate
  }

  final inline fun <reified T: AbstractAggregateRoot<T>> createOrLoadAggregate(
      aggregateId: String,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    return loadAggregate<T>(aggregateId).orElseGet { createAggregate(aggregateId, initialCreator, afterCreator) }
  }

  final inline fun <reified T: AbstractAggregateRoot<T>> loadAggregate(
      aggregateId: String, onlyActive: Boolean = true
  ): Optional<T> {
    return Optional.ofNullable(domainEventsService.loadAggregate(
        aggregateId = aggregateId,
        clazz = T::class,
        onlyActive = onlyActive
    ))
  }
}