# R8 rules for the release build.
#
# Most libraries here ship their own consumer rules, so this file only states
# what the project itself depends on. The rules below are deliberately narrow:
# keeping "everything" would defeat the point of shrinking a 24 MB APK.

-keepattributes *Annotation*, InnerClasses, Signature, RuntimeVisible*Annotations
# Retrofit reads generic return types (Response<T>, List<T>) reflectively at
# runtime; without the signature attribute those erase and calls fail.
-keepattributes EnclosingMethod

# ---------------------------------------------------------------- networking
#
# Retrofit resolves service methods from annotations at runtime, so the
# interface declarations must survive even though their names may be renamed.
-keep,allowobfuscation,allowshrinking interface retrofit2.Call
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-if interface * { @retrofit2.http.* <methods>; }
-keep,allowobfuscation interface <1>

# --------------------------------------------------------------- serialization
#
# kotlinx.serialization looks a serializer up from its owning class. If R8
# removes the generated $serializer or the companion that returns it, every
# request and response fails at runtime rather than at build time - which is
# exactly the kind of break that only shows up on a shipped build.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# The wire types are only ever referenced through the serializer, so R8 cannot
# see the connection on its own.
-keep @kotlinx.serialization.Serializable class com.cit.attendance.core.network.dto.** { *; }

# ------------------------------------------------------------------- ML Kit
#
# Barcode scanning loads its model implementation reflectively.
-keep class com.google.mlkit.** { *; }
-dontwarn com.google.mlkit.**

# --------------------------------------------------------------------- misc
#
# Reported by R8 as absent from the classpath; they are optional dependencies of
# OkHttp and pdf/zxing tooling that this application never reaches.
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**
