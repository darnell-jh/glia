package com.dhenry.glia.cassandra.domain.template

import com.datastax.driver.core.Session
import com.dhenry.glia.cassandra.exceptions.WriteNotAppliedException
import org.springframework.data.cassandra.SessionFactory
import org.springframework.data.cassandra.core.CassandraAdminTemplate
import org.springframework.data.cassandra.core.EntityWriteResult
import org.springframework.data.cassandra.core.InsertOptions
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.data.cassandra.core.WriteResult
import org.springframework.data.cassandra.core.convert.CassandraConverter
import org.springframework.data.cassandra.core.cql.QueryOptions
import org.springframework.data.cassandra.core.cql.session.DefaultSessionFactory

class GliaCassandraTemplate(sessionFactory: SessionFactory, converter: CassandraConverter,
                            private val options: Options?):
    CassandraAdminTemplate(sessionFactory, converter) {

  constructor(session: Session, converter: CassandraConverter, options: Options?)
      : this(DefaultSessionFactory(session), converter, options)

  override fun <T : Any?> insert(entity: T, options: InsertOptions): EntityWriteResult<T> {
    val result: EntityWriteResult<T> = super.insert(entity, options)
    if (!result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  override fun <T : Any?> update(entity: T, options: UpdateOptions): EntityWriteResult<T> {
    val result: EntityWriteResult<T> = super.update(entity, options)
    if (this.options?.throwExceptionsWhenWritesNotApplied == true && !result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  override fun delete(entity: Any, options: QueryOptions): WriteResult {
    val result: WriteResult = super.delete(entity, options)
    if (this.options?.throwExceptionsWhenWritesNotApplied == true && !result.wasApplied()) throw WriteNotAppliedException()
    return result
  }

  class Options {
    var throwExceptionsWhenWritesNotApplied: Boolean = false
  }
}