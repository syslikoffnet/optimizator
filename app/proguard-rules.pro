# Keep Hilt-generated classes and annotations.
-keep class dagger.hilt.** { *; }
-keep class com.duchamp.** { *; }

# libsu uses reflection-free binder/IPC; keep its service entry points.
-keep class com.github.topjohnwu.libsu.** { *; }

# Vico charts
-keep class com.patrykandpatrick.vico.** { *; }

# Do not obfuscate Room entities / DAOs used reflectively.
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

-dontobfuscate
