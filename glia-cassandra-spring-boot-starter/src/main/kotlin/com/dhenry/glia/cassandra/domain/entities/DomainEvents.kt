package com.dhenry.glia.cassandra.domain.entities

import com.datastax.driver.core.DataType
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table
import java.time.Instant
import java.util.*

const val TBL_DOMAIN_EVENTS = "domainevents"

@Table(value = TBL_DOMAIN_EVENTS)
class DomainEvents internal constructor (
    @PrimaryKey override val aggregatePrimaryKey: AggregatePrimaryKey,
    active: Boolean = true,
    events: List<AggregateEvent> = listOf(),
    timestamp: Date = Date.from(Instant.now())
): BaseAbstractAggregateRoot<DomainEvents>(aggregatePrimaryKey) {

  var active: Boolean = true
    internal set

  @CassandraType(type = DataType.Name.UDT) var events: List<AggregateEvent> = listOf()
    internal set

  var timestamp: Date = Date.from(Instant.now())
    internal set

  operator fun component1() = aggregatePrimaryKey
  operator fun component2() = active
  operator fun component3() = events
  operator fun component4() = timestamp

  companion object {
    const val FIELD_AGGREGATE_ID = "aggregateid"
    const val FIELD_SEQUENCE = "sequence"
    const val FIELD_ACTIVE = "active"
    const val FIELD_EVENTS = "events"
  }

  init {
    this.active = active
    this.events = events
    this.timestamp = timestamp
  }

  internal fun setAggregateRoot(aggregate: BaseAbstractAggregateRoot<*>) {
    aggregateRoot = aggregate
  }
}