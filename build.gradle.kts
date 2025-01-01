import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    kotlin("jvm") version "2.0.20"
    id("fabric-loom") version "1.9-SNAPSHOT"
}

loom {
    serverOnlyMinecraftJar()

    accessWidenerPath = file("src/main/resources/minekord.accesswidener")
}

val modVersion: String by project
val mavenGroup: String by project

base.archivesName.set("minekord")

version = "$modVersion+${libs.versions.minecraft.get()}"
group = mavenGroup

repositories {
    maven("https://maven.nucleoid.xyz")
    maven("https://snapshots-repo.kordex.dev")
    maven("https://repo.kord.dev/snapshots")
}

val includeImplementation: Configuration by configurations.creating {
    configurations.implementation.configure { extendsFrom(this@creating) }
}

dependencies {
    minecraft(libs.minecraft)
    mappings(libs.yarn)

    modImplementation(libs.fabric.loader)
    modImplementation(libs.fabric.kotlin)
    modImplementation(libs.fabric.api)

    includeImplementation(libs.kordex)
    includeImplementation(libs.konf.core)
    includeImplementation(libs.konf.toml)

    implementAndInclude(libs.discord.reserializer)
    implementAndInclude(libs.simple.ast)

    implementAndInclude(libs.kyori)
    implementAndInclude(libs.placeholder.api)

    modCompileOnly(libs.luckperms)
    implementAndInclude(libs.permissions)
}

afterEvaluate {
    dependencies {
        handleIncludes(includeImplementation)
    }
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

/* Thanks to https://github.com/jakobkmar for original script */
fun DependencyHandlerScope.includeTransitive(
    dependencies: Set<ResolvedDependency>,
    minecraftLibs: Set<ResolvedDependency>,
    kotlinDependency: ResolvedDependency,
    checkedDependencies: MutableSet<ResolvedDependency> = HashSet()
) {
    dependencies.forEach {
        if (checkedDependencies.contains(it) || it.moduleGroup == "org.jetbrains.kotlin" || it.moduleGroup == "org.jetbrains.kotlinx") return@forEach

        if (kotlinDependency.children.any { dep -> dep.name == it.name }) {
            println("Skipping -> ${it.name} (already in fabric-language-kotlin)")
        } else if (minecraftLibs.any { dep -> dep.moduleGroup == it.moduleGroup && dep.moduleName == it.moduleName }) {
            println("Skipping -> ${it.name} (already in minecraft)")
        } else {
            include(it.name)
            println("Including -> ${it.name}")
        }
        checkedDependencies += it

        includeTransitive(it.children, minecraftLibs, kotlinDependency, checkedDependencies)
    }
}

fun DependencyHandlerScope.handleIncludes(configuration: Configuration) {
    includeTransitive(
        configuration.resolvedConfiguration.firstLevelModuleDependencies,
        configurations.minecraftLibraries.get().resolvedConfiguration.firstLevelModuleDependencies,
        configurations.modImplementation.get().resolvedConfiguration.firstLevelModuleDependencies
            .first { it.moduleGroup == "net.fabricmc" && it.moduleName == "fabric-language-kotlin" },
    )
}

fun DependencyHandlerScope.implementAndInclude(dep: Any) {
    modImplementation(dep)
    include(dep)
}
