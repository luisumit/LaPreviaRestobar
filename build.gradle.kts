// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.10.1" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("com.google.dagger.hilt.android") version "2.57.1" apply false
    id("com.google.gms.google-services") version "4.4.4" apply false
    id("com.google.firebase.firebase-perf") version "2.0.2" apply false
    id("com.google.firebase.crashlytics") version "3.0.6" apply false
    id("com.google.devtools.ksp") version "2.2.10-2.0.2" apply false
    id("org.sonarqube") version "6.3.1.5724"
    alias(libs.plugins.kotlin.compose) apply false
}

sonar {
    properties {
        property("sonar.projectKey", "luisumit_LaPreviaRestobar")
        property("sonar.organization", "luisumit")
        property("sonar.host.url", "https://sonarcloud.io")
        property("sonar.sources", "app/src/main/java")
        property("sonar.tests", "app/src/test/java")
        property("sonar.exclusions", "app/src/test/**,app/src/androidTest/**,**/build/**")
        property("sonar.test.inclusions", "app/src/test/**/*.kt,app/src/test/**/*.java")
        property("sonar.androidLint.reportPaths", "app/build/reports/lint-results-debug.xml")
        property("sonar.coverage.jacoco.xmlReportPaths", "app/build/reports/jacoco/jacocoTestReport/jacocoTestReport.xml")
    }
}

tasks.register("clean", Delete::class) {
    delete(layout.buildDirectory)
}
