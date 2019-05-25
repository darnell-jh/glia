package com.dhenry.glia.config

import com.datastax.driver.core.utils.UUIDs
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.dhenry.glia.cassandra.domain.services.AbstractDomainEventsService
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
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

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(DomainEventsService::class.java)
  }

  override fun loadEvent(event: AggregateEvent): Any {
    LOGGER.debug("Loading event {}", event)
    val messageProps = MessageProperties().apply {
      receivedRoutingKey = event.routingKey
      contentType = MessageProperties.CONTENT_TYPE_JSON
    }
    LOGGER.debug("Retrieved message properties", messageProps)
    val message = Message(event.payload.toByteArray(), messageProps)
    LOGGER.debug("Converted payload {} to message {}", event.payload, message)
    val result = rabbitTemplate.messageConverter.fromMessage(message)
    LOGGER.debug("Message converted to {}", result)
    return result
  }

  override fun doPublish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*>) {
    val timestamp = Date(UUIDs.unixTimestamp(aggregate.aggregateId.timeUUID))
    LOGGER.debug("Publishing to routing key {}, using timestamp {}, payload {}", routingKey, timestamp, payload)
    rabbitTemplate.convertAndSend(routingKey, payload) { message ->
      message.messageProperties.timestamp = timestamp
      message
    }
  }
}