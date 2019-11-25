package com.dhenry.glia.cassandra.domain.services

import com.datastax.driver.core.utils.UUIDs
import com.dhenry.glia.annotations.AggregateId
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.data.DataDomainEventsOperations
import com.dhenry.glia.data.DomainEventsOperations
import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import com.dhenry.glia.data.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.data.models.EventState
import com.dhenry.glia.service.EventMediator
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.time.Instant
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
@Service
class CassandraDomainEventsService(
    private val domainEventsOperations: DomainEventsOperations,
    private val objectMapper: ObjectMapper,
    private val eventMediator: EventMediator
): DataDomainEventsOperations {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(CassandraDomainEventsService::class.java)
  }

  /**
   * Updates the aggregate with payload
   * @param aggregate Aggregate root to update
   * @param payload Data that aggregate will update based on
   * @param eventAnnotation Event annotation to capture version and routing key
   * @return A pair of the aggregate that was updated and the event that was emitted as a result of the payload
   */
  private fun updateAggregate(aggregate: AbstractAggregateRoot<*, *>, payload: Any, eventAnnotation: Event)
      : Pair<AbstractAggregateRoot<*, *>, AggregateEvent> {
    // Create a copy of the aggregate and insert into the repository
    val payloadJson = objectMapper.writeValueAsString(payload)
    val aggregateEvent = AggregateEvent(eventAnnotation.routingKey, eventAnnotation.version, payloadJson)
    domainEventsOperations.prependEvent(aggregate, aggregateEvent)
    invokeEventListeners(aggregate, payload)
    return Pair(aggregate, aggregateEvent)
  }

  private fun saveInternal(domainEvents: DomainEvents) = domainEventsOperations.save(domainEvents)

  /**
   * Saves the aggregate root to the repository
   * @param aggregateRoot Aggregate root to save
   */
  override fun save(aggregateRoot: BaseAbstractAggregateRoot<*, *>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey as AggregatePrimaryKey)
    domainEvents.andEventsFrom(aggregateRoot)
    domainEvents.setAggregateRoot(aggregateRoot)

    saveInternal(domainEvents)
  }

  /**
   * Marks the aggregate root as deleted in the repository
   * @param aggregateRoot Aggregate root to delete
   */
  override fun markDeleted(aggregateRoot: BaseAbstractAggregateRoot<*, *>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey as AggregatePrimaryKey)
    domainEvents.active = false
    domainEvents.andEventsFrom(aggregateRoot)

    saveInternal(domainEvents)
  }

  /**
   * Restores the deleted aggregate root in the repository
   * @param aggregateRoot Aggregate root to restore
   */
  override fun markRestored(aggregateRoot: BaseAbstractAggregateRoot<*, *>) {
    val domainEvents = DomainEvents(aggregateRoot.aggregatePrimaryKey as AggregatePrimaryKey)
    domainEvents.active = true
    domainEvents.andEventsFrom(aggregateRoot)

    saveInternal(domainEvents)
  }

  override fun <T : AbstractAggregateRoot<T, *>> createOrLoadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      initialAggregate: (() -> T)?,
      fromOldest: Boolean
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
  override fun <T : AbstractAggregateRoot<T, *>> loadAggregate(
      aggregateId: String,
      clazz: KClass<T>,
      onlyActive: Boolean,
      fromOldest: Boolean
  ): T? {
    var aggregate: T? = null
    getEventStream(aggregateId, fromOldest).use { it
        .peek{ domainEvent ->
          LOGGER.debug("Received domain pk: [aggregateId: {}, sequence: {}], events: {}",
              domainEvent.aggregatePrimaryKey.aggregateId,
              domainEvent.aggregatePrimaryKey.sequence,
              domainEvent.events)
        }
        .forEach{ domainEvent -> aggregate = processDomainEvent(clazz, domainEvent, onlyActive, aggregate) }
    }

    LOGGER.debug("Loaded aggregate with id: {}, sequence: {}",
        aggregate?.aggregatePrimaryKey?.aggregateId, aggregate?.aggregatePrimaryKey?.sequence)
    return aggregate
  }

  private fun <T : AbstractAggregateRoot<T, *>> processDomainEvent(
      clazz: KClass<T>, domainEvents: DomainEvents, onlyActive: Boolean, inAggregate: T?
  ): T? {
    var aggregate: T? = inAggregate
    LOGGER.debug("Processing domain events: {}", domainEvents.events)
    if (aggregate == null) aggregate = clazz.createInstance()
    aggregate.aggregatePrimaryKey.aggregateId = domainEvents.aggregatePrimaryKey.aggregateId
    aggregate.aggregatePrimaryKey.sequence = aggregate.aggregatePrimaryKey.sequence
        .coerceAtLeast(domainEvents.aggregatePrimaryKey.sequence)

    if (onlyActive && !domainEvents.active) return aggregate
    for (event in domainEvents.events) {
      val eventPayload = eventMediator.load(event.routingKey, event.payload)
      invokeEventListeners(aggregate, eventPayload, domainEvents)

      // Short-circuit if we only want the latest entry
      if (aggregate.latestOnly) return aggregate
      // Short-circuit if the aggregate is considered completely populated
      if (aggregate.fullyPopulated()) return aggregate
    }

    return aggregate
  }

  @Suppress("UNCHECKED_CAST")
  private fun getEventStream(aggregateId: String, fromOldest: Boolean = false): Stream<DomainEvents> {
    return when (fromOldest) {
      true -> domainEventsOperations.streamByIdFromOldest(aggregateId) as Stream<DomainEvents>
      false -> domainEventsOperations.streamById(aggregateId) as Stream<DomainEvents>
    }
  }

  /**
   * Invokes event listeners on the aggregate, namely those with the EventSourceHandler annotation.
   * @param aggregate The aggregate to invoke event listeners on
   * @param event The event that will be emitted to the event listeners
   * @param domainEvents The DomainEvents entity
   */
  private fun <T : AbstractAggregateRoot<*, *>> invokeEventListeners(
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
      aggregate: AbstractAggregateRoot<*, *>, validFlags: AtomicInteger
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
  private fun getDomainEvents(aggregate: AbstractAggregateRoot<*, *>): DomainEvents {
    val (aggregateId, sequence) = aggregate.aggregatePrimaryKey
    return domainEventsOperations.findOneById(aggregateId, sequence) as DomainEvents
  }

  /**
   * Checks if flags are valid
   * @param validFlags Flags to verify against.f
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
   * Handles the event emitted after aggregate was saved to the repository
   * @param payloadApplicationEvent The event that was emitted
   */
  @Suppress("UNCHECKED_CAST")
  @EventListener
  fun on(payloadApplicationEvent: PayloadApplicationEvent<*>) {
    val source = payloadApplicationEvent.source as AbstractAggregateRoot<*, *>
    val payload = payloadApplicationEvent.payload
    val eventAnnotation = payload::class.findAnnotation<Event>()
        ?: throw NullPointerException("Failed to find Event annotation")
    val (aggregate, event) = updateAggregate(source, payload, eventAnnotation)
    publishEvent(payload, eventAnnotation, aggregate, event)
  }

  private fun publishEvent(payload: Any, eventAnnotation: Event,
                           aggregate: AbstractAggregateRoot<*, *>, event: AggregateEvent) {
    try {
      val timestamp = Instant.ofEpochMilli(UUIDs.unixTimestamp(event.eventId))
      eventMediator.publish(eventAnnotation.routingKey, payload, aggregate, event, timestamp)
      domainEventsOperations.updateEventState(aggregate, event.eventId.toString(), EventState.SENT)
    } catch (ex: Exception) {
      domainEventsOperations.updateEventState(aggregate, event.eventId.toString(), EventState.FAIL)
      throw ex
    }
  }

}