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
    // OkHttp - деталь ядра (4.12.x, ARCH-03): наружу - свои типы (`HttpLink`), `:app` его не видит
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

kotlin { jvmToolchain(17) }

// Симуляции баланса (отчёты, не тесты): свой набор исходников поверх main - вне test и ворот CI, новых зависимостей не требует;
// видит internal main. Запуск - задача `mobBal` ниже.
val sim: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}
kotlin.target.compilations.getByName("sim").associateWith(kotlin.target.compilations.getByName("main"))
// Сторож темпа MOB-BAL (`MobBalGuardTest`) зовёт эталонных героев сима: тестам виден набор sim
sourceSets.test {
    compileClasspath += sim.output
    runtimeClasspath += sim.output
}

// Баланс монстров MOB-BAL (отчёт, не тест): эталонные герои против монстров и боссов кампании и карт настоящим боем клиента.
// ./gradlew :core:mobBal [-Pcontent=<папка>] -Pmode=<campaign|probe|maps|sweep|runtime|bossmight|...> [-Pout=<tsv>] [-Pfights=16 -Prolls=4 ...]
tasks.register<JavaExec>("mobBal") {
    group = "reporting"
    description = "Monster and boss balance against reference heroes: build/reports/mobbal.tsv"
    classpath = sim.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.sim.MobBalKt")
    maxHeapSize = "6g"
    val content = providers.gradleProperty("content").orElse(rootProject.layout.projectDirectory.dir("backend/src/main/resources/content").asFile.path)
    systemProperty("content", content.get())
    systemProperty("heroCache", providers.gradleProperty("heroCache").getOrElse(layout.buildDirectory.dir("mobbal-heroes").get().asFile.path))
    listOf("mode", "fights", "rolls", "samples", "cap", "classes", "zones", "tiers", "mapRarities", "mobs", "level", "band", "dummy", "levels", "variants", "force", "ladder", "countScales", "retune", "source", "gearSeeds").forEach { name -> providers.gradleProperty(name).orNull?.let { systemProperty(name, it) } }
    args(providers.gradleProperty("out").getOrElse(layout.buildDirectory.file("reports/mobbal.tsv").get().asFile.path))
    outputs.upToDateWhen { false }
}

// Нагрузочный прогон аудита (отчёт, не тест): виртуальные игроки на настоящем клиенте против ЛОКАЛЬНОГО сервера,
// вне test и ворот CI. Запуск: ./gradlew :core:loadRun -Purl=http://localhost:8081/ -Pplayers=50 -Pseconds=120 -Pprofile=mixed
val load: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}
kotlin.target.compilations.getByName("load").associateWith(kotlin.target.compilations.getByName("main"))

tasks.register<JavaExec>("loadRun") {
    group = "reporting"
    description = "Load profile against a local server: build/reports/load-<profile>-<players>.md"
    classpath = load.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.load.LoadRunKt")
    systemProperty("content", rootProject.layout.projectDirectory.dir("backend/src/main/resources/content").asFile.path)
    listOf("url", "players", "seconds", "profile", "ramp", "speedup", "fightSpeed").forEach { name -> providers.gradleProperty(name).orNull?.let { systemProperty(name, it) } }
    val tag = "${providers.gradleProperty("profile").getOrElse("mixed")}-${providers.gradleProperty("players").getOrElse("20")}"
    args(layout.buildDirectory.file("reports/load-$tag.md").get().asFile.path)
    outputs.upToDateWhen { false }
}

// Пробы эксплойтов аудита против ЛОКАЛЬНОГО сервера (отчёт, не тест): ./gradlew :core:probeRun -Padmin=<пароль> -Pdb=<база>
tasks.register<JavaExec>("probeRun") {
    group = "reporting"
    classpath = load.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.load.ExploitProbesKt")
    systemProperty("content", rootProject.layout.projectDirectory.dir("backend/src/main/resources/content").asFile.path)
    listOf("url", "admin", "db").forEach { name -> providers.gradleProperty(name).orNull?.let { systemProperty(name, it) } }
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
