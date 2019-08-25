package com.dhenry.glia.data.aggregate

import com.dhenry.glia.data.models.IAggregatePrimaryKey
import org.springframework.context.PayloadApplicationEvent
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.AfterDomainEventPublication
import org.springframework.data.domain.DomainEvents
import org.springframework.util.Assert
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.jvm.isAccessible

abstract class BaseAbstractAggregateRoot<A : BaseAbstractAggregateRoot<A, K>, K: IAggregatePrimaryKey>(
    @Transient open val aggregatePrimaryKey: K
) {

  @Transient
  protected var domainEvents = mutableListOf<Any>()

  @Transient
  protected var aggregateRoot: BaseAbstractAggregateRoot<*, *>? = null

  /**
   * Registers the given event object for publication on a call to a Spring Data repository's save methods.
   *
   * @param event must not be null.
   * @return the event that has been added.
   * @see .andEvent
   */
  protected fun <T> registerEvent(event: T): T {

    Assert.notNull(event, "Domain event must not be null!")

    // Update time uuid for each event registered
    domainEvents.add(PayloadApplicationEvent(this, event as Any))
    return event
  }

  /**
   * Clears all domain events currently held. Usually invoked by the infrastructure in place in Spring Data
   * repositories.
   */
  @AfterDomainEventPublication
  protected fun clearDomainEvents() {
    domainEvents.clear()
    if (aggregateRoot != null) {
      aggregateRoot!!::class.functions
          .filter { it.findAnnotation<AfterDomainEventPublication>() != null }
          .forEach {
            it.isAccessible = true
            it.call(aggregateRoot)
          }
    }
  }

  /**
   * All domain events currently captured by the aggregate.
   */
  @DomainEvents
  fun domainEvents(): Collection<Any> {
    return domainEvents.toList()
  }

  /**
   * Adds all events contained in the given aggregate to the current one.
   *
   * @param aggregate must not be null.
   * @return the aggregate
   */
  @Suppress("UNCHECKED_CAST")
  fun andEventsFrom(aggregate: BaseAbstractAggregateRoot<*, *>): A {

    Assert.notNull(aggregate, "Aggregate must not be null!")
    Assert.notNull(aggregate.domainEvents, "Domain events must exist")
    if (aggregate.domainEvents.isEmpty()) throw IllegalArgumentException("Domain events must not be empty")

    domainEvents.addAll(aggregate.domainEvents())

    return this as A
  }

  /**
   * Adds the given event to the aggregate for later publication when calling a Spring Data repository's save-method.
   * Does the same as [.registerEvent] but returns the aggregate instead of the event.
   *
   * @param event must not be null.
   * @return the aggregate
   * @see .registerEvent
   */
  @Suppress("UNCHECKED_CAST")
  protected fun andEvent(event: Any): A {

    registerEvent(event)

    return this as A
  }

}