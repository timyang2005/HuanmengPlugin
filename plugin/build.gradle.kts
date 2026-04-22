plugins {
    alias(libs.plugins.android.application)
    // AGP 9.0+ 已内置 kotlin.android，无需再声明
    // Kotlin 2.0+ 必须显式声明 compose.compiler
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.huanmeng.plugin"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.huanmeng.plugin"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // AGP 9.0+ 内置 Kotlin，jvmTarget 通过 kotlin DSL 设置
    kotlin {
        jvmToolchain(17)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    // LightNovelReader Plugin API
    compileOnly(libs.lightnovelreader.api)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.foundation.layout)
    implementation(libs.androidx.navigation.runtime.ktx)

    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // Network
    implementation(libs.cxhttp)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)

    // HTML Parsing
    implementation(libs.jsoup)
}
