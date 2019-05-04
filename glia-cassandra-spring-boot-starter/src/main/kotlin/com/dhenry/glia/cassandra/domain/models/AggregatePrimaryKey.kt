package com.dhenry.glia.cassandra.domain.models

import com.datastax.driver.core.utils.UUIDs
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*

@PrimaryKeyClass
data class AggregatePrimaryKey constructor(
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val aggregateId: String,

    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    val timeUUID: UUID = UUIDs.timeBased()
)