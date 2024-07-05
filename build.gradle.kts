import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.reporter.ReporterType

plugins {
    kotlin("jvm") version "1.8.22"
    id("org.jlleitschuh.gradle.ktlint") version "11.5.1"
    application
}

val exposedVersion: String by project
val ktorVersion: String by project
group = "io.beatmaps"
version = "1.0-SNAPSHOT"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(16))
    }
    sourceSets.all {
        with(languageSettings) {
            optIn("kotlin.io.path.ExperimentalPathApi")
            optIn("io.ktor.server.locations.KtorExperimentalLocationsAPI")
            optIn("kotlin.time.ExperimentalTime")
            optIn("io.ktor.util.KtorExperimentalAPI")
            optIn("kotlin.ExperimentalUnsignedTypes")
        }
    }
}

dependencies {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        maven { url = uri("https://artifactory.kirkstall.top-cat.me") }
    }

    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-html-builder:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.8.0")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    implementation("ch.qos.logback:logback-classic:1.5.6")
    implementation("io.ktor:ktor-server-forwarded-header:$ktorVersion")
    implementation("io.ktor:ktor-server-locations:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")

    // Database drivers
    implementation("org.postgresql:postgresql:42.5.0")
    implementation("pl.jutupe:ktor-rabbitmq:0.5.19")
    implementation("com.rabbitmq:amqp-client:5.9.0")

    // Database library
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.flywaydb:flyway-core:9.2.2")

    implementation("io.beatmaps:BeatMaps-CommonMP:1.0.+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

application {
    mainClass.set("io.beatmaps.cdn.ServerKt")
}

ktlint {
    version.set("0.50.0")
    reporters {
        reporter(ReporterType.CHECKSTYLE)
    }
}
