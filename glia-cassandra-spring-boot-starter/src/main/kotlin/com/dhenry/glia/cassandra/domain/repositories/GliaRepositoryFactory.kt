package com.dhenry.glia.cassandra.domain.repositories

import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactory
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository
import org.springframework.data.repository.core.RepositoryMetadata

class GliaRepositoryFactory(operations: CassandraOperations): CassandraRepositoryFactory(operations) {

  override fun getRepositoryBaseClass(metadata: RepositoryMetadata): Class<*> {
    return when (metadata.domainType) {
      DomainEvents::class.java -> GliaRepositoryBaseClassImpl::class.java
      else -> SimpleCassandraRepository::class.java
    }
  }
}