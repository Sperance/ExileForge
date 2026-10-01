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
        versionCode = 211
        versionName = "3.67.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true; buildConfig = true }
    // The instrumentation tests draw real items: the pinned server's content and dictionaries ride in the test APK.
    sourceSets.getByName("androidTest").assets.srcDir("../backend/src/main/resources")
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release {
            // R8 shrinks the extended icon set and the rest to what the app draws; CI builds this
            // variant so a class it removes by mistake fails there rather than on a phone.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // The release build under instrumentation (`-PminifiedTests`): R8 shrinks it as it ships, the smoke test runs over it.
        // Signed with the debug key so the emulator takes it; the test rules keep only what the test APK calls into.
        create("minifiedTest") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
            proguardFile("proguard-test-rules.pro")
            testProguardFiles("proguard-test-rules.pro")
        }
    }
    // The debug UI checks stay the default; the shrunk build is tested only when asked for.
    testBuildType = if (project.hasProperty("minifiedTests")) "minifiedTest" else "debug"
}

// The shrunk build ships release code: it takes release's player-only screens (admin, redemption) through the variant API,
// the source set DSL does not reach the built-in Kotlin compilation.
androidComponents {
    onVariants(selector().withBuildType("minifiedTest")) { variant ->
        variant.sources.kotlin?.addStaticSourceDirectory("src/release/kotlin")
    }
}

dependencies {
    implementation(project(":core"))
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    "minifiedTestImplementation"("androidx.compose.ui:ui-test-manifest")
}
