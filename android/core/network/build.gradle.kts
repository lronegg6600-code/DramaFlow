plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.dramaflow.core.network"
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
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8083/\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"http://10.0.2.2:8081/\"")
            buildConfigField("String", "CONTENT_BASE_URL", "\"http://10.0.2.2:8082/\"")
            buildConfigField("String", "PROGRESS_BASE_URL", "\"http://10.0.2.2:8084/\"")
            buildConfigField("String", "PLAYBACK_BASE_URL", "\"http://10.0.2.2:8085/\"")
            buildConfigField("String", "ENTITLEMENT_BASE_URL", "\"http://10.0.2.2:8086/\"")
            buildConfigField("String", "BILLING_BASE_URL", "\"http://10.0.2.2:8087/\"")
            buildConfigField("String", "NETWORK_ENV", "\"debug\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.placeholder.dramaflow/\"")
            buildConfigField("String", "AUTH_BASE_URL", "\"https://auth.placeholder.dramaflow/\"")
            buildConfigField("String", "CONTENT_BASE_URL", "\"https://content.placeholder.dramaflow/\"")
            buildConfigField("String", "PROGRESS_BASE_URL", "\"https://progress.placeholder.dramaflow/\"")
            buildConfigField("String", "PLAYBACK_BASE_URL", "\"https://playback.placeholder.dramaflow/\"")
            buildConfigField("String", "ENTITLEMENT_BASE_URL", "\"https://entitlement.placeholder.dramaflow/\"")
            buildConfigField("String", "BILLING_BASE_URL", "\"https://billing.placeholder.dramaflow/\"")
            buildConfigField("String", "NETWORK_ENV", "\"release\"")
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

    implementation(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp.core)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)

    testImplementation(libs.junit4)
}
