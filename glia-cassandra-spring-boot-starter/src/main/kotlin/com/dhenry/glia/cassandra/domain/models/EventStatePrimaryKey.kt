package com.dhenry.glia.cassandra.domain.models

import com.datastax.driver.core.utils.UUIDs
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*
import javax.validation.constraints.NotBlank

@PrimaryKeyClass
data class EventStatePrimaryKey constructor(
    @NotBlank
    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    val aggregateId: String = "",

    @NotBlank
    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED)
    val eventId: UUID = UUIDs.timeBased()
)