import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.compose")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    // Todo el nucleo compartido: dominio, modelos, repos GitLive y Room KMP
    implementation(project(":shared"))
    // Backend JVM de GitLive (expone FirebasePlatform para inicializar en escritorio)
    implementation("dev.gitlive:firebase-java-sdk:0.4.5")

    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.1")
}

compose.desktop {
    application {
        mainClass = "com.laprevia.restobar.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "LaPreviaRestobar"
            packageVersion = "1.0.0"
        }
    }
}
