plugins {
    id("com.android.application")
}

android {
    namespace = "ca.hdclark.mtbsim"
    compileSdk = 36

    defaultConfig {
        applicationId = "ca.hdclark.mtbsim"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    lint {
        abortOnError = true
        checkReleaseBuilds = true
        warningsAsErrors = false
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.4")
}
