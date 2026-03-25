plugins {
    kotlin("jvm")
    id("com.gradleup.shadow")
    id("application")
}

application {
    mainClass.set("net.mcbrawls.fracture.webhook.Main")
}

repositories {
    mavenCentral()
    maven("https://maven.mcbrawls.net/releases/")
    maven("https://libraries.minecraft.net/")
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":"))

    implementation("ch.qos.logback:logback-classic:1.5.27")
    implementation("com.google.code.gson:gson:2.13.2")

    val ktorVersion = "3.3.3"
    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
}

tasks.shadowJar {
    archiveClassifier.set("")
    mergeServiceFiles()
    manifest {
        attributes["Main-Class"] = "net.mcbrawls.fracture.webhook.Main"
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.jar {
    enabled = false
}

tasks.named("startScripts", CreateStartScripts::class) {
    dependsOn(tasks.shadowJar)
}

tasks.named("distZip", Zip::class) {
    dependsOn(tasks.shadowJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.named("distTar", Tar::class) {
    dependsOn(tasks.shadowJar)
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
