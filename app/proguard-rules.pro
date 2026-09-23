# kotlinx.serialization and OkHttp ship their own R8 rules; these keep what they cannot know about.

# Every wire model is found through its generated serializer, which R8 cannot see being reached.
-keepattributes *Annotation*, InnerClasses, Signature
-keepclassmembers @kotlinx.serialization.Serializable class com.sperance.exileforge.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class com.sperance.exileforge.**$$serializer { *; }
-keepclasseswithmembers class com.sperance.exileforge.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# libGDX (the campaign's scene, since 2.24.0) reaches its backends and natives by reflection and JNI.
-keep class com.badlogic.gdx.** { *; }
-dontwarn com.badlogic.gdx.**
