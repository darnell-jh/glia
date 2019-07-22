package com.dhenry.glia.integration

import com.dhenry.glia.Application
import com.dhenry.glia.annotations.Event
import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.cassandra.domain.repositories.DomainEventsRepository
import com.dhenry.glia.service.DomainEventsService
import com.dhenry.glia.test.ProducerEventListener
import com.dhenry.glia.test.autoconfigure.amqp.rabbit.AutoConfigureRabbitProducerListener
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.cassandra.config.CassandraSessionFactoryBean
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.cql.CqlIdentifier
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.junit4.SpringRunner
import kotlin.streams.toList
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertNotNull

@ActiveProfiles("integration")
@RunWith(SpringRunner::class)
@SpringBootTest(classes = [Application::class])
@AutoConfigureRabbitProducerListener(producerEventPackages = ["com.dhenry.glia.integration"])
class CassandraRabbitIntegrationTest {

  @Event(routingKey = "test.event")
  data class TestEvent(val property: String = "")

  data class TestAggregate(var id: String = "", var property: String = "")
    : AbstractAggregateRoot<TestAggregate>(AggregatePrimaryKey(id)) {

    fun updateProperty(property: String) {
      registerEvent(TestEvent(property))
    }

    @EventSourceHandler
    fun on(testEvent: TestEvent) {
      id = aggregatePrimaryKey.aggregateId
      property = property.ifEmpty { testEvent.property }
    }
  }

  @Autowired
  private lateinit var domainEventsService: DomainEventsService

  @Autowired
  private lateinit var domainEventsRepository: DomainEventsRepository

  @Autowired
  private lateinit var producerEventListener: ProducerEventListener

  @Autowired
  private lateinit var cassandraAdminTemplate: CassandraAdminTemplate

  @Autowired
  private lateinit var cassandraSessionFactoryBean: CassandraSessionFactoryBean

  @BeforeTest
  fun setupBase() {
    cassandraAdminTemplate.keyspaceMetadata.tables.onEach {
      cassandraAdminTemplate.dropTable(CqlIdentifier.of(it.name))
    }

    producerEventListener.clearMessages()
    cassandraSessionFactoryBean.afterPropertiesSet()
  }

  @Test
  fun savingAggregateShouldSaveToCassandraEmitEventsToRabbitAndStoreStatuses() {
    // Arrange
    val property = "PROP"
    val id = "ID"
    val aggregate = TestAggregate(id)
    aggregate.updateProperty(property)

    // Act
    domainEventsService.save(aggregate)

    // Assert
    await.untilAsserted {
      val (aggregateId, sequence) = aggregate.aggregatePrimaryKey
      assertNotNull("aggregate should exist") {
        domainEventsRepository.findOneById(aggregateId, sequence)
      }

      assertThat(aggregate.property).isEqualTo(property)
      assertThat(aggregate.id).isEqualTo(id)

      producerEventListener.hasMessages(1)
      assertThat(producerEventListener.getPayloads<TestEvent>()).contains(TestEvent(property))
    }
  }

  @Test
  fun loadingAggregateShouldLoadAllEventsToAggregate() {
    // Arrange
    val property = "PROP"
    val id = "ID"
    val aggregate = TestAggregate(id)
    aggregate.updateProperty(property)
    aggregate.updateProperty(property + "1")
    aggregate.updateProperty(property + "2")

    val expectedAggregate = aggregate.copy(property = property + "2")

    // Act
    domainEventsService.save(aggregate)

    // Assert
    await.untilAsserted {
      val (aggregateId, _) = aggregate.aggregatePrimaryKey
      assertThat(domainEventsRepository.countById(aggregateId)).isEqualTo(1)
      val events = domainEventsRepository.streamById(aggregateId)
          .flatMap { record -> record.events.stream() }
          .toList()
      assertThat(events).hasSize(3)
    }

    val actualAggregate = domainEventsService.loadAggregate(
        aggregate.aggregatePrimaryKey.aggregateId,
        TestAggregate::class
    )
    assertThat(actualAggregate).isEqualTo(expectedAggregate)
  }
}