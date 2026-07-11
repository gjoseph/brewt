package net.incongru.brewt

fun doTheThing(brewt: Brewt, cfg: Config) {
    val a = ApplescriptHelper(brewt.sh)
    a.notif("Brew update starting 😊")

    if (cfg.update == UpdateConfig.always
        || (cfg.update == UpdateConfig.prompt && a.confirm("Update Homebrew?"))
    ) {
        brewt.log.info("Updating Homebrew:")
        // brew update-if-needed is faster, but unsure since when it's available, nor exactly what it does'doesn't do

        val updateOutput = brewt.sh("${brewt.env.brewBinary} update").output
        if (updateOutput.isNotBlank()) {
            a.notif("Brew update: \n$updateOutput")
        }
        // TODO report updates, particularly if brew itself was updated (recent updates show a changelog)
        //     ==> Updated Homebrew from 5.0.13 (6203165813) to 5.0.14 (8ab39821c5).
        // [...]
        //     The 5.0.14 changelog can be found at:
        //     https://github.com/Homebrew/brew/releases/tag/5.0.14
    }

    val outdated = brewt.sh("${brewt.env.brewBinary} outdated --greedy --json").jsonTo<BrewOutdatedOutput>()
    brewt.log.info("Outdated formulae: ${outdated.formulae}")
    brewt.log.info("Outdated casks: ${outdated.casks}")
    // TODO configurable schedules: e.g maybe we don't need to update docker-desktop every time
    val formulaeToUpdate =
        outdated.formulae.filterNot { cfg.ignoredFormulae.contains(it.name) || cfg.ignored.contains(it.name) }
    val casksToUpdate = outdated.casks.filterNot { cfg.ignoredCasks.contains(it.name) || cfg.ignored.contains(it.name) }

    if (formulaeToUpdate.isNotEmpty()) {
        brewt.log.info("Brew pre-fetch outdated formulae:")
        brewt.sh("${brewt.env.brewBinary} fetch --formula --deps ${formulaeToUpdate.asCliArgs()}")
    }

    if (casksToUpdate.isNotEmpty()) {
        brewt.log.info("Brew pre-fetch outdated casks:")
        brewt.sh("${brewt.env.brewBinary} fetch --cask ${casksToUpdate.asCliArgs()}")
    }

    brewt.log.info("Update and download done.")
    if (formulaeToUpdate.isNotEmpty() || casksToUpdate.isNotEmpty()) {
        brewt.log.info("Brew upgrade dry-run:")
        runUpgrade(brewt, true, formulaeToUpdate, casksToUpdate)
        brewt.log.info("Brew upgrade dry-run done")

        if (!cfg.prompt || a.confirm(
                // TODO nicer formatting - perhaps a kotlin native + swift dialog ? (or more lazily, invoke https://github.com/swiftDialog/swiftDialog)
                // https://kotlinlang.org/compose-multiplatform/
                "Outdated formulae: ${formulaeToUpdate.bulletListOfUpdates()}\n" +
                        "Outdated casks: ${casksToUpdate.bulletListOfUpdates()}\n\n" +
                        "Do you want to install them now?"
            )
        ) {
            runUpgrade(brewt, false, formulaeToUpdate, casksToUpdate)
            a.notif("All your Homebrew packages are up-to-date 😀")
        } else {
            brewt.log.info("Kthxbye")
        }
    } else {
        brewt.log.info("Nothing to upgrade")
        a.notif(".... there was nothing to upgrade 😏")
    }
}
