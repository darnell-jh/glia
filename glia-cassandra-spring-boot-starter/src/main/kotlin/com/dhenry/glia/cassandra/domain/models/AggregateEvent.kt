package com.dhenry.glia.cassandra.domain.models

import com.datastax.driver.core.utils.UUIDs
import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.util.*

const val TYPE_AGGREGATE_EVENT = "aggregateevent"

@UserDefinedType
data class AggregateEvent (
    var routingKey: String,
    var version: String,
    val payload: String,
    val eventId: UUID = UUIDs.timeBased(),
    var state: EventState = EventState.INIT
)