# SQLCipher loads its JNI entry points by name.
-keep class net.zetetic.sqlcipher.** { *; }
-keep class net.sqlcipher.** { *; }

# Room and Kotlin metadata are kept by the libraries' own rules.
-dontwarn org.slf4j.**
-dontwarn org.conscrypt.**
