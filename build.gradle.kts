plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.fabric.loom)
}

val modVersion: String = "1.0.0"

version = "$modVersion+${libs.versions.minecraft.get()}"
group = "ua.bonfiremc"

repositories {

}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)
    implementation(libs.fabric.api)
}

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks {
    processResources {
        val minecraftVersion = libs.versions.minecraft.get()
        val fabricLoaderVersion = libs.versions.fabric.loader.get()
        val fabricKotlinVersion = libs.versions.fabric.kotlin.get()

        inputs.property("version", version)
        inputs.property("minecraft_version", minecraftVersion)
        inputs.property("fabric_loader_version", fabricLoaderVersion)
        inputs.property("fabric_kotlin_version", fabricKotlinVersion)

        filesMatching("fabric.mod.json") {
            expand(
                "version" to version,
                "minecraft_version" to minecraftVersion,
                "fabric_loader_version" to fabricLoaderVersion,
                "fabric_kotlin_version" to fabricKotlinVersion
            )
        }
    }

    jar {
        from("LICENSE")
    }
}
