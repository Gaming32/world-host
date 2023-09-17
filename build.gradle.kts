import groovy.lang.GroovyObjectSupport
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig
import xyz.wagyourtail.unimined.api.task.ExportMappingsTask
import xyz.wagyourtail.unimined.internal.mapping.MappingsProvider
import xyz.wagyourtail.unimined.internal.mapping.task.ExportMappingsTaskImpl
import java.nio.file.Path

plugins {
//    `kotlin-dsl`
    id("xyz.deftu.gradle.preprocess-root") version "0.4.1"
    id("xyz.wagyourtail.unimined") version "1.0.6-SNAPSHOT" apply false
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

