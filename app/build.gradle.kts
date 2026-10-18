plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}
composeCompiler {
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

android {
    namespace = "com.sperance.exileforge"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.sperance.exileforge"
        minSdk = 26
        targetSdk = 37
        versionCode = 375
        versionName = "0.16.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("boolean", "UPDATES", "false")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    sourceSets.getByName("androidTest").assets.srcDir("../backend/src/main/resources")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    val releaseKey = System.getenv("RELEASE_KEYSTORE_PATH")?.let { file(it) }?.takeIf { it.exists() }
    signingConfigs {
        if (releaseKey != null) {
            create("release") {
                storeFile = releaseKey
                storePassword = System.getenv("RELEASE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "UPDATES", "true")
        }
        create("minifiedTest") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            buildConfigField("boolean", "UPDATES", "false")
            matchingFallbacks += listOf("release")
            proguardFile("proguard-test-rules.pro")
            testProguardFiles("proguard-test-rules.pro")
            if (project.hasProperty("uiTests")) {
                proguardFile("proguard-ui-rules.pro")
                testProguardFiles("proguard-ui-rules.pro")
            }
        }
    }
    testBuildType = "minifiedTest"
}

androidComponents {
    beforeVariants(selector().withBuildType("debug")) { it.enable = false }
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2026.09.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.navigation3:navigation3-runtime:1.2.0")
    implementation("androidx.navigation3:navigation3-ui:1.2.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-androidx-compose:4.2.2")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.09.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    "minifiedTestImplementation"("androidx.compose.ui:ui-test-manifest")
}

/**
 * Сторож границы модулей (`RULES.md` → «Архитектура»): в `:app` нет файла Kotlin без Android. Каждый файл `src/main` импортирует
 * `android.*` или `androidx.*` (DI-граф - привязку Koin к Android) либо объявляет `@Composable`; расчёты, модели экранов и сервисы
 * лежат в `:core`. Падение перечисляет нарушителей.
 */
abstract class AndroidOnlyCheck : DefaultTask() {
    /** Исходники Kotlin приложения. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** Корень модуля: пути в отчёте - от него. */
    @get:Internal
    abstract val root: DirectoryProperty

    @TaskAction
    fun check() {
        val base = root.get().asFile
        val android = Regex("""^import (android|androidx|org\.koin\.androidx?)\.""", RegexOption.MULTILINE)
        val offenders = sources.files.filter { it.extension == "kt" }
            .filterNot { file -> file.readText().let { android.containsMatchIn(it) || "@Composable" in it } }
            .map { it.relativeTo(base).path }
            .sorted()
        if (offenders.isNotEmpty()) {
            throw GradleException("Files in :app without Android or @Composable belong to :core (RULES.md, «Архитектура»): ${offenders.size}\n" + offenders.joinToString("\n"))
        }
    }
}

/** Сторож `:app` рядом с `:app:ktlintCheck` в job `app` CI. */
val androidOnly by tasks.registering(AndroidOnlyCheck::class) {
    group = "verification"
    description = "Fails if a Kotlin file of :app has neither an Android import nor @Composable."
    sources.from(fileTree("src/main") { include("**/*.kt") })
    root.set(layout.projectDirectory)
}
