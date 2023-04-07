plugins {
    id("java")
    id("fabric-loom") version "1.0.+"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

group = "io.github.gaming32.world-host"
version = "0.1"

repositories {
    mavenCentral()

    maven("https://maven.fabricmc.net/")

    maven("https://maven.terraformersmc.com/releases")

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    mappings("net.fabricmc:yarn:1.19.4+build.1:v2")
    modImplementation("net.fabricmc:fabric-loader:0.14.14")
}

tasks {
    jar {
        archiveBaseName.set("world-host-common")
    }
}