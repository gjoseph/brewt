package net.incongru.brewery

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun runUpgrade(log: Logger, dryRun: Boolean, formulae: List<BrewFormula>, casks: List<BrewFormula>) {
    val cmdSuffix = if (dryRun) "--dry-run" else ""
    val logSuffix = if (dryRun) " (dry-run):" else ":"
    log("Brew upgrade$logSuffix")
    "brew upgrade --yes --greedy $cmdSuffix ${formulae.asCliArgs()} ${casks.asCliArgs()}".runCommand()

    log("Brew autoremove$logSuffix")
    "brew autoremove $cmdSuffix".runCommand()

    log("Brew cleanup$logSuffix")
    "brew cleanup $cmdSuffix".runCommand()

    log("Brew doctor$logSuffix")
    if ("brew doctor".runCommand(!dryRun) != 0) {
        // TODO user feedback instead of exit on fail with no user feedback
        log("... the doc wasn't happy 🧑")
    }
}

@Serializable
 data class BrewFormula(
    val name: String,
    @SerialName("installed_versions")
    val installedVersions: List<String>,
    @SerialName("current_version")
    val currentVersion: String
) {
    override fun toString(): String {
        return "$name (${installedVersions.joinToString()} → $currentVersion)"
    }
}

@Serializable
 data class BrewOutdatedOutput(
    val formulae: List<BrewFormula>,
    val casks: List<BrewFormula>
)

 fun List<BrewFormula>.asCliArgs(): String {
    return this.joinToString(separator = " ", transform = { it.name })
}

 fun List<BrewFormula>.bulletListOfUpdates(): String {
    return this.joinToString(separator = "\n", transform = { "* ${it.toString()}" })
}