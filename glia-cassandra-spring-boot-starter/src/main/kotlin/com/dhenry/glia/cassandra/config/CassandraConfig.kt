package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.dhenry.glia.cassandra.domain.models.TYPE_AGGREGATE_EVENT
import com.dhenry.glia.cassandra.domain.repositories.GliaRepositoryBaseClassImpl
import com.dhenry.glia.cassandra.domain.template.GliaCassandraTemplate
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
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import org.springframework.data.cassandra.core.cql.keyspace.DropKeyspaceSpecification
import org.springframework.data.cassandra.repository.config.EnableCassandraRepositories
import java.util.*

private const val DOMAIN_EVENTS_PACKAGE = "com.dhenry.glia.cassandra.domain"

@ConditionalOnMissingBean(Cluster::class)
@Configuration
@EnableConfigurationProperties(
    ReplicationConfiguration::class, CassandraConfiguration::class, GliaConsumerConfig::class
)
@EnableCassandraRepositories(
    basePackages = ["com.dhenry.glia.cassandra.domain.repositories"],
    repositoryBaseClass = GliaRepositoryBaseClassImpl::class
)
class CassandraConfig(
    @Value("\${spring.data.cassandra.keyspace-name}") private val keyspace: String,
    private val cassandraConfiguration: CassandraConfiguration,
    private val replicationConfig: ReplicationConfiguration,
    private val consumerConfig: GliaConsumerConfig
) : AbstractCassandraConfiguration() {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(CassandraConfig::class.java)
  }

  val formattedKeyspace by lazy {
    keyspace.replace("""[^A-Za-z0-9]""".toRegex(), "").toLowerCase(Locale.US)
  }

  // Below method creates keyspace if it does not exist.
  private val keySpaceSpecification: CreateKeyspaceSpecification by lazy {
    LOGGER.info("Keyspace {} will be created", formattedKeyspace)
    val spec = CreateKeyspaceSpecification.createKeyspace(formattedKeyspace)
        .ifNotExists()
    replicationConfig.configureKeyspaceCreation(spec)
    spec
  }

  override fun cassandraTemplate(): CassandraAdminTemplate {
    val options = GliaCassandraTemplate.Options()
    options.throwExceptionsWhenWritesNotApplied = true
    return GliaCassandraTemplate(sessionFactory(), cassandraConverter(), options)
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
    return formattedKeyspace
  }

  override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
    return listOf(keySpaceSpecification)
  }

  override fun getKeyspaceDrops(): List<DropKeyspaceSpecification> {
    LOGGER.info("Keyspace {} will be dropped", formattedKeyspace)
    return listOf(DropKeyspaceSpecification.dropKeyspace(formattedKeyspace).ifExists())
  }

  override fun getEntityBasePackages(): Array<String> {
    LOGGER.info("Configured base packages {}", replicationConfig.entityBasePackages)
    var basePackages = replicationConfig.entityBasePackages ?: arrayOf()
    if (replicationConfig.enableDomainEvents) basePackages += DOMAIN_EVENTS_PACKAGE
    LOGGER.info("Using entity base packages {}", basePackages)
    return basePackages
  }

  override fun getStartupScripts(): MutableList<String> {
    return if (!consumerConfig.enabled) mutableListOf(
        """
          CREATE TYPE IF NOT EXISTS $keyspace.$TYPE_AGGREGATE_EVENT (
            eventid uuid,
            payload text,
            routingkey text,
            state text,
            version text
          )
        """.trimIndent(),
        """
          CREATE TABLE IF NOT EXISTS $keyspace.$TBL_DOMAIN_EVENTS (
            aggregateid text,
            sequence bigint,
            active boolean static,
            events list<frozen<$TYPE_AGGREGATE_EVENT>>,
            timestamp timestamp,
            PRIMARY KEY (aggregateid, sequence)
          ) WITH CLUSTERING ORDER BY (sequence DESC)
        """.trimIndent()
    ) else mutableListOf()
  }
}
