package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.repository.Query
import java.util.*
import java.util.stream.Stream

interface DomainEventsRepository: GliaRepositoryBaseClass, DomainEventsExtRepository {

    @Query("select * from domainevents where aggregateId = ?0")
    fun streamById(aggregateId: String): Stream<DomainEvents>

    @Query("select * from domainevents where aggregateId = ?0 ORDER BY sequence ASC")
    fun streamByIdFromOldest(aggregateId: String): Stream<DomainEvents>

    @Query(count = true, value = "select count(*) from domainevents where aggregateId = ?0")
    fun countById(aggregateId: String): Long

    @Query("select * from domainevents where aggregateId = ?0 AND sequence = ?1")
    fun findOneById(aggregateId: String, sequence: Long): DomainEvents

}