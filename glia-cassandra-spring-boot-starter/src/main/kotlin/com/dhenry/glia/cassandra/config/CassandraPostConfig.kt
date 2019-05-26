package com.dhenry.glia.cassandra.config

import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.cql.CqlIdentifier

@Configuration
@ConditionalOnProperty(value = ["glia.consumer.enabled"], havingValue = "false", matchIfMissing = true)
class CassandraPostConfig(
    private val cassandraAdminTemplate: CassandraAdminTemplate,
    @Value("\${spring.data.cassandra.keyspace-name}") private val keyspace: String
) {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(CassandraPostConfig::class.java)
  }

  init {
    setupStaticActiveColumn()
  }

  final fun setupStaticActiveColumn() {
    val hasStaticActiveCol = cassandraAdminTemplate.getTableMetadata(keyspace, CqlIdentifier.of(TBL_DOMAIN_EVENTS))
        .map { it.columns.asSequence() }
        .orElseGet { emptySequence() }
        .any { it.name == "active" && it.isStatic }

    if (!hasStaticActiveCol) {
      LOGGER.info("Re-adding active column as static boolean")
      cassandraAdminTemplate.cqlOperations
          .execute("""ALTER TABLE $keyspace.$TBL_DOMAIN_EVENTS DROP active""")
      cassandraAdminTemplate.cqlOperations
          .execute("""ALTER TABLE $keyspace.$TBL_DOMAIN_EVENTS ADD active boolean static""")
    }
  }
}