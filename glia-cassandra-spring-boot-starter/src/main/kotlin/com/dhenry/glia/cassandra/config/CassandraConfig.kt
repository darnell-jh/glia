package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import org.springframework.data.cassandra.core.cql.keyspace.DropKeyspaceSpecification

private const val DOMAIN_EVENTS_PACKAGE = "com.dhenry.glia.cassandra.domain"

@ConditionalOnMissingBean(Cluster::class)
@Configuration
@EnableConfigurationProperties(ReplicationConfiguration::class, CassandraConfiguration::class)
class CassandraConfig(
    @Value("\${spring.data.cassandra.keyspace-name}") private val keyspace: String,
    private val cassandraConfiguration: CassandraConfiguration,
    private val replicationConfig: ReplicationConfiguration
) : AbstractCassandraConfiguration() {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(CassandraConfig::class.java)
  }

  // Below method creates keyspace if it does not exist.
  private val keySpaceSpecification: CreateKeyspaceSpecification by lazy {
    LOGGER.info("Keyspace {} will be created", keyspace)
    val spec = CreateKeyspaceSpecification.createKeyspace(keyspace)
        .ifNotExists()
    replicationConfig.configureKeyspaceCreation(spec)
    spec
  }

  @Bean
  override fun cluster(): CassandraCqlClusterFactoryBean {
    val bean = CassandraCqlClusterFactoryBean()
    bean.setJmxReportingEnabled(false)
    if (replicationConfig.recreateKeyspace) bean.keyspaceDrops = keyspaceDrops
    bean.keyspaceCreations = keyspaceCreations
    bean.setReconnectionPolicy(ExponentialReconnectionPolicy(1000, 8000))
    bean.setContactPoints(cassandraConfiguration.contactPoints.joinToString(","))
    return bean
  }

  override fun getSchemaAction(): SchemaAction {
    return SchemaAction.CREATE_IF_NOT_EXISTS
  }

  override fun getKeyspaceName(): String {
    return keyspace
  }

  override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
    return listOf(keySpaceSpecification)
  }

  override fun getKeyspaceDrops(): List<DropKeyspaceSpecification> {
    LOGGER.info("Keyspace {} will be dropped", keyspace)
    return listOf(DropKeyspaceSpecification.dropKeyspace(keyspace).ifExists())
  }

  override fun getEntityBasePackages(): Array<String> {
    LOGGER.info("Using entity base packages {}", replicationConfig.entityBasePackages)
    var basePackages = replicationConfig.entityBasePackages ?: arrayOf()
    if (replicationConfig.enableDomainEvents) basePackages += DOMAIN_EVENTS_PACKAGE
    return basePackages
  }

}