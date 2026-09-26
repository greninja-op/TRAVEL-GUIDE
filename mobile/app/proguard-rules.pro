# R8 rules — currently unused (isMinifyEnabled = false in release).
# When minification is enabled, add keep rules verified against:
# - Room entities/DAOs in guide.app.data
# - MapLibre native/JNI surface (org.maplibre.**)
# - Kotlin serialization/reflection used by PackLoader
-keep class org.maplibre.** { *; }
