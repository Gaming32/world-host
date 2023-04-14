version = "0.2.6"
group = "io.github.gaming32.world-host"

subprojects {
    repositories {
        mavenCentral()

        maven("https://maven.fabricmc.net/")

        maven("https://maven.terraformersmc.com/releases")

        maven {
            name = "ParchmentMC"
            url = uri("https://maven.parchmentmc.org")
        }

        maven("https://pkgs.dev.azure.com/djtheredstoner/DevAuth/_packaging/public/maven/v1")

        maven("https://jitpack.io")
    }
}
