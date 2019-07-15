package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.repository.CassandraRepository
import org.springframework.data.repository.NoRepositoryBean

@NoRepositoryBean
interface GliaRepositoryBaseClass: CassandraRepository<DomainEvents, AggregatePrimaryKey>