import org.gradle.api.Project
import java.io.ByteArrayOutputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

/** Monotonic-ish version so adb / package installer can upgrade over prior debug APKs. */
fun careerOpsVersionCode(project: Project): Int {
    return try {
        val out = ByteArrayOutputStream()
        project.exec {
            commandLine("git", "rev-list", "--count", "HEAD")
            workingDir = project.rootProject.projectDir
            standardOutput = out
        }
        out.toString().trim().toIntOrNull()?.coerceIn(1, 2_100_000_000) ?: 1
    } catch (_: Exception) {
        1
    }
}

android {
    namespace = "com.careerops.mobile"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.careerops.mobile"
        minSdk = 29
        targetSdk = 34
        versionCode = careerOpsVersionCode(project)
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        create("careerOpsSharedDebug") {
            storeFile = rootProject.file("keystore/career-ops-debug.jks")
            storePassword = "android"
            keyAlias = "careeropsdebug"
            keyPassword = "android"
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("careerOpsSharedDebug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

configurations.all {
    resolutionStrategy {
        force("androidx.core:core-ktx:1.13.1")
        force("androidx.core:core:1.13.1")
        // LiteRT-LM may pull kotlin-stdlib 2.x; pin to compiler-compatible stdlib.
        force("org.jetbrains.kotlin:kotlin-stdlib:1.9.24")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24")
        force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24")
        force("org.jetbrains.kotlin:kotlin-reflect:1.9.24")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("com.google.android.material:material:1.12.0")

    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.json:json:20240303")
    implementation("org.codeshipping:llama-kotlin-android:0.1.0")
    // Google AI Edge LiteRT-LM (Gemma on-device via .litertlm models)
    implementation("com.google.ai.edge.litertlm:litertlm-android:latest.release")
    implementation("androidx.activity:activity-ktx:1.9.1")
    implementation("androidx.browser:browser:1.8.0")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    testImplementation("junit:junit:4.13.2")
}
