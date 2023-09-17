import groovy.lang.GroovyObjectSupport
import xyz.wagyourtail.unimined.internal.minecraft.MinecraftProvider
import xyz.wagyourtail.unimined.util.sourceSets

import net.lenni0451.classtransform.TransformerManager
import net.lenni0451.classtransform.utils.ASMUtils
import net.lenni0451.classtransform.utils.tree.BasicClassProvider
import net.lenni0451.classtransform.utils.tree.IClassProvider
import net.raphimc.javadowngrader.JavaDowngrader
import net.raphimc.javadowngrader.runtime.RuntimeRoot
import org.jetbrains.kotlin.incremental.isClassFile
import xyz.wagyourtail.unimined.api.minecraft.MinecraftConfig
import xyz.wagyourtail.unimined.api.task.ExportMappingsTask
import xyz.wagyourtail.unimined.internal.mapping.MappingsProvider
import xyz.wagyourtail.unimined.internal.mapping.task.ExportMappingsTaskImpl
import java.nio.ByteBuffer
import java.nio.file.*
import java.nio.file.Path
import java.util.function.Supplier
import kotlin.streams.asSequence

plugins {
    java
    `maven-publish`
    id("xyz.deftu.gradle.preprocess")
    id("xyz.wagyourtail.unimined")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}


fun Any.setGroovyProperty(name: String, value: Any) = withGroovyBuilder { metaClass }.setProperty(this, name, value)
fun Any.getGroovyProperty(name: String): Any = withGroovyBuilder { metaClass }.getProperty(this, name)!!


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
                1_20_01 -> "47.1.3"
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

buildscript {
    repositories {
        maven("https://maven.lenni0451.net/snapshots")
    }

    dependencies {
        classpath("net.raphimc.javadowngrader:core:1.0.0-SNAPSHOT")
        classpath("net.lenni0451.classtransform:core:1.9.1")
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

val shade by configurations.creating

val bundle: Configuration by configurations.creating {
    configurations.getByName(if (loaderName == "fabric") "include" else "shade").extendsFrom(this)
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
    fun bundleImplementation(dependency: Any) {
        implementation(dependency)
        bundle(dependency)
    }


    bundleImplementation("org.quiltmc.qup:json:0.2.0")

    //TODO: bump to unimined 1.1.0+ to use these, also enable the processor in unimined's mixin config settings
//    includeImplementation("com.github.LlamaLad7.MixinExtras:mixinextras-${mcData.loader.name}:0.2.0-beta.6")h
//    if (mcData.isForge) {
//        implementation("com.github.LlamaLad7.MixinExtras:mixinextras-common:0.2.0-beta.6")
//    }

    if (loaderName == "fabric") {
        when (mcVersion) {
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

//    modRuntimeOnly("me.djtheredstoner:DevAuth-${if (mcData.isFabric) "fabric" else "forge-latest"}:1.1.2")

    if (loaderName == "fabric") {
        when (mcVersion) {
            1_20_01 -> "0.83.0+1.20.1"
            1_19_04 -> "0.80.0+1.19.4"
            1_19_02 -> "0.76.0+1.19.2"
            1_18_02 -> "0.76.0+1.18.2"
            1_17_01 -> "0.46.1+1.17"
            1_16_05, 1_16_01 -> "0.42.0+1.16"
            else -> null
        }?.let { fabricApi.module("fabric-resource-loader-v0", it) }
            ?.let {
                "modImplementation"(it)
                bundle(it)
            }
    }

    if (loaderName == "fabric" && mcVersion >= 1_18_02) {
        modCompileOnly("dev.isxander:main-menu-credits:1.1.2")
    }

    if (loaderName == "fabric") {
        when {
            mcVersion >= 1_20_00 -> "2.7.6"
            mcVersion >= 1_19_04 -> "2.7.5"
            else -> null
        }?.let {
            modCompileOnly("de.florianmichael:viafabricplus:$it") {
                isTransitive = false
            }
        }
    }
}

java {
    withSourcesJar()
}

preprocess {
    vars.putAll(mapOf(
        "FORGE" to 0,
        "FABRIC" to 0,
    ))
    vars.putAll(mapOf(
        loaderName.uppercase() to 1,
        "MC" to mcVersion
    ))

    patternAnnotation.set("io.github.gaming32.worldhost.versions.Pattern")
    keywords.put(".json", keywords.get().getValue(".json").copy(eval = "//??"))
}
// TODO: fix
//
//toolkitReleases {
//    modrinth {
//        projectId.set("world-host")
//    }
//    rootProject.file("changelogs/${modData.version}.md").let {
//        if (it.exists()) {
//            changelogFile.set(it)
//        }
//    }
//    describeFabricWithQuilt.set(true)
//    useSourcesJar.set(true)
//    if (mcData.isFabric) {
//        if (mcVersion == 1_19_04) {
//            gameVersions.add("23w13a_or_b")
//        } else if (mcVersion == 1_20_01) {
//            gameVersions.add("1.20")
//        }
//    }
//}

tasks.shadowJar {
    configurations = listOf(bundle)
}

tasks.processResources {
    filesMatching("pack.mcmeta") {
        expand("pack_format" to when {
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
            "version" to project.version,
            "mc_version" to mcVersionString,
            "java_version" to "JAVA_${mcJavaVersion.majorVersion}"
        ))
    }

    doLast {
        val resources = "${layout.buildDirectory.get()}/resources/main"
        if (loaderName == "forge") {
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

val mcJavaVersion = (minecraft as MinecraftProvider).minecraftData.metadata.javaVersion

if (mcJavaVersion < java.sourceCompatibility) {
    // These ClassProviders are from
    // https://github.com/RaphiMC/JavaDowngrader/tree/main/Standalone/src/main/java/net/raphimc/javadowngrader/standalone/transform
    abstract class AbstractClassProvider(val parent: IClassProvider? = null) : IClassProvider {
        override fun getClass(name: String): ByteArray =
            parent?.getClass(name)
                ?: throw NoSuchElementException("Unable to find class '$name'")

        override fun getAllClasses(): Map<String, Supplier<ByteArray>> = parent?.allClasses ?: mapOf()
    }

    open class PathClassProvider(
        private val root: Path, parent: IClassProvider? = null
    ) : AbstractClassProvider(parent) {
        override fun getClass(name: String): ByteArray {
            val path = root.resolve(name.replace('.', '/') + ".class")
            if (Files.exists(path)) {
                return Files.readAllBytes(path)
            }
            return super.getClass(name)
        }

        override fun getAllClasses() = super.getAllClasses() + Files.walk(root)
            .asSequence()
            .filter(Files::isRegularFile)
            .filter { it.toString().endsWith(".class") }
            .map {
                root.relativize(it)
                    .toString()
                    .removeSuffix(".class")
                    .replace('/', '.')
                    .replace('\\', '.') to Supplier { Files.readAllBytes(it) }
            }
    }

    class ClosingFileSystemClassProvider(
        private val fs: FileSystem, parent: IClassProvider? = null
    ) : PathClassProvider(fs.rootDirectories.first(), parent), AutoCloseable {
        override fun close() = fs.close()
    }

    class LazyFileClassProvider(
        path: List<File>, parent: IClassProvider? = null
    ) : AbstractClassProvider(parent), AutoCloseable {
        val path: Array<Any> = path.toTypedArray()

        override fun getClass(name: String): ByteArray {
            repeat(path.size) {
                var element = path[it]
                if (element is File) {
                    synchronized(path) {
                        element = path[it]
                        if (element is File) {
                            element = open(element as File)
                            path[it] = element
                        }
                    }
                }
                try {
                    @Suppress("KotlinConstantConditions")
                    return (element as PathClassProvider).getClass(name)
                } catch (_: NoSuchElementException) {
                }
            }
            return super.getClass(name)
        }

        private fun open(file: File) = ClosingFileSystemClassProvider(FileSystems.newFileSystem(file.toPath()))

        override fun close() = path.forEach { (it as? AutoCloseable)?.close() }
    }

    val targetClassVersion = mcJavaVersion.ordinal + 45
    println("Classes need downgrading to Java $mcJavaVersion ($targetClassVersion)")

    tasks.register("downgradeClasses") {
        dependsOn("classes")
        doLast {
            val buildClasses = file("build/classes/java/main")
            println("Downgrading classes")
            run {
                val transformerManager = TransformerManager(
                    PathClassProvider(
                        buildClasses.toPath(),
                        LazyFileClassProvider(
                            sourceSets.main.get().compileClasspath.toList(),
                            BasicClassProvider()
                        )
                    )
                )
                transformerManager.addBytecodeTransformer { className, bytecode, calculateStackMapFrames ->
                    if (ByteBuffer.wrap(bytecode, 4, 4).int <= targetClassVersion) {
                        return@addBytecodeTransformer null
                    }
                    if (!buildClasses.resolve(className.replace('.', '/') + ".class").exists()) {
                        return@addBytecodeTransformer null
                    }
                    val node = ASMUtils.fromBytes(bytecode)
                    JavaDowngrader.downgrade(node, targetClassVersion)
                    if (calculateStackMapFrames) {
                        ASMUtils.toBytes(node, transformerManager.classTree, transformerManager.classProvider)
                    } else {
                        ASMUtils.toStacklessBytes(node)
                    }
                }
                buildClasses.walk().forEach {
                    if (!it.isClassFile()) return@forEach
                    transformerManager.transform(
                        it.toRelativeString(buildClasses)
                            .removeSuffix(".class")
                            .replace('/', '.')
                            .replace('\\', '.'),
                        it.readBytes()
                    )?.let(it::writeBytes)
                }
            }
            println("Copying JavaDowngrader runtime classes")
            RuntimeRoot::class.java.getResource("RuntimeRoot.class")!!.let { url ->
                FileSystems.newFileSystem(url.toURI(), mapOf<String, Any>()).use { fs ->
                    val runtimePackage = RuntimeRoot::class.java.packageName.replace('.', '/')
                    val runtimeRoot = fs.getPath("/$runtimePackage")
                    Files.walk(runtimeRoot)
                        .asSequence()
                        .filter(Files::isRegularFile)
                        .filter { it.startsWith(runtimeRoot) }
                        .filter { it.toString().endsWith(".class") }
                        .filter { it.fileName.toString() != "RuntimeRoot.class" }
                        .forEach {
                            val inClasses = buildClasses.toPath().resolve(
                                "$runtimePackage/${runtimeRoot.relativize(it)}"
                            )
                            Files.createDirectories(inClasses.parent)
                            Files.copy(it, inClasses, StandardCopyOption.REPLACE_EXISTING)
                        }
                }
            }
        }
    }

    tasks.shadowJar.get().dependsOn += "downgradeClasses"
}
