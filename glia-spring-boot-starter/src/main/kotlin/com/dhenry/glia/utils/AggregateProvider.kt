package com.dhenry.glia.utils

import com.dhenry.glia.data.DataDomainEventsOperations
import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

/**
 * Provides abilities to create and/or load aggregates
 */
@Component
class AggregateProvider constructor(val domainEventsOperations: DataDomainEventsOperations) {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(AggregateProvider::class.java)
  }

  final inline fun <reified T: AbstractAggregateRoot<T, *>> loadAggregate(
      aggregateId: String, onlyActive: Boolean = true
  ): Optional<T> {
    return loadAggregate(aggregateId, T::class, onlyActive)
  }

  final fun <T: AbstractAggregateRoot<T, *>> loadAggregate(
      aggregateId: String, clazz: KClass<T>, onlyActive: Boolean = true
  ): Optional<T> {
    LOGGER.debug("Loading aggregate for $aggregateId")
    val result = Optional.ofNullable(domainEventsOperations.loadAggregate(
        aggregateId = aggregateId,
        clazz = clazz,
        onlyActive = onlyActive
    ))
    result.ifPresent{ LOGGER.debug("Successfully loaded aggregate. Result: {}", it) }
    return result
  }

  final inline fun <reified T: AbstractAggregateRoot<T, *>> createAggregate(
      aggregateId: String? = null,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    return createAggregate(aggregateId, T::class, initialCreator, afterCreator)
  }

  @PublishedApi
  internal fun <T : AbstractAggregateRoot<*, *>> createAggregate(
      aggregateId: String? = null,
      clazz: KClass<T>,
      initialCreator: (() -> T)? = null,
      afterCreator: ((T) -> Unit)? = null
  ): T {
    LOGGER.debug("Creating aggregate for $aggregateId")
    val aggregate = initialCreator?.invoke() ?: clazz.createInstance()
    Optional.ofNullable(aggregateId).ifPresent { aggregate.aggregatePrimaryKey.aggregateId = it }
    afterCreator?.invoke(aggregate)
    return aggregate
  }

  final inline fun <reified T : AbstractAggregateRoot<T, *>> createOrLoadAggregate(
      aggregateId: String,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    return loadAggregate(aggregateId, T::class).orElseGet {
      createAggregate(aggregateId, T::class, initialCreator, afterCreator)
    }
  }
}