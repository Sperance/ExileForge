# The minifiedTest build only (-PminifiedTests): the app is shrunk as in release, and these rules keep what the test APK
# links into by name — the libraries the test runner drives and the few entry points ReleaseSmokeTest calls. Everything
# under those entry points (the content, the wire models and their serializers, the screens) is shrunk as it ships.

# The libraries the instrumentation and the Compose test rule reach into; they sit in the app, not in the test APK.
-keep class kotlin.** { *; }
-keep class kotlinx.coroutines.** { *; }
-keep class kotlinx.serialization.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.foundation.layout.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.lifecycle.** { *; }
-keep class androidx.core.** { *; }
-keep class androidx.savedstate.** { *; }
-keep class androidx.tracing.** { *; }

# ReleaseSmokeTest's and TestWorld's own calls into the app.
-keep class com.sperance.exileforge.ui.theme.ThemeKt { *; }
-keep class com.sperance.exileforge.ui.components.ItemCardKt { *; }
-keep class com.sperance.exileforge.core.contract.ContractKt { *; }
-keep class com.sperance.exileforge.core.display.TextKt { *; }
-keep class com.sperance.exileforge.core.display.ItemView* { public *; }
-keep class com.sperance.exileforge.core.display.ItemLine { public *; }
-keep class com.sperance.exileforge.rules.roll.Roll { public *; }
-keep class com.sperance.exileforge.core.i18n.ServerLocaleKt { *; }
-keep class com.sperance.exileforge.core.i18n.LocaleBundle* { public *; }
-keep class com.sperance.exileforge.core.model.hero.HeroInfo* { public *; }
-keep class com.sperance.exileforge.rules.content.ContentLoader { public *; }
-keep class com.sperance.exileforge.rules.content.ContentIndex { public *; }
-keep class com.sperance.exileforge.rules.content.ItemTemplate { public *; }
-keep class com.sperance.exileforge.rules.content.Line* { public *; }
-keep enum com.sperance.exileforge.rules.content.Rarity { *; }
-keep enum com.sperance.exileforge.rules.content.Slot { *; }
-keep class com.sperance.exileforge.rules.roll.ItemFactory { public *; }
-keep class com.sperance.exileforge.rules.roll.Dice { public *; }
-keep class com.sperance.exileforge.rules.roll.ItemInstance* { public *; }

# The test APK's side: its classes are what the runner looks up by name, its methods found by their annotations.
# The other UI tests ride in the same APK unrun and are shrunk away; what they called in the app may be gone.
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keep class com.sperance.exileforge.ui.ReleaseSmokeTest { *; }
-keep class com.sperance.exileforge.ui.TestWorld { *; }
-keep class androidx.test.** { *; }
-keep class org.junit.** { *; }
-dontwarn org.junit.**
-dontwarn org.hamcrest.**
-dontwarn androidx.test.**
-dontwarn com.sperance.exileforge.**
