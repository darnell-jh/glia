package com.dhenry.glia.cassandra.domain.entities

import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.models.EventStatePrimaryKey
import org.springframework.data.cassandra.core.mapping.PrimaryKey
import org.springframework.data.cassandra.core.mapping.Table

const val TBL_AGGREGATE_EVENT_STATE = "aggregateeventstate"

@Table(value = TBL_AGGREGATE_EVENT_STATE)
data class AggregateEventState internal constructor(
    @PrimaryKey val eventStatePrimaryKey: EventStatePrimaryKey,
    val state: EventState = EventState.INIT
) {
  companion object {
    const val FIELD_AGGREGATE_ID = "aggregateid"
    const val FIELD_EVENT_ID = "eventid"
    const val FIELD_STATE = "state"
  }
}