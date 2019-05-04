package com.dhenry.glia.cassandra.domain.repositories


import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.data.cassandra.core.UpdateOptions
import java.time.Instant

class OutOfOrderRepositoryImpl<T> (
    private val cassandraOperations: CassandraOperations
) : OutOfOrderRepository<T> {

    override fun saveLatest(entity: T, timestamp: Instant) {
        val updateOptions = UpdateOptions.builder().timestamp(timestamp).build()
        cassandraOperations.update(entity, updateOptions)
    }

}