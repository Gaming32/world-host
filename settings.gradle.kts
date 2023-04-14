pluginManagement {
    repositories {
        maven {
            name = "Fabric"
            url = uri("https://maven.fabricmc.net/")
        }
        gradlePluginPortal()
    }
}

rootProject.name = "world-host"

include("common")
include("mc-1.19.2")
include("mc-1.19.4")
include("gui-1.19.2")
include("gui-1.19.4")
include("monojar")
