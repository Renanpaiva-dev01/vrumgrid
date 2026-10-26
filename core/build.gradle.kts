plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.miaugrid"
version = "1.0.0"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // Fazem parte da API pública do núcleo (StateFlow, KSerializer).
    api(libs.kotlinx.coroutines.core)
    api(libs.kotlinx.serialization.json)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    testLogging {
        events("failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

// Ferramenta de conteúdo: gera o pacote de fases da campanha a partir de sementes fixas
// (`gradle -p core generateLevels`). O resultado é verificado pelos testes.
tasks.register<JavaExec>("generateLevels") {
    group = "content"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.miaugrid.core.levels.tools.GenerateCampaignKt")
    args(file("src/main/resources/levels/campaign.json").absolutePath)
}
