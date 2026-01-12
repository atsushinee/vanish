import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.10"
}

group = "com.vanish"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    // Ktor for networking
    implementation("io.ktor:ktor-client-core:2.3.10")
    implementation("io.ktor:ktor-client-cio:2.3.10")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.10")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.10")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    // Kotlinx Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // 核心修正：使用 Compose Multiplatform 的版本目录来引入图标库
    // 目的：从根本上解决依赖版本冲突。
    // 原理：`compose.materialIconsExtended` 会让 `org.jetbrains.compose` 插件自动选择与项目中其他 Compose 库完全兼容的版本，
    // 从而避免了因手动指定版本（如 1.6.7）而导致的 `androidx.compose.ui` 重复或冲突，彻底消除了“幻象歧义”。
    implementation(compose.materialIconsExtended)
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Vanish"
            packageVersion = "1.0.0"
        }
    }
}
