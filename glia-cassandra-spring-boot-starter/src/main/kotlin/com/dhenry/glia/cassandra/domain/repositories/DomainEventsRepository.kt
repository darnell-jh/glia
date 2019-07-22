package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.querybuilder.Batch
import com.datastax.driver.core.querybuilder.QueryBuilder
import com.datastax.driver.mapping.MappingManager
import com.dhenry.glia.cassandra.domain.aggregate.BaseAbstractAggregateRoot
import com.dhenry.glia.cassandra.domain.entities.AggregateEventState
import com.dhenry.glia.cassandra.domain.entities.DomainEvents
import com.dhenry.glia.cassandra.domain.entities.TBL_DOMAIN_EVENTS
import com.dhenry.glia.cassandra.domain.models.AggregateEvent
import com.dhenry.glia.cassandra.domain.models.EventState
import com.dhenry.glia.cassandra.domain.models.EventStatePrimaryKey
import com.dhenry.glia.cassandra.exceptions.WriteNotAppliedException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.data.cassandra.core.CassandraOperations
import org.springframework.stereotype.Repository
import org.springframework.util.Assert
import java.util.stream.Stream
import java.util.stream.StreamSupport

@Repository
class DomainEventsRepository(
    private val aggregateEventStateRepository: AggregateEventStateRepository,
    private val operations: CassandraOperations,
    private val mappingManager: MappingManager,
    private val domainEventsRepo: DomainEventsSpringRepository
) {

    companion object {
        private val LOGGER: Logger = LoggerFactory.getLogger(DomainEventsRepository::class.java)
    }

    private val domainEventsAccessor by lazy {
        mappingManager.createAccessor(DomainEventsAccessor::class.java)
    }

    fun prependEvent(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent) {
        Assert.notNull(aggregateRoot, "Aggregate must not be null")
        Assert.notNull(event, "Event must not be null")

        val (aggregateId, sequence) = aggregateRoot.aggregatePrimaryKey
        val eventInCql = operations.converter.convertToColumnType(event)

        val update = QueryBuilder.update(TBL_DOMAIN_EVENTS)
            .where(QueryBuilder.eq(DomainEvents.FIELD_AGGREGATE_ID, aggregateId))
            .and(QueryBuilder.eq(DomainEvents.FIELD_SEQUENCE, sequence))
            .with(QueryBuilder.prepend(DomainEvents.FIELD_EVENTS, eventInCql))
        operations.cqlOperations.execute(update)
    }

    fun updateEventState(aggregateRoot: BaseAbstractAggregateRoot<*>, event: AggregateEvent,
                                  eventState: EventState) {
        Assert.notNull(aggregateRoot, "Aggregate must not be null")
        Assert.notNull(event, "Event must not be null")
        Assert.notNull(eventState, "Event state must not be null")

        aggregateEventStateRepository.save(AggregateEventState(
            EventStatePrimaryKey(aggregateRoot.aggregatePrimaryKey.aggregateId, event.eventId),
            eventState
        ))
    }

    fun <S : DomainEvents?> insert(entity: S): S {
        Assert.notNull(entity, "Entity must not be null")
        entity!!.aggregatePrimaryKey.sequence++
        return domainEventsRepo.insert(entity)
    }

    fun <S : DomainEvents?> insert(entities: MutableIterable<S>): MutableList<S> {
        Assert.notNull(entities, "Entity must not be null")

        if (entities.count() == 1) {
            entities.forEach {
                Assert.notNull(it, "Entity must not be null")
                it!!.aggregatePrimaryKey.sequence++
            }
            return domainEventsRepo.insert(entities)
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

    fun <S : DomainEvents?> save(entity: S): S {
        Assert.notNull(entity, "Entity must not be null")
        entity!!.aggregatePrimaryKey.sequence++
        return domainEventsRepo.save(entity)
    }

    fun <S : DomainEvents?> saveAll(entities: MutableIterable<S>): MutableList<S> {
        Assert.notNull(entities, "Entity must not be null")

        if (entities.count() == 1) {
            entities.forEach {
                Assert.notNull(it, "Entity must not be null")
                it!!.aggregatePrimaryKey.sequence++
            }
            return domainEventsRepo.insert(entities)
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

    fun streamById(aggregateId: String): Stream<DomainEvents> {
        val spliterator = domainEventsAccessor.streamById(aggregateId)
            .map { operations.converter.read(DomainEvents::class.java, it) }
            .spliterator()
        return StreamSupport.stream(spliterator, false)
    }

    fun streamByIdFromOldest(aggregateId: String): Stream<DomainEvents> {
        val spliterator = domainEventsAccessor.streamByIdFromOldest(aggregateId)
            .map { operations.converter.read(DomainEvents::class.java, it) }
            .spliterator()
        return StreamSupport.stream(spliterator, false)
    }

    fun countById(aggregateId: String): Long {
        return domainEventsAccessor.countById(aggregateId).one().getLong(0)
    }

    fun findOneById(aggregateId: String, sequence: Long): DomainEvents {
        return operations.converter.read(
            DomainEvents::class.java,
            domainEventsAccessor.findOneById(aggregateId, sequence).one()
        )
    }
}