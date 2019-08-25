package com.dhenry.glia.cassandra.domain.models

import com.dhenry.glia.data.models.IAggregatePrimaryKey
import org.springframework.data.cassandra.core.cql.Ordering
import org.springframework.data.cassandra.core.cql.PrimaryKeyType
import org.springframework.data.cassandra.core.mapping.PrimaryKeyClass
import org.springframework.data.cassandra.core.mapping.PrimaryKeyColumn
import java.util.*

@PrimaryKeyClass
class AggregatePrimaryKey(

    @PrimaryKeyColumn(type = PrimaryKeyType.PARTITIONED)
    override var aggregateId: String = UUID.randomUUID().toString(),

    @PrimaryKeyColumn(type = PrimaryKeyType.CLUSTERED, ordering = Ordering.DESCENDING)
    override var sequence: Long = -1

): IAggregatePrimaryKey {



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

}