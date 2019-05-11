package com.dhenry.glia.unit

import com.dhenry.glia.annotations.EventSourceHandler
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.cassandra.domain.services.AggregationService
import kotlin.test.Test
import kotlin.test.assertEquals

class AggregationServiceTest: BaseUnitTest() {

    class TestAggregate: AbstractAggregateRoot<TestAggregate>(AggregatePrimaryKey("id")) {

        lateinit var event: String

        @EventSourceHandler
        fun handler(event: String) {
            this.event = event
        }
    }

    @Test
    fun shouldReturnEventGiven() {
        val aggregate = TestAggregate()
        val event = "event"
        AggregationService.invokeEventListeners(aggregate, event)
        assertEquals(event, aggregate.event)
    }

    @Test(expected = UninitializedPropertyAccessException::class)
    fun shouldFailAsEventTypeDoesNotMatchHandler() {
        val aggregate = TestAggregate()
        val event = 1
        AggregationService.invokeEventListeners(aggregate, event)
        aggregate.event
    }

}