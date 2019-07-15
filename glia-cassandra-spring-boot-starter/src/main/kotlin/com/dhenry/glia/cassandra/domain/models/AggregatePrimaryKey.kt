package com.dhenry.glia.cassandra.domain.models

import org.springframework.data.annotation.PersistenceConstructor
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*
import javax.validation.constraints.NotBlank

@PrimaryKeyClass
class AggregatePrimaryKey constructor(
    @NotBlank aggregateId: String = UUID.randomUUID().toString()
) {

    @PrimaryKeyColumn(ordinal = 0, type = PrimaryKeyType.PARTITIONED)
    var aggregateId: String = UUID.randomUUID().toString()
        internal set

    @PrimaryKeyColumn(ordinal = 1, type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    var sequence: Long = -1
        internal set

    init {
        this.aggregateId = aggregateId
    }

    operator fun component1() = aggregateId
    operator fun component2() = sequence
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AggregatePrimaryKey

        if (aggregateId != other.aggregateId) return false
        if (sequence != other.sequence) return false

        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + sequence.hashCode()
        return result
    }

    @PersistenceConstructor
    constructor(aggregateId: String, sequence: Long): this(aggregateId) {
        this.sequence = sequence
    }

}