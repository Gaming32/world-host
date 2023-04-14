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

    implementation("org.eclipse.jetty.toolchain:jetty-jakarta-websocket-api:2.0.0")

    runtimeOnly("org.eclipse.jetty.websocket:websocket-jakarta-client:11.0.14")
    include("org.eclipse.jetty.websocket:websocket-jakarta-client:11.0.14")
    include("org.eclipse.jetty:jetty-client:11.0.14")
    include("org.eclipse.jetty:jetty-http:11.0.14")
    include("org.eclipse.jetty:jetty-util:11.0.14")
    include("org.eclipse.jetty:jetty-io:11.0.14")
    include("org.eclipse.jetty:jetty-alpn-client:11.0.14")
    include("org.eclipse.jetty.toolchain:jetty-jakarta-websocket-api:2.0.0")
    include("org.eclipse.jetty.websocket:websocket-jakarta-common:11.0.14")
    include("org.eclipse.jetty.websocket:websocket-core-client:11.0.14")
    include("org.eclipse.jetty.websocket:websocket-core-common:11.0.14")
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
