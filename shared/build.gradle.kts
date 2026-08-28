plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.library")
    id("com.google.devtools.ksp")
    id("androidx.room")
}

kotlin {
    // Targets: Android + Desktop (JVM). Web (wasmJs) se agrega en una fase posterior.
    androidTarget {
        compilations.all {
            compileTaskProvider.configure {
                compilerOptions {
                    jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
                }
            }
        }
    }
    jvm("desktop")

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
                // Firebase multiplataforma (GitLive): Android + Desktop/JVM + JS
                api("dev.gitlive:firebase-database:2.1.0")
                api("dev.gitlive:firebase-auth:2.1.0")
                // Room multiplataforma (base local): Android + Desktop/JVM
                api("androidx.room:room-runtime:2.8.4")
                api("androidx.sqlite:sqlite-bundled:2.6.1")
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
    }
}

android {
    namespace = "com.laprevia.restobar.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    // Compilador de Room por target (genera DAOs y el constructor de la BD)
    add("kspAndroid", "androidx.room:room-compiler:2.8.4")
    add("kspDesktop", "androidx.room:room-compiler:2.8.4")
}
