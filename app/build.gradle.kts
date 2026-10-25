import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.macetaelefante"

    compileSdk {
        version = release(37) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.macetaelefante"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val perenualApiKey = providers.gradleProperty("PERENUAL_API_KEY")
            .orElse(providers.environmentVariable("PERENUAL_API_KEY"))
            .orElse(
                providers.provider {
                    val archivo = rootProject.file("local.properties")
                    if (archivo.exists()) {
                        Properties().apply {
                            archivo.inputStream().use { load(it) }
                        }.getProperty("PERENUAL_API_KEY", "")
                    } else {
                        ""
                    }
                }
            )
            .get()

        buildConfigField(
            "String",
            "PERENUAL_API_KEY",
            "\"${perenualApiKey.replace("\\", "\\\\").replace("\"", "\\\"")}\""
        )

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation("com.google.mlkit:translate:17.0.3")
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    // Reconocimiento de texto en fotografías
    implementation(libs.mlkit.text.recognition)

    testImplementation(libs.junit)

    androidTestImplementation(
        platform(libs.androidx.compose.bom)
    )
    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
    debugImplementation(libs.androidx.compose.ui.tooling)
}
