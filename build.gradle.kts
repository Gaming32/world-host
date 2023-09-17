import groovy.lang.GroovyObjectSupport
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig
import xyz.wagyourtail.unimined.api.task.ExportMappingsTask
import xyz.wagyourtail.unimined.internal.mapping.MappingsProvider
import xyz.wagyourtail.unimined.internal.mapping.task.ExportMappingsTaskImpl
import java.nio.file.Path

plugins {
//    `kotlin-dsl`
    id("xyz.deftu.gradle.preprocess-root") version "0.4.1"
    id("xyz.wagyourtail.unimined") version "1.0.6-SNAPSHOT"
}

repositories {
    mavenCentral()
}

preprocess {
    val fabric12001 = createNode("1.20.1-fabric", 1_20_01, "yarn")
//    val forge12001 = createNode("1.20.1-forge", 1_20_01, "srg")
    val fabric11904 = createNode("1.19.4-fabric", 1_19_04, "yarn")
    val forge11904 = createNode("1.19.4-forge", 1_19_04, "srg")
    val fabric11902 = createNode("1.19.2-fabric", 1_19_02, "yarn")
    val forge11902 = createNode("1.19.2-forge", 1_19_02, "srg")
    val fabric11802 = createNode("1.18.2-fabric", 1_18_02, "yarn")
    val forge11802 = createNode("1.18.2-forge", 1_18_02, "srg")
    val fabric11701 = createNode("1.17.1-fabric", 1_17_01, "yarn")
    val forge11701 = createNode("1.17.1-forge", 1_17_01, "srg")
    val fabric11605 = createNode("1.16.5-fabric", 1_16_05, "yarn")
    val forge11605 = createNode("1.16.5-forge", 1_16_05, "srg")
    val fabric11601 = createNode("1.16.1-fabric", 1_16_01, "yarn")

    fabric12001.link(fabric11904)
    fabric11904.link(forge11904)
    forge11904.link(forge11902)
    forge11902.link(fabric11902)
    fabric11902.link(fabric11802)
    fabric11802.link(forge11802)
    forge11802.link(forge11701)
    forge11701.link(fabric11701)
    fabric11701.link(fabric11605)
    fabric11605.link(forge11605)
    fabric11605.link(fabric11601)

//    subprojects {
//        apply(plugin = "java")
//        if (name == "1.20.1-fabric") {
//            sourceSets.main {
//                java {
//                    srcDir("$rootDir/src/main/java")
//                }
//            }
//        }
//    }
}

fun Any.setGroovyProperty(name: String, value: Any) = withGroovyBuilder { metaClass }.setProperty(this, name, value)
fun Any.getGroovyProperty(name: String): Any = withGroovyBuilder { metaClass }.getProperty(this, name)!!

subprojects {
    this@subprojects.apply(plugin = "xyz.wagyourtail.unimined")

    val vers = project.properties["mod.version"] as String
    val mcVersionString by extra(name.substringBefore("-"))
    val loaderName by extra(name.substringAfter("-"))

    // major.minor.?patch
    // to MMmmPP
    val mcVersion by extra(mcVersionString.split(".").map { it.toInt() }
        .let {
            it[0] * 1_00_00 + it[1] * 1_00 + (if (it.size == 2 || it[2] == 0) 0 else it[2])
        })

    println("MC_VERSION: " + mcVersionString + " " + mcVersion)
    version = "${vers}+${mcVersionString}-${loaderName}"

    repositories {
        mavenCentral()
    }

    lateinit var minecraft: MinecraftConfig
    unimined.minecraft {
        version(mcVersionString)

        mappings {
            intermediary()
            searge()
            mojmap()
            when {
                mcVersion >= 1_19_03 -> "1.19.3:2023.03.12"
                mcVersion >= 1_19_00 -> "1.19.2:2022.11.27"
                mcVersion >= 1_18_00 -> "1.18.2:2022.11.06"
                mcVersion >= 1_17_00 -> "1.17.1:2021.12.12"
                mcVersion >= 1_16_00 -> "1.16.5:2022.03.06"
                else -> null
            }?.let {
                parchment(it.substringBefore(":"), it.substringAfter(":"))
            }
        }

        if (loaderName == "fabric") {
            fabric {
                loader("0.14.22")
            }
        } else {
            forge {
                loader(when(mcVersion) {
                    1_20_01 -> "47.1.0"
                    1_19_04 -> "45.1.0"
                    1_19_02 -> "43.2.0"
                    1_18_02 -> "40.2.0"
                    1_17_01 -> "37.1.1"
                    1_16_05 -> "36.2.34"
                    else -> throw IllegalArgumentException("unknown forge version for $mcVersionString")
                })
                mixinConfig("world-host.mixins.json")
            }
        }

        minecraft = this
    }

    // jank hax to pretend to be arch-loom
    class LoomGradleExtension : GroovyObjectSupport() {
        var mappingConfiguration: Any? = null
    }

    val loom = LoomGradleExtension()
    extensions.add("loom", loom)
    val mappingsConfig = object {
        var tinyMappings: Path? = null
        var tinyMappingsWithSrg: Path? = null
    }
    loom.setGroovyProperty("mappingConfiguration", mappingsConfig)
    val tinyMappings: File = file("${projectDir}/build/tmp/tinyMappings.tiny").also { file ->
        val export = ExportMappingsTaskImpl.ExportImpl(minecraft.mappings as MappingsProvider).apply {
            location = file
            type = ExportMappingsTask.MappingExportTypes.TINY_V2
            setSourceNamespace("official")
            setTargetNamespaces(listOf("intermediary", "mojmap"))
            renameNs[minecraft.mappings.getNamespace("mojmap")] = "named"
        }
        export.validate()
        export.exportFunc((minecraft.mappings as MappingsProvider).mappingTree)
    }
    mappingsConfig.setGroovyProperty("tinyMappings", tinyMappings.toPath())
    if (loaderName == "forge") {
        val tinyMappingsWithSrg: File = file("${projectDir}/build/tmp/tinyMappingsWithSrg.tiny").also { file ->
            val export = ExportMappingsTaskImpl.ExportImpl(minecraft.mappings as MappingsProvider).apply {
                location = file
                type = ExportMappingsTask.MappingExportTypes.TINY_V2
                setSourceNamespace("official")
                setTargetNamespaces(listOf("intermediary", "searge", "mojmap"))
                renameNs[minecraft.mappings.getNamespace("mojmap")] = "named"
                renameNs[minecraft.mappings.getNamespace("searge")] = "srg"
            }
            export.validate()
            export.exportFunc((minecraft.mappings as MappingsProvider).mappingTree)
        }
        mappingsConfig.setGroovyProperty("tinyMappingsWithSrg", tinyMappingsWithSrg.toPath())
    }
}
