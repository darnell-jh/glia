package com.dhenry.glia.cassandra.domain.repositories

import java.time.Instant

interface OutOfOrderRepository<T> {

    fun saveLatest(entity: T, timestamp: Instant)

}