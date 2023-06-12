pluginManagement {
    repositories {
        maven("https://maven.deftu.xyz/releases")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.jab125.dev/")

        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("xyz.deftu.gradle.multiversion-root") version("1.17.1")
    }
}

rootProject.name = "world-host"
rootProject.buildFileName = "build.gradle.kts"

listOf(
    "1.16.1-fabric",
    "1.16.5-forge",
    "1.16.5-fabric",
    "1.17.1-forge",
    "1.17.1-fabric",
    "1.18.2-forge",
    "1.18.2-fabric",
    "1.19.2-forge",
    "1.19.2-fabric",
    "1.19.4-forge",
    "1.19.4-fabric",
//    "1.20.1-forge",
    "1.20.1-fabric",
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        buildFileName = "../../version.gradle.kts"
    }
}
