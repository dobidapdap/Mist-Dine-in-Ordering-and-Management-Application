plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    defaultConfig {
        vectorDrawables.useSupportLibrary = true
    }
}


android {
    namespace = "com.example.mistcaferestaurantpanglao"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.mistcaferestaurantpanglao"
        minSdk = 26
        targetSdk = 35
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
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.firestore)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.google.zxing:core:3.5.0")


    implementation("androidx.recyclerview:recyclerview:1.3.2")


    implementation("androidx.recyclerview:recyclerview:1.3.0")
    implementation("androidx.cardview:cardview:1.0.0")


    implementation("com.github.bumptech.glide:glide:4.15.1")
    annotationProcessor("com.github.bumptech.glide:compiler:4.15.1")


    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    implementation("com.google.android.material:material:1.9.0")

    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.5.1")

    implementation("androidx.work:work-runtime:2.9.0")

    implementation("androidx.cardview:cardview:1.0.0")


}