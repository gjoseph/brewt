package net.incongru.brewt

fun doTheThing(cfg: Config) {
    notif("Brew update starting 😊")

    log("Configuration: $cfg")

    if (cfg.update == UpdateConfig.always
        || (cfg.update == UpdateConfig.prompt && confirm("Update Homebrew?"))
    ) {
        log("Updating Homebrew:")
        // brew update-if-needed is faster, but unsure since when it's available, nor exactly what it does'doesn't do

        val updateOutput = "brew update".runCommandAndCaptureOutput()
        if (updateOutput.isNotBlank()) {
            notif("Brew update: \n$updateOutput")
        }
        // TODO report updates, particularly if brew itself was updated (recent updates show a changelog)
        //     ==> Updated Homebrew from 5.0.13 (6203165813) to 5.0.14 (8ab39821c5).
        // [...]
        //     The 5.0.14 changelog can be found at:
        //     https://github.com/Homebrew/brew/releases/tag/5.0.14
    }

    val outdated = "brew outdated --greedy --json".runCommandAndCaptureOutputAs<BrewOutdatedOutput>()
    log("Outdated formulae: ${outdated.formulae}")
    log("Outdated casks: ${outdated.casks}")
    // TODO configurable schedules: e.g maybe we don't need to update docker-desktop every time
    val formulaeToUpdate =
        outdated.formulae.filterNot { cfg.ignoredFormulae.contains(it.name) || cfg.ignored.contains(it.name) }
    val casksToUpdate = outdated.casks.filterNot { cfg.ignoredCasks.contains(it.name) || cfg.ignored.contains(it.name) }

    if (formulaeToUpdate.isNotEmpty()) {
        log("Brew pre-fetch outdated formulae:")
        "brew fetch --formula --deps ${formulaeToUpdate.asCliArgs()}".runCommand()
    }

    if (casksToUpdate.isNotEmpty()) {
        log("Brew pre-fetch outdated casks:")
        "brew fetch --cask ${casksToUpdate.asCliArgs()}".runCommand()
    }

    log("Update and download done.")
    if (formulaeToUpdate.isNotEmpty() || casksToUpdate.isNotEmpty()) {
        log("Brew upgrade dry-run:")
        runUpgrade(log, true, formulaeToUpdate, casksToUpdate)
        log("Brew upgrade dry-run done")

        if (!cfg.prompt || confirm(
                // TODO nicer formatting - perhaps a kotlin native + swift dialog ? (or more lazily, invoke https://github.com/swiftDialog/swiftDialog)
                // https://kotlinlang.org/compose-multiplatform/
                "Outdated formulae: ${formulaeToUpdate.bulletListOfUpdates()}\n" +
                        "Outdated casks: ${casksToUpdate.bulletListOfUpdates()}\n\n" +
                        "Do you want to install them now?"
            )
        ) {
            runUpgrade(log, false, formulaeToUpdate, casksToUpdate)
            notif("All your Homebrew packages are up-to-date 😀")
        } else {
            log("Kthxbye")
        }
    } else {
        log("Nothing to upgrade")
        notif(".... there was nothing to upgrade 😏")
    }
}
