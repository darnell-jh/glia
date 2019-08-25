package com.dhenry.glia.data.models

interface IAggregatePrimaryKey {

  var aggregateId: String
  var sequence: Long

  operator fun component1() = aggregateId
  operator fun component2() = sequence

}