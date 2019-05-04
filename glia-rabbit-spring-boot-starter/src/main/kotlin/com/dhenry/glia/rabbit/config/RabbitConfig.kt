package com.dhenry.glia.rabbit.config

import com.dhenry.glia.annotations.Event
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Declarables
import org.springframework.amqp.core.Exchange
import org.springframework.amqp.core.ExchangeBuilder
import org.springframework.amqp.core.MessageProperties
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.QueueBuilder
import org.springframework.amqp.rabbit.annotation.EnableRabbit
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ScannedGenericBeanDefinition
import org.springframework.core.type.filter.AnnotationTypeFilter
import javax.annotation.PostConstruct


private const val CONSUMER_PACKAGE = "com.dhenry.domain.events.consumed"

@Configuration
@EnableRabbit
class RabbitConfig(@Value("\${spring.application.name}") val queueName: String,
                   @Value("\${spring.rabbitmq.template.exchange}") val exchangeName: String) {

    @Autowired
    lateinit var rabbitTemplate: RabbitTemplate

    companion object {
        val LOGGER: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)

        private val CLASS_TO_ROUTING_KEY: Map<Class<*>, String>
        private val ROUTING_KEY_TO_CLASS: Map<String, Class<*>>

        init {
            CLASS_TO_ROUTING_KEY = hashMapOf(*findEventRoutingKeys(CONSUMER_PACKAGE).toTypedArray())
            ROUTING_KEY_TO_CLASS = hashMapOf(*CLASS_TO_ROUTING_KEY.map {(k, v) -> v to k}.toTypedArray())
        }

        private fun findEventRoutingKeys(pkgPath: String): Collection<Pair<Class<*>, String>> {
            val classPathScanningCandidateComponentProvider = ClassPathScanningCandidateComponentProvider(true)
            classPathScanningCandidateComponentProvider.addIncludeFilter(AnnotationTypeFilter(Event::class.java))
            val beanDefinitions = classPathScanningCandidateComponentProvider.findCandidateComponents(pkgPath)

            return beanDefinitions
                .map {
                    it as ScannedGenericBeanDefinition
                    val clazz = Class.forName(it.beanClassName)
                    val annotation = clazz.getAnnotation(Event::class.java)
                    Pair<Class<*>, String>(clazz, annotation.routingKey)
                }
        }
    }

    @Bean
    fun exchange(): Exchange =
        ExchangeBuilder.topicExchange(exchangeName).durable(true).autoDelete().build()

    @Bean
    fun queue(): Queue =
        QueueBuilder.durable(queueName).autoDelete().build()

    @Bean
    fun bindings(queue: Queue, exchange: Exchange): Declarables {
        return CLASS_TO_ROUTING_KEY.values
            .map { BindingBuilder.bind(queue).to(exchange).with(it).noargs() }
            .onEach { LOGGER.info("Binding {} to {} with routing key {}", it.destination, it.exchange, it.routingKey) }
            .let { Declarables(it) }
    }

    @Bean
    fun messageConverter(objectMapper: ObjectMapper): MessageConverter {
        return Jackson2JsonMessageConverter(objectMapper).also {
            it.setClassMapper(customClassMapper())
        }
    }

    fun customClassMapper(): ClassMapper {
        return object: ClassMapper{
            override fun toClass(properties: MessageProperties): Class<*> {
                return ROUTING_KEY_TO_CLASS.getValue(properties.receivedRoutingKey)
            }

            override fun fromClass(clazz: Class<*>, properties: MessageProperties) {

            }
        }
    }

    @PostConstruct
    fun init() {
        rabbitTemplate.isChannelTransacted = true
    }

}