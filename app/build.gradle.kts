// 乒乓球训练助手 PingPang · app 模块构建脚本
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.pingpang.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pingpang.app"
        minSdk = 26
        targetSdk = 34
        // 发布新版本时同步修改；version.txt 的 versionCode 必须 >= 此值（判更新唯一依据）
        versionCode = 1
        versionName = "0.1.0"
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // AGP 7.4.2 + Kotlin 1.9.24 的 Compose 编译器版本
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
    packagingOptions {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx) {
        // 强制 Java 8 字节码版本，规避 AGP 7.4 D8 对高版本字节码的 NPE
        version { strictly(libs.versions.lifecycle.get()) }
    }
    implementation(libs.androidx.lifecycle.viewmodel.compose) {
        version { strictly(libs.versions.lifecycle.get()) }
    }
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    // Room 数据层
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    // 网络（version.txt 更新检查 / AI 调用）
    implementation(libs.okhttp)
    implementation(libs.kotlinx.coroutines.android)

    // 媒体：多路视频播放 + 录制
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.ui)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)
    implementation(libs.camerax.video)

    debugImplementation(libs.androidx.ui.tooling)
}
