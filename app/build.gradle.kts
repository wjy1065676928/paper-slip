import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.baselineprofile)
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
        versionCode = 2
        versionName = "2.0"

        // 🔥 极致体积优化：只保留 arm64
        ndk {
            //noinspection ChromeOsAbiSupport
            abiFilters += listOf("arm64-v8a")
        }
    }

    val keystoreProperties = Properties().apply {
        val f = rootProject.file("local.properties")
        if (f.exists()) load(f.inputStream())
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties.getProperty("path") ?: "keystore.jks")
            storePassword = keystoreProperties.getProperty("KEYSTORE_PASSWORD") ?: ""
            keyAlias = keystoreProperties.getProperty("KEY_ALIAS") ?: ""
            keyPassword = keystoreProperties.getProperty("KEY_PASSWORD") ?: ""
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

    // 🚀 Android App Bundle 配置
    bundle {
        language {
            // 只打包中文语言资源
            enableSplit = false
        }
        density {
            // 不分 density 模块（因为我们只有通用资源）
            enableSplit = false
        }
        abi {
            // 根据 ABI 分拆（配合仅 arm64 配置，效果更好）
            enableSplit = true
        }
    }

    packaging {
        resources {
            // 🚀 极致资源剔除
            excludes += setOf(
                "META-INF/*.version",
                "META-INF/*.properties",
                "META-INF/*kotlin*",
                "META-INF/*room*",
                "META-INF/*coroutines*",
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

    // 🚀 Baseline Profile（性能提升 — 需要设备执行生成任务）
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.profileinstaller)

    debugImplementation(libs.androidx.ui.tooling)
}