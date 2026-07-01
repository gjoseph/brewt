package net.incongru.brewt

import ca.gosyer.appdirs.AppDirs
import com.akuleshov7.ktoml.file.TomlFileReader
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import okio.FileNotFoundException
import okio.FileSystem

class Brewt(val log: Logger, val env: Env, val sh: Shell, val fileSystem: FileSystem) {
    val appDirs: AppDirs = AppDirs {
        appName = "brewt"
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun readConfig(): Config {
        val configPath: String = appDirs.getUserConfigDir()
        val tomlFilePath = "$configPath/brewt.toml"
        try {
            return TomlFileReader.decodeFromFile(serializer<Config>(), tomlFilePath)
        } catch (e: FileNotFoundException) {
            this.log.error("No configuration file at $tomlFilePath, using defaults")
            return Config()
        }
    }
}
