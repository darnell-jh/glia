package com.dhenry.glia.test.autoconfigure.amqp.rabbit

import com.dhenry.glia.rabbit.config.RabbitConfig
import com.dhenry.glia.test.ProducerEventListener
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.util.*

@Configuration
@ConditionalOnProperty(prefix = "glia.test.rabbit.producer.queues", name = ["enabled"])
@EnableConfigurationProperties(RabbitProducerListenerAutoConfiguration.RabbitProducerListenerQueueProperties::class)
class RabbitProducerListenerAutoConfiguration(
    private val objectMapper: ObjectMapper,
    private val rabbitProducerQueueProps: RabbitProducerListenerQueueProperties
) {

  @ConfigurationProperties("glia.test.rabbit.producer.queues")
  class RabbitProducerListenerQueueProperties(var producerEventPackages: Array<out String>? = null)

  companion object {
    val QUEUE_NAME = UUID.randomUUID().toString()
  }

  val producerPackages: Array<out String>? by lazy { rabbitProducerQueueProps.producerEventPackages }

  private val classToRoutingKey: Map<Class<*>, String> by lazy {
    hashMapOf(*RabbitConfig.findEventRoutingKeys(producerPackages ?: arrayOf()).toTypedArray())
  }

  private val routingKeyToClass: Map<String, Class<*>> by lazy {
    hashMapOf(*classToRoutingKey.map {(k, v) -> v to k}.toTypedArray())
  }

  @Bean
  fun producerQueue(): Queue = QueueBuilder.nonDurable(QUEUE_NAME).autoDelete().build()

  @Bean
  fun producerBindings(
      producerQueue: Queue, exchange: Exchange, applicationContext: ApplicationContext
  ): Declarables {
    // glia.test.rabbit.producer.queues.producerEventsPackage
    return RabbitConfig.findEventRoutingKeys(producerPackages ?: arrayOf())
        .map { BindingBuilder.bind(producerQueue).to(exchange).with(it.second).noargs() }
        .let { Declarables(it) }
  }

  @Bean
  fun producedEventListener(): ProducerEventListener =
      ProducerEventListener(messageConverter())

  @Bean
  fun listenerContainer(connectionFactory: ConnectionFactory, eventListener: ProducerEventListener)
      : SimpleMessageListenerContainer {
    return SimpleMessageListenerContainer(connectionFactory).apply {
      setQueueNames(QUEUE_NAME)
      setMessageListener(eventListener)
    }
  }

  fun messageConverter(): MessageConverter {
    return Jackson2JsonMessageConverter(objectMapper).also {
      it.setClassMapper(customClassMapper())
    }
  }

  private fun customClassMapper(): ClassMapper {
    return object: ClassMapper {
      override fun toClass(properties: MessageProperties): Class<*> {
        return routingKeyToClass.getValue(properties.receivedRoutingKey)
      }

      override fun fromClass(clazz: Class<*>, properties: MessageProperties) {
        properties.receivedRoutingKey = classToRoutingKey[clazz]
      }
    }
  }
}