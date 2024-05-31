@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    `maven-publish`
}

kotlin {
    androidTarget {
        compilations.all {
            this@androidTarget.compilerOptions {
                jvmTarget.set(JvmTarget.JVM_1_8)
                freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
            }
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach {
        it.binaries.framework {
            baseName = "ReachabilityCore"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(libs.jetbrains.kotlinx.coroutines)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

publishing {
    repositories {
        maven {
            name = "Reachability"
            description = "Reachability library for Android and iOS"
            url = uri("https://maven.pkg.github.com/hipporasy/reachability")
            credentials {
                username = System.getenv("GITHUB_AUTHOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

android {
    namespace = "dev.hipporasy.reachability"
    compileSdk = 34
    defaultConfig {
        minSdk = 27
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}