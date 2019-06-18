package com.dhenry.glia.cassandra.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("cassandra")
class CassandraConfiguration(
    var contactPoints: Array<out String> = arrayOf("localhost")
)