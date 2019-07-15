package com.dhenry.glia.cassandra.domain.template

import com.datastax.driver.core.Session
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.exceptions.WriteNotAppliedException
import org.springframework.data.cassandra.SessionFactory
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.convert.CassandraConverter
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.cql.WriteOptions
import org.springframework.data.cassandra.core.cql.session.DefaultSessionFactory

class GliaCassandraTemplate(sessionFactory: SessionFactory, converter: CassandraConverter,
                            private val options: Options?):
    CassandraAdminTemplate(sessionFactory, converter) {

  companion object {
    private val EVENT_CLASSES = listOf<Any>(DomainEvents::class)
  }

  constructor(session: Session, converter: CassandraConverter, options: Options?)
      : this(DefaultSessionFactory(session), converter, options)

  override fun <T : Any> insert(entity: T, options: InsertOptions): EntityWriteResult<T> {
    addWriteOptions(entity, options)
    val result: EntityWriteResult<T> = super.insert(entity, options)
    if (isThrowable(entity) && !result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  override fun <T : Any> update(entity: T, options: UpdateOptions): EntityWriteResult<T> {
    val result: EntityWriteResult<T> = super.update(entity, options)
    if (isThrowable(entity) && !result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  override fun delete(entity: Any, options: QueryOptions): WriteResult {
    val result: WriteResult = super.delete(entity, options)
    if (isThrowable(entity) && !result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  private fun <T : Any> isThrowable(entity: T): Boolean {
    return EVENT_CLASSES.contains(entity::class) && this.options?.throwExceptionsWhenWritesNotApplied == true
  }

  private fun <T : Any> addWriteOptions(entity: T, options: WriteOptions) {
    if (!EVENT_CLASSES.contains(entity::class)) return
    when(options) {
      is InsertOptions -> options.mutate().ifNotExists(true)
    }
  }

  class Options {
    var throwExceptionsWhenWritesNotApplied: Boolean = false
  }
}