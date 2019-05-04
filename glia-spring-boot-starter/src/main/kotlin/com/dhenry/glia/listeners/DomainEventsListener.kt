package com.dhenry.glia.listeners

import com.dhenry.glia.annotations.Event
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.services.DomainEventsService
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.PayloadApplicationEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component
import java.util.*
import kotlin.reflect.full.findAnnotation


@Component
class DomainEventsListener(val rabbitTemplate: RabbitTemplate, val domainEventsService: DomainEventsService) {

    companion object {
        const val NUM_100NS_INTERVALS_SINCE_UUID_EPOCH = 0x01b21dd213814000L
    }

    @EventListener
    fun on(payloadApplicationEvent: PayloadApplicationEvent<*>) {
        val source = payloadApplicationEvent.source as AbstractAggregateRoot<*>
        val payload = payloadApplicationEvent.payload
        val eventAnnotation = payload::class.findAnnotation<Event>()!!
        val (aggregate, event) = domainEventsService.updateAggregate(source, payload, eventAnnotation)
        publishEvent(payload, eventAnnotation, aggregate, event)
    }

    fun getTimeFromUUID(uuid: UUID): Long {
        return (uuid.timestamp() - NUM_100NS_INTERVALS_SINCE_UUID_EPOCH) / 10000
    }

    private fun publishEvent(payload: Any, eventAnnotation: Event,
                             aggregate: AbstractAggregateRoot<*>, event: AggregateEvent) {
        try {
            rabbitTemplate.convertAndSend(eventAnnotation.routingKey, payload) { message ->
                message.messageProperties.timestamp = Date(getTimeFromUUID(aggregate.aggregateId.timeUUID))
                message
            }
            domainEventsService.updateEventState(aggregate, event, EventState.SENT)
        } catch(ex: Exception) {
            domainEventsService.updateEventState(aggregate, event, EventState.FAIL)
            throw ex
        }
    }

}