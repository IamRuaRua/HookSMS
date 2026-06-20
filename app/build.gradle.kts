import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
val hasReleaseKeystore = keystorePropertiesFile.exists()
if (hasReleaseKeystore) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "top.cuteruarua.hooksms"
    compileSdk = 36

    defaultConfig {
        applicationId = "top.cuteruarua.hooksms"
        minSdk = 30
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseKeystore) {
                signingConfig = signingConfigs.getByName("release")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    // Xposed API
    compileOnly("de.robv.android.xposed:api:82")
    compileOnly("de.robv.android.xposed:api:82:sources")

    implementation("androidx.core:core-ktx:1.10.1")
    implementation("androidx.viewpager2:viewpager2:1.0.0")
    implementation("com.google.android.material:material:1.9.0")

    testImplementation("junit:junit:4.13.2")
}

android.applicationVariants.all {
    if (buildType.name == "release") {
        outputs.all {
            val version = versionName ?: "unknown"
            @Suppress("UNCHECKED_CAST")
            (this as? com.android.build.gradle.internal.api.BaseVariantOutputImpl)
                ?.outputFileName = "HookSMS_${version}.apk"
        }
    }
}
