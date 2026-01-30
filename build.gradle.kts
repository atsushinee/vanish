import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    // 升级到 Kotlin 2.0.0 或更高版本（例如 2.0.20 是当前稳定版）
    kotlin("jvm") version "2.2.21"
    id("org.jetbrains.compose") version "1.6.10"
    // 同步升级序列化插件版本
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.21"
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
    val ktorVersion = "3.3.3" // 这个版本是为 Kotlin 2.x 准备的，没问题
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // 注意：协程库也需要兼容 Kotlin 2.x 的版本
    // 1.8.1 可能太旧了，建议升级
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0-RC.2")

    // 同样，序列化库也需要匹配 Kotlin 2.x
    // 1.7.3 是为 Kotlin 1.9 准备的，应升级
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3") // 1.7.3 实际上也支持 Kotlin 2.0，可以先保留试试。如果不行再升到 1.8.0-RC。

    implementation("ch.qos.logback:logback-classic:1.4.14")

    implementation("com.google.genai:google-genai:1.35.0")
    implementation("com.volcengine:volcengine-java-sdk-ark-runtime:1.0.6")
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