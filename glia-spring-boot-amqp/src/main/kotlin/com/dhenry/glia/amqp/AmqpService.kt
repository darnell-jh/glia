package com.dhenry.glia.amqp

import org.springframework.amqp.core.Message
import org.springframework.amqp.core.MessagePostProcessor

interface AmqpService {

  fun send(event: Any)

  fun send(event: Any, messagePostProcessor: MessagePostProcessor)

  fun convertMessage(message: Message): Any

}