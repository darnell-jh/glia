package com.dhenry.glia.rabbit.service

import com.dhenry.glia.amqp.AmqpService
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import com.dhenry.glia.service.EventMediator
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.util.*
import kotlin.reflect.full.findAnnotation

@Service
class RabbitService(val rabbitTemplate: RabbitTemplate): EventMediator, AmqpService {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(RabbitService::class.java)
  }

  override fun send(event: Any) {
    val eventAnnotation = event::class.findAnnotation<Event>()
        ?: throw UnsupportedOperationException("Cannot send event, @Event annotation not found")
    rabbitTemplate.convertAndSend(eventAnnotation.routingKey, event)
  }

  override fun send(event: Any, messagePostProcessor: MessagePostProcessor) {
    val eventAnnotation = event::class.findAnnotation<Event>()
        ?: throw UnsupportedOperationException("Cannot send event, @Event annotation not found")
    rabbitTemplate.convertAndSend(eventAnnotation.routingKey, event, messagePostProcessor)
  }

  override fun convertMessage(message: Message): Any {
    return rabbitTemplate.messageConverter.fromMessage(message)
  }

  override fun load(routingKey: String, payloadJson: String): Any {
    LOGGER.debug("Loading event from routing key {}", routingKey)
    val messageProps = MessageProperties().apply {
      receivedRoutingKey = routingKey
      contentType = MessageProperties.CONTENT_TYPE_JSON
    }
    LOGGER.debug("Retrieved message properties", messageProps)
    val message = Message(payloadJson.toByteArray(), messageProps)
    LOGGER.debug("Converted payload {} to message {}", payloadJson, message)
    val result = convertMessage(message)
    LOGGER.debug("Message converted to {}", result)
    return result
  }

  override fun publish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*, *>,
                       event: Any, timestamp: Instant) {
    LOGGER.debug("Publishing to routing key {}, using timestamp {}, payload {}", routingKey, timestamp, payload)
    send(payload, MessagePostProcessor { message ->
      message.messageProperties.timestamp = Date.from(timestamp)
      message
    })
  }
}