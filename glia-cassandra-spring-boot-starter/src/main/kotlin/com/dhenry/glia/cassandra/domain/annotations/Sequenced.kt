package com.dhenry.glia.cassandra.domain.annotations

@MustBeDocumented
@Target(allowedTargets = [AnnotationTarget.CLASS])
annotation class Sequenced(
  val counterTable: String = "",
  val sequenceId: String = "",
  val sequence: String = ""
)