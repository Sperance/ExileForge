import javax.inject.Inject

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    // @Serializable records of this module (pending commands, the icon cache) need generated serializers.
    id("org.jetbrains.kotlin.plugin.serialization")
}
android {
    namespace = "com.sperance.exileforge"
    compileSdk = 37
    defaultConfig {
        applicationId = "com.sperance.exileforge"
        minSdk = 26
        targetSdk = 37
        versionCode = 42
        versionName = "2.24.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures { compose = true }
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
    }
}

/**
 * libGDX draws the campaign's map and fights (since 2.24.0). Its native library ships as one jar per
 * ABI with `libgdx.so` at the root, which AGP does not package from a dependency, so the jars are
 * unpacked into `src/main/jniLibs/<abi>` — the default native folder, ignored by git — before a build.
 *
 * The task touches no `project` while it runs, so it works with the configuration cache on, and it
 * declares no output: the folder is a source folder every packaging task already reads, and naming
 * it an output would make Gradle demand an explicit dependency from each of them.
 */
abstract class UnpackGdxNatives @Inject constructor(
    private val files: FileSystemOperations,
    private val archives: ArchiveOperations,
) : DefaultTask() {
    @get:InputFiles abstract val jars: ConfigurableFileCollection
    @get:Input abstract val abis: ListProperty<String>
    @get:Internal abstract val target: DirectoryProperty

    @TaskAction fun unpack() {
        jars.forEach { jar ->
            val abi = abis.get().first { jar.name.endsWith("natives-$it.jar") }
            files.copy {
                from(archives.zipTree(jar)) { include("*.so") }
                into(target.dir(abi))
            }
        }
    }
}

val gdxVersion = "1.14.2"
val gdxNatives: Configuration by configurations.creating
val gdxAbis = listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
val copyGdxNatives by tasks.registering(UnpackGdxNatives::class) {
    jars.from(gdxNatives)
    abis.set(gdxAbis)
    target.set(layout.projectDirectory.dir("src/main/jniLibs"))
}
tasks.named("preBuild") { dependsOn(copyGdxNatives) }

dependencies {
    implementation(project(":core"))
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    gdxAbis.forEach { gdxNatives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-$it") }
    // The scene is a fragment inside the Compose tree: AndroidFragment hosts libGDX's own, and
    // MainActivity is a FragmentActivity. Both are named here rather than left to arrive transitively.
    implementation("androidx.fragment:fragment-ktx:1.8.6")
    implementation("androidx.fragment:fragment-compose:1.8.6")
    implementation(platform("androidx.compose:compose-bom:2026.08.00"))
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.11.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.11.0")
    implementation("androidx.datastore:datastore-preferences:1.2.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.11.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
    androidTestImplementation(platform("androidx.compose:compose-bom:2026.08.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
