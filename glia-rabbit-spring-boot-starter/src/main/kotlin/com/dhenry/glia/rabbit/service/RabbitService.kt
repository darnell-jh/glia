package com.dhenry.glia.rabbit.service

import com.dhenry.glia.annotations.Event
import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import kotlin.reflect.full.findAnnotation

@Service
class RabbitService(val rabbitTemplate: RabbitTemplate) {

  fun send(event: Any) {
    val eventAnnotation = event::class.findAnnotation<Event>()
        ?: throw UnsupportedOperationException("Cannot send event, @Event annotation not found")
    rabbitTemplate.convertAndSend(eventAnnotation.routingKey, event)
  }

  fun send(event: Any, messagePostProcessor: MessagePostProcessor) {
    val eventAnnotation = event::class.findAnnotation<Event>()
        ?: throw UnsupportedOperationException("Cannot send event, @Event annotation not found")
    rabbitTemplate.convertAndSend(eventAnnotation.routingKey, event, messagePostProcessor)
  }

  fun convertMessage(message: Message): Any {
    return rabbitTemplate.messageConverter.fromMessage(message)
  }

}