plugins {
    `java-library`
    `maven-publish`
    `signing`
    id("com.google.protobuf") version "0.9.1"
}

repositories {
    mavenCentral()
    maven {
        url = uri("https://s01.oss.sonatype.org/content/repositories/releases/")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

dependencies {
    testCompileOnly("org.projectlombok:lombok:1.18.24")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.24")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.7.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
    testImplementation("org.mockito:mockito-core:4.9.0")

    compileOnly("org.projectlombok:lombok:1.18.24")
    annotationProcessor("org.projectlombok:lombok:1.18.24")

    api("io.netty:netty-buffer:${Version.NETTY}")
    api("io.netty:netty-codec:${Version.NETTY}")
    api("io.netty:netty-common:${Version.NETTY}")
    api("io.netty:netty-handler:${Version.NETTY}")
    api("io.netty:netty-transport:${Version.NETTY}")
    api("io.netty:netty-transport-native-unix-common:${Version.NETTY}")
    api("io.netty:netty-transport-native-epoll:${Version.NETTY}")
    api("io.netty:netty-transport-native-kqueue:${Version.NETTY}")
    api("kr.jclab.netty:netty-transport-classes-iocp:${Version.NETTY_IOCP}")

    implementation("com.google.protobuf:protobuf-java:${Version.PROTOBUF}")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("kr.jclab:noise-java:0.0.1")
    implementation("org.slf4j:slf4j-api:2.0.6")

    compileOnly("net.java.dev.jna:jna:5.13.0")
    compileOnly("net.java.dev.jna:jna-platform:5.13.0")

    testImplementation("org.slf4j:slf4j-simple:2.0.6")
    testImplementation("net.java.dev.jna:jna:5.13.0")
    testImplementation("net.java.dev.jna:jna-platform:5.13.0")
    testImplementation("io.netty:netty-transport-native-epoll:${Version.NETTY}:linux-x86_64")
    testImplementation("io.netty:netty-transport-native-epoll:${Version.NETTY}:linux-aarch_64")
    testImplementation("io.netty:netty-transport-native-kqueue:${Version.NETTY}:osx-x86_64")
    testImplementation("io.netty:netty-transport-native-kqueue:${Version.NETTY}:osx-aarch_64")
    testImplementation("kr.jclab.netty:netty-transport-native-iocp:${Version.NETTY_IOCP}")
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:${Version.PROTOBUF}"
    }
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])

            pom {
                name.set(project.name)
                description.set("secure ipc")
                url.set("https://github.com/jc-lab/sipc")
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                developers {
                    developer {
                        id.set("jclab")
                        name.set("Joseph Lee")
                        email.set("joseph@jc-lab.net")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/jc-lab/sipc.git")
                    developerConnection.set("scm:git:ssh://git@github.com/jc-lab/sipc.git")
                    url.set("https://github.com/jc-lab/sipc")
                }
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
            val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
            url = uri(if ("$version".endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
            credentials {
                username = findProperty("ossrhUsername") as String?
                password = findProperty("ossrhPassword") as String?
            }
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

tasks.withType<Sign>().configureEach {
    onlyIf { project.hasProperty("signing.gnupg.keyName") || project.hasProperty("signing.keyId") }
}
