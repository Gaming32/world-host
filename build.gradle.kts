plugins {
    id("io.github.gaming32.gradle.preprocess-root") version "0.4.6"
    id("dev.architectury.loom") version "1.10.432" apply false
    id("com.modrinth.minotaur") version "2.8.7" apply false
    id("xyz.wagyourtail.jvmdowngrader") version "1.3.3" apply false
}

repositories {
    mavenCentral()
}

preprocess {
    addProject("1.21.5-fabric", 1_21_05)
    addProject("1.21.5-neoforge", 1_21_05)
    addProject("1.21.4-fabric", 1_21_04)
    addProject("1.21.4-neoforge", 1_21_04)
    addProject("1.21.3-fabric", 1_21_03)
    addProject("1.21.3-neoforge", 1_21_03)
    addProject("1.21.1-fabric", 1_21_01)
    addProject("1.21.1-neoforge", 1_21_01)
    addProject("1.20.4-fabric", 1_20_04)
    addProject("1.20.4-neoforge", 1_20_04)
    addProject("1.20.1-fabric", 1_20_01)
    addProject("1.20.1-forge", 1_20_01)
    addProject("1.19.2-fabric", 1_19_02)
    addProject("1.19.2-forge", 1_19_02)
}

subprojects {
    extra["loom.platform"] = name.substringAfter('-')
}

afterEvaluate {
    var previousPublishTask: Task? = null
    for (project in subprojects) {
        val publishTask = project.tasks.findByName("modrinth") ?: continue
        if (previousPublishTask != null) {
            publishTask.mustRunAfter(previousPublishTask)
        }
        previousPublishTask = publishTask
    }
}
