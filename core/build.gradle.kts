plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    // Общие правила сервера и клиента (сабмодуль backend/rules): контент, роллы, лист, заход по семени.
    api("com.sperance.exileforge:rules:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    api("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

kotlin { jvmToolchain(17) }

// Баланс-сим «классы против боссов» (отчёт, не тест): свой набор исходников поверх main - вне test и ворот CI, новых
// зависимостей не требует; видит internal main (прогон «Весов» OddsPlan). Запуск:
// ./gradlew :core:simulateBosses [-Pcontent=<папка content>] [-Pfights=<K>] [-Prolls=<бросков босса>] [-Pzones=<коды через запятую>]
val sim: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}
kotlin.target.compilations.getByName("sim").associateWith(kotlin.target.compilations.getByName("main"))

tasks.register<JavaExec>("simulateBosses") {
    group = "reporting"
    description = "Classes against zone bosses: build/reports/boss-sim.md"
    classpath = sim.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.sim.BossSimKt")
    val content = providers.gradleProperty("content").orElse(rootProject.layout.projectDirectory.dir("backend/src/main/resources/content").asFile.path)
    systemProperty("content", content.get())
    providers.gradleProperty("fights").orNull?.let { systemProperty("fights", it) }
    providers.gradleProperty("rolls").orNull?.let { systemProperty("rolls", it) }
    providers.gradleProperty("zones").orNull?.let { systemProperty("zones", it) }
    args(layout.buildDirectory.file("reports/boss-sim.md").get().asFile.path)
    outputs.upToDateWhen { false }
}

// A failing test in CI is read from the log, not from a report nobody can open: print the stack.
tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

/**
 * Закреплённый сервер (3.88.2) - из сабмодуля `backend/`, не из кода: коммит - его HEAD, ветка - из `.gitmodules`, версия -
 * как её считает сам сервер (база из `backend/server-version` плюс коммиты после её правки). Закрепить новый сервер -
 * сдвинуть сабмодуль; `ServerPin.kt` пишется при сборке.
 */
val backendDir = rootProject.layout.projectDirectory.dir("backend").asFile

fun git(dir: File, vararg args: String) = providers.exec {
    workingDir = dir
    commandLine("git", *args)
    isIgnoreExitValue = true
}.standardOutput.asText.map { it.trim() }

val pinCommit = git(backendDir, "rev-parse", "HEAD")
val pinBranch = git(rootDir, "config", "-f", ".gitmodules", "submodule.backend.branch")
val pinVersion = git(backendDir, "show", "HEAD:server-version").zip(git(backendDir, "log", "-1", "--format=%H", "--", "server-version")) { base, changed -> base to changed }
    .flatMap { (base, changed) ->
        git(backendDir, "rev-list", "--count", "$changed..HEAD").map { ahead ->
            val parts = base.split('.').mapNotNull(String::toIntOrNull)
            if (parts.size != 3 || changed.isBlank() || ahead.isBlank()) "?" else "${parts[0]}.${parts[1]}.${parts[2] + ahead.toInt()}"
        }
    }

val serverPin by tasks.registering {
    val version = pinVersion
    val commit = pinCommit
    val branch = pinBranch
    val out = layout.buildDirectory.dir("generated/serverPin/kotlin")
    inputs.property("version", version)
    inputs.property("commit", commit)
    inputs.property("branch", branch)
    outputs.dir(out)
    doLast {
        val file = out.get().file("com/sperance/exileforge/core/contract/ServerPin.kt").asFile
        file.parentFile.mkdirs()
        file.writeText(
            """
            |package com.sperance.exileforge.core.contract
            |
            |/** Сервер, против которого собран клиент: сабмодуль `backend/` на момент сборки (пишет Gradle, руками не править). */
            |const val SERVER_COMMIT = "${commit.get()}"
            |const val SERVER_BRANCH = "${branch.get()}"
            |const val SERVER_VERSION = "${version.get()}"
            |
            """.trimMargin(),
        )
    }
}
kotlin.sourceSets.main { kotlin.srcDir(serverPin) }
