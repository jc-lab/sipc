import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.20"
}

val projectGroup = "kr.jclab.sipc"
val projectVersion = "0.2.3-rc4"

group = projectGroup
version = projectVersion

allprojects {
    group = projectGroup
    version = projectVersion

    repositories {
        maven {
            url = uri("https://s01.oss.sonatype.org/content/repositories/releases")
        }
    }
}

repositories {
    mavenCentral()
}

dependencies {
}
