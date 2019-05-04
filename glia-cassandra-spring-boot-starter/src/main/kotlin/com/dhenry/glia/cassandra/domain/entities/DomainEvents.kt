package com.dhenry.glia.cassandra.domain.entities

import com.datastax.driver.core.DataType
import com.dhenry.glia.cassandra.domain.aggregate.AbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.core.mapping.CassandraType
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

const val TBL_DOMAIN_EVENTS = "domainevents"

@Table(value = TBL_DOMAIN_EVENTS)
data class DomainEvents(
        @PrimaryKey val aggregatePrimaryKey: AggregatePrimaryKey,
        var active: Boolean = true,
        @CassandraType(type = DataType.Name.UDT) var events: List<AggregateEvent> = listOf()
): AbstractAggregateRoot<DomainEvents>(aggregatePrimaryKey)