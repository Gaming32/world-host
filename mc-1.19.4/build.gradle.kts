plugins {
    id("java")
    id("fabric-loom") version "1.1.+"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

group = "io.github.gaming32.world-host"
version = "0.2.6"

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.19.3:2023.03.12@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.14.14")

    implementation(project(":common"))
    implementation(project(":gui-1.19.4"))

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.1.0")

    modImplementation("com.github.LlamaLad7.MixinExtras:mixinextras-fabric:0.2.0-beta.6")
    annotationProcessor("com.github.LlamaLad7.MixinExtras:mixinextras-fabric:0.2.0-beta.6")
}

loom {
    runs {
        runConfigs.configureEach {
            isIdeConfigGenerated = true
        }
    }
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    remapJar {
        archiveBaseName.set("world-host-1-19-4")
    }
}
