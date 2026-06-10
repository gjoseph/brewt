plugins {
    kotlin("multiplatform") version "2.3.20"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20"
}

kotlin {
    jvmToolchain(25)
}

group = "net.incongru.brewery"
version = "1.0-SNAPSHOT"
description = "An over-engineered script to routinely update a Homebrew setup"

repositories {
    mavenCentral()
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.10.0")
                implementation("com.akuleshov7:ktoml-core:0.7.1")
                implementation("com.akuleshov7:ktoml-file:0.7.1")
                implementation("ca.gosyer:kotlin-multiplatform-appdirs:2.0.0")
            }
        }
    }

    listOf(
        macosArm64(), // Mac M1
        macosX64(),   // Mac Legacy
    ).forEach { nativeTarget ->
        nativeTarget.apply {
            binaries {
                executable {
                    entryPoint = "net.incongru.brewery.main"
                }
            }
        }
    }
}

tasks.register<Exec>("macosUniversalBinary") {
    group = "Build"
    description = "Assembles the outputs of macosArm64MainBinaries and macosX64MainBinaries in a universal binary. Only runs on Macos, where `/usr/bin/lipo` is available."

    onlyIf { System.getProperty("os.name").lowercase().contains("mac") }
    dependsOn("build")
    val output = layout.buildDirectory.file("bin/macos-universal/brew-update-all-the-things").get().asFile
    output.parentFile.mkdirs()

    commandLine(
        "lipo",
        "-create",
        // TODO get those from above rather than hardcode paths
        "build/bin/macosArm64/releaseExecutable/brew-update-all-the-things.kexe",
        "build/bin/macosX64/releaseExecutable/brew-update-all-the-things.kexe",
        "-output", output
    )

    doLast {
        println("Universal binary created at: $output")
    }
}
