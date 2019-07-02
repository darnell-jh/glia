package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.cassandra.repository.Query
import java.util.*
import java.util.stream.Stream

interface DomainEventsRepository: CassandraRepository<DomainEvents, AggregatePrimaryKey>, DomainEventsExtRepository {

    @Query("select * from domainevents where aggregateId = ?0")
    fun streamById(aggregateId: String): Stream<DomainEvents>

    @Query("select * from domainevents where aggregateId = ?0 ORDER BY timeUUID ASC")
    fun streamByIdFromOldest(aggregateId: String): Stream<DomainEvents>

    @Query(count = true, value = "select count(*) from domainevents where aggregateId = ?0")
    fun countById(aggregateId: String): Long

    @Query("select * from domainevents where aggregateId = ?0 AND timeUUID = ?1")
    fun findOneById(aggregateId: String, timeUUID: UUID)

}