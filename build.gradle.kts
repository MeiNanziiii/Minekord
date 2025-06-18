import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin)
    alias(libs.plugins.kordex)
    alias(libs.plugins.detekt)
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

    implementation(libs.konf.core)
    implementation(libs.konf.toml)
}

kordEx {
    kordVersion = libs.versions.kord
    kordExVersion = libs.versions.kordex
}

detekt {
    buildUponDefaultConfig = true

    config.setFrom("$projectDir/detekt.yml")
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

afterEvaluate {
    dependencies {
        includeTransitive(libs.konf.core.get().toString())
        includeTransitive(libs.konf.toml.get().toString())

        includeTransitive("dev.kord:kord-core:feat-components-v2-SNAPSHOT")
        includeTransitive("dev.kordex:kord-extensions:2.3.2-SNAPSHOT")
    }
}

fun DependencyHandlerScope.includeTransitive(dependency: String) {
    val resolvedDependency: ResolvedDependency = configurations.runtimeClasspath.get().resolvedConfiguration.firstLevelModuleDependencies.firstOrNull { it.name == dependency } ?: return

    val minecraftLibraries: Set<ResolvedDependency> = configurations.minecraftLibraries.get().resolvedConfiguration.firstLevelModuleDependencies
    val kotlinLibraries: Set<ResolvedDependency> = configurations.modImplementation.get().resolvedConfiguration.firstLevelModuleDependencies.first { it.moduleGroup == "net.fabricmc" && it.moduleName == "fabric-language-kotlin" }.children
    val checkedLibraries: MutableSet<ResolvedDependency> = HashSet()

    includeDependency(resolvedDependency, minecraftLibraries, kotlinLibraries, checkedLibraries)
}

fun DependencyHandlerScope.includeDependency(
    dependency: ResolvedDependency,
    minecraftLibraries: Set<ResolvedDependency>,
    kotlinLibraries: Set<ResolvedDependency>,
    checkedLibraries: MutableSet<ResolvedDependency>
) {
    if (dependency in checkedLibraries) return

    if (minecraftLibraries.any { it.name == dependency.name }) {
        println("Skipping -> ${dependency.name} (already in minecraft)")
    } else if (kotlinLibraries.any { it.name == dependency.name }) {
        println("Skipping -> ${dependency.name} (already in fabric-language-kotlin)")
    } else {
        include(dependency.name)
        println("Including -> ${dependency.name}")
    }

    checkedLibraries += dependency

    dependency.children.forEach {
        includeDependency(it, minecraftLibraries, kotlinLibraries, checkedLibraries)
    }
}
