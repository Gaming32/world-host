plugins {
    id("dev.deftu.gradle.preprocess-root") version "0.4.2"
    id("xyz.wagyourtail.unimined") version "1.2.4" apply false
}

repositories {
    mavenCentral()
}

preprocess {
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
    val fabric11802 = createNode("1.18.2-fabric", 1_18_02, "yarn")
    val forge11802 = createNode("1.18.2-forge", 1_18_02, "srg")
    val fabric11701 = createNode("1.17.1-fabric", 1_17_01, "yarn")
    val forge11701 = createNode("1.17.1-forge", 1_17_01, "srg")

    fabric12006.link(neoforge12006)
    neoforge12006.link(neoforge12004)
    neoforge12004.link(fabric12004)
    fabric12004.link(fabric12001)
    fabric12001.link(forge12001)
    forge12001.link(forge11904)
    forge11904.link(fabric11904)
    fabric11904.link(fabric11902)
    fabric11902.link(forge11902)
    forge11902.link(forge11802)
    forge11802.link(fabric11802)
    fabric11802.link(fabric11701)
    fabric11701.link(forge11701)

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

//gradle.projectsEvaluated {
//    subprojects.asSequence().zipWithNext().forEach { (left, right) ->
//        right.tasks.named("modrinth").get().mustRunAfter(left.tasks.named("modrinth"))
//    }
//}
