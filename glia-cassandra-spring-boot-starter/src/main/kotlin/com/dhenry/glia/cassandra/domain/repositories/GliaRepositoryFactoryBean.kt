package com.dhenry.glia.cassandra.domain.repositories

import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.CassandraTemplate
import org.springframework.data.cassandra.repository.support.CassandraRepositoryFactoryBean
import org.springframework.data.repository.Repository
import org.springframework.data.repository.core.support.RepositoryFactorySupport

class GliaRepositoryFactoryBean<T : Repository<S, ID>, S, ID>(repositoryInterface: Class<T>)
  : CassandraRepositoryFactoryBean<T, S, ID>(repositoryInterface) {

  private lateinit var operations: CassandraOperations

  override fun createRepositoryFactory(): RepositoryFactorySupport {
    return GliaRepositoryFactory(operations)
  }

  override fun setCassandraTemplate(cassandraTemplate: CassandraTemplate) {
    super.setCassandraTemplate(cassandraTemplate)
    this.operations = cassandraTemplate
  }
}