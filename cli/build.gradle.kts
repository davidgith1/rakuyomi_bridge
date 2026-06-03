plugins {
    alias(libs.plugins.kotlinJvm)
    application
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.kotlinx.coroutines.core)
}

application {
    mainClass = "git.shin.rakuyomi_bridge.BridgeCLIKt"
}

kotlin {
    jvmToolchain(8)
}
