package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.querybuilder.Batch
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import com.dhenry.glia.cassandra.exceptions.WriteNotAppliedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository
import org.springframework.util.Assert

internal class GliaRepositoryBaseClassImpl(
    metadata: CassandraEntityInformation<DomainEvents, AggregatePrimaryKey>,
    private val operations: CassandraOperations
) : SimpleCassandraRepository<DomainEvents, AggregatePrimaryKey>(metadata, operations), GliaRepositoryBaseClass {

  companion object {
    private val LOGGER: Logger = LoggerFactory.getLogger(GliaRepositoryBaseClassImpl::class.java)
  }

  override fun <S : DomainEvents?> insert(entity: S): S {
    Assert.notNull(entity, "Entity must not be null")
    entity!!.aggregatePrimaryKey.sequence++
    return super.insert(entity)
  }

  override fun <S : DomainEvents?> insert(entities: MutableIterable<S>): MutableList<S> {
    Assert.notNull(entities, "Entity must not be null")

    if (entities.count() == 1) {
      entities.forEach {
        Assert.notNull(it, "Entity must not be null")
        it!!.aggregatePrimaryKey.sequence++
      }
      return super.insert(entities)
    }

    val batches = mutableMapOf<String, Batch>()

    entities.forEach {
      Assert.notNull(it, "Entity must not be null")
      it!!.aggregatePrimaryKey.sequence++
      val insert = QueryBuilder.insertInto(TBL_DOMAIN_EVENTS)
      insert.ifNotExists()
      operations.converter.write(it as Any, insert)
      val batch = batches.getOrPut(it.aggregatePrimaryKey.aggregateId) { QueryBuilder.batch() }
      batch.add(insert)
    }

    var allApplied  = true
    batches.forEach { (key, batch) ->
      val applied = operations.cqlOperations.execute(batch)
      if (!applied) LOGGER.warn("Failed to apply writes for aggregateId {}", key)
      allApplied = allApplied and applied
    }
    if (!allApplied) throw WriteNotAppliedException()
    return entities.toMutableList()
  }

  override fun <S : DomainEvents?> save(entity: S): S {
    Assert.notNull(entity, "Entity must not be null")
    entity!!.aggregatePrimaryKey.sequence++
    return super.save(entity)
  }

  override fun <S : DomainEvents?> saveAll(entities: MutableIterable<S>): MutableList<S> {
    Assert.notNull(entities, "Entity must not be null")

    if (entities.count() == 1) {
      entities.forEach {
        Assert.notNull(it, "Entity must not be null")
        it!!.aggregatePrimaryKey.sequence++
      }
      return super.insert(entities)
    }

    val batches = mutableMapOf<String, Batch>()

    entities.forEach {
      Assert.notNull(it, "Entity must not be null")
      it!!.aggregatePrimaryKey.sequence++
      val insert = QueryBuilder.insertInto(TBL_DOMAIN_EVENTS)
      insert.ifNotExists()
      val toInsert = LinkedHashMap<String, Any>()
      operations.converter.write(it as Any, toInsert,
          operations.converter.mappingContext.getRequiredPersistentEntity(DomainEvents::class.java))
      for (entry in toInsert.entries) {
        insert.value(entry.key, entry.value)
      }
      val batch = batches.getOrPut(it.aggregatePrimaryKey.aggregateId)  { QueryBuilder.batch() }
      batch.add(insert)
    }

    var allApplied  = true
    batches.forEach { (key, batch) ->
      val applied = operations.cqlOperations.execute(batch)
      if (!applied) LOGGER.warn("Failed to apply writes for aggregateId {}", key)
      allApplied = allApplied and applied
    }
    if (!allApplied) throw WriteNotAppliedException()
    return entities.toMutableList()
  }
}