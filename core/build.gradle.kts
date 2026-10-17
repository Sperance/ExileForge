plugins {
    kotlin("jvm")
    `java-library`
    id("org.jetbrains.kotlin.plugin.serialization")
}

dependencies {
    api("com.sperance.exileforge:rules:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("com.squareup.okhttp3:okhttp:5.5.0")
    testImplementation(kotlin("test-junit"))
    testImplementation("com.squareup.okhttp3:mockwebserver:5.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0")
}

kotlin { jvmToolchain(17) }

/**
 * Симуляции баланса (отчёты, не тесты): свой набор исходников поверх main - вне test и ворот CI, новых зависимостей не требует; видит
 * internal main. Запуск - задача `mobBal` ниже.
 *
 * Тестам виден набор sim: сторож темпа `MobBalGuardTest` зовёт эталонных героев. Задача `mobBal` - баланс монстров (отчёт, не тест):
 * эталонные герои против монстров и боссов кампании и карт настоящим боем клиента; `./gradlew :core:mobBal [-Pcontent=<папка>]
 * -Pmode=<campaign|probe|maps|sweep|runtime|bossmight|...> [-Pout=<tsv>] [-Pfights=16 -Prolls=4 ...]`.
 */
val sim: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}
kotlin.target.compilations.getByName("sim").associateWith(kotlin.target.compilations.getByName("main"))
sourceSets.test {
    compileClasspath += sim.output
    runtimeClasspath += sim.output
}

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

/**
 * Нагрузочный прогон аудита (отчёт, не тест): виртуальные игроки на настоящем клиенте против ЛОКАЛЬНОГО сервера, вне test и ворот CI.
 * Запуск: ./gradlew :core:loadRun -Purl=http://localhost:8081/ -Pplayers=50 -Pseconds=120 -Pprofile=mixed
 *
 * Нагрузке виден набор sim: заход без игрока `Walkthrough`. Задача `probeRun` - пробы эксплойтов против локального сервера (отчёт, не тест): `./gradlew :core:probeRun -Padmin=<пароль> -Pdb=<база>`.
 */
val load: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + sourceSets.main.get().compileClasspath
    runtimeClasspath += output + compileClasspath + sourceSets.main.get().runtimeClasspath
}
kotlin.target.compilations.getByName("load").associateWith(kotlin.target.compilations.getByName("main"))
sourceSets.named("load") {
    compileClasspath += sim.output
    runtimeClasspath += sim.output
}

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

tasks.register<JavaExec>("probeRun") {
    group = "reporting"
    classpath = load.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.load.ExploitProbesKt")
    systemProperty("content", rootProject.layout.projectDirectory.dir("backend/src/main/resources/content").asFile.path)
    listOf("url", "admin", "db").forEach { name -> providers.gradleProperty(name).orNull?.let { systemProperty(name, it) } }
    outputs.upToDateWhen { false }
}

tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

/**
 * Классы сторожей баланса: бои эталонов на минуты. Обычный `test` их не берёт - быстрые тесты ядра, классы в нескольких JVM
 * на ядра машины.
 */
val balanceGuards = "com.sperance.exileforge.core.sim.*GuardTest"

tasks.test {
    filter { excludeTestsMatching(balanceGuards) }
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

/**
 * Сторожа баланса (`MobBalGuardTest`, `ModesBalanceGuardTest`, `ClassBalanceGuardTest`) - своя задача и свой job CI рядом с `core`:
 * одна JVM, эталоны общим пулом на прогон (`GuardHeroes`). `./gradlew :core:balanceGuard [--tests <класс>]`.
 */
val balanceGuard by tasks.registering(Test::class) {
    group = "verification"
    description = "Balance guards: reference heroes against guardians, modes and classes"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    filter { includeTestsMatching(balanceGuards) }
    maxHeapSize = "4g"
}

/**
 * Закреплённый сервер - из сабмодуля `backend/`, не из кода: коммит - его HEAD, ветка - из `.gitmodules`, версия -
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
