package com.dhenry.glia.cassandra.domain.repositories


import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import org.springframework.util.Assert
import java.time.Instant

class OutOfOrderRepositoryImpl<T> (
    private val cassandraOperations: CassandraOperations
) : OutOfOrderRepository<T> {

    override fun saveLatest(entity: T, timestamp: Instant) {
        Assert.notNull(entity, "Entity must not be null")

        val updateOptions = UpdateOptions.builder().timestamp(timestamp).build()
        cassandraOperations.update(entity, updateOptions)
    }

}