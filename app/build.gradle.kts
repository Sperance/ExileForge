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
        versionCode = 385
        versionName = "0.17.0"
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
 * Сторож исходников `:app`: проходит файлы Kotlin [sources] и падает, если [offenses] нашёл нарушения; пути в отчёте - от корня
 * модуля [root]. Наследник задаёт проверку файла и текст падения. Пройденная проверка пишет маркер [marker]: пока исходники
 * не менялись, задача актуальна.
 */
abstract class SourceGuard : DefaultTask() {
    /** Исходники Kotlin приложения. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sources: ConfigurableFileCollection

    /** Корень модуля: пути в отчёте и проверке - от него. */
    @get:Internal
    abstract val root: DirectoryProperty

    /** Маркер пройденной проверки: единственный выход задачи. */
    @get:OutputFile
    abstract val marker: RegularFileProperty

    /** Нарушения файла [text] по пути [path] от корня модуля; пусто - файл чист. */
    protected abstract fun offenses(path: String, text: String): List<String>

    /** Первая строка падения при [count] файлах-нарушителях. */
    protected abstract fun failure(count: Int): String

    @TaskAction
    fun check() {
        val base = root.get().asFile
        val offenders = sources.files.map { it.relativeTo(base).invariantSeparatorsPath to it }
            .mapNotNull { (path, file) -> offenses(path, file.readText()).takeIf { it.isNotEmpty() }?.let { "$path: ${it.joinToString()}" } }
            .sorted()
        if (offenders.isNotEmpty()) throw GradleException(failure(offenders.size) + "\n" + offenders.joinToString("\n"))
        marker.get().asFile.apply { parentFile.mkdirs() }.writeText("ok\n")
    }
}

/** Сторож [T] по имени задачи: исходники `src/main`, корень модуля и маркер `build/<задача>.ok`. */
inline fun <reified T : SourceGuard> guard(name: String, crossinline configure: T.() -> Unit) = tasks.register<T>(name) {
    group = "verification"
    sources.from(fileTree("src/main") { include("**/*.kt") })
    root.set(layout.projectDirectory)
    marker.set(layout.buildDirectory.file("$name.ok"))
    configure()
}

/**
 * Сторож границы модулей (`RULES.md` → «Архитектура»): в `:app` нет файла Kotlin без Android. Каждый файл `src/main` импортирует
 * `android.*` или `androidx.*` (DI-граф - привязку Koin к Android) либо объявляет `@Composable`; расчёты, модели экранов и сервисы
 * лежат в `:core`.
 */
abstract class AndroidOnlyCheck : SourceGuard() {
    private val android = Regex("""^import (android|androidx|org\.koin\.androidx?)\.""", RegexOption.MULTILINE)

    override fun offenses(path: String, text: String): List<String> = if (android.containsMatchIn(text) || "@Composable" in text) emptyList() else listOf("no Android")

    override fun failure(count: Int) = "Files in :app without Android or @Composable belong to :core (RULES.md, «Архитектура»): $count"
}

/** Сторож `:app` рядом с `:app:ktlintCheck` в job `app` CI. */
val androidOnly = guard<AndroidOnlyCheck>("androidOnly") {
    description = "Fails if a Kotlin file of :app has neither an Android import nor @Composable."
}

/**
 * Сторож кнопок (`RULES.md` → «Вёрстка»): кнопки, переключатели и чипы Material 2 и 3 зовёт только `ui/components` - экраны берут
 * `ForgeButton`, `ForgeIconButton` и `ForgeChip`. Вне `ui/components` нарушение - импорт запрещённого имени, его вызов при
 * `material.*`/`material3.*` или полным именем, `Surface`/`Card` с `onClick`. Комментарии и строки перед поиском вырезаются.
 * Самодельные кнопки на `clickable` сторож не видит - их ловит ревью.
 */
abstract class ForgeButtonsCheck : SourceGuard() {
    private val banned = listOf(
        "Button", "TextButton", "OutlinedButton", "ElevatedButton", "FilledTonalButton",
        "IconButton", "FilledIconButton", "FilledTonalIconButton", "OutlinedIconButton",
        "IconToggleButton", "FilledIconToggleButton", "FilledTonalIconToggleButton", "OutlinedIconToggleButton",
        "FloatingActionButton", "SmallFloatingActionButton", "LargeFloatingActionButton", "ExtendedFloatingActionButton",
        "SegmentedButton", "SingleChoiceSegmentedButtonRow", "MultiChoiceSegmentedButtonRow",
        "Chip", "FilterChip", "AssistChip", "InputChip", "SuggestionChip", "ElevatedFilterChip", "ElevatedAssistChip", "ElevatedSuggestionChip",
    ).joinToString("|")
    private val material = """androidx\.compose\.material3?"""
    private val named = Regex("""^import $material\.($banned)\b""", RegexOption.MULTILINE)
    private val wildcard = Regex("""^import $material\.\*$""", RegexOption.MULTILINE)
    private val call = Regex("""(?<![\w.])($banned)\s*[({]""")
    private val qualified = Regex("""$material\.($banned)\s*[({]""")
    private val clickableSurface = Regex("""(?<![\w.])(Surface|Card|ElevatedCard|OutlinedCard)\s*\((?:[^()]|\([^()]*\))*?\bonClick\s*=""")
    private val noise = Regex(""""{3}[\s\S]*?"{3}|/\*[\s\S]*?\*/|//[^\n]*|"(?:\\.|[^"\\\n])*"|'(?:\\.|[^'\\])'""")
    private val own = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?($banned)\s*\(""")
    private val exempt = "src/main/kotlin/com/sperance/exileforge/ui/components/"

    override fun offenses(path: String, text: String): List<String> {
        if (path.startsWith(exempt)) return emptyList()
        val code = noise.replace(text, " ")
        val owned = own.findAll(code).map { it.groupValues[1] }.toSet()
        val calls = if (wildcard.containsMatchIn(code)) call.findAll(code).map { it.groupValues[1] }.filterNot { it in owned } else emptySequence()
        return (named.findAll(code).map { it.groupValues[1] } + qualified.findAll(code).map { it.groupValues[1] } + calls + clickableSurface.findAll(code).map { "${it.groupValues[1]}(onClick)" })
            .toSortedSet().toList()
    }

    override fun failure(count: Int) = "Material buttons and chips outside ui/components - use ForgeButton, ForgeIconButton, ForgeChip (RULES.md, «Вёрстка»): $count"
}

/** Сторож кнопок рядом с `:app:androidOnly` в job `app` CI. */
val forgeButtonsOnly = guard<ForgeButtonsCheck>("forgeButtonsOnly") {
    description = "Fails if a file outside ui/components uses Material buttons, toggles or chips."
}

tasks.named("check") { dependsOn(androidOnly, forgeButtonsOnly) }
