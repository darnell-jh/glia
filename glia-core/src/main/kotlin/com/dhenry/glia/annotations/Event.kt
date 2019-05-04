package com.dhenry.domain.annotations

@Target(AnnotationTarget.CLASS)
@MustBeDocumented
annotation class Event(val routingKey: String, val version: String = "1")