pluginManagement {
    repositories {
        maven("https://maven.deftu.dev/releases")
        maven("https://maven.fabricmc.net")
        maven("https://maven.architectury.dev/")
        maven("https://maven.minecraftforge.net")
        maven("https://maven.neoforged.net/releases")
        maven("https://repo.essential.gg/repository/maven-public")
        maven("https://maven.jab125.dev/")
        maven("https://maven.wagyourtail.xyz/releases")
        maven("https://maven.jemnetworks.com/releases")
        gradlePluginPortal()
        mavenCentral()
    }
}

rootProject.name = "world-host"
rootProject.buildFileName = "build.gradle.kts"

listOf(
    "1.19.2-forge",
    "1.19.2-fabric",
    "1.19.4-forge",
    "1.19.4-fabric",
    "1.20.1-forge",
    "1.20.1-fabric",
    "1.20.4-neoforge",
    "1.20.4-fabric",
    "1.21.1-neoforge",
    "1.21.1-fabric",
    "1.21.3-neoforge",
    "1.21.3-fabric",
    "1.21.4-neoforge",
    "1.21.4-fabric",
    "1.21.5-neoforge",
    "1.21.5-fabric",
).forEach { version ->
    include(":$version")
    project(":$version").apply {
        projectDir = file("versions/$version")
        projectDir.mkdirs()
        buildFileName = "../../version.gradle.kts"
    }
}
