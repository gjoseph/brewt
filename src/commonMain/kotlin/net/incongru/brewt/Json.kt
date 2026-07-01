package net.incongru.brewt

import kotlinx.serialization.json.Json

/**
 * Just a default configuration for all our json conversions
 */
val json: Json by lazy {
    Json {
        ignoreUnknownKeys = true
    }
}