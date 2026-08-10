plugins {
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.kotlin)
}

val modVersion: String = "1.0.0"

version = "$modVersion+${libs.versions.minecraft.get()}"
group = "ua.bonfiremc"

repositories {
    mavenCentral()

    maven("https://oss.sonatype.org/content/repositories/snapshots/")
    maven("https://snapshots.kord.dev")
    maven("https://snapshots-repo.kordex.dev")
    maven("https://releases-repo.kordex.dev")
    maven("https://maven.nucleoid.xyz")
}

val includeImplementation: Configuration by configurations.creating {
    configurations.implementation.configure { extendsFrom(this@creating) }
}

dependencies {
    minecraft(libs.minecraft)

    implementation(libs.fabric.loader)
    implementation(libs.fabric.kotlin)
    implementation(libs.fabric.api)

    includeImplementation(libs.kordex)

    includeImplementation(libs.konf.core)
    includeImplementation(libs.konf.toml)

    implementAndInclude(libs.placeholder.api)
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

afterEvaluate {
    dependencies {
        handleIncludes(includeImplementation)
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
        if (checkedDependencies.contains(it) || (it.moduleName == "sweetspi-bom") || it.moduleGroup == "org.jetbrains.kotlin" || it.moduleGroup == "org.jetbrains.kotlinx") return@forEach

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
        configurations.runtimeClasspath.get().resolvedConfiguration.firstLevelModuleDependencies
            .first { it.moduleGroup == "net.fabricmc" && it.moduleName == "fabric-language-kotlin" },
    )
}

fun DependencyHandlerScope.implementAndInclude(dep: Any) {
    implementation(dep)
    include(dep)
}
