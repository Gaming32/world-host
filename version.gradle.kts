plugins {
    java
    id("xyz.deftu.gradle.multiversion")
    id("xyz.deftu.gradle.tools")
    id("xyz.deftu.gradle.tools.blossom")
    id("xyz.deftu.gradle.tools.minecraft.loom")
    id("xyz.deftu.gradle.tools.minecraft.releases")
}

val generatedResources = "$buildDir/generated-resources/main"
val langRel = "assets/world-host/lang"

sourceSets {
    main {
        output.dir(generatedResources, "builtBy" to "generateLangFiles")
    }
}

tasks.register("generateLangFiles") {
    doLast {
        if (mcData.version >= 11300) return@doLast
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
