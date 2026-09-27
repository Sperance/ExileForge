pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ExileForge"
// Правила игры - общий код с сервером: сабмодуль `backend/` закреплён на коммите сервера, его модуль `rules` собирается вместе с клиентом.
// Свежий клон держит сабмодуль пустым, пока его не подтянут: здесь это делается само, чтобы синхронизация в IDE не падала.
val rules = java.io.File(rootDir, "backend/rules")
if (rules.list().isNullOrEmpty()) {
    val fetched = runCatching {
        ProcessBuilder("git", "submodule", "update", "--init", "--recursive").directory(rootDir).inheritIO().start().waitFor() == 0
    }.getOrDefault(false)
    check(fetched && !rules.list().isNullOrEmpty()) {
        "Сабмодуль backend/ пуст: выполните `git submodule update --init` в корне репозитория (сервер ktor-bestgame подключён сабмодулем)."
    }
}
includeBuild("backend/rules")
include(":app", ":core")
