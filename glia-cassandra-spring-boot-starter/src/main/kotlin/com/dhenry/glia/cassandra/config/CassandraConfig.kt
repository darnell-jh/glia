package com.dhenry.glia.cassandra.config

import com.datastax.driver.core.policies.ExponentialReconnectionPolicy
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.sun.xml.internal.ws.spi.db.BindingContextFactory.LOGGER
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration
import org.springframework.data.cassandra.config.CassandraCqlClusterFactoryBean
import org.springframework.data.cassandra.config.SchemaAction
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.cql.CqlIdentifier
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import javax.annotation.PostConstruct

@Configuration
class CassandraConfig(
    @Value("\${spring.data.cassandra.keyspace-name}") val keyspace: String
) : AbstractCassandraConfiguration() {

    @Autowired
    private lateinit var cassandraAdminTemplate: CassandraAdminTemplate

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

    override fun getKeyspaceCreations(): List<CreateKeyspaceSpecification> {
        val createKeyspaceSpecifications = ArrayList<CreateKeyspaceSpecification>()
        createKeyspaceSpecifications.add(keySpaceSpecification)
        return createKeyspaceSpecifications
    }

    override fun getEntityBasePackages(): Array<String> {
        return arrayOf("com.dhenry.glia.cassandra.domain")
    }

    @PostConstruct
    fun postInit() {
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