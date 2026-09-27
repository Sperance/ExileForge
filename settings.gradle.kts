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
includeBuild("backend/rules")
include(":app", ":core")
