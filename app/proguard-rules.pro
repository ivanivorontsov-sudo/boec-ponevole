# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class org.ivanivorontsov.boec.**$$serializer { *; }
-keepclassmembers class org.ivanivorontsov.boec.** { *** Companion; }
-keepclasseswithmembers class org.ivanivorontsov.boec.** { kotlinx.serialization.KSerializer serializer(...); }
-dontwarn org.jetbrains.annotations.**
