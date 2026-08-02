# Gradle & Manifest Setup — Insta-Save

Paste-ready build configuration. Versions are the pinning target; bump deliberately, not casually.

---

## 1. `settings.gradle.kts`

```kotlin
pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")   // youtubedl-android
    }
}

rootProject.name = "Insta-Save"
include(":app")
```

---

## 2. `gradle/libs.versions.toml`

```toml
[versions]
agp = "8.7.3"
kotlin = "2.1.0"
ksp = "2.1.0-1.0.29"
hilt = "2.53.1"
composeBom = "2024.12.01"
room = "2.6.1"
media3 = "1.5.1"
coil = "3.0.4"
okhttp = "5.0.0-alpha.14"
coroutines = "1.9.0"
serialization = "1.7.3"
navigation = "2.8.5"
lifecycle = "2.8.7"
work = "2.10.0"
datastore = "1.1.1"
securityCrypto = "1.1.0-alpha06"
jsoup = "1.18.3"
youtubedl = "0.17.0"
junit5 = "5.11.4"
mockk = "1.13.14"
turbine = "1.2.0"

[libraries]
androidx-core-ktx            = { module = "androidx.core:core-ktx", version = "1.15.0" }
androidx-activity-compose    = { module = "androidx.activity:activity-compose", version = "1.9.3" }
androidx-lifecycle-runtime   = { module = "androidx.lifecycle:lifecycle-runtime-compose", version.ref = "lifecycle" }
androidx-lifecycle-viewmodel = { module = "androidx.lifecycle:lifecycle-viewmodel-compose", version.ref = "lifecycle" }

compose-bom        = { module = "androidx.compose:compose-bom", version.ref = "composeBom" }
compose-ui         = { module = "androidx.compose.ui:ui" }
compose-graphics   = { module = "androidx.compose.ui:ui-graphics" }
compose-tooling    = { module = "androidx.compose.ui:ui-tooling" }
compose-preview    = { module = "androidx.compose.ui:ui-tooling-preview" }
compose-material3  = { module = "androidx.compose.material3:material3" }
compose-icons      = { module = "androidx.compose.material:material-icons-extended" }
compose-test-junit = { module = "androidx.compose.ui:ui-test-junit4" }
compose-test-mnf   = { module = "androidx.compose.ui:ui-test-manifest" }

navigation-compose = { module = "androidx.navigation:navigation-compose", version.ref = "navigation" }

hilt-android          = { module = "com.google.dagger:hilt-android", version.ref = "hilt" }
hilt-compiler         = { module = "com.google.dagger:hilt-android-compiler", version.ref = "hilt" }
hilt-navigation       = { module = "androidx.hilt:hilt-navigation-compose", version = "1.2.0" }
hilt-work             = { module = "androidx.hilt:hilt-work", version = "1.2.0" }
hilt-work-compiler    = { module = "androidx.hilt:hilt-compiler", version = "1.2.0" }

room-runtime = { module = "androidx.room:room-runtime", version.ref = "room" }
room-ktx     = { module = "androidx.room:room-ktx", version.ref = "room" }
room-compiler= { module = "androidx.room:room-compiler", version.ref = "room" }
room-testing = { module = "androidx.room:room-testing", version.ref = "room" }

okhttp         = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }
okhttp-mockweb = { module = "com.squareup.okhttp3:mockwebserver3-junit5", version.ref = "okhttp" }
jsoup          = { module = "org.jsoup:jsoup", version.ref = "jsoup" }
serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }
coroutines-android = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-android", version.ref = "coroutines" }
coroutines-test    = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-test", version.ref = "coroutines" }

media3-exoplayer = { module = "androidx.media3:media3-exoplayer", version.ref = "media3" }
media3-dash      = { module = "androidx.media3:media3-exoplayer-dash", version.ref = "media3" }
media3-ui        = { module = "androidx.media3:media3-ui", version.ref = "media3" }

coil-compose = { module = "io.coil-kt.coil3:coil-compose", version.ref = "coil" }
coil-network = { module = "io.coil-kt.coil3:coil-network-okhttp", version.ref = "coil" }

work-runtime = { module = "androidx.work:work-runtime-ktx", version.ref = "work" }
datastore    = { module = "androidx.datastore:datastore-preferences", version.ref = "datastore" }
security-crypto = { module = "androidx.security:security-crypto", version.ref = "securityCrypto" }

youtubedl-library = { module = "io.github.junkfood02.youtubedl-android:library", version.ref = "youtubedl" }
youtubedl-ffmpeg  = { module = "io.github.junkfood02.youtubedl-android:ffmpeg", version.ref = "youtubedl" }

junit5-api    = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit5" }
junit5-engine = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "junit5" }
junit5-params = { module = "org.junit.jupiter:junit-jupiter-params", version.ref = "junit5" }
mockk         = { module = "io.mockk:mockk", version.ref = "mockk" }
turbine       = { module = "app.cash.turbine:turbine", version.ref = "turbine" }

[plugins]
android-application = { id = "com.android.application", version.ref = "agp" }
kotlin-android      = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
kotlin-compose      = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
kotlin-serialization= { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
ksp                 = { id = "com.google.devtools.ksp", version.ref = "ksp" }
hilt                = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

---

## 3. Root `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}
```

`gradle.properties`:

```properties
org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
org.gradle.parallel=true
org.gradle.caching=true
org.gradle.configuration-cache=true
android.useAndroidX=true
android.nonTransitiveRClass=true
android.enableR8.fullMode=true
kotlin.code.style=official
```

---

## 4. `app/build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.instasave.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.instasave.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ksp { arg("room.schemaLocation", "$projectDir/schemas") }

        ndk { abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64") }
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true   // one fat APK for sideload convenience
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true   // java.time on API 26
    }
    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true; buildConfig = true }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/LICENSE*",
        )
        jniLibs.useLegacyPackaging = true   // required by youtubedl-android
    }

    testOptions { unitTests { isReturnDefaultValues = true } }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.androidx.lifecycle.viewmodel)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.graphics)
    implementation(libs.compose.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.icons)
    debugImplementation(libs.compose.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.okhttp)
    debugImplementation(libs.okhttp.logging)
    implementation(libs.jsoup)
    implementation(libs.serialization.json)
    implementation(libs.coroutines.android)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.dash)
    implementation(libs.media3.ui)

    implementation(libs.coil.compose)
    implementation(libs.coil.network)

    implementation(libs.work.runtime)
    implementation(libs.datastore)
    implementation(libs.security.crypto)

    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    testImplementation(libs.junit5.api)
    testImplementation(libs.junit5.params)
    testRuntimeOnly(libs.junit5.engine)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.coroutines.test)
    testImplementation(libs.okhttp.mockweb)
    testImplementation(libs.room.testing)

    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.compose.test.junit)
    debugImplementation(libs.compose.test.mnf)
}

tasks.withType<Test> { useJUnitPlatform() }
```

---

## 5. `AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_DATA_SYNC" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />

    <!-- Legacy storage: API 28 and below only. Scoped storage above. -->
    <uses-permission
        android:name="android.permission.WRITE_EXTERNAL_STORAGE"
        android:maxSdkVersion="28" />

    <application
        android:name=".InstaSaveApplication"
        android:allowBackup="false"
        android:dataExtractionRules="@xml/data_extraction_rules"
        android:fullBackupContent="false"
        android:icon="@mipmap/ic_launcher"
        android:label="@string/app_name"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:usesCleartextTraffic="false"
        android:enableOnBackInvokedCallback="true"
        android:theme="@style/Theme.InstaSave"
        tools:targetApi="34">

        <activity
            android:name=".presentation.MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            android:windowSoftInputMode="adjustResize"
            android:theme="@style/Theme.InstaSave">

            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>

            <!-- Share sheet from the Instagram app -->
            <intent-filter>
                <action android:name="android.intent.action.SEND" />
                <category android:name="android.intent.category.DEFAULT" />
                <data android:mimeType="text/plain" />
            </intent-filter>

            <!-- Deep links -->
            <intent-filter android:autoVerify="false">
                <action android:name="android.intent.action.VIEW" />
                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />
                <data android:scheme="https" android:host="www.instagram.com" />
                <data android:scheme="https" android:host="instagram.com" />
            </intent-filter>
        </activity>

        <service
            android:name=".data.download.DownloadForegroundService"
            android:exported="false"
            android:foregroundServiceType="dataSync" />

        <!-- Hilt supplies the WorkManager factory -->
        <provider
            android:name="androidx.startup.InitializationProvider"
            android:authorities="${applicationId}.androidx-startup"
            android:exported="false"
            tools:node="merge">
            <meta-data
                android:name="androidx.work.WorkManagerInitializer"
                android:value="androidx.startup"
                tools:node="remove" />
        </provider>
    </application>
</manifest>
```

`res/values/themes.xml` — the window must be black before Compose draws, or you get a white flash on cold start:

```xml
<resources>
    <style name="Theme.InstaSave" parent="android:Theme.Material.NoActionBar">
        <item name="android:windowBackground">@android:color/black</item>
        <item name="android:statusBarColor">@android:color/transparent</item>
        <item name="android:navigationBarColor">@android:color/transparent</item>
        <item name="android:windowLightStatusBar">false</item>
    </style>
</resources>
```

`res/xml/data_extraction_rules.xml` — never back up the session:

```xml
<data-extraction-rules>
    <cloud-backup>
        <exclude domain="sharedpref" path="instasave_session.xml" />
    </cloud-backup>
    <device-transfer>
        <exclude domain="sharedpref" path="instasave_session.xml" />
    </device-transfer>
</data-extraction-rules>
```

---

## 6. `proguard-rules.pro`

```proguard
# Strip all logging in release (agent.md Rule 4)
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
}

# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class com.instasave.app.** {
    *** Companion;
}
-keepclasseswithmembers class com.instasave.app.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }

# OkHttp / Okio
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn org.conscrypt.**

# youtubedl-android reflects into its bundled payload
-keep class com.yausername.youtubedl_android.** { *; }
-keep class com.yausername.ffmpeg.** { *; }

# Media3
-dontwarn androidx.media3.**

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
```

---

## 7. Build & Verify

```bash
./gradlew ktlintCheck
./gradlew assembleDebug
./gradlew test
./gradlew lint
./gradlew assembleRelease     # confirms R8 rules don't strip something live
```

Release artefacts land in `app/build/outputs/apk/release/` — one APK per ABI plus the universal.

**Signing.** Keep the keystore out of the repo. Configure via `~/.gradle/gradle.properties` or `ANDROID_KEYSTORE_*` environment variables and a `signingConfigs` block guarded by `if (System.getenv("ANDROID_KEYSTORE_PATH") != null)`. Never commit a keystore or its password.

**F-Droid note.** Reproducible builds require the yt-dlp/FFmpeg native payloads to be buildable from source with a documented recipe, or the submission is rejected. Budget time for this — it is the single most likely blocker to F-Droid inclusion.
