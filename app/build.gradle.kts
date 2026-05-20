plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.ioristudios.anydoc"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.ioristudios.anydoc"
        minSdk = 29
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
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
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
            excludes += "META-INF/versions/9/OSGI-INF/MANIFEST.MF"
        }
        jniLibs {
            keepDebugSymbols += "**/libdatastore_shared_counter.so"
        }
    }
}

configurations.all {
    resolutionStrategy.force(
        "org.jetbrains.kotlin:kotlin-stdlib:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.9.24",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.24",
        "org.jetbrains.kotlin:kotlin-reflect:1.9.24"
    )
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)

    // Core
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.2")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Material3
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // File detection.
    implementation("org.apache.tika:tika-core:2.9.2")

    // DOCX/XLSX basic editing is implemented with a small OpenXML ZIP engine in
    // DocumentFileIo. Apache POI is intentionally not bundled here because the
    // Android build would carry a large unused JVM dependency surface.

    // PDF rendering uses Android PdfRenderer. Jetpack PDF alpha18 currently requires
    // AGP 8.9.1, compileSdk 36, and SDK extension 19, which this project does not have yet.

    // Text/code editing uses a Compose fallback for this Kotlin 1.9 project.
    // Sora Editor 0.24.4 currently pulls Kotlin 2.2 artifacts, which are not
    // readable by the Kotlin 1.9 compiler configured here.

    // Debug
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
