package com.dhenry.glia.utils

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.helpers.AbstractAggregateProvider
import com.dhenry.glia.service.DomainEventsService
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.KClass

/**
 * Provides abilities to create and/or load aggregates
 */
@Component
class AggregateProvider constructor(val domainEventsService: DomainEventsService): AbstractAggregateProvider() {

  final inline fun <reified T: AbstractAggregateRoot<T>> loadAggregate(
      aggregateId: String, onlyActive: Boolean = true
  ): Optional<T> {
    return loadAggregate(aggregateId, T::class, onlyActive)
  }

  final override fun <T: AbstractAggregateRoot<T>> loadAggregate(
      aggregateId: String, clazz: KClass<T>, onlyActive: Boolean
  ): Optional<T> {
    return Optional.ofNullable(domainEventsService.loadAggregate(
        aggregateId = aggregateId,
        clazz = clazz,
        onlyActive = onlyActive
    ))
  }
}