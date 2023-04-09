plugins {
    id("java")
    id("fabric-loom") version "1.1.+"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

group = "io.github.gaming32.world-host"
version = "0.2.2"

repositories {
    mavenCentral()

    maven("https://maven.fabricmc.net/")

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

    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")

    maven("https://jitpack.io")
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.2")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.19.2:2022.11.27@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.14.14")

    implementation(project(":common"))

    modImplementation("maven.modrinth:midnightlib:1.0.0-fabric")
    include("maven.modrinth:midnightlib:1.0.0-fabric")

    modRuntimeOnly("me.djtheredstoner:DevAuth-fabric:1.1.0")

    modImplementation("com.github.LlamaLad7:MixinExtras:0.2.0-beta.6")
    annotationProcessor("com.github.LlamaLad7:MixinExtras:0.2.0-beta.6")
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
        archiveBaseName.set("world-host-1-19-2")
    }
}
