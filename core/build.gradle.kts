plugins { kotlin("jvm"); `java-library`; id("org.jetbrains.kotlin.plugin.serialization") }

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

// A failing test in CI is read from the log, not from a report nobody can open: print the stack.
tasks.withType<Test>().configureEach {
    testLogging {
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        showStackTraces = true
    }
}

// Отчёт баланса (`./gradlew :core:balanceReport`, `-Pdeep` — долгий прогон): типовые герои на ядре боя
// автопробегают зоны и бьются на арене; пишет build/balance/report.html и report.json. В CI не запускается.
val balance: SourceSet by sourceSets.creating {
    compileClasspath += sourceSets.main.get().output + configurations.runtimeClasspath.get()
    runtimeClasspath += output + compileClasspath
}
tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Simulates typical heroes of every class and writes the balance report."
    classpath = balance.runtimeClasspath
    mainClass.set("com.sperance.exileforge.core.balance.BalanceReportKt")
    maxHeapSize = "2g"
    args(rootProject.file("backend/src/main/resources/content").path, layout.buildDirectory.dir("balance").get().asFile.path,
        if (project.hasProperty("deep")) "deep" else "fast")
}
