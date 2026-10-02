import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

// Load external API credentials from local.properties so they stay out of git.
//   USDA — falls back to public DEMO_KEY (rate-limited but works out of the box)
private val externalProps: Properties = run {
    val props = Properties()
    val localProps = rootProject.file("local.properties")
    if (localProps.exists()) localProps.inputStream().use { props.load(it) }
    props
}
private fun creds(name: String, default: String = ""): String =
    externalProps.getProperty(name) ?: System.getenv(name) ?: default

val usdaApiKey: String = creds("USDA_API_KEY", "DEMO_KEY")

android {
    namespace = "com.example.foodtracker"
    compileSdk {
        version = release(37)
    }
    compileSdkMinor = 2

    defaultConfig {
        applicationId = "com.example.foodtracker"
        minSdk = 31
        targetSdk = 37
        versionCode = 5
        versionName = "1.4"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "USDA_API_KEY", "\"$usdaApiKey\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)

    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.guava)
    // Play-Services-hosted ML Kit — the scan engine + model live in Google Play
    // Services, so our APK doesn't ship them (~19 MB saved across 4 ABIs).
    // Tradeoff: requires GMS installed on the device. Acceptable since the
    // target audience is mainstream Play Store users.
    implementation(libs.play.services.mlkit.barcode.scanning)

    // Jetpack Glance — Compose-style API for AppWidgets. We read directly from
    // the existing Room DAOs inside provideGlance (suspend context, no extra
    // serialization layer) and ping widgets to update after food log writes.
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
