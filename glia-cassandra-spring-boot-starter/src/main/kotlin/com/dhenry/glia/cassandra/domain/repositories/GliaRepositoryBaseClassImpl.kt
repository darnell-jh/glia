package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.querybuilder.Insert
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.core.querybuilder.Update
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.models.AggregatePrimaryKey
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.mapping.event.BeforeSaveEvent
import org.springframework.data.cassandra.repository.query.CassandraEntityInformation
import org.springframework.data.cassandra.repository.support.SimpleCassandraRepository
import java.time.Instant
import java.time.temporal.ChronoUnit

internal class GliaRepositoryBaseClassImpl(
    metadata: CassandraEntityInformation<DomainEvents, AggregatePrimaryKey>,
    operations: CassandraOperations
) : SimpleCassandraRepository<DomainEvents, AggregatePrimaryKey>(metadata, operations), GliaRepositoryBaseClass {

  override fun onApplicationEvent(beforeSaveEvent: BeforeSaveEvent<*>) {
    val microseconds = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now())
    val timestamp = QueryBuilder.timestamp(microseconds)
    with (beforeSaveEvent) {
      when(statement) {
        is Insert -> {
          val insert = statement as Insert
          insert.using(timestamp)
        }
        is Update -> {
          val update = statement as Update
          update.using(timestamp)
        }
        else -> {}
      }
    }
  }

  override fun <S : DomainEvents?> insert(entity: S): S {
    if (entity != null) {
      entity.aggregatePrimaryKey.sequence++
    }
    return super.insert(entity)
  }

  override fun <S : DomainEvents?> insert(entities: MutableIterable<S>): MutableList<S> {
    entities.onEach { if (it != null) {
      it.aggregatePrimaryKey.sequence++
    } }
    return super.insert(entities)
  }

  override fun <S : DomainEvents?> save(entity: S): S {
    if (entity != null) {
      entity.aggregatePrimaryKey.sequence++
    }
    return super.save(entity)
  }

  override fun <S : DomainEvents?> saveAll(entities: MutableIterable<S>): MutableList<S> {
    entities.onEach { if (it != null) {
      it.aggregatePrimaryKey.sequence++
    } }
    return super.saveAll(entities)
  }
}