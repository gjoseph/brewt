package net.incongru.brewt

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wrapper around common brew calls
 */
class BrewWrapper(val brewt: Brewt) {
    val log = brewt.log

    fun brew(brewCmd: String): ShellResult = brewt.sh("${brewt.env.brewBinary} $brewCmd")

    fun runUpgrade(dryRun: Boolean, formulae: List<BrewFormula>, casks: List<BrewFormula>) {
        val cmdSuffix = if (dryRun) "--dry-run" else ""
        val logSuffix = if (dryRun) " (dry-run):" else ":"
        log.info("Brew upgrade$logSuffix")
        brew("upgrade --yes --greedy $cmdSuffix ${formulae.asCliArgs()} ${casks.asCliArgs()}")

        log.info("Brew autoremove$logSuffix")
        brew("autoremove $cmdSuffix")

        log.info("Brew cleanup$logSuffix")
        brew("cleanup $cmdSuffix")

        log.info("Brew doctor$logSuffix")
        val doctored = brewt.sh.withoutThrowing("${brewt.env.brewBinary} doctor")
        if (!doctored.ok) {
            // TODO user feedback instead of exit on fail with no user feedback
            log.warn("... the doc wasn't happy 🧑")
            if (!dryRun) {
                doctored.orThrow()
            }
        }
    }

    // TODO report/format updates, particularly if brew itself was updated (recent updates show a changelog)
    //     ==> Updated Homebrew from 5.0.13 (6203165813) to 5.0.14 (8ab39821c5).
    // [...]
    //     The 5.0.14 changelog can be found at:
    //     https://github.com/Homebrew/brew/releases/tag/5.0.14
    fun update(): String {
        return brew("update").output
    }

    fun outdated(): BrewOutdatedOutput {
        return brew("outdated --greedy --json").jsonTo<BrewOutdatedOutput>()
    }

    fun fetch(type: BrewType, toFetch: List<BrewFormula>) {
        when (type) {
            BrewType.FORMULA -> brew("fetch --formula --deps ${toFetch.asCliArgs()}")
            BrewType.CASK -> brew("fetch --cask --deps ${toFetch.asCliArgs()}")
            else -> error("unknown type: $type")
        }
    }

}

@Serializable
data class BrewOutdatedOutput(
    val formulae: List<BrewFormula>,
    val casks: List<BrewFormula>
)

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

private fun List<BrewFormula>.asCliArgs(): String {
    return this.joinToString(separator = " ", transform = { it.name })
}

fun List<BrewFormula>.bulletListOfUpdates(): String {
    return this.joinToString(separator = "\n", transform = { "* ${it.toString()}" })
}

enum class BrewType {
    FORMULA, CASK
}