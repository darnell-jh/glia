package com.dhenry.glia.test.autoconfigure.amqp.rabbit

import org.springframework.boot.autoconfigure.ImportAutoConfiguration
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping
import java.lang.annotation.Inherited

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("glia.test.rabbit.producer.queues")
annotation class AutoConfigureRabbitProducerListener(
    /**
     * Enables the configuration
     */
    val enabled: Boolean = true,

    /**
     * Package where all events produced are found
     */
    val producerEventsPackage: String = ""
)