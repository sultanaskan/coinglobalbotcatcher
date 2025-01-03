plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    
}

buildscript {
    dependencies {
        classpath(libs.google.services) // Ensure this line is present
    }
}


android {
   namespace = "com.askan.coinglobalbot"
    //    namespace = "com.askan.metamaxpkr"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.askan.coinglobalbot"
        //   applicationId = "com.askan.metamaxpkr"
        minSdk = 26
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("com.google.code.gson:gson:2.8.9")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    // Firebase Authentication
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.v2211)
    implementation(libs.firebase.auth)
    // Firebase Realtime Database
    implementation(libs.firebase.database)
    // Firebase Firestore (Optional)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.espresso.core)
    implementation(libs.androidx.espresso.core)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Apply Google services plugin at the bottom of the file
apply(plugin = "com.google.gms.google-services")
