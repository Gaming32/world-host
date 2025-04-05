plugins {
    id("io.github.gaming32.gradle.preprocess-root") version "0.4.5"
    id("dev.architectury.loom") version "1.10.430" apply false
    id("com.modrinth.minotaur") version "2.8.7" apply false
    id("xyz.wagyourtail.jvmdowngrader") version "1.2.2" apply false
}

repositories {
    mavenCentral()
}

preprocess {
    val fabric12105 = createNode("1.21.5-fabric", 1_21_05)
    val neoforge12105 = createNode("1.21.5-neoforge", 1_21_05)
    val fabric12104 = createNode("1.21.4-fabric", 1_21_04)
    val neoforge12104 = createNode("1.21.4-neoforge", 1_21_04)
    val fabric12103 = createNode("1.21.3-fabric", 1_21_03)
    val neoforge12103 = createNode("1.21.3-neoforge", 1_21_03)
    val fabric12101 = createNode("1.21.1-fabric", 1_21_01)
    val neoforge12101 = createNode("1.21.1-neoforge", 1_21_01)
    val fabric12004 = createNode("1.20.4-fabric", 1_20_04)
    val neoforge12004 = createNode("1.20.4-neoforge", 1_20_04)
    val fabric12001 = createNode("1.20.1-fabric", 1_20_01)
    val forge12001 = createNode("1.20.1-forge", 1_20_01)
    val fabric11904 = createNode("1.19.4-fabric", 1_19_04)
    val forge11904 = createNode("1.19.4-forge", 1_19_04)
    val fabric11902 = createNode("1.19.2-fabric", 1_19_02)
    val forge11902 = createNode("1.19.2-forge", 1_19_02)

    fabric12105.link(neoforge12105)
    neoforge12105.link(neoforge12104)
    neoforge12104.link(fabric12104)
    fabric12104.link(fabric12103)
    fabric12103.link(neoforge12103)
    neoforge12103.link(neoforge12101)
    neoforge12101.link(fabric12101)
    fabric12101.link(fabric12004)
    fabric12004.link(neoforge12004)
    neoforge12004.link(forge12001)
    forge12001.link(fabric12001)
    fabric12001.link(fabric11904)
    fabric11904.link(forge11904)
    forge11904.link(forge11902)
    forge11902.link(fabric11902)
}

subprojects {
    extra["loom.platform"] = name.substringAfter('-')
}
