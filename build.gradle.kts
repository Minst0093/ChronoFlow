plugins {
    // Kotlin Multiplatform / JVM 统一使用 2.0.0
    kotlin("multiplatform") version "2.0.0" apply false
    // Android Gradle Plugin 8.3.x（与 Kotlin 2.0.0 官方已测试兼容）
    id("com.android.application") version "8.13.2" apply false
    id("com.android.library") version "8.13.2" apply false
    // Kotlin Compose Compiler plugin（Kotlin 2.0+ 启用 Compose 必须引入）
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.0" apply false
}

