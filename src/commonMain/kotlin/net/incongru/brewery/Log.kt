package net.incongru.brewery

import kotlin.time.Clock
import kotlin.time.ExperimentalTime

typealias Logger = (String) -> Unit

@OptIn(ExperimentalTime::class)
 val log: (String) -> Unit = { msg ->
    println("${Clock.System.now()} $msg")
}