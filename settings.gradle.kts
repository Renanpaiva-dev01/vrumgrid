pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "MiauGrid"

// O núcleo do jogo (regras, solucionador, estado da partida) é uma build Kotlin/JVM
// independente: pode ser compilado e testado sem o Android SDK (`gradle -p core test`).
includeBuild("core")
include(":app")
