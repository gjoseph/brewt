package net.incongru.brewt

import ca.gosyer.appdirs.AppDirs
import com.akuleshov7.ktoml.file.TomlFileReader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.NoOpCliktCommand
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import okio.FileNotFoundException

class Brewt : CliktCommand() {
    override val invokeWithoutSubcommand = true
    val cfg by findOrSetObject { readConfig() }
    override fun run() {
        if (currentContext.invokedSubcommand == null || currentContext.invokedSubcommand is UpdateAllTheThings) {
            doTheThing(cfg)
        }
    }
}

// Just a placeholder which we might as well remove, since we let the root Brewt cmd do the job
class UpdateAllTheThings : NoOpCliktCommand() {
}

class Schedule : CliktCommand(
) {
    override fun help(context: Context): String {
        return "Schedule updates"
    }

    override fun run() {
        throw NotImplementedError()
    }
}

fun main(args: Array<String>) = Brewt()
    .subcommands(
        UpdateAllTheThings(),
        Schedule()
    ).main(args)

@OptIn(ExperimentalSerializationApi::class)
private fun readConfig(): Config {
    val appDirs = AppDirs {
        appName = "brewt"
    }
    val configPath: String = appDirs.getUserConfigDir()
    val tomlFilePath = "$configPath/brewt.toml"
    try {
        return TomlFileReader.decodeFromFile(serializer<Config>(), tomlFilePath)
    } catch (e: FileNotFoundException) {
        log("No configuration file at $tomlFilePath, using defaults")
        return Config()
    }
}
