import xyz.deftu.gradle.GameInfo.fetchMcpMappings
import xyz.deftu.gradle.GameInfo.fetchYarnMappings

plugins {
    java
    id("xyz.deftu.gradle.multiversion")
    id("xyz.deftu.gradle.tools")
    id("xyz.deftu.gradle.tools.blossom")
    id("xyz.deftu.gradle.tools.minecraft.loom")
    id("xyz.deftu.gradle.tools.minecraft.releases")
    id("xyz.deftu.gradle.tools.shadow")
}

version = "${modData.version}+${mcData.versionStr}-${mcData.loader.name}"

repositories {
    maven {
        name = "ParchmentMC"
        url = uri("https://maven.parchmentmc.org")
    }

    maven("https://repo.polyfrost.cc/releases")
}

val bundle: Configuration by configurations.creating {
    configurations.getByName(if (mcData.isFabric) "include" else "shade").extendsFrom(this)
}

dependencies {
    fun includeImplementation(dependency: Any) {
        implementation(dependency)
        bundle(dependency)
    }

    if (mcData.version > 1_15_02) {
        @Suppress("UnstableApiUsage")
        mappings(loom.layered {
            officialMojangMappings()
            when {
                mcData.version >= 1_19_03 -> "1.19.3:2023.03.12"
                mcData.version >= 1_19_02 -> "1.19.2:2022.11.27"
                mcData.version >= 1_18_02 -> "1.18.2:2022.11.06"
                mcData.version >= 1_17_01 -> "1.17.1:2021.12.12"
                mcData.version >= 1_16_05 -> "1.16.5:2022.03.06"
                else -> null
            }?.let {
                parchment("org.parchmentmc.data:parchment-$it@zip")
            }
        })
    } else if (mcData.isForge) {
        mappings("de.oceanlabs.mcp:mcp_${fetchMcpMappings(mcData.version)}")
    } else if (mcData.isFabric) {
        mappings("net.fabricmc:yarn:${fetchYarnMappings(mcData.version)}")
    } else {
        mappings(loom.officialMojangMappings())
    }

    if (mcData.version < 1_10_00) {
        includeImplementation("it.unimi.dsi:fastutil-core:8.5.5")
    }

    if (mcData.isLegacyForge) {
        compileOnly("org.spongepowered:mixin:0.7.11-SNAPSHOT")
        shade("cc.polyfrost:oneconfig-wrapper-launchwrapper:1.0.0-beta+")
    }

    includeImplementation("org.quiltmc:quilt-json5:1.0.2")
}

val generatedResources = "$buildDir/generated-resources/main"
val langRel = "assets/world-host/lang"

sourceSets {
    main {
        output.dir(generatedResources, "builtBy" to "generateLangFiles")
    }
}

java {
    withSourcesJar()
}

loom {
    if (mcData.isLegacyForge) {
        launchConfigs.named("client") {
            arg("--tweakClass", "cc.polyfrost.oneconfig.loader.stage0.LaunchWrapperTweaker")

            property("mixin.debug.export", "true")
        }
    }

    if (mcData.isForge) {
        forge {
            mixinConfig("world-host.mixins.json")
        }
    }

    @Suppress("UnstableApiUsage")
    mixin.defaultRefmapName.set("world-host.mixins.refmap.json")
}

tasks.register("generateLangFiles") {
    doLast {
        if (mcData.version >= 1_13_00) return@doLast
        val srcDir = File("$buildDir/resources/main/$langRel")
        if (!srcDir.exists()) return@doLast
        val destDir = File(generatedResources, langRel)
        destDir.mkdirs()
        for (file in srcDir.listFiles()!!) {
            @Suppress("UNCHECKED_CAST") val json = groovy.json.JsonSlurper().parse(file) as Map<String, String>
            destDir.resolve(file.name.substringBeforeLast('.') + ".lang").writer().use {
                json.forEach { (key, value) ->
                    it.write("$key=${value.replace("\n", "\\n")}\n")
                }
            }
        }
    }
}

tasks.jar {
    archiveBaseName.set(rootProject.name)
}

tasks.remapJar {
    archiveBaseName.set(rootProject.name)
}
