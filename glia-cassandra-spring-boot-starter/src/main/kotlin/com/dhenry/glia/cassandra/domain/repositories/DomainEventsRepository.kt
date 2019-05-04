package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.domain.entities.AggregatePrimaryKey
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import java.util.stream.Stream

interface DomainEventsRepository: CassandraRepository<DomainEvents, AggregatePrimaryKey> {

    @Query("select * from domainevents where aggregateId = ?0")
    fun streamById(aggregateId: String): Stream<DomainEvents>

}