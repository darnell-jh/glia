package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import java.util.stream.Stream

interface DomainEventsRepository: CassandraRepository<DomainEvents, AggregatePrimaryKey>, DomainEventsExtRepository {

    @Query("select * from domainevents where aggregateId = ?0")
    fun streamById(aggregateId: String): Stream<DomainEvents>

}