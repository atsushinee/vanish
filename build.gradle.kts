import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "1.9.23"
    id("org.jetbrains.compose") version "1.6.10"
    // 核心修正：添加 Kotlinx Serialization 编译器插件
    // 目的：这是解决 "Serializer not found" 异常的根本方法。
    // 原理：此插件会在编译时扫描所有带 @Serializable 注解的类，并为它们自动生成序列化和反序列化所需的代码（即 Serializer）。
    //      如果没有这个插件，Kotlin 编译器会完全忽略 @Serializable 注解，导致运行时 kotlinx.serialization 库找不到任何序列化器，从而抛出异常。
    // 注意：插件的版本应与项目中的 Kotlin 版本保持一致或兼容。此处使用与 kotlin("jvm") 相同的版本 "1.9.23"。
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.23"
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

    // Logback 日志实现
    // 目的：替换 slf4j-simple，使用功能更强大的 Logback 作为日志框架。
    // 原理：Logback 是一个成熟、稳定且功能丰富的日志实现，是 SLF4J 官方推荐的实现之一。
    //      `logback-classic` 依赖会自动传递 `slf4j-api` 和 `logback-core`，因此只需要添加这一个依赖即可。
    //      它允许通过 `logback.xml` 文件进行灵活的配置。
    implementation("ch.qos.logback:logback-classic:1.4.14")

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
