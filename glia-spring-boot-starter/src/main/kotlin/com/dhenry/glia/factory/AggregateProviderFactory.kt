package com.dhenry.glia.factory

import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.config.DomainEventsService
import org.springframework.stereotype.Component
import java.util.function.Consumer
import kotlin.reflect.KClass
import kotlin.reflect.jvm.reflect

@Component
class AggregateProviderFactory(private val domainEventsService: DomainEventsService) {

  fun <T: AbstractAggregateRoot<T>> from(creator: Function<T>, vararg args: Any): AggregateProvider<T> {
    return AggregateProvider(creator, domainEventsService, args)
  }

  class AggregateProvider<T: AbstractAggregateRoot<T>> constructor(private val creator: Function<T>,
                                         private val domainEventsService: DomainEventsService,
                                         private vararg val args: Any) {

    private var afterCreator: Consumer<T>? = null

    fun withAfterCreation(consumer: Consumer<T>): AggregateProvider<T> {
      afterCreator = consumer
      return this
    }

    fun createAggregate(): T {
      val aggregate = creator.reflect()!!.call(args)
      afterCreator?.accept(aggregate)
      return aggregate
    }

    fun createOrLoadAggregate(aggregateId: String, clazz: KClass<T>): T {
      return loadAggregate(aggregateId, clazz) ?: createAggregate()
    }

    fun loadAggregate(aggregateId: String, clazz: KClass<T>, onlyActive: Boolean = true): T? {
      return domainEventsService.loadAggregate(
          aggregateId = aggregateId,
          clazz = clazz,
          onlyActive = onlyActive
      )
    }
  }
}