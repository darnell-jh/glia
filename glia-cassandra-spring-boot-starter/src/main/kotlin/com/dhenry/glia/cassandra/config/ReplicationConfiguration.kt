package com.dhenry.glia.cassandra.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.data.cassandra.core.cql.keyspace.CreateKeyspaceSpecification
import org.springframework.data.cassandra.core.cql.keyspace.DataCenterReplication
import org.springframework.data.cassandra.core.cql.keyspace.KeyspaceOption

@ConfigurationProperties("glia.cassandra")
class ReplicationConfiguration(
    var entityBasePackages: Array<String>? = null,
    var replication: ReplicationProps? = null,
    var recreateKeyspace: Boolean = false,
    var enableDomainEvents: Boolean = true
) {

  fun configureKeyspaceCreation(keyspaceSpec: CreateKeyspaceSpecification) {
    when (replication?.strategy) {
      KeyspaceOption.ReplicationStrategy.SIMPLE_STRATEGY ->
        keyspaceSpec.withSimpleReplication(replication?.replicationFactor ?: 1)
      KeyspaceOption.ReplicationStrategy.NETWORK_TOPOLOGY_STRATEGY -> {
        val dataCenterReplications = replication?.dataCenters
            ?.map { DataCenterReplication.of(it.key, it.value) }
            ?.toTypedArray() ?: arrayOf()
        keyspaceSpec.withNetworkReplication(*dataCenterReplications)
      }
      else -> keyspaceSpec.withSimpleReplication()
    }
  }
}

class ReplicationProps {
  var strategy: KeyspaceOption.ReplicationStrategy? = null
  var replicationFactor: Long = 1
  lateinit var dataCenters: Map<String, Long>
}

