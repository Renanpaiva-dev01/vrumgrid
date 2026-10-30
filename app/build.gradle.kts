import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

/**
 * IDs de produção do AdMob ficam fora do repositório, em `local.properties` (não versionado)
 * ou em propriedades do Gradle/variáveis de ambiente do CI:
 *   miaugrid.admob.appId=ca-app-pub-XXXXXXXX~YYYYYYYY
 *   miaugrid.admob.rewardedId=ca-app-pub-XXXXXXXX/ZZZZZZZZ
 *   miaugrid.admob.interstitialId=ca-app-pub-XXXXXXXX/WWWWWWWW
 * Sem eles, a build de release é gerada com a publicidade DESLIGADA.
 */
val localProps = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
fun adProp(name: String): String? =
    (localProps.getProperty(name) ?: providers.gradleProperty(name).orNull)?.takeIf { it.isNotBlank() }

// IDs oficiais de TESTE do Google (https://developers.google.com/admob/android/test-ads).
val testAppId = "ca-app-pub-3940256099942544~3347511713"
val testRewardedId = "ca-app-pub-3940256099942544/5224354917"
val testInterstitialId = "ca-app-pub-3940256099942544/1033173712"

val prodAppId = adProp("miaugrid.admob.appId")
val prodRewardedId = adProp("miaugrid.admob.rewardedId")
val prodInterstitialId = adProp("miaugrid.admob.interstitialId")
val prodAdsConfigured = prodAppId != null && prodRewardedId != null && prodInterstitialId != null

android {
    namespace = "com.miaugrid.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.miaugrid.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "com.miaugrid.app.VrumGridTestRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            manifestPlaceholders["admobAppId"] = testAppId
            buildConfigField("boolean", "ADS_ENABLED", "true")
            buildConfigField("boolean", "ADS_ARE_TEST", "true")
            buildConfigField("String", "REWARDED_AD_UNIT", "\"$testRewardedId\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"$testInterstitialId\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // O SDK exige um APPLICATION_ID no manifesto; sem IDs de produção usamos o de teste
            // apenas para não travar a inicialização, e a publicidade fica desligada.
            manifestPlaceholders["admobAppId"] = prodAppId ?: testAppId
            buildConfigField("boolean", "ADS_ENABLED", prodAdsConfigured.toString())
            buildConfigField("boolean", "ADS_ARE_TEST", "false")
            buildConfigField("String", "REWARDED_AD_UNIT", "\"${prodRewardedId.orEmpty()}\"")
            buildConfigField("String", "INTERSTITIAL_AD_UNIT", "\"${prodInterstitialId.orEmpty()}\"")
            // Assinatura: configure um signingConfig próprio (keystore fora do repositório).
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation("com.miaugrid:core")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.play.services.ads)
    implementation(libs.ump)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
