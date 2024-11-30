plugins {
    id("io.github.gaming32.gradle.preprocess-root") version "0.4.4"
    id("dev.architectury.loom") version "1.7.415" apply false
    id("com.modrinth.minotaur") version "2.8.7" apply false
    id("xyz.wagyourtail.jvmdowngrader") version "1.2.1" apply false
}

repositories {
    mavenCentral()
}

preprocess {
    val fabric12103 = createNode("1.21.3-fabric", 1_21_03, "yarn")
    val neoforge12103 = createNode("1.21.3-neoforge", 1_21_03, "yarn")
    val fabric12101 = createNode("1.21.1-fabric", 1_21_01, "yarn")
    val neoforge12101 = createNode("1.21.1-neoforge", 1_21_01, "yarn")
    val fabric12006 = createNode("1.20.6-fabric", 1_20_06, "yarn")
    val neoforge12006 = createNode("1.20.6-neoforge", 1_20_06, "yarn")
    val fabric12004 = createNode("1.20.4-fabric", 1_20_04, "yarn")
    val neoforge12004 = createNode("1.20.4-neoforge", 1_20_04, "yarn")
    val fabric12001 = createNode("1.20.1-fabric", 1_20_01, "yarn")
    val forge12001 = createNode("1.20.1-forge", 1_20_01, "srg")
    val fabric11904 = createNode("1.19.4-fabric", 1_19_04, "yarn")
    val forge11904 = createNode("1.19.4-forge", 1_19_04, "srg")
    val fabric11902 = createNode("1.19.2-fabric", 1_19_02, "yarn")
    val forge11902 = createNode("1.19.2-forge", 1_19_02, "srg")

    fabric12103.link(neoforge12103)
    neoforge12103.link(neoforge12101)
    neoforge12101.link(fabric12101)
    fabric12101.link(fabric12006)
    fabric12006.link(neoforge12006)
    neoforge12006.link(neoforge12004)
    neoforge12004.link(fabric12004)
    fabric12004.link(fabric12001)
    fabric12001.link(forge12001)
    forge12001.link(forge11904)
    forge11904.link(fabric11904)
    fabric11904.link(fabric11902)
    fabric11902.link(forge11902)
}

subprojects {
    extra["loom.platform"] = name.substringAfter('-')
}
