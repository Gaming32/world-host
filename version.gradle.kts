import com.replaymod.gradle.preprocess.PreprocessTask
import java.net.NetworkInterface
import java.util.*

plugins {
    java
    `maven-publish`
    id("io.github.gaming32.gradle.preprocess")
    id("dev.architectury.loom")
    id("com.modrinth.minotaur")
    id("xyz.wagyourtail.jvmdowngrader")
}

group = "io.github.gaming32"

fun ByteArray.toHexString() = joinToString("") { "%02x".format(it) }

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
        it[0] * 1_00_00 + it[1] * 1_00 + it.getOrElse(2) { 0 }
    })

println("MC_VERSION: $mcVersionString $mcVersion")
version = "${modVersion}+${mcVersionString}-${loaderName}"

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases")
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

tasks.compileJava {
    options.release = 21
    options.compilerArgs.add("-Xlint:all")
}

val targetJava = when {
    mcVersion >= 1_20_06 -> JavaVersion.VERSION_21
    mcVersion >= 1_18_00 -> JavaVersion.VERSION_17
    else -> throw IllegalStateException("Unknown Java version for $mcVersionString")
}
if (targetJava < java.sourceCompatibility) {
    println("Classes need downgrading to Java $targetJava")

    tasks.downgradeJar {
        downgradeTo = targetJava
    }

    tasks.shadeDowngradedApi {
        downgradeTo = targetJava
        shadePath = { "io/github/gaming32/worldhost" }
    }

    tasks.remapJar {
        dependsOn(tasks.shadeDowngradedApi)
        inputFile = tasks.shadeDowngradedApi.get().archiveFile
    }
}

loom {
    @Suppress("UnstableApiUsage")
    mixin {
        useLegacyMixinAp = false
    }

    if (isForge) {
        forge {
            mixinConfigs("world-host.mixins.json")
        }
    }

    runs {
        getByName("client") {
            ideConfigGenerated(true)
            runDir = "run/client"
        }
        remove(getByName("server"))

        val usernameSuffix = NetworkInterface.getNetworkInterfaces()
            .nextElement()
            .hardwareAddress
            .toHexString()
            .substring(0, 10)
        for (name in listOf("Host", "Joiner")) {
            val runName = "test$name"
            val user = name.uppercase()
            register(runName) {
                inherit(getByName("client"))
                ideConfigGenerated(false)

                configName = "Test $user"
                runDir = "run/$runName"

                val username = "$user$usernameSuffix"
                programArgs(
                    "--username", username,
                    "--uuid", UUID.nameUUIDFromBytes("OfflinePlayer:$username".encodeToByteArray()).toString()
                )
                vmArgs(
                    "-Dworld-host-testing.enabled=true",
                    "-Dworld-host-testing.user=$user",
                    "-Ddevauth.enabled=false"
                )
            }
        }
    }
}

repositories {
    maven("https://maven.quiltmc.org/repository/release/")
    maven("https://maven.terraformersmc.com/releases")
    maven("https://maven.isxander.dev/releases")
    maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")
    maven("https://repo.viaversion.com")
    maven("https://maven.maxhenkel.de/repository/public")
    maven("https://api.modrinth.com/maven") {
        content {
            includeGroup("maven.modrinth")
        }
    }
}

println("loaderName: $loaderName")
println("mcVersion: $mcVersion")

dependencies {
    minecraft("com.mojang:minecraft:$mcVersionString")
    @Suppress("UnstableApiUsage")
    mappings(loom.layered {
        officialMojangMappings {
            nameSyntheticMembers = true
        }
        when {
            mcVersion >= 1_21_04 -> "1.21.4:2025.03.23"
            mcVersion >= 1_21_03 -> "1.21.3:2024.12.07"
            mcVersion >= 1_21_01 -> "1.21.1:2024.11.17"
            mcVersion >= 1_20_04 -> "1.20.4:2024.04.14"
            mcVersion >= 1_20_01 -> "1.20.1:2023.09.03"
            mcVersion >= 1_19_04 -> "1.19.4:2023.06.26"
            mcVersion >= 1_19_02 -> "1.19.2:2022.11.27"
            else -> null
        }?.let {
            parchment("org.parchmentmc.data:parchment-$it@zip")
        }
    })

    when {
        isFabric -> modImplementation("net.fabricmc:fabric-loader:0.16.10")
        isForge ->
            when (mcVersion) {
                1_20_01 -> "47.1.3"
                1_19_04 -> "45.1.0"
                1_19_02 -> "43.2.0"
                else -> throw IllegalStateException("Unknown Forge version for $mcVersionString")
            }.let { "forge"("net.minecraftforge:forge:$mcVersionString-$it") }
        isNeoForge ->
            when (mcVersion) {
                1_21_05 -> "21.5.26-beta"
                1_21_04 -> "21.4.121"
                1_21_03 -> "21.3.56"
                1_21_01 -> "21.1.1"
                1_20_04 -> "20.4.167"
                else -> throw IllegalStateException("Unknown NeoForge version for $mcVersionString")
            }.let { "neoForge"("net.neoforged:neoforge:$it") }
    }

    fun simpleJavaLibrary(notation: Any) = minecraftRuntimeLibraries(include(implementation(notation)!!)!!)

    simpleJavaLibrary("org.quiltmc.parsers:json:0.3.0")
    simpleJavaLibrary("org.semver4j:semver4j:5.3.0")

    if (isFabric) {
        when (mcVersion) {
            1_21_05 -> "14.0.0-rc.2"
            1_21_04 -> "13.0.3"
            1_21_03 -> "12.0.0"
            1_21_01 -> "11.0.3"
            1_20_04 -> "9.0.0"
            1_20_01 -> "7.2.2"
            1_19_04 -> "6.3.1"
            1_19_02 -> "4.2.0-beta.2"
            else -> null
        }?.let {
            modImplementation("com.terraformersmc:modmenu:$it")
        }
    }

    when {
        isFabric -> "fabric"
        isForge -> "forge-latest"
        isNeoForge -> "neoforge"
        else -> null
    }?.let { modRuntimeOnly("me.djtheredstoner:DevAuth-$it:1.2.1") }

    if (isFabric) {
        when (mcVersion) {
            1_21_05 -> "0.119.9+1.21.5"
            1_21_04 -> "0.119.2+1.21.4"
            1_21_03 -> "0.114.0+1.21.3"
            1_21_01 -> "0.115.3+1.21.1"
            1_20_04 -> "0.97.2+1.20.4"
            1_20_01 -> "0.92.5+1.20.1"
            1_19_04 -> "0.87.2+1.19.4"
            1_19_02 -> "0.77.0+1.19.2"
            else -> null
        }?.let { fapiVersion ->
            val resourceLoader = fabricApi.module("fabric-resource-loader-v0", fapiVersion)
            include(modImplementation(resourceLoader)!!)

            for (module in listOf(
                "fabric-screen-api-v1",
                "fabric-key-binding-api-v1",
                "fabric-lifecycle-events-v1"
            )) {
                modRuntimeOnly(fabricApi.module(module, fapiVersion))
            }
        }
    }

    if (isFabric) {
        modCompileOnly("dev.isxander:main-menu-credits:1.1.2") {
            isTransitive = false
        }
    }

    if (mcVersion >= 1_20_04 && isFabric) {
        modCompileOnly("de.florianmichael:viafabricplus:3.0.2") {
            isTransitive = false
        }
    }

    compileOnly("de.maxhenkel.voicechat:voicechat-api:2.5.0")
    when (mcVersion) {
        1_21_05 -> "2.5.28"
        1_21_04 -> "2.5.28"
        1_21_03 -> "2.5.28"
        1_21_01 -> "2.5.28"
        1_20_04 -> "2.5.22"
        1_20_01 -> "2.5.28"
        1_19_04 -> "2.5.12"
        1_19_02 -> "2.5.28"
        else -> null
    }?.let {
        modCompileOnly("maven.modrinth:simple-voice-chat:$loaderName-$mcVersionString-$it")
    }

    compileOnly("com.demonwav.mcdev:annotations:2.1.0")

    // Resolves javac warnings about Guava
    compileOnly("com.google.errorprone:error_prone_annotations:2.11.0")
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
    token.set(project.properties["modrinth.token"] as String? ?: System.getenv("MODRINTH_TOKEN"))
    projectId.set(if (isStaging) "world-host-staging" else "world-host")
    versionNumber.set(version.toString())
    val loaderName = when {
        isFabric -> "Fabric"
        isForge -> "Forge"
        isNeoForge -> "NeoForge"
        else -> throw IllegalStateException()
    }
    versionName.set("[$loaderName $mcVersionString] World Host $modVersion")
    uploadFile.set(tasks.named("remapJar"))
    additionalFiles.add(tasks.named("sourcesJar"))
    gameVersions.add(mcVersionString)
    when (mcVersion) {
        1_19_04 -> "23w13a_or_b"
        1_20_01 -> "1.20"
        1_20_04 -> "1.20.3"
        1_21_01 -> "1.21"
        1_21_03 -> "1.21.2"
        else -> null
    }?.let(gameVersions::add)
    loaders.add(this@Version_gradle.loaderName)
    dependencies {
        if (isFabric) {
            optional.project("modmenu")
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
    // TODO: Remove pack.mcmeta in 1.20.4
    filesMatching("pack.mcmeta") {
        expand("pack_format" to when {
            mcVersion >= 1_21_05 -> 55
            mcVersion >= 1_21_04 -> 46
            mcVersion >= 1_21_02 -> 42
            mcVersion >= 1_21_00 -> 34
            mcVersion >= 1_20_03 -> 22
            mcVersion >= 1_20_00 -> 15
            mcVersion >= 1_19_04 -> 13
            mcVersion >= 1_19_00 -> 9
            else -> return@filesMatching
        })
    }

    filesMatching("fabric.mod.json") {
        filter {
            if (it.trim().startsWith("//")) "" else it
        }
    }

    filesMatching(listOf(
        "fabric.mod.json",
        "META-INF/mods.toml",
        "META-INF/neoforge.mods.toml",
        "*.mixins.json"
    )) {
        expand(mapOf(
            "version" to version,
            "mc_version" to mcVersionString
        ))
    }

    if (isFabric) {
        exclude("pack.mcmeta", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")
    } else {
        exclude("fabric.mod.json")
        if (isNeoForge && mcVersion >= 1_20_05) {
            exclude("META-INF/mods.toml")
        } else {
            exclude("META-INF/neoforge.mods.toml")
        }
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

tasks.jar {
    archiveClassifier = "dev"
    from("$rootDir/LICENSE")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            artifactId = "world-host"
            artifact(tasks.named("remapJar"))
            artifact(tasks.named("sourcesJar")) {
                classifier = "sources"
            }
        }
    }
}
