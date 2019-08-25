package com.dhenry.glia.medium

import com.dhenry.glia.Application
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.cassandra.domain.services.CassandraDomainEventsService
import com.dhenry.glia.data.aggregate.AbstractAggregateRoot
import com.dhenry.glia.service.EventMediator
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
import java.time.Instant
import kotlin.test.BeforeTest

@ActiveProfiles("component")
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class, BaseComponentTest.TestConfig::class])
abstract class BaseComponentTest {

    @Autowired
    lateinit var cassandraAdminTemplate: CassandraAdminTemplate

    @Autowired
    lateinit var cassandraSessionFactoryBean: CassandraSessionFactoryBean

    @Autowired
    protected lateinit var objectMapper: ObjectMapper

    @Autowired
    protected lateinit var domainEventsService: CassandraDomainEventsService

    @Autowired
    protected lateinit var cassandraTemplate: CassandraTemplate

    @BeforeTest
    fun setupBase() {
        cassandraAdminTemplate.keyspaceMetadata.tables.onEach {
            cassandraAdminTemplate.dropTable(CqlIdentifier.of(it.name))
        }

        cassandraSessionFactoryBean.afterPropertiesSet()
    }

    class TestAggregate(val id: String = "test")
        : AbstractAggregateRoot<TestAggregate, AggregatePrimaryKey>(AggregatePrimaryKey(id)) {

        var updated: Boolean = false
            private set

        var property: String? = null
            private set

        fun update(property: String) {
            registerEvent(TestEvent(property))
        }

        @EventSourceHandler
        fun on(testEvent: TestEvent) {
            updated = true
            property = testEvent.property
        }

        override fun toString(): String {
            return "TestAggregate(id='$id', updated=$updated, property=$property)"
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as TestAggregate

            if (id != other.id) return false
            if (updated != other.updated) return false
            if (property != other.property) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + updated.hashCode()
            result = 31 * result + (property?.hashCode() ?: 0)
            return result
        }


    }

    @Event("routingKey")
    data class TestEvent(val property: String = "")

    @TestConfiguration
    class TestConfig {
        @Bean
        fun eventMediator(objectMapper: ObjectMapper): EventMediator {
            return object: EventMediator {
                override fun load(routingKey: String, payloadJson: String): Any {
                    return objectMapper.readValue(payloadJson, TestEvent::class.java)
                }

                override fun publish(routingKey: String, payload: Any, aggregate: AbstractAggregateRoot<*, *>,
                                     event: Any, timestamp: Instant) {
                }
            }
        }

    }

}