import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.wjy.meditate"
    compileSdk = 37

    androidResources {
        @Suppress("UnstableApiUsage")
        localeFilters += listOf("zh", "zh-rCN")
    }

    defaultConfig {
        applicationId = "io.github.wjy.meditate"
        minSdk = 24
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        // 🔥 极致体积优化：只保留 arm64
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
    }

    val keystoreProperties = Properties().apply {
        load(rootProject.file("local.properties").inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(path)
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD")
            keyAlias = keystoreProperties.getProperty("KEY_ALIAS")
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            // 🔥 混淆 + 压缩
            isMinifyEnabled = true
            isShrinkResources = true
            isCrunchPngs = true
            // 🔒 安全
            isDebuggable = false

            // ⚡ 优化规则
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            signingConfig = signingConfigs.getByName("release")

            // 🚀 禁用日志（配合 Proguard）
            buildConfigField("boolean", "LOG_DEBUG", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            // 🚀 极致资源剔除
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/*.properties",
                "META-INF/*kotlin*",
                "META-INF/*room*",
                "META-INF/licenses/**",
                "**/debug/*"
            )
        }
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.core)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.zxing.cpp)
    implementation(libs.qrcode)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    implementation(libs.ktor.client.android)

    // 🚀 Baseline Profile（性能提升）
    implementation(libs.androidx.profileinstaller)

    debugImplementation(libs.androidx.ui.tooling)
}