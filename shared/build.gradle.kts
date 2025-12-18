plugins {
    kotlin("multiplatform")
    id("com.android.library")
}

kotlin {
    androidTarget()

    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.0")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        val androidMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
            }
        }
        val androidUnitTest by getting
        val desktopMain by getting
        val desktopTest by getting
    }
}

android {
    namespace = "com.minst.chronoflow.shared"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    // --- Java 编译版本 ---
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}