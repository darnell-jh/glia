package com.dhenry.glia.component

import com.dhenry.glia.component.produced.ROUTING_KEY
import com.dhenry.glia.test.autoconfigure.amqp.rabbit.RabbitProducerListenerAutoConfiguration
import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.mockito.Mockito.mock
import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.MapPropertySource
import org.springframework.core.env.PropertySource
import java.util.*
import kotlin.test.Test

class RabbitProducerListenerAutoConfigurationTests {

  private lateinit var context: AnnotationConfigApplicationContext

  @After
  fun close() {
    this.context.close()
  }

  @Test
  fun shouldCustomizeProducedEventsPackage() {
    val propertySource = MapPropertySource("test",
        mapOf(
            "glia.test.rabbit.producer.queues.producer-events-package" to "com.dhenry.glia.component.produced",
            "glia.test.rabbit.producer.queues.enabled" to true
        )
    )
    load(propertySource, BaseConfiguration::class.java)
    val declarables = this.context.getBean(Declarables::class.java)
    val routingKey = declarables.declarables.asSequence()
        .filter { it is Binding }
        .map { (it as Binding).routingKey }
        .first()
    assertThat(routingKey).isEqualTo(ROUTING_KEY)
  }

  private fun load(vararg config: Class<*>) {
    load(null, *config)
  }

  private fun load(propertySource: PropertySource<*>?, vararg config: Class<*>) {
    val context = AnnotationConfigApplicationContext()
    Optional.ofNullable(propertySource).ifPresent { context.environment.propertySources.addFirst(it) }
    context.register(*config)
    context.register(RabbitProducerListenerAutoConfiguration::class.java)
    context.refresh()
    this.context = context
  }

  @Configuration
  class BaseConfiguration {
    @Bean
    fun exchange(): Exchange = mock(Exchange::class.java)

    @Bean
    fun messageConverter(): MessageConverter = mock(MessageConverter::class.java)

    @Bean
    fun connectionFactory(): ConnectionFactory = mock(ConnectionFactory::class.java)

    @Bean
    fun objectMapper(): ObjectMapper = mock(ObjectMapper::class.java)
  }

}