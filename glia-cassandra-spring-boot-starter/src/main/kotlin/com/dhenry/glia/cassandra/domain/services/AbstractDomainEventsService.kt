package com.dhenry.glia.cassandra.domain.services

import com.dhenry.glia.annotations.Event
import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.EventListener
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.memberFunctions
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
    private fun <T: AbstractAggregateRoot<T>> updateAggregate(aggregate: T, payload: Any, eventAnnotation: Event):
            Pair<AbstractAggregateRoot<*>, AggregateEvent> {
        // Create a copy of the aggregate and insert into the repository
        val payloadJson = objectMapper.writeValueAsString(payload)
        val aggregateEvent = AggregateEvent(eventAnnotation.routingKey, eventAnnotation.version, payloadJson)
        invokeEventListeners(aggregate, payload)
        domainEventsRepository.prependEvent(aggregate, aggregateEvent)
        return Pair(aggregate, aggregateEvent)
    }

    /**
     * Updates the event state of the aggregate
     * @param aggregate Aggregate root to update
     * @param event Event that will be updated
     * @param state The event state to update to
     */
    private fun updateEventState(aggregate: AbstractAggregateRoot<*>, event: AggregateEvent, state: EventState) {
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

    fun <T: AbstractAggregateRoot<T>> createOrLoadAggregate(
        aggregateId: String,
        aggregateCreator: (aggregateId: String) -> T
    ): T {
        return loadAggregate(aggregateId, aggregateCreator) ?: aggregateCreator(aggregateId)
    }

    /**
     * Loads aggregate
     * @param T Aggregate to load
     * @param aggregateId Aggregate ID
     * @param aggregateCreator Function to create a new aggregate if aggregate is not loaded
     * @param onlyActive Only process active aggregates
     * @return The loaded or newly created aggregate if aggregate did not exist
     */
    fun <T: AbstractAggregateRoot<T>> loadAggregate(
            aggregateId: String,
            aggregateCreator: (aggregateId: String) -> T,
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
                    invokeEventListeners(aggregate.get(), eventPayload)
                }
            }
        }

        return aggregate.get()
    }

    /**
     * Invokes event listeners on the aggregate, namely those with the EventSourceHandler annotation.
     * @param aggregate The aggregate to invoke event listeners on
     * @param events The events that will be emitted to the event listeners
     */
    private fun <T: AbstractAggregateRoot<T>> invokeEventListeners(aggregate: T, event: Any) {
        val eventSourceHandlers = aggregate::class.memberFunctions
            .filter { it.findAnnotation<EventSourceHandler>() != null }
        for (handlerFun in eventSourceHandlers) {
            // Find EventSourceHandler functions
            val annotation: EventSourceHandler = handlerFun.findAnnotation()
                ?: throw NullPointerException("Failed to find event source handler functions")
            if (annotation.value === Any::class &&
                handlerFun.valueParameters[0].type.javaType === event::class.java) {
                // Call function with the matching payload
                handlerFun.call(aggregate, event)
            }
        }
    }

    /**
     * Loads the event when attempting to hydrate the aggregate
     * @param event: The event we're attempting to load for hydration
     * @return The actual event as the correct class
     */
    protected abstract fun loadEvent(event: AggregateEvent): Any

    /**
     * Handles the event emitted after aggregate was saved to the repository
     * @param payloadApplicationEvent The event that was emitted
     */
    @EventListener
    fun <T: AbstractAggregateRoot<T>> on(payloadApplicationEvent: PayloadApplicationEvent<*>) {
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
     * @param event The event to be published
     */
    protected abstract fun doPublish(routingKey: String, payload: Any,
                           aggregate: AbstractAggregateRoot<*>)

    private fun publishEvent(payload: Any, eventAnnotation: Event,
                             aggregate: AbstractAggregateRoot<*>, event: AggregateEvent) {
        try {
            doPublish(eventAnnotation.routingKey, payload, aggregate)
            updateEventState(aggregate, event, EventState.SENT)
        } catch(ex: Exception) {
            updateEventState(aggregate, event, EventState.FAIL)
            throw ex
        }
    }

}