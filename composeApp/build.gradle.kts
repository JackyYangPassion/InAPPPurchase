import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }
    
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
        iosTarget.binaries.all {
            linkerOpts += listOf(
                "-framework", "StoreKit",
                "-framework", "Foundation"
            )
        }
        iosTarget.apply {
            compilations.configureEach {
                kotlinOptions {
                    freeCompilerArgs += listOf(
                        "-Xobjc-generics",
                        "-linker-option", "-framework", "-linker-option", "StoreKit",
                        "-Xexport-kdoc"
                    )
                }
            }
            binaries.all {
                binaryOptions["deploymentTarget"] = "16.0"
            }
        }
    }
    
    sourceSets {
        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.material)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodel)
            implementation(libs.androidx.lifecycle.runtime.compose)
            // 添加 Kermit 依赖
            // 使用版本目录引用 Kermit
            implementation(libs.touchlab.kermit)
        }
    }

    targets.withType<org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget> {
        compilations.configureEach {
            kotlinOptions {
                freeCompilerArgs += listOf(
                    "-Xbinary=ios_min_version=15.0",
                    "-Xexport-kdoc"
                )
            }
        }
    }
}

android {
    namespace = "compose.project.demo"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "compose.project.demo"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.androidx.material3.android)
    implementation(project(":composeApp"))
    debugImplementation(compose.uiTooling)
}

