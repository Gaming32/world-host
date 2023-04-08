plugins {
    id("java")
    id("org.quiltmc.loom") version "1.1.+"
}

group = "io.github.gaming32.world-host"
version = "0.2"

repositories {
    mavenCentral()
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.19.3:2023.03.12@zip")
    })
    modImplementation("org.quiltmc:quilt-loader:0.18.5")

    include(project(":mc-1.19.2"))
    include(project(":mc-1.19.4"))
}

tasks {
    processResources {
        filesMatching("quilt.mod.json") {
            expand("version" to project.version)
        }
    }

    remapJar {
        archiveBaseName.set("world-host")
    }
}
