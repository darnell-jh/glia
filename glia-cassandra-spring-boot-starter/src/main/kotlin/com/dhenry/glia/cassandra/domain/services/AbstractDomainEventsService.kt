package com.dhenry.glia.cassandra.domain.services

import com.dhenry.glia.annotations.AggregateId
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.EventListener
import java.util.concurrent.atomic.AtomicInteger
import java.util.stream.Stream
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.javaType

/**
 * Service responsible for managing functionality in the domain events collection
 */
abstract class AbstractDomainEventsService(
    protected open val domainEventsRepository: DomainEventsRepository,
    protected open val objectMapper: ObjectMapper
) {

  /**
   * Updates the aggregate with payload
   * @param aggregate Aggregate root to update
   * @param payload Data that aggregate will update based on
   * @param eventAnnotation Event annotation to capture version and routing key
   * @return A pair of the aggregate that was updated and the event that was emitted as a result of the payload
   */
  private fun <T : AbstractAggregateRoot<T>> updateAggregate(
      aggregate: T, payload: Any, eventAnnotation: Event): Pair<AbstractAggregateRoot<*>, AggregateEvent> {
    // Create a copy of the aggregate and insert into the repository
    val payloadJson = objectMapper.writeValueAsString(payload)
    val aggregateEvent = AggregateEvent(eventAnnotation.routingKey, eventAnnotation.version, payloadJson)
    domainEventsRepository.prependEvent(aggregate, aggregateEvent)
    invokeEventListeners(aggregate, payload)
    return Pair(aggregate, aggregateEvent)
  }

  /**
   * Updates the event state of the aggregate
   * @param aggregate Aggregate root to update
   * @param event Event that will be updated
   * @param state The event state to update to
   */
  private fun updateEventState(aggregate: BaseAbstractAggregateRoot<*>, event: AggregateEvent, state: EventState) {
    val newEvent = event.copy(state = state)
    domainEventsRepository.updateLastEvent(aggregate, newEvent)
  }

  /**
   * Saves the aggregate root to the repository
   * @param aggregateRoot Aggregate root to save
   */
  fun save(aggregateRoot: BaseAbstractAggregateRoot<*>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey)
    domainEvents.andEventsFrom(aggregateRoot)

    domainEventsRepository.save(domainEvents)
  }

  /**
   * Marks the aggregate root as deleted in the repository
   * @param aggregateRoot Aggregate root to delete
   */
  fun markDeleted(aggregateRoot: BaseAbstractAggregateRoot<*>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey)
    domainEvents.active = false
    domainEvents.andEventsFrom(aggregateRoot)

    domainEventsRepository.save(domainEvents)
  }

  /**
   * Restores the deleted aggregate root in the repository
   * @param aggregateRoot Aggregate root to restore
   */
  fun markRestored(aggregateRoot: BaseAbstractAggregateRoot<*>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey)
    domainEvents.active = true
    domainEvents.andEventsFrom(aggregateRoot)

    domainEventsRepository.save(domainEvents)
  }

  fun <T : AbstractAggregateRoot<T>> createOrLoadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      initialAggregate: (() -> T)? = null,
      fromOldest: Boolean = false
  ): T {
    return loadAggregate(aggregateId, clazz, false, fromOldest)
        ?: initialAggregate?.invoke()
        ?: clazz.createInstance()
  }

  /**
   * Loads aggregate
   * @param T Aggregate to load
   * @param aggregateId Aggregate ID
   * @param onlyActive Only process active aggregates
   * @return The loaded aggregate
   */
  fun <T : AbstractAggregateRoot<T>> loadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      onlyActive: Boolean = true,
      fromOldest: Boolean = false
  ): T? {
    var aggregate: T? = null
    getEventStream(aggregateId, fromOldest).use {
      for (domainEvents in it) {
        if (aggregate == null) aggregate = clazz.createInstance()
        aggregate!!.aggregatePrimaryKey.aggregateId = domainEvents.aggregatePrimaryKey.aggregateId
        aggregate!!.aggregatePrimaryKey.timeUUID = domainEvents.aggregatePrimaryKey.timeUUID

        if (onlyActive && !domainEvents.active) return@use
        for (event in domainEvents.events) {
          val eventPayload = loadEvent(event)
          invokeEventListeners(aggregate!!, eventPayload, domainEvents)

          // Short-circuit if we only want the latest entry
          if (aggregate!!.latestOnly) return aggregate
          // Short-circuit if the aggregate is considered completely populated
          if (aggregate!!.fullyPopulated()) return aggregate
        }
      }
    }

    return aggregate
  }

  private fun getEventStream(aggregateId: String, fromOldest: Boolean = false): Stream<DomainEvents> {
    return when (fromOldest) {
      true -> domainEventsRepository.streamByIdFromOldest(aggregateId)
      false -> domainEventsRepository.streamById(aggregateId)
    }
  }

  /**
   * Invokes event listeners on the aggregate, namely those with the EventSourceHandler annotation.
   * @param aggregate The aggregate to invoke event listeners on
   * @param event The event that will be emitted to the event listeners
   * @param domainEvents The DomainEvents entity
   */
  private fun <T : AbstractAggregateRoot<T>> invokeEventListeners(
      aggregate: T, event: Any, domainEvents: DomainEvents? = null
  ) {
    // Find EventSourceHandler functions
    val args = mutableListOf<Any>()
    val validFlags = AtomicInteger(0)
    val aggregateHandler = aggregate.classToHandler[event::class]
    aggregateHandler?.valueParameters?.forEach {
      populateArgs(args, it, event, domainEvents, aggregate, validFlags)
    }
    if (aggregateHandler != null && !areFlagsValid(validFlags)) throw IllegalArgumentException(
        "Invalid parameters found with EventSourceHandler. Code (${validFlags.get()}")
    aggregateHandler?.call(aggregate, *args.toTypedArray())
  }

  /**
   * Populates the arguments for the aggregate handler function
   * @param args Arguments to populate
   * @param param Aggregate handler functions parameters
   * @param event The event that will be passed to the handler
   * @param domainEvents The DomainEvents entity
   * @param aggregate The aggregate
   * @param validFlags determines if the args were populated correctly
   */
  private fun populateArgs(
      args: MutableList<Any>, param: KParameter, event: Any, domainEvents: DomainEvents?,
      aggregate: AbstractAggregateRoot<*>, validFlags: AtomicInteger
  ) {
    when {
      param.type.javaType === event::class.java -> {
        args.add(event)
        validFlags.with(0x001)
      }
      param.type.javaType === DomainEvents::class.java -> {
        args.add(domainEvents ?: getDomainEvents(aggregate))
        validFlags.with(0x010)
      }
      param.findAnnotation<AggregateId>() != null -> {
        args.add(aggregate.aggregatePrimaryKey.aggregateId)
        validFlags.with(0x100)
      }
      else -> throw IllegalArgumentException("Unable to determine EventSourceHandler args.")
    }
  }

  /**
   * Retrieves domain events from the repository
   * @param aggregate Aggregate to retrieve
   * @return the domain event
   */
  private fun getDomainEvents(aggregate: AbstractAggregateRoot<*>): DomainEvents {
    val (aggregateId, timeUUID) = aggregate.aggregatePrimaryKey
    return domainEventsRepository.findOneById(aggregateId, timeUUID)
  }

  /**
   * Checks if flags are valid
   * @param validFlags Flags to verify against.
   * @return true if flags are valid
   */
  private fun areFlagsValid(validFlags: AtomicInteger): Boolean {
    return !validFlags.test(0x011)
  }

  /**
   * Runs bitwise operations against current flags
   * @param flag Flag to perform bitwise operation with
   */
  private fun AtomicInteger.with(flag: Int) {
    set(get() and flag)
  }

  /**
   * Tests flag against current set of flags
   * @param flag Flag to test against
   * @return true if given flag matches the current flag
   */
  private fun AtomicInteger.test(flag: Int): Boolean {
    return get() and flag == flag
  }

  /**
   * Loads the event when attempting to hydrate the aggregate
   * @param event: The event we're attempting to load for hydration
   * @return The actual event as the correct class
   */
  abstract fun loadEvent(event: AggregateEvent): Any

  /**
   * Handles the event emitted after aggregate was saved to the repository
   * @param payloadApplicationEvent The event that was emitted
   */
  @Suppress("UNCHECKED_CAST")
  @EventListener
  fun <T : AbstractAggregateRoot<T>> on(payloadApplicationEvent: PayloadApplicationEvent<*>) {
    val source = payloadApplicationEvent.source as T
    val payload = payloadApplicationEvent.payload
    val eventAnnotation = payload::class.findAnnotation<Event>()
        ?: throw NullPointerException("Failed to find Event annotation")
    val (aggregate, event) = updateAggregate(source, payload, eventAnnotation)
    publishEvent(payload, eventAnnotation, aggregate, event)
  }

  /**
   * Publishes the event to a message broker
   * @param routingKey Key to route messages to
   * @param payload Data to send over message broker
   * @param aggregate Aggregate that the event is created from
   */
  protected abstract fun doPublish(routingKey: String, payload: Any,
                                   aggregate: AbstractAggregateRoot<*>)

  private fun publishEvent(payload: Any, eventAnnotation: Event,
                            aggregate: AbstractAggregateRoot<*>, event: AggregateEvent) {
    try {
      doPublish(eventAnnotation.routingKey, payload, aggregate)
      updateEventState(aggregate, event, EventState.SENT)
    } catch (ex: Exception) {
      updateEventState(aggregate, event, EventState.FAIL)
      throw ex
    }
  }

}