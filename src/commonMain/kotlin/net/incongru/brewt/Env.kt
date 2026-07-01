package net.incongru.brewt

expect fun makeEnv(): Env

/**
 * Tiny wrapper around some environment variables/values we need
 */
data class Env(
    val selfBinaryPath: String,
    val userId: String,
    val userName: String,
    val userHome: String,
)
