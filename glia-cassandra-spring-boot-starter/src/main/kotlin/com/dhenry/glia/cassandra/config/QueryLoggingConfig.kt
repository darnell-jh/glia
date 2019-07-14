package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.QueryLogger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.core.mapping.event.BeforeSaveEvent


@Configuration
@ConditionalOnProperty(prefix = "glia.cassandra.logging", name = ["enabled"])
class QueryLoggingConfig: ApplicationListener<BeforeSaveEvent<*>> {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(QueryLoggingConfig::class.java)
  }

  @Bean
  @ConditionalOnMissingBean
  fun queryLogger(cluster: Cluster): QueryLogger {
    val queryLogger = QueryLogger.builder()
        .withMaxLoggedParameters(50)
        .withMaxQueryStringLength(500)
        .withMaxParameterValueLength(100)
        .build()
    cluster.register(queryLogger)
    return queryLogger
  }

  override fun onApplicationEvent(event: BeforeSaveEvent<*>) {
    LOGGER.debug("Before Saving -> Statement: {}, Table: {}, timestamp: {}",
        event.statement, event.tableName, event.timestamp)
  }
}
