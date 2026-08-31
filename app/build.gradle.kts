plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val releaseStoreFilePath = System.getenv("TIBO_RELEASE_STORE_FILE")
val releaseStorePassword = System.getenv("TIBO_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = System.getenv("TIBO_RELEASE_KEY_ALIAS")
val releaseKeyPassword = System.getenv("TIBO_RELEASE_KEY_PASSWORD")

val releaseSigningValues = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
)
val anyReleaseSigningValue = releaseSigningValues.any { !it.isNullOrBlank() }
val hasReleaseSigning = releaseSigningValues.all { !it.isNullOrBlank() }

check(!anyReleaseSigningValue || hasReleaseSigning) {
    "Release signing variables must be provided together: " +
        "TIBO_RELEASE_STORE_FILE, TIBO_RELEASE_STORE_PASSWORD, " +
        "TIBO_RELEASE_KEY_ALIAS, and TIBO_RELEASE_KEY_PASSWORD."
}

android {
    namespace = "com.tibobutton.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.tibobutton.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 5
        versionName = "0.3.1"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(requireNotNull(releaseStoreFilePath))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation("junit:junit:4.13.2")
}
