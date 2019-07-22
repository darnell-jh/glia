package com.dhenry.glia.cassandra.config

import org.springframework.boot.context.properties.ConfigurationProperties

//TODO: Move this to glia-spring-boot-starter
@ConfigurationProperties(prefix = "glia.consumer")
class GliaConsumerConfig(
    var enabled: Boolean = false
)