plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "io.github.wjy.meditate.baselineprofile"
    compileSdk = 37

    defaultConfig {
        minSdk = 24
        targetSdk = 37

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    targetProjectPath = ":app"
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(libs.benchmark.macro.junit4)
    implementation(libs.test.ext.junit)
    implementation(libs.espresso.core)
    implementation(libs.test.uiautomator)
}
