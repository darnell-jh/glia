package com.dhenry.glia.rabbit.config

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Configuration

@Configuration
class RabbitPostConfig(rabbitTemplate: RabbitTemplate) {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(RabbitPostConfig::class.java)
  }

  init {
    LOGGER.info("Setting rabbit channel to transacted")
    rabbitTemplate.isChannelTransacted = true
  }
}