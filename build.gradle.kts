// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("com.android.library") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.multiplatform") version "2.2.10" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.10" apply false
    id("org.jetbrains.compose") version "1.8.2" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("androidx.room") version "2.8.4" apply false
    id("org.sonarqube") version "7.2.3.7755"
    alias(libs.plugins.kotlin.compose) apply false
}

sonar {
    properties {
        property("sonar.projectKey", "luisumit_LaPreviaRestobar")
        property("sonar.organization", "luisumit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.androidLint.reportPaths", rootProject.file("app/build/reports/lint-results-debug.xml").absolutePath)
        property(
            "sonar.coverage.jacoco.xmlReportPaths",
            rootProject.file("app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml").absolutePath
        )
        property(
            "sonar.coverage.exclusions",
            listOf(
                "**/presentation/**",
                "**/ui/**",
                "**/di/**",
                "**/*Activity*.*",
                "**/*Application*.*",
                "**/LaPreviaApp.kt",
                "**/*Worker*.*",
                "**/data/remote/**",
                "**/data/local/dao/**",
                "**/data/local/db/**",
                "**/data/local/entity/**",
                "**/data/repository/Firebase*RepositoryImpl.kt",
                "**/data/repository/GitLive*Repository.kt",
                "**/data/repository/*RepositoryImpl.kt",
                "**/data/repository/Unified*Repository.kt",
                "**/data/local/datastore/**",
                "**/data/local/sync/**",
                "**/domain/service/FirebaseInitializerService.kt",
                "**/domain/service/InventorySyncService.kt",
                "**/desktop/**"
            ).joinToString(",")
        )
    }
}

project(":shared") {
    sonar {
        properties {
            property(
                "sonar.sources",
                listOf(
                    "src/commonMain/kotlin",
                    "src/androidMain/kotlin",
                    "src/desktopMain/kotlin"
                ).joinToString(",")
            )
        }
    }
}

project(":desktopApp") {
    sonar {
        properties {
            property("sonar.sources", "src/main/kotlin")
        }
    }
}

tasks.named("sonar") {
    dependsOn(":app:jacocoTestReport")
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
