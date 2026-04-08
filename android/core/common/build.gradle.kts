plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.dramaflow.core.common"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        debug {
            buildConfigField("String", "PLAYBACK_DATA_MODE", "\"HYBRID\"")
            buildConfigField("boolean", "ALLOW_PLAYBACK_FALLBACK", "true")
            buildConfigField("boolean", "USE_REAL_BILLING", "true")
            buildConfigField("boolean", "USE_REAL_ENTITLEMENTS", "true")
        }
        release {
            buildConfigField("String", "PLAYBACK_DATA_MODE", "\"REMOTE_PLAYBACK\"")
            buildConfigField("boolean", "ALLOW_PLAYBACK_FALLBACK", "false")
            buildConfigField("boolean", "USE_REAL_BILLING", "true")
            buildConfigField("boolean", "USE_REAL_ENTITLEMENTS", "true")
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
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.hilt.android)
    implementation(libs.kotlinx.coroutines.android)

    ksp(libs.hilt.compiler)
}
