package com.dhenry.glia.cassandra.domain.models

import org.springframework.data.annotation.PersistenceConstructor
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*
import javax.validation.constraints.NotBlank

@PrimaryKeyClass
class AggregatePrimaryKey constructor(
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    @NotBlank
    var aggregateId: String = UUID.randomUUID().toString()
) {

    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    lateinit var timeUUID: UUID

    operator fun component1() = aggregateId
    operator fun component2() = timeUUID

    @PersistenceConstructor
    constructor(aggregateId: String, timeUUID: UUID): this(aggregateId) {
        this.timeUUID = timeUUID
    }

}