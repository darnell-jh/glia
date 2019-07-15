package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.AggregateEventState
import com.dhenry.glia.cassandra.domain.models.EventStatePrimaryKey
import org.springframework.data.cassandra.repository.CassandraRepository

interface AggregateEventStateRepository: CassandraRepository<AggregateEventState, EventStatePrimaryKey>