import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
    alias(libs.plugins.android.application)
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
        versionCode = 8
        versionName = "1.6.0"
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    compileOptions {
        // 启用 desugar 以支持 java.time 在低 API 设备上编译
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach {
            val outputImpl = it as com.android.build.api.variant.impl.VariantOutputImpl
            val originalFileName = outputImpl.outputFileName.get()
            val newFileName = originalFileName.replace(".apk", ".apk.lnrp")
            outputImpl.outputFileName = newFileName
        }
    }
}

tasks.withType<KotlinJvmCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

dependencies {
    // 核心：使用 compileOnly 引入 desugar 库
    // 这样编译时 java.time 会被转换为 j$.time，但 desugar 运行时不打包进 APK
    // 运行时由 LNR 宿主 App 提供 j$.time 类，避免版本冲突
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.androidx.runtime)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.foundation.layout)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.cxhttp)
    implementation(libs.okhttp3.okhttp)
    implementation(libs.okhttp3.logging.interceptor)
    implementation(libs.jsoup)

    //LNR Api
    compileOnly(libs.lightnovelreader.api)
}
