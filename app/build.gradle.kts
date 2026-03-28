import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "io.github.wjy.meditate"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.github.wjy.meditate"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures {
        compose = true
    }
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    
    // 添加扩展图标库
    implementation("androidx.compose.material:material-icons-extended")
    
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    implementation(libs.androidx.datastore.preferences)
    
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)

    debugImplementation(libs.androidx.ui.tooling)
}
