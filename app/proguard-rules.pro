# ProGuard rules for TVAnimeApp

# Keep Room entities
-keep class com.tvanime.app.data.local.entity.** { *; }

# Keep Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }
