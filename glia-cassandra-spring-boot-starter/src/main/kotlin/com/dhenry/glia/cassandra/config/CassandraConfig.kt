package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification

@Configuration
class CassandraConfig(
    @Value("\${spring.data.cassandra.keyspace-name}") val keyspace: String,
    @Value("\${entityBasePacakges:}") val basePackages: Array<String>
) : AbstractCassandraConfiguration() {

    // Below method creates keyspace if it does not exist.
    private val keySpaceSpecification: CreateKeyspaceSpecification
        get() {
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

    override fun getEntityBasePackages(): Array<String> {
        return basePackages
    }

    override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
        val createKeyspaceSpecifications = ArrayList<CreateKeyspaceSpecification>()
        createKeyspaceSpecifications.add(keySpaceSpecification)
        return createKeyspaceSpecifications
    }

}