import com.modrinth.minotaur.ModrinthExtension
import com.replaymod.gradle.preprocess.PreprocessTask
import groovy.lang.GroovyObjectSupport
import net.raphimc.javadowngrader.gradle.task.DowngradeSourceSetTask
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig
import xyz.wagyourtail.unimined.api.task.ExportMappingsTask
import xyz.wagyourtail.unimined.api.task.RemapJarTask
import xyz.wagyourtail.unimined.internal.mapping.MappingsProvider
import xyz.wagyourtail.unimined.internal.mapping.task.ExportMappingsTaskImpl
import xyz.wagyourtail.unimined.internal.minecraft.MinecraftProvider
import xyz.wagyourtail.unimined.util.sourceSets
import java.nio.file.Path

plugins {
    java
    `maven-publish`
    id("dev.deftu.gradle.preprocess")
    id("xyz.wagyourtail.unimined")
    id("com.modrinth.minotaur") version "2.+"
}


fun Any.setGroovyProperty(name: String, value: Any) = withGroovyBuilder { metaClass }.setProperty(this, name, value)
fun Any.getGroovyProperty(name: String): Any = withGroovyBuilder { metaClass }.getProperty(this, name)!!


val modVersion = project.properties["mod.version"] as String
val mcVersionString by extra(name.substringBefore("-"))
val loaderName by extra(name.substringAfter("-"))

val isFabric = loaderName == "fabric"
val isForge = loaderName == "forge"
val isNeoForge = loaderName == "neoforge"
val isForgeLike = isForge || isNeoForge

base.archivesName.set(rootProject.name)

// major.minor.?patch
// to MMmmPP
val mcVersion by extra(mcVersionString.split(".").map { it.toInt() }
    .let {
        it[0] * 1_00_00 + it[1] * 1_00 + (if (it.size == 2 || it[2] == 0) 0 else it[2])
    })

println("MC_VERSION: $mcVersionString $mcVersion")
version = "${modVersion}+${mcVersionString}-${loaderName}"

repositories {
    mavenCentral()
}

lateinit var minecraft: MinecraftConfig
unimined.minecraft {
    version(mcVersionString)
    if (mcVersion != 1_20_01 || !isForge) {
        side("client")
    }

    mappings {
        intermediary()
        searge()
        mojmap()
        when {
            mcVersion >= 1_20_01 -> "1.20.1:2023.09.03"
            mcVersion >= 1_19_04 -> "1.19.4:2023.06.26"
            mcVersion >= 1_19_03 -> "1.19.3:2023.06.25"
            mcVersion >= 1_19_00 -> "1.19.2:2022.11.27"
            mcVersion >= 1_18_00 -> "1.18.2:2022.11.06"
            mcVersion >= 1_17_00 -> "1.17.1:2021.12.12"
            mcVersion >= 1_16_00 -> "1.16.5:2022.03.06"
            else -> null
        }?.let {
            parchment(it.substringBefore(":"), it.substringAfter(":"))
        }

        stub.withMappings("searge", listOf("mojmap")) {
            if (mcVersion <= 1_19_00) {
                c("net/minecraft/client/gui/chat/NarratorChatListener", "net/minecraft/client/GameNarrator")
            }
            if (mcVersion < 1_17_00) {
                c("net/minecraft/client/multiplayer/ServerAddress", "net/minecraft/client/multiplayer/resolver/ServerAddress")
            }
        }
    }

    when {
        isFabric -> fabric {
            loader("0.14.22")
        }
        isForge -> minecraftForge {
            loader(when(mcVersion) {
                1_20_01 -> "47.1.3"
                1_19_04 -> "45.1.0"
                1_19_02 -> "43.2.0"
                1_18_02 -> "40.2.0"
                1_17_01 -> "37.1.1"
                1_16_05 -> "36.2.34"
                else -> throw IllegalStateException("Unknown Forge version for $mcVersionString")
            })
            mixinConfig("world-host.mixins.json")
        }
        isNeoForge -> neoForged {
            loader(when (mcVersion) {
                1_20_02 -> "35-beta"
                else -> throw IllegalStateException("Unknown NeoForge version for $mcVersionString")
            })
        }
        else -> throw IllegalStateException()
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
if (isForge) {
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

buildscript {
    repositories {
        maven("https://maven.lenni0451.net/everything")
    }

    dependencies {
        classpath("net.raphimc.javadowngrader:gradle-plugin:1.1.1-SNAPSHOT")
    }
}

repositories {
    maven("https://maven.quiltmc.org/repository/release/")

    maven("https://maven.terraformersmc.com/releases")

    maven("https://maven.isxander.dev/releases")

    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")

    maven("https://repo.viaversion.com")

    maven("https://jitpack.io")
}

println("loaderName: $loaderName")
println("mcVersion: $mcVersion")

val shade: Configuration by configurations.creating {
    isTransitive = false
}

val modCompileOnly: Configuration by configurations.creating {
    configurations.getByName("compileOnly").extendsFrom(this)
}

val modRuntimeOnly: Configuration by configurations.creating {
    configurations.getByName("runtimeOnly").extendsFrom(this)
}

minecraft.apply {
    mods.remap(modCompileOnly)
    mods.remap(modRuntimeOnly)
}

dependencies {
    fun bundle(dependency: Any) {
        if (isFabric) {
            "include"(dependency)
        } else {
            "shade"(dependency)
        }
    }

    fun bundleImplementation(dependency: Any) {
        implementation(dependency)
        bundle(dependency)
    }

    bundleImplementation("org.quiltmc.qup:json:0.2.0")
    if (isForgeLike) {
        "minecraftLibraries"("org.quiltmc.qup:json:0.2.0")
    }

    //TODO: bump to unimined 1.1.0+ to use these, also enable the processor in unimined's mixin config settings
//    includeImplementation("com.github.LlamaLad7.MixinExtras:mixinextras-${mcData.loader.name}:0.2.0-beta.6")h
//    if (mcData.isForge) {
//        implementation("com.github.LlamaLad7.MixinExtras:mixinextras-common:0.2.0-beta.6")
//    }

    if (isFabric) {
        when (mcVersion) {
            1_20_02 -> "8.0.0-beta.2" // TODO: Update out of beta
            1_20_01 -> "7.0.1"
            1_19_04 -> "6.2.3"
            1_19_02 -> "4.2.0-beta.2"
            1_18_02 -> "3.2.5"
            1_17_01 -> "2.0.17"
            1_16_05 -> "1.16.23"
            else -> null
        }?.let {
            "modImplementation"("com.terraformersmc:modmenu:$it")
        }
        if (mcVersion == 1_16_01) {
            "modImplementation"("io.github.prospector:modmenu:1.14.5+build.30")
        }
    }

    if (!isNeoForge) { // TODO: Remove check when DevAuth for NeoForge is released (which depends on Arch)
        modRuntimeOnly("me.djtheredstoner:DevAuth-${if (isFabric) "fabric" else "forge-latest"}:1.1.2")
    }

    if (isFabric) {
        when (mcVersion) {
            1_20_02 -> "0.89.2+1.20.2"
            1_20_01 -> "0.89.0+1.20.1"
            1_19_04 -> "0.87.0+1.19.4"
            1_19_02 -> "0.76.1+1.19.2"
            1_18_02 -> "0.76.0+1.18.2"
            1_17_01 -> "0.46.1+1.17"
            1_16_05, 1_16_01 -> "0.42.0+1.16"
            else -> null
        }?.let { fabricApi.fabricModule("fabric-resource-loader-v0", it) }
            ?.let {
                "modImplementation"(it)
                bundle(it)
            }
    }

    if (isFabric && mcVersion >= 1_18_02) {
        modCompileOnly("dev.isxander:main-menu-credits:1.1.2")
    }

    if (isFabric) {
        when {
            mcVersion >= 1_20_02 -> "2.9.0"
            mcVersion >= 1_20_01 -> "2.8.7-SNAPSHOT"
            mcVersion >= 1_20_00 -> "2.7.6"
            mcVersion >= 1_19_04 -> "2.7.5"
            else -> null
        }?.let {
            modCompileOnly("de.florianmichael:viafabricplus:$it") {
                isTransitive = false
            }
        }
    }

    compileOnly("com.demonwav.mcdev:annotations:2.0.0")
}

java {
    withSourcesJar()
}

preprocess {
    fun Boolean.toInt() = if (this) 1 else 0

    vars.putAll(mapOf(
        "FABRIC" to isFabric.toInt(),
        "FORGE" to isForge.toInt(),
        "NEOFORGE" to isNeoForge.toInt(),
        "FORGELIKE" to isForgeLike.toInt(),
        "MC" to mcVersion,
    ))

    patternAnnotation.set("io.github.gaming32.worldhost.versions.Pattern")
    keywords.value(keywords.get())
    keywords.put(".json", PreprocessTask.DEFAULT_KEYWORDS.copy(eval = "//??"))
    keywords.put(".toml", PreprocessTask.CFG_KEYWORDS.copy(eval = "#??"))
}

//println("Parallel: ${gradle.startParameter.isParallelProjectExecutionEnabled}")

modrinth {
    val isStaging = true
    token.set(
        project.properties["modrinth.token${if (isStaging) ".staging" else ""}"] as String?
            ?: System.getenv("MODRINTH_TOKEN${if (isStaging) "_STAGING" else ""}")
    )
    if (isStaging) {
        apiUrl.set(ModrinthExtension.STAGING_API_URL)
    }
    projectId.set("world-host")
    versionNumber.set(version.toString())
    val loadersText = when {
        isFabric -> "Fabric/Quilt"
        isForge -> "Forge"
        isNeoForge -> "NeoForge"
        else -> throw IllegalStateException()
    }
    versionName.set("[$loadersText $mcVersionString] World Host $modVersion")
    uploadFile.set(tasks.named("remapJar"))
    additionalFiles.add(tasks.named("sourcesJar"))
    gameVersions.add(mcVersionString)
    if (mcVersion == 1_19_04) {
        gameVersions.add("23w13a_or_b")
    } else if (mcVersion == 1_20_01) {
        gameVersions.add("1.20")
    }
    loaders.add(loaderName)
    if (isFabric) {
        loaders.add("quilt")
    }
    dependencies {
        if (isFabric) {
            optional.project(if (isStaging) "fred-3" else "modmenu")
        }
    }
    rootProject.file("changelogs/$modVersion.md").let {
        if (it.exists()) {
            println("Setting changelog file to $it")
            changelog.set(it.readText())
        } else {
            println("Changelog file $it does not exist!")
        }
    }
}

tasks.processResources {
    filesMatching("pack.mcmeta") {
        expand("pack_format" to when {
            mcVersion >= 1_20_02 -> 18
            mcVersion >= 1_20_00 -> 15
            mcVersion >= 1_19_04 -> 13
            mcVersion >= 1_19_03 -> 12
            mcVersion >= 1_19_00 -> 9
            mcVersion >= 1_18_00 -> 8
            mcVersion >= 1_17_00 -> 7
            mcVersion >= 1_16_02 -> 6
            mcVersion >= 1_15_00 -> 5
            mcVersion >= 1_13_00 -> 4
            mcVersion >= 1_11_00 -> 3
            mcVersion >= 1_09_00 -> 2
            mcVersion >= 1_06_01 -> 1
            else -> return@filesMatching
        })
    }

    filesMatching("fabric.mod.json") {
        filter {
            if (it.trim().startsWith("//")) "" else it
        }
    }

    filesMatching(listOf(
        "mcmod.info",
        "fabric.mod.json",
        "quilt.mod.json",
        "META-INF/mods.toml",
        "mixins.*.json",
        "*.mixins.json"
    )) {
        expand(mapOf(
            "version" to modVersion,
            "mc_version" to mcVersionString,
            "java_version" to "JAVA_${mcJavaVersion.majorVersion}"
        ))
    }

    if (isFabric) {
        exclude("pack.mcmeta", "META-INF/mods.toml")
    } else {
        exclude("fabric.mod.json")
    }

    doLast {
        val resources = "${layout.buildDirectory.get()}/resources/main"
        if (isForgeLike) {
            copy {
                from(file("$resources/assets/world-host/icon.png"))
                into(resources)
            }
            delete(file("$resources/assets/world-host/icon.png"))
        } else {
            delete(file("$resources/pack.mcmeta"))
        }
    }
}

tasks.withType<RemapJarTask> {
    shade.files.forEach { from(project.zipTree(it)) }
    manifest {
        if (isForge) {
            attributes["MixinConfigs"] = "world-host.mixins.json"
        }
    }
}

val mcJavaVersion = (minecraft as MinecraftProvider).minecraftData.metadata.javaVersion

if (mcJavaVersion < java.sourceCompatibility) {
    val targetClassVersion = mcJavaVersion.ordinal + 45
    println("Classes need downgrading to Java $mcJavaVersion ($targetClassVersion)")

    val downgradeClasses by tasks.registering(DowngradeSourceSetTask::class) {
        dependsOn(tasks.classes)
        sourceSet.set(sourceSets["main"])
        targetVersion.set(targetClassVersion)
    }
    tasks.classes.get().finalizedBy(downgradeClasses)
}
