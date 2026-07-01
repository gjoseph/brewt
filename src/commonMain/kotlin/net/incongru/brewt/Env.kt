package net.incongru.brewt

/**
 * Tiny wrapper around some environment variables/values we need
 */
data class Env(
    val selfBinaryPath: String,
    val userId: String,
    val userName: String,
    val userHome: String,
)
