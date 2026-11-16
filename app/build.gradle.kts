plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "app.nordico.launcher"
    compileSdk = 37

    defaultConfig {
        applicationId = "app.nordico.launcher"
        minSdk = 34
        targetSdk = 34
        versionCode = 1
        versionName = "0.1"
    }

    flavorDimensions += "target"
    productFlavors {
        create("sim") {
            dimension = "target"
            applicationIdSuffix = ".sim"
        }
        create("device") {
            dimension = "target"
        }
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.animation)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)
    implementation(libs.maplibre)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
