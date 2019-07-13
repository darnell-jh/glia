package com.dhenry.glia.cassandra.domain.helpers

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.createInstance

abstract class AbstractAggregateProvider {

  inline fun <reified T: AbstractAggregateRoot<T>> createAggregate(
      aggregateId: String? = null,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    return createAggregate(aggregateId, T::class, initialCreator, afterCreator)
  }

  @PublishedApi
  internal fun <T : AbstractAggregateRoot<T>> createAggregate(
      aggregateId: String? = null,
      clazz: KClass<T>,
      initialCreator: (() -> T)? = null,
      afterCreator: ((T) -> Unit)? = null
  ): T {
    val aggregate = initialCreator?.invoke() ?: clazz.createInstance()
    Optional.ofNullable(aggregateId).ifPresent { aggregate.aggregatePrimaryKey.aggregateId = it }
    afterCreator?.invoke(aggregate)
    return aggregate
  }

  inline fun <reified T : AbstractAggregateRoot<T>> createOrLoadAggregate(
      aggregateId: String,
      noinline initialCreator: (() -> T)? = null,
      noinline afterCreator: ((T) -> Unit)? = null
  ): T {
    return loadAggregate(aggregateId, T::class).orElseGet {
      createAggregate(aggregateId, T::class, initialCreator, afterCreator)
    }
  }

  abstract fun <T : AbstractAggregateRoot<T>> loadAggregate(
      aggregateId: String, clazz: KClass<T>, onlyActive: Boolean = true
  ): Optional<T>
}