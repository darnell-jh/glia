package com.dhenry.glia.cassandra.domain.repositories

import com.datastax.driver.core.ResultSet
import com.datastax.driver.mapping.annotations.Accessor
import com.datastax.driver.mapping.annotations.Query

@Accessor
interface DomainEventsAccessor {

  @Query("select * from domainevents where aggregateId = ?")
  fun streamById(aggregateId: String): ResultSet

  @Query("select * from domainevents where aggregateId = ? ORDER BY sequence ASC")
  fun streamByIdFromOldest(aggregateId: String): ResultSet

  @Query("select count(*) from domainevents where aggregateId = ?")
  fun countById(aggregateId: String): ResultSet

  @Query("select * from domainevents where aggregateId = ? AND sequence = ?")
  fun findOneById(aggregateId: String, sequence: Long): ResultSet
}