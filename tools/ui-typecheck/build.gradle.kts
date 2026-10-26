plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.compose") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
}
val app = file("../../app/src/main/java")
val core = file("../../core/src/main/kotlin")
sourceSets["main"].kotlin.srcDirs(app, core, file("stubs"))
sourceSets["main"].resources.srcDirs(file("../../core/src/main/resources"))
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    implementation("org.jetbrains.compose.ui:ui-desktop:1.8.2")
    implementation("org.jetbrains.compose.foundation:foundation-desktop:1.8.2")
    implementation("org.jetbrains.compose.material3:material3-desktop:1.8.2")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.7.3")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-viewmodel-compose-desktop:2.9.1")
    implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose-desktop:2.9.1")
    implementation("org.jetbrains.androidx.navigation:navigation-compose-desktop:2.9.0")
}
configurations.all {
    // Artefatos androidx.* (KMP) só existem no Google Maven, bloqueado neste ambiente.
    exclude(group = "androidx.annotation")
    exclude(group = "androidx.collection")
    exclude(group = "androidx.lifecycle")
    exclude(group = "androidx.savedstate")
    exclude(group = "androidx.arch.core")
}
sourceSets["test"].kotlin.srcDirs(file("../../app/src/androidTest/java"), file("teststubs"))
dependencies {
    testImplementation("org.jetbrains.compose.ui:ui-test-junit4-desktop:1.8.2")
    testImplementation("junit:junit:4.13.2")
}
