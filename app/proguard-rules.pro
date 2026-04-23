# Keep serializable settings and presets stable across obfuscated releases.
-keepclassmembers class com.qicheng.workbenchkeeper.model.** {
    public static ** Companion;
    public static kotlinx.serialization.KSerializer serializer(...);
}

# Keep BuildConfig fields used by runtime integrity checks.
-keep class com.qicheng.workbenchkeeper.BuildConfig { *; }
