# Keep native methods
-keep class git.shin.rakuyomi_bridge.JniBridge { *; }
-keep class git.shin.rakuyomi_bridge.RakuyomiServer { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
