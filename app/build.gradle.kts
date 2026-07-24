plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.staticquo"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.staticquo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val storeFileEnv = System.getenv("STORE_FILE")
            if (storeFileEnv != null) {
                storeFile = file(storeFileEnv)
                storePassword = System.getenv("STORE_PASSWORD")
                keyAlias = System.getenv("KEY_ALIAS")
                keyPassword = System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
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
}

tasks.register("checkNoTodos") {
    doLast {
        val violations = mutableListOf<String>()
        fileTree("src/main/java").visit { details ->
            if (!details.isDirectory && details.name.endsWith(".kt")) {
                val file = details.file
                file.readLines().forEachIndexed { index, line ->
                    if (line.contains(Regex("\\b(TODO|FIXME|stub|STUB|todo|fixme)\\b")) &&
                        !line.contains(Regex("(TODO|FIXME|stub|STUB|todo|fixme)\\s*\\(\\s*\"(ongoing|planned|future|next)\\s*\\)"))
                    ) {
                        violations.add("${file.relativeTo(projectDir)}:$index: $line")
                    }
                }
            }
        }
        if (violations.isNotEmpty()) {
            throw GradleException(
                "Found TODO/FIXME/stub markers in completed code:\n${violations.joinToString("\n")}"
            )
        }
    }
}
tasks.named("check") { dependsOn("checkNoTodos") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons)
    debugImplementation(libs.compose.ui.tooling)

    // Navigation
    implementation(libs.navigation.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)
    implementation(libs.hilt.navigation.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Argon2 for PIN hashing
    implementation(libs.argon2kt)

    // Security
    implementation(libs.tink.android)

    // MapLibre
    implementation(libs.maplibre.sdk)

    // Location
    implementation(libs.play.services.location)

    // HTTP
    implementation(libs.okhttp)
    implementation(libs.okio)

    // Valhalla Routing
    implementation(libs.valhalla.mobile)

    // Coroutines Play Services
    implementation(libs.coroutines.play.services)
}
