plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.onlyfanshop"
    compileSdk = 36
    buildFeatures {
        viewBinding= true
    }

    defaultConfig {
        applicationId = "com.example.onlyfanshop"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }

}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.github.bumptech.glide:glide:5.0.5")
    annotationProcessor("com.github.bumptech.glide:compiler:5.0.5")
    implementation(libs.firebase.messaging)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.recyclerview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    
    // Firebase dependencies
    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    
    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.4.0")

    implementation(platform("com.google.firebase:firebase-bom:34.5.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging:24.0.0")

    implementation("androidx.activity:activity:1.8.2")

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("com.google.code.gson:gson:2.13.2")
    implementation("com.github.ismaeldivita:chip-navigation-bar:1.3.4")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // Nếu bạn đang dùng version catalog cho viewpager2
    // Hãy chắc chắn trong libs.versions.toml đã khai báo. Nếu chưa, thay bằng:
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.cardview:cardview:1.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata:2.9.4")
    implementation("androidx.fragment:fragment:1.8.9")
// Networking & JSON
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.google.code.gson:gson:2.13.2")
// OSM (osmdroid)
    implementation("org.osmdroid:osmdroid-android:6.1.20")
// (Tùy chọn) Play services location nếu muốn fused location (bạn có thể bỏ)
    implementation("com.google.android.gms:play-services-location:21.3.0")
// or latest version
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    implementation("me.leolin:ShortcutBadger:1.1.22@aar")
    implementation(platform("com.squareup.okhttp3:okhttp-bom:5.2.1"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    
    // Volley for FCM notifications
    implementation("com.android.volley:volley:1.2.1")

    implementation("com.google.mlkit:translate:17.0.3")

    // Feather Icons - Modern icon library
    implementation("com.github.duanhong169:drawabletoolbox:1.0.7")
    implementation("androidx.vectordrawable:vectordrawable:1.2.0")
    implementation("androidx.vectordrawable:vectordrawable-animated:1.2.0")

}
