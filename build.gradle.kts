plugins {
    kotlin("jvm") version("2.3.0")
    id("java")
    id("maven-publish")
}

group = "net.mcbrawls"
version = "1.1.0"

repositories {
    mavenCentral()
    maven("https://maven.mcbrawls.net/releases/")
    maven("https://libraries.minecraft.net/")
}

dependencies {
    api("net.mcbrawls:codex:2.0.0")
    api("io.github.rybalkinsd:kohttp:0.12.0")

    val kacheVersion = "2.1.1"
    api("com.mayakapps.kache:kache:$kacheVersion")
    api("com.mayakapps.kache:file-kache:$kacheVersion")

    api("org.slf4j:slf4j-api:2.0.17")
    api("org.apache.commons:commons-text:1.15.0")

    val ktorVersion = "3.4.0"
    api("io.ktor:ktor-server-core:$ktorVersion")
    api("io.ktor:ktor-server-netty:$ktorVersion")
}

kotlin {
    jvmToolchain(25)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            groupId = project.group as String
            artifactId = project.name
            version = project.version as String
        }
    }

    repositories {
        val mavenUrl = System.getenv("MAVEN_URL")
        if (mavenUrl != null) {
            maven {
                name = "envmaven"
                url = uri(mavenUrl)
                credentials {
                    username = System.getenv("MAVEN_USERNAME")
                    password = System.getenv("MAVEN_PASSWORD")
                }
            }
        }
    }
}
