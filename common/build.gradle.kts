plugins {
    id("java")
    id("fabric-loom") version "1.1.+"
    id("io.github.juuxel.loom-quiltflower") version "1.8.0"
}

group = "io.github.gaming32.world-host"
version = "0.1"

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

    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:1.19.4")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings()
        parchment("org.parchmentmc.data:parchment-1.19.3:2023.03.12@zip")
    })
    modImplementation("net.fabricmc:fabric-loader:0.14.14")

    // The exact JiJed version is dependent on the Minecraft version. However, it needs to be ABI-compatible.
    modImplementation("maven.modrinth:midnightlib:1.3.0-fabric")

    implementation("javax.websocket:javax.websocket-api:1.1")
    include("javax.websocket:javax.websocket-api:1.1")

    runtimeOnly("org.eclipse.jetty.websocket:javax-websocket-client-impl:9.4.51.v20230217")
    include("org.eclipse.jetty.websocket:javax-websocket-client-impl:9.4.51.v20230217")
    include("org.eclipse.jetty.websocket:websocket-client:9.4.51.v20230217")
    include("org.eclipse.jetty.websocket:websocket-common:9.4.51.v20230217")
    include("org.eclipse.jetty.websocket:websocket-api:9.4.51.v20230217")
    include("org.eclipse.jetty:jetty-io:9.4.51.v20230217")
    include("org.eclipse.jetty:jetty-util:9.4.51.v20230217")
    include("org.eclipse.jetty:jetty-client:9.4.51.v20230217")
    include("org.eclipse.jetty:jetty-http:9.4.51.v20230217")
}

tasks {
    processResources {
        filesMatching("fabric.mod.json") {
            expand("version" to project.version)
        }
    }

    remapJar {
        archiveBaseName.set("world-host-common")
    }
}
