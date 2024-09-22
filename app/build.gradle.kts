plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.navigation)
    alias(libs.plugins.ksp)
    kotlin("plugin.serialization") version "2.0.20"
}

android {
    namespace = "com.satohk.fjphoto"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.satohk.fjphoto"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        dataBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.leanback)
    implementation(libs.glide)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.github.bumptech.glide:glide:4.11.0")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.0")
    implementation("com.eclipsesource.minimal-json:minimal-json:0.9.5")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.0")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.0")
    implementation("androidx.preference:preference-ktx:1.2.1")
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    val coroutines_version = "1.3.9" //Kotlin coroutines用ライブラリ(async, await)のバージョン
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutines_version") //Kotlin coroutines用ライブラリ(async, await)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3") //Kotlin coroutines用ライブラリ(async, await)
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.1")
    implementation("io.insert-koin:koin-android:3.1.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("androidx.fragment:fragment-ktx:1.8.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.material:material:1.12.0")
    implementation("com.google.android.material:material:1.12.0")
    // Tensorflow lite dependencies
    implementation("org.tensorflow:tensorflow-lite:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.11.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.3")
    val exoplayer_version = "2.19.1"
    implementation("com.google.android.exoplayer:exoplayer-core:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-dash:$exoplayer_version")
    implementation("com.google.android.exoplayer:exoplayer-ui:$exoplayer_version")

    val room_version = "2.6.1"
    implementation("androidx.room:room-runtime:$room_version")
    annotationProcessor("androidx.room:room-compiler:$room_version")
    ksp("androidx.room:room-compiler:$room_version")

    implementation("androidx.room:room-ktx:$room_version")
    //ksp("androidx.room:room-compiler:$room_version")
    implementation("androidx.room:room-rxjava2:$room_version")
    implementation("androidx.room:room-rxjava3:$room_version")
    implementation("androidx.room:room-guava:$room_version")
    testImplementation("androidx.room:room-testing:$room_version")
    implementation("androidx.room:room-paging:$room_version")

}