plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.20" apply false
    kotlin("jvm") version "2.4.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.4.20" apply false
    id("org.jlleitschuh.gradle.ktlint") version "14.2.0" apply false
}

// Стиль кода - ktlint, настройки в .editorconfig; CI падает на нарушениях.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version.set("1.8.0")
    }
}
