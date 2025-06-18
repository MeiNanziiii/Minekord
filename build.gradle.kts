import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.loom)
}

loom {
    serverOnlyMinecraftJar()
}

val modId: String by project
val modVersion: String by project
val mavenGroup: String by project

base.archivesName.set(modId)

version = "$modVersion+${libs.versions.minecraft.get()}"
group = mavenGroup

repositories {

}

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.yarn)

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.api)
    modImplementation(libs.fabric.kotlin)
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
        inputs.property("id", modId)
        inputs.property("version", modVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "id" to modId,
                "version" to modVersion,
                "fabricKotlin" to libs.versions.fabric.kotlin.get(),
                "fabricApi" to libs.versions.fabric.api.get(),
                "minecraft" to libs.versions.minecraft.get()
            )
        }
    }

    jar {
        from("LICENSE")
    }
}
