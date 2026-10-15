plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // @Serializable records of this module (pending commands, the icon cache) need generated serializers.
    id("org.jetbrains.kotlin.plugin.serialization")
}
composeCompiler {
    // Models of the shared rules and the wire are immutable (3.55.0): named stable, an unchanged slice skips recomposition.
    stabilityConfigurationFiles.add(layout.projectDirectory.file("compose_stability.conf"))
}

android {
    namespace = "com.sperance.exileforge"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.sperance.exileforge"
        minSdk = 26
        targetSdk = 37
        versionCode = 370
        versionName = "0.14.3"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        // Updates from GitHub Releases (3.72.0) close the game until checked: only the published build does that.
        buildConfigField("boolean", "UPDATES", "false")
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // The instrumentation tests draw real items: the pinned server's content and dictionaries ride in the test APK.
    sourceSets.getByName("androidTest").assets.srcDir("../backend/src/main/resources")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // The published APK (3.68.2): signed with the key from the CI secrets, so each release installs over the last;
    // without them it falls back to the debug key and still installs, though not over a release-key build.
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
            // R8 shrinks the extended icon set and the rest to what the app draws; CI builds this
            // variant so a class it removes by mistake fails there rather than on a phone.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            buildConfigField("boolean", "UPDATES", "true")
        }
        // The release build under instrumentation (4.12.x: единственная тестовая сборка - отладочной нет): R8 shrinks it as it ships.
        // Signed with the debug key so the emulator takes it; the test rules keep only what the test APK calls into. С `-PuiTests`
        // код приложения не ужимается (`proguard-ui-rules.pro`) - так на ней идут все UI-тесты; без него - строгий смоук R8.
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
    // Отладочной сборки нет (4.12.x, решение владельца): инструментальные тесты - только на minifiedTest.
    testBuildType = "minifiedTest"
}

// Экраны администратора и промокодов (4.12.x, ADMIN-UI) - в основной сборке, их открывает роль аккаунта, а не вариант.
androidComponents {
    // Debug не собирается нигде (4.12.x): вариант выключен - его задач нет ни локально, ни в CI. Ключ debug остаётся лишь ключом
    // подписи minifiedTest.
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
    // Navigation 3 (3.80.23): стек экранов принадлежит приложению, ключи типизированы.
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
