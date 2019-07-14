package com.dhenry.glia.cassandra.domain.models

import com.datastax.driver.core.utils.UUIDs
import org.springframework.data.cassandra.core.mapping.UserDefinedType
import java.util.*

const val TYPE_AGGREGATE_EVENT_STATE = "aggregateeventstate"

@UserDefinedType
class AggregateEventState(
    val eventId: UUID = UUIDs.timeBased(),
    var state: EventState = EventState.INIT
)