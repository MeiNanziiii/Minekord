import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    id("fabric-loom") version "1.9-SNAPSHOT"
}

loom {
    serverOnlyMinecraftJar()
}

val modVersion: String by project
val mavenGroup: String by project

base.archivesName.set("minekord")

version = "$modVersion+${libs.versions.minecraft.get()}"
group = mavenGroup

repositories {

}

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.yarn)

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.kotlin)
    modImplementation(libs.fabric.api)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_21)
    }
}

tasks {
    processResources {
        inputs.property("version", modVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to modVersion
            )
        }
    }

    jar {
        from("LICENSE")
    }
}
