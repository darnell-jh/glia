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
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.ClassMapper
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.amqp.support.converter.MessageConverter
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.ScannedGenericBeanDefinition
import org.springframework.core.type.filter.AnnotationTypeFilter


@Configuration
@EnableRabbit
@ConditionalOnClass(ConnectionFactory::class, RabbitTemplate::class)
class RabbitConfig(
    @Value("\${spring.application.name}") val queueName: String,
    @Value("\${spring.rabbitmq.template.exchange}") val exchangeName: String
) {

    private lateinit var consumerPackage: String

    private val classToRoutingKey: Map<Class<*>, String> by lazy {
        hashMapOf(*findEventRoutingKeys(consumerPackage).toTypedArray())
    }

    private val routingKeyToClass: Map<String, Class<*>> by lazy {
        hashMapOf(*classToRoutingKey.map {(k, v) -> v to k}.toTypedArray())
    }

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(RabbitConfig::class.java)

        fun findEventRoutingKeys(pkgPath: String): Collection<Pair<Class<*>, String>> {
            LOGGER.debug("Finding routing keys for package {}", pkgPath)
            val classPathScanningCandidateComponentProvider = ClassPathScanningCandidateComponentProvider(true)
            classPathScanningCandidateComponentProvider.addIncludeFilter(AnnotationTypeFilter(Event::class.java))
            val beanDefinitions = classPathScanningCandidateComponentProvider.findCandidateComponents(pkgPath)
            LOGGER.debug("Found {} bean definitions", beanDefinitions.size)

            return beanDefinitions
                .map {
                    LOGGER.debug("Mapping bean definition {}", it)
                    it as ScannedGenericBeanDefinition
                    val clazz = Class.forName(it.beanClassName)
                    val annotation = clazz.getAnnotation(Event::class.java)
                    LOGGER.debug("Returning pair {} -> {}", clazz, annotation.routingKey)
                    Pair<Class<*>, String>(clazz, annotation.routingKey)
                }
        }
    }

    @Bean
    @ConditionalOnMissingBean
    fun exchange(): Exchange =
        ExchangeBuilder.topicExchange(exchangeName).durable(true).autoDelete().build()

    @Bean
    @ConditionalOnProperty(name = ["glia.consumer.enabled"], havingValue = "true")
    fun queue(): Queue =
        QueueBuilder.durable(queueName).autoDelete().build()

    @Bean
    @ConditionalOnProperty(name = ["glia.consumer.enabled"], havingValue = "true")
    fun bindings(queue: Queue, exchange: Exchange, @Value("\${glia.consumer.package}") consumerPackage: String)
        : Declarables {
        this.consumerPackage = consumerPackage

        return classToRoutingKey.values
            .map { BindingBuilder.bind(queue).to(exchange).with(it).noargs() }
            .onEach { LOGGER.info("Binding {} to {} with routing key {}", it.destination, it.exchange, it.routingKey) }
            .let { Declarables(it) }
    }

    @Bean
    @ConditionalOnMissingBean
    fun messageConverter(objectMapper: ObjectMapper,
                         @Value("\${glia.consumer.package}") consumerPackage: String): MessageConverter {
        this.consumerPackage = consumerPackage

        return Jackson2JsonMessageConverter(objectMapper).also {
            it.setClassMapper(customClassMapper())
        }
    }

    private fun customClassMapper(): ClassMapper {
        return object: ClassMapper{
            override fun toClass(properties: MessageProperties): Class<*> {
                LOGGER.debug("Got properties {}", properties)
                val clazz = routingKeyToClass.getValue(properties.receivedRoutingKey)
                LOGGER.debug("Converting {} to class {}", properties.receivedRoutingKey, clazz.simpleName)
                return clazz
            }

            override fun fromClass(clazz: Class<*>, properties: MessageProperties) {
                val routingKey = classToRoutingKey[clazz]
                LOGGER.debug("Converting class {} to routingKey {}", clazz.simpleName, routingKey)
                properties.receivedRoutingKey = routingKey
            }
        }
    }

}