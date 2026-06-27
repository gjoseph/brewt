package net.incongru.brewery

import ca.gosyer.appdirs.AppDirs
import com.akuleshov7.ktoml.file.TomlFileReader
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.findOrSetObject
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.requireObject
import com.github.ajalt.clikt.core.subcommands
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import okio.FileNotFoundException

class Brewt : CliktCommand() {
    override val invokeWithoutSubcommand = true
    val cfg by findOrSetObject { readConfig() }
    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            doTheThing(cfg)
        }
    }
}

class UpdateAllTheThings : CliktCommand() {
    val cfg by requireObject<Config>()
    override fun run() {
        doTheThing(cfg)
    }
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
        Schedule(),
        UpdateAllTheThings()
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
