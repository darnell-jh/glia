package com.dhenry.glia.cassandra.domain.entities

import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.data.aggregate.BaseAbstractAggregateRoot
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.*
import com.datastax.driver.mapping.annotations.Table as DatastaxTable

const val TBL_DOMAIN_EVENTS = "domainevents"

@Table(value = TBL_DOMAIN_EVENTS)
@DatastaxTable(name = TBL_DOMAIN_EVENTS)
class DomainEvents internal constructor(
    @PrimaryKey override val aggregatePrimaryKey: AggregatePrimaryKey,
    active: Boolean = true,
    events: List<AggregateEvent> = listOf(),
    timestamp: Date = Date.from(Instant.now())
) : BaseAbstractAggregateRoot<DomainEvents, AggregatePrimaryKey>(aggregatePrimaryKey) {

  companion object {
    const val FIELD_AGGREGATE_ID = "aggregateid"
    const val FIELD_SEQUENCE = "sequence"
    const val FIELD_ACTIVE = "active"
    const val FIELD_EVENTS = "events"
  }

  var active: Boolean = active
    internal set

  var events = events
    internal set

  var timestamp: Date = timestamp
    internal set

  operator fun component1() = aggregatePrimaryKey
  operator fun component2() = active
  operator fun component3() = events
  operator fun component4() = timestamp

  internal fun setAggregateRoot(aggregate: BaseAbstractAggregateRoot<*, *>) {
    aggregateRoot = aggregate
  }
}