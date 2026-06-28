package net.incongru.brewt

import kotlinx.serialization.Serializable

@Serializable
data class Config(
    /**
     * Whether to prompt before installing upgraded formulae and casks.
     */
    val prompt: Boolean = true,

    /**
     * Whether to update brew itself.
     */
    val update: UpdateConfig = UpdateConfig.always,

    /**
     * Ignores update for the given formulae or casks (simple configuration when there is no name clash)
     */
    val ignored: List<String> = emptyList(),

    /**
     * Ignores update for the given formulae (explicit configuration when there is a name clash with a cask)
     */
    val ignoredFormulae: List<String> = emptyList(),

    /**
     * Ignores update for the given casks (explicit configuration when there is a name clash with a formula)
     */
    val ignoredCasks: List<String> = emptyList(),

    /**
     * List of SSIDs where the update process should run; if connected to any other SSID, the update won't proceed.
     * If empty, the update will proceed on any network.
     */
    val onlyOnWifi: List<String> = emptyList()

    // TODO schedules ?
)

enum class UpdateConfig {
    /**
     * Never update.
     */
    never,

    /**
     * Prompt before updating.
     */
    prompt,

    /**
     * Always update without prompting.
     */
    always
}