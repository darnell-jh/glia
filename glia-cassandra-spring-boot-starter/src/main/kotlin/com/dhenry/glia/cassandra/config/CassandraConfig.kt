package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.Cluster
import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import java.util.*
import kotlin.collections.ArrayList

private const val DOMAIN_EVENTS_PACKAGE = "com.dhenry.glia.cassandra.domain"

@Configuration
@ConditionalOnMissingBean(Cluster::class)
class CassandraConfig(
    @Value("\${spring.data.cassandra.keyspace-name}") val keyspace: String,
    @Value("\${glia.entity-base-packages:#{null}}") var entityPackages: Array<String>?
) : AbstractCassandraConfiguration() {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(CassandraConfig::class.java)
    }

    // Below method creates keyspace if it does not exist.
    private val keySpaceSpecification: CreateKeyspaceSpecification
        get() {
            LOGGER.info("Creating keyspace {}", keyspace)
            return CreateKeyspaceSpecification.createKeyspace(keyspace).ifNotExists()
        }

    @Bean
    override fun cluster(): CassandraCqlClusterFactoryBean {
        val bean = CassandraCqlClusterFactoryBean()
        bean.setJmxReportingEnabled(false)
        bean.keyspaceCreations = keyspaceCreations
        bean.setReconnectionPolicy(ExponentialReconnectionPolicy(1000, 8000))
        return bean
    }

    override fun getSchemaAction(): SchemaAction {
        return SchemaAction.CREATE_IF_NOT_EXISTS
    }

    override fun getKeyspaceName(): String {
        return keyspace
    }

    override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
        val createKeyspaceSpecifications = ArrayList<CreateKeyspaceSpecification>()
        createKeyspaceSpecifications.add(keySpaceSpecification)
        return createKeyspaceSpecifications
    }

    override fun getEntityBasePackages(): Array<String> {
        LOGGER.info("Using entity base packages {}", entityPackages)
        return Optional.ofNullable(entityPackages)
            .map{ it.plus(DOMAIN_EVENTS_PACKAGE) }
            .orElse(arrayOf(DOMAIN_EVENTS_PACKAGE))
    }

}