import java.util.Properties

plugins {
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.kotlinAndroid)
}

android {
  namespace = "git.shin.rakuyomi_bridge.headless"
  compileSdk = 37

  val localProperties = Properties()
  val localPropertiesFile = rootProject.file("local.properties")
  if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
  }

  defaultConfig {
    applicationId = "git.shin.rakuyomi_bridge.headless"
    minSdk = 18
    targetSdk = 34
    versionCode = project.property("versionCode").toString().toInt()
    versionName = project.property("versionName").toString()

    // Share the .so files (librakuyomi_server.so) with the Compose app to
    // avoid duplicating the 50MB native payload. The set of supported ABIs
    // is defined by what the Rust server build script drops into
    // androidApp/src/main/jniLibs.
    sourceSets["main"].jniLibs.srcDirs(
      "src/main/jniLibs",
      "../androidApp/src/main/jniLibs"
    )
  }

  buildTypes {
    release {
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  implementation(project(":shared"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.kotlinx.coroutines.android)
  // OkHttp 3.12.x is the last release that supports minSdk 9. The 4.x
  // line requires API 21+ (Java 8 APIs) and would not work on the
  // Android 4.3 / 4.4 devices the headless build targets.
  implementation(libs.okhttp.legacy)
  implementation(libs.kotlinx.serialization.json)
}
