package com.dhenry.glia.cassandra.domain.services

import com.dhenry.glia.annotations.Event
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.full.findAnnotation

/**
 * Service responsible for managing functionality in the domain events collection
 */
@Service
abstract class DomainEventsService(
        val domainEventsRepository: DomainEventsRepository,
        val objectMapper: ObjectMapper
) {

    companion object {
        const val NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L
    }

    /**
     * Updates the aggregate with payload
     * @param aggregate Aggregate root to update
     * @param payload Data that aggregate will update based on
     * @param eventAnnotation Event annotation to capture version and routing key
     * @return A pair of the aggregate that was updated and the event that was emitted as a result of the payload
     */
    fun updateAggregate(aggregate: AbstractAggregateRoot<*>, payload: Any, eventAnnotation: Event):
            Pair<AbstractAggregateRoot<*>, AggregateEvent> {
        // Create a copy of the aggregate and insert into the repository
        val payloadJson = objectMapper.writeValueAsString(payload)
        val aggregateEvent = AggregateEvent(eventAnnotation.routingKey, eventAnnotation.version, payloadJson)
        AggregationService.invokeEventListeners(aggregate, payload)
        domainEventsRepository.prependEvent(aggregate, aggregateEvent)
        return Pair(aggregate, aggregateEvent)
    }

    /**
     * Updates the event state of the aggregate
     * @param aggregate Aggregate root to update
     * @param event Event that will be updated
     * @param state The event state to update to
     */
    fun updateEventState(aggregate: AbstractAggregateRoot<*>, event: AggregateEvent, state: EventState) {
        val newEvent = event.copy(state = state)
        domainEventsRepository.updateLastEvent(aggregate, newEvent)
    }

    /**
     * Saves the aggregate root to the repository
     * @param aggregateRoot Aggregate root to save
     */
    fun save(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

    /**
     * Marks the aggregate root as deleted in the repository
     * @param aggregateRoot Aggregate root to delete
     */
    fun markDeleted(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.active = false
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

    /**
     * Restores the deleted aggregate root in the repository
     * @param aggregateRoot Aggregate root to restore
     */
    fun markRestored(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.active = true
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

    /**
     * Loads aggregate
     * @param T Aggregate to load
     * @param aggregateId Aggregate ID
     * @param aggregateCreator Function to create a new aggregate if aggregate is not loaded
     * @param onlyActive Only process active aggregates
     * @return The loaded or newly created aggregate if aggregate did not exist
     */
    final inline fun <reified T: AbstractAggregateRoot<*>> loadAggregate(
            aggregateId: String,
            crossinline aggregateCreator: (aggregateId: String) -> T,
            onlyActive: Boolean = true
    ): T? {
        val aggregate = AtomicReference<T>()
        domainEventsRepository.streamById(aggregateId).use {
            for (domainEvents in it) {
                if (onlyActive && !domainEvents.active) return@use
                for (event in domainEvents.events) {
                    aggregate.set(
                        Optional.ofNullable(aggregate.get()).orElseGet{ aggregateCreator.invoke(aggregateId) }
                    )
                    val eventPayload = loadEvent(event)
                    AggregationService.invokeEventListeners(aggregate.get(), eventPayload)
                }
            }
        }

        return aggregate.get()
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
    @EventListener
    fun on(payloadApplicationEvent: PayloadApplicationEvent<*>) {
        val source = payloadApplicationEvent.source as AbstractAggregateRoot<*>
        val payload = payloadApplicationEvent.payload
        val eventAnnotation = payload::class.findAnnotation<Event>()!!
        val (aggregate, event) = updateAggregate(source, payload, eventAnnotation)
        publishEvent(payload, eventAnnotation, aggregate, event)
    }

    /**
     * Retrieves time in milliseconds from time UUID
     * @param uuid Time UUID
     */
    fun getTimeFromUUID(uuid: UUID): Long {
        return (uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000
    }

    /**
     * Publishes the event to a message broker
     * @param routingKey Key to route messages to
     * @param payload Data to send over message broker
     * @param aggregate Aggregate that the event is created from
     * @param event The event to be published
     */
    abstract fun doPublish(routingKey: String, payload: Any,
                           aggregate: AbstractAggregateRoot<*>, event: AggregateEvent)

    private fun publishEvent(payload: Any, eventAnnotation: Event,
                             aggregate: AbstractAggregateRoot<*>, event: AggregateEvent) {
        try {
            doPublish(eventAnnotation.routingKey, payload, aggregate, event)
            updateEventState(aggregate, event, EventState.SENT)
        } catch(ex: Exception) {
            updateEventState(aggregate, event, EventState.FAIL)
            throw ex
        }
    }

}