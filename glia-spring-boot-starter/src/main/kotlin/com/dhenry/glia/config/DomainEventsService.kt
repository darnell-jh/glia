package com.dhenry.glia.config

import com.datastax.driver.core.utils.UUIDs
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.dhenry.glia.cassandra.domain.services.AbstractDomainEventsService
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.util.*

@Service
class DomainEventsService(override val domainEventsRepository: DomainEventsRepository,
                          override val objectMapper: ObjectMapper,
                          val rabbitTemplate: RabbitTemplate)
  : AbstractDomainEventsService(domainEventsRepository, objectMapper) {

  override fun loadEvent(event: AggregateEvent): Any {
    val messageProps = MessageProperties().apply { receivedRoutingKey = event.routingKey }
    val message = Message(event.payload.toByteArray(), messageProps)
    return rabbitTemplate.messageConverter.fromMessage(message)
  }

  override fun doPublish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*>) {
    rabbitTemplate.convertAndSend(routingKey, payload) { message ->
      message.messageProperties.timestamp = Date(UUIDs.unixTimestamp(aggregate.aggregateId.timeUUID))
      message
    }
  }
}