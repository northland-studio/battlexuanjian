plugins {
    java
}

group = "com.northland"
version = "1.0.0"
description = "玄剑·战争 BattledXuanjian - 类《战地》大战场小游戏插件 (Paper 26.2+)"

/** Paper 服务端 API：26.2 及以上（动态解析 26.2 的最新稳定构建）。 */
val paperApiVersion = "26.2.build.+"

/** 可选依赖版本，仅编译期使用，运行期由服务端提供。 */
val protocolLibVersion = "5.4.0"
val placeholderApiVersion = "2.12.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc" }
    maven("https://repo.extendedclip.com/releases/") { name = "placeholderapi" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApiVersion")

    // 可选依赖：隐藏 Tab 列表中的敌方玩家 ID（5.4.0 起由 Maven Central 发布，groupId 为 net.dmulloy2）
    compileOnly("net.dmulloy2:ProtocolLib:$protocolLibVersion") { isTransitive = false }
    // 可选依赖：PlaceholderAPI 变量扩展
    compileOnly("me.clip:placeholderapi:$placeholderApiVersion") { isTransitive = false }

    testImplementation(platform("org.junit:junit-bom:5.11.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
    options.compilerArgs.addAll(listOf("-Xlint:all", "-Xlint:-processing", "-Xlint:-serial", "-Xlint:-deprecation", "-Xlint:-removal"))
}

tasks.processResources {
    val tokens = mapOf("version" to project.version.toString())
    inputs.properties(tokens)
    filesMatching("plugin.yml") {
        expand(tokens)
    }
}

tasks.jar {
    archiveFileName.set("BattledXuanjian-${project.version}.jar")
    manifest {
        attributes(
            "Implementation-Title" to "BattledXuanjian",
            "Implementation-Version" to project.version.toString(),
            "Implementation-Vendor" to "Northland Studio",
        )
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

/** CI 日志辅助：打印本次构建实际使用的环境信息。 */
tasks.register("printBuildInfo") {
    val api = paperApiVersion
    doLast {
        println("[BX-CI] project=${project.name} version=${project.version}")
        println("[BX-CI] paper-api=$api")
        println("[BX-CI] java-toolchain=${java.toolchain.languageVersion.get()}")
        println("[BX-CI] java-runtime=${System.getProperty("java.version")}")
        println("[BX-CI] os=${System.getProperty("os.name")}")
    }
}
