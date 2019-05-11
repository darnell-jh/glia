package com.dhenry.glia.cassandra.domain

import com.dhenry.glia.Application
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.config.CassandraConfig
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.dhenry.glia.cassandra.domain.services.DomainEventsService
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.core.cql.CqlIdentifier
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import kotlin.test.BeforeTest

@ActiveProfiles("component")
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class, BaseComponentTest.TestConfig::class])
abstract class BaseComponentTest {

    @Autowired
    lateinit var cassandraAdminTemplate: CassandraAdminTemplate

    @Autowired
    lateinit var cassandraConfig: CassandraConfig

    @Autowired
    lateinit var cassandraSessionFactoryBean: CassandraSessionFactoryBean

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var domainEventsService: DomainEventsService

    @Autowired
    protected lateinit var cassandraTemplate: CassandraTemplate

    @BeforeTest
    fun setupBase() {
        cassandraAdminTemplate.keyspaceMetadata.tables.onEach {
            cassandraAdminTemplate.dropTable(CqlIdentifier.of(it.name))
        }

        cassandraSessionFactoryBean.afterPropertiesSet()
        cassandraConfig.postInit()
    }

    data class TestAggregate(val id: String = "test", var updated: Boolean = false)
        : AbstractAggregateRoot<TestAggregate>(AggregatePrimaryKey(id)) {

        fun update() {
            registerEvent(TestEvent())
        }

        @EventSourceHandler
        fun on(testEvent: TestEvent) {
            updated = true
        }

    }

    @Event("routingKey")
    data class TestEvent(val data: String = "data")

    @TestConfiguration
    class TestConfig {
        @Bean
        fun objectMapper(): ObjectMapper {
            return ObjectMapper()
        }

        @Bean
        fun domainEventService(repository: DomainEventsRepository, objectMapper: ObjectMapper): DomainEventsService {
            return object: DomainEventsService(repository, objectMapper) {
                override fun loadEvent(event: AggregateEvent): Any {
                    return objectMapper.readValue(event.payload, TestEvent::class.java)
                }

                override fun doPublish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*>,
                                       event: AggregateEvent) {

                }
            }
        }
    }

}