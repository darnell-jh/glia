package com.dhenry.glia.cassandra.domain.services

import com.dhenry.domain.entities.AggregateEvent
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.core.ExecutableUpdateOperation
import org.springframework.data.cassandra.core.query.Query
import org.springframework.data.cassandra.core.query.Update
import org.springframework.data.cassandra.core.query.isEqualTo
import org.springframework.data.cassandra.core.query.where
import org.springframework.data.cassandra.core.update
import org.springframework.stereotype.Service
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@Service
class DomainEventsService(
        private val cassandraTemplate: CassandraTemplate,
        val domainEventsRepository: DomainEventsRepository,
        val objectMapper: ObjectMapper
) {

    fun prependEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent) {
        val (aggregateId, timeUUID) = aggregateRoot.aggregateId

        val update = Update.empty().addTo("events").prepend(event)
        (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
            .matching(Query.query(
                where("aggregateid").isEqualTo(aggregateId),
                where("timeuuid").isEqualTo(timeUUID)
            ))
            .apply(update)
    }

    fun updateLastEvent(aggregateRoot: AbstractAggregateRoot<*>, event: AggregateEvent) {
        val (aggregateId, timeUUID) = aggregateRoot.aggregateId

        val update = Update.empty().set("events").atIndex(0).to(event)
        (cassandraTemplate as ExecutableUpdateOperation).update(DomainEvents::class)
            .matching(Query.query(
                where("aggregateid").isEqualTo(aggregateId),
                where("timeuuid").isEqualTo(timeUUID)
            ))
            .apply(update)
    }

    fun updateAggregate(aggregate: AbstractAggregateRoot<*>, payload: Any, eventAnnotation: Event):
            Pair<AbstractAggregateRoot<*>, AggregateEvent> {
        // Create a copy of the aggregate and insert into the repository
        val payloadJson = objectMapper.writeValueAsString(payload)
        val aggregateEvent = AggregateEvent(eventAnnotation.routingKey, eventAnnotation.version, payloadJson)
        AggregationService.invokeEventListeners(aggregate, payload)
        prependEvent(aggregate, aggregateEvent)
        return Pair(aggregate, aggregateEvent)
    }

    fun updateEventState(aggregate: AbstractAggregateRoot<*>, event: AggregateEvent, state: EventState) {
        val newEvent = event.copy(state = state)
        updateLastEvent(aggregate, newEvent)
    }

    fun save(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

    fun markDeleted(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.active = false
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

    fun markRestored(aggregateRoot: AbstractAggregateRoot<*>) {
        val domainEvents = DomainEvents(aggregateRoot.aggregateId)
        domainEvents.active = true
        domainEvents.andEventsFrom(aggregateRoot)

        domainEventsRepository.save(domainEvents)
    }

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
                    val eventPayload = objectMapper.readValue(event.payload, T::class.java)
                    AggregationService.invokeEventListeners(aggregate.get(), eventPayload)
                }
            }
        }

        return aggregate.get()
    }

    private fun checkIfValid(valid: Boolean) {
        if (!valid)
            throw UnsupportedOperationException("there must only be one @AggregateIdentifier annotation")
    }

}