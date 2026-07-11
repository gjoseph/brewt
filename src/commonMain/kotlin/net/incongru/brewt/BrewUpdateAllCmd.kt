package net.incongru.brewt

import net.incongru.brewt.BrewType.CASK
import net.incongru.brewt.BrewType.FORMULA

fun updateAll(brew: BrewWrapper, a: ApplescriptHelper, log: Logger, cfg: Config) {
    a.notif("Brew update starting 😊")

    if (cfg.update == UpdateConfig.always
        || (cfg.update == UpdateConfig.prompt && a.confirm("Update Homebrew?"))
    ) {
        log.info("Updating Homebrew:")
        // brew update-if-needed is faster, but unsure since when it's available, nor exactly what it does/doesn't do

        val updateOutput = brew.update()
        if (updateOutput.isNotBlank()) {
            a.notif("Brew update: \n$updateOutput")
        }
    }

    val outdated = brew.outdated()
    log.info("Outdated formulae: ${outdated.formulae}")
    log.info("Outdated casks: ${outdated.casks}")
    // TODO configurable schedules: e.g maybe we don't need to update docker-desktop every time
    val formulaeToUpdate =
        outdated.formulae.filterNot { cfg.ignoredFormulae.contains(it.name) || cfg.ignored.contains(it.name) }
    val casksToUpdate = outdated.casks.filterNot { cfg.ignoredCasks.contains(it.name) || cfg.ignored.contains(it.name) }

    if (formulaeToUpdate.isNotEmpty()) {
        log.info("Brew pre-fetch outdated formulae:")
        brew.fetch(FORMULA, formulaeToUpdate)
    }

    if (casksToUpdate.isNotEmpty()) {
        log.info("Brew pre-fetch outdated casks:")
        brew.fetch(CASK, casksToUpdate)
    }

    log.info("Update and download done.")
    if (formulaeToUpdate.isNotEmpty() || casksToUpdate.isNotEmpty()) {
        log.info("Brew upgrade dry-run:")
        brew.runUpgrade(true, formulaeToUpdate, casksToUpdate)
        log.info("Brew upgrade dry-run done")

        if (!cfg.prompt || a.confirm(
                // TODO nicer formatting - perhaps a kotlin native + swift dialog ? (or more lazily, invoke https://github.com/swiftDialog/swiftDialog)
                // https://kotlinlang.org/compose-multiplatform/
                "Outdated formulae: ${formulaeToUpdate.bulletListOfUpdates()}\n" +
                        "Outdated casks: ${casksToUpdate.bulletListOfUpdates()}\n\n" +
                        "Do you want to install them now?"
            )
        ) {
            brew.runUpgrade(false, formulaeToUpdate, casksToUpdate)
            a.notif("All your Homebrew packages are up-to-date 😀")
        } else {
            log.info("Kthxbye")
        }
    } else {
        log.info("Nothing to upgrade")
        a.notif(".... there was nothing to upgrade 😏")
    }
}
