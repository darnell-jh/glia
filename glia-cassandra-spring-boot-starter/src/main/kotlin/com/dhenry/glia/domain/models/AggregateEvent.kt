package com.dhenry.domain.entities

import com.datastax.driver.core.utils.UUIDs
import com.dhenry.glia.domain.models.EventState
import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.util.UUID

@UserDefinedType
data class AggregateEvent (
    var routingKey: String,
    var version: String,
    val payload: String,
    val eventId: UUID = UUIDs.timeBased(),
    var state: EventState = EventState.INIT
)