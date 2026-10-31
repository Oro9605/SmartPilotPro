plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android { namespace = "pl.smartpilot.pro"; compileSdk = 35
 defaultConfig { applicationId = "pl.smartpilot.pro"; minSdk = 26; targetSdk = 28; versionCode = 1; versionName = "0.1" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("com.squareup.okhttp3:okhttp:4.12.0") }
