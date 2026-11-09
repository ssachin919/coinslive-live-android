import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(keystorePropertiesFile.inputStream())
}

fun String?.envOrNull(): String? = this?.takeIf { it.isNotBlank() }

android {
    namespace = "in.coinslive.live"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "in.coinslive.live"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "0.1.1"

        buildConfigField("String", "API_BASE_URL", "\"https://www.coinslive.in\"")
        // Clerk publishable keys are public by design (same as iOS Config.xcconfig).
        buildConfigField(
            "String",
            "CLERK_PUBLISHABLE_KEY",
            "\"pk_live_Y2xlcmsuY29pbnNsaXZlLmluJA\"",
        )
        manifestPlaceholders["deepLinkScheme"] = "coinslive"
    }

    signingConfigs {
        create("release") {
            val storeFilePath =
                (keystoreProperties["storeFile"] as String?).envOrNull()
                    ?: System.getenv("KEYSTORE_FILE").envOrNull()
            val storePasswordValue =
                (keystoreProperties["storePassword"] as String?).envOrNull()
                    ?: System.getenv("KEYSTORE_PASSWORD").envOrNull()
            val keyAliasValue =
                (keystoreProperties["keyAlias"] as String?).envOrNull()
                    ?: System.getenv("KEY_ALIAS").envOrNull()
            val keyPasswordValue =
                (keystoreProperties["keyPassword"] as String?).envOrNull()
                    ?: System.getenv("KEY_PASSWORD").envOrNull()

            if (
                storeFilePath != null &&
                storePasswordValue != null &&
                keyAliasValue != null &&
                keyPasswordValue != null
            ) {
                storeFile = rootProject.file(storeFilePath)
                storePassword = storePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            ndk {
                debugSymbolLevel = "full"
            }
            signingConfigs.getByName("release").storeFile?.let {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "CoinsLive Live Dev")
            isMinifyEnabled = false
            isShrinkResources = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
        resValues = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okhttp)
    implementation(libs.datastore.preferences)
    implementation(libs.clerk.android.ui)
    implementation(libs.livekit.android)
    implementation(libs.livekit.android.camerax)
    implementation(libs.livekit.compose.components)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
