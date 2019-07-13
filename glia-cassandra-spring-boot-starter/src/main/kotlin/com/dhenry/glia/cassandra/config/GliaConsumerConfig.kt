package com.dhenry.glia.cassandra.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "glia.consumer")
class GliaConsumerConfig(
    val enabled: Boolean = false
)