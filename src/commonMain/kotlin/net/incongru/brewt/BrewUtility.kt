package net.incongru.brewt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

fun runUpgrade(brewt: Brewt, dryRun: Boolean, formulae: List<BrewFormula>, casks: List<BrewFormula>) {
    val cmdSuffix = if (dryRun) "--dry-run" else ""
    val logSuffix = if (dryRun) " (dry-run):" else ":"
    brewt.log.info("Brew upgrade$logSuffix")
    brewt.sh("${brewt.env.brewBinary} upgrade --yes --greedy $cmdSuffix ${formulae.asCliArgs()} ${casks.asCliArgs()}")

    brewt.log.info("Brew autoremove$logSuffix")
    brewt.sh("${brewt.env.brewBinary} autoremove $cmdSuffix")

    brewt.log.info("Brew cleanup$logSuffix")
    brewt.sh("${brewt.env.brewBinary} cleanup $cmdSuffix")

    brewt.log.info("Brew doctor$logSuffix")
    val doctored = brewt.sh.withoutThrowing("${brewt.env.brewBinary} doctor")
    if (!doctored.ok) {
        // TODO user feedback instead of exit on fail with no user feedback
        brewt.log.warn("... the doc wasn't happy 🧑")
        if (!dryRun) {
            doctored.orThrow()
        }
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