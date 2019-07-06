package com.dhenry.glia.cassandra.domain.entities

import com.datastax.driver.core.DataType
import com.datastax.driver.core.utils.UUIDs
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

const val TBL_DOMAIN_EVENTS = "domainevents"

@Table(value = TBL_DOMAIN_EVENTS)
data class DomainEvents internal constructor (
        @PrimaryKey override val aggregatePrimaryKey: AggregatePrimaryKey,
        var active: Boolean = true,
        @CassandraType(type = DataType.Name.UDT) var events: List<AggregateEvent> = listOf()
): BaseAbstractAggregateRoot<DomainEvents>(aggregatePrimaryKey) {
  init {
    aggregatePrimaryKey.timeUUID = UUIDs.timeBased()
  }
}