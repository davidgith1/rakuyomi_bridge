# Keep native methods
-keep class git.shin.rakuyomi_bridge.JniBridge { *; }
-keep class git.shin.rakuyomi_bridge.RakuyomiServer { *; }

# Kotlin coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Kotlin Serialization
-keepattributes *Annotation*, EnclosingMethod, InnerClasses
-keepclassmembers class ** {
    @kotlinx.serialization.Serializable *;
}
-keepclassmembers class ** {
    @kotlinx.serialization.SerialName *;
}
# Specific classes used in bridge
-keep class git.shin.rakuyomi_bridge.service.BridgeRequest { *; }
-keep class git.shin.rakuyomi_bridge.service.BridgeRequest$$serializer { *; }
