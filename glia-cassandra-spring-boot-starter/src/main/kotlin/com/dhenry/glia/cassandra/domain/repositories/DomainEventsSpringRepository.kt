package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.repository.CassandraRepository

interface DomainEventsSpringRepository: CassandraRepository<DomainEvents, AggregatePrimaryKey>