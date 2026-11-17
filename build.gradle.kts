import java.util.jar.JarFile

plugins {
    java
    id("net.neoforged.moddev") version "2.0.141"
}

group = "com.p1nero.cobblemon"
version = providers.gradleProperty("mod_version").get()
base.archivesName.set("cobblemon-rumble-core")
java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    flatDir { dirs("libs") }
}

neoForge {
    version = providers.gradleProperty("neo_version").get()
    runs {
        configureEach {
            if (providers.gradleProperty("rumblePacks").orNull != "false") {
                systemProperty("rumble.developmentPacks", layout.projectDirectory.dir("libs/packs").asFile.absolutePath)
            }
        }
        create("client") {
            client()
            gameDirectory.set(layout.projectDirectory.dir("runs/neoforge-client"))
            jvmArgument("-Xmx${providers.gradleProperty("rumbleClientHeap").getOrElse("6G")}")
        }
        create("server") {
            server()
            gameDirectory.set(layout.projectDirectory.dir("runs/neoforge-server"))
            programArgument("--nogui")
            jvmArgument("-Xmx${providers.gradleProperty("rumbleServerHeap").getOrElse("6G")}")
            providers.gradleProperty("rumbleServerWorld").orNull?.let {
                programArgument("--world")
                programArgument(it)
            }
        }
    }
    mods {
        create("cobblemon_rumble_core") { sourceSet(sourceSets.main.get()) }
    }
}

// NeoForge releases already use Mojang names; FML discovers their nested jars itself.
val developmentMods = fileTree("libs") {
    include("*.jar")
    exclude("*-sources.jar", "*-javadoc.jar")
}

// MIT mods also bundled in IncandescentVerityCore. Preserve their original descriptors and resources.
val bundledMitMods = mapOf(
    "cobbreeding" to "Cobbreeding-neoforge-2.2.2.jar",
    "fsweep_stackfix" to "fsweep_stackfix-1.0.0.jar",
    "msd_special_move_fix" to "msd_special_move_fix-1.0.0.jar",
)

val bundledModInputs = bundledMitMods.mapNotNull { (id, filename) ->
    val source = layout.projectDirectory.file("libs/$filename").asFile
    if (!source.isFile) return@mapNotNull null
    val originalManifest = JarFile(source).use { jar ->
        jar.manifest?.mainAttributes?.entries?.associate { (key, value) -> key.toString() to value }
            .orEmpty()
    }
    tasks.register<Jar>("prepareBundledMod_$id") {
        archiveFileName.set(filename)
        destinationDirectory.set(layout.buildDirectory.dir("bundled-mods"))
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        from(zipTree(source)) { exclude("META-INF/MANIFEST.MF") }
        manifest.attributes(originalManifest + ("Automatic-Module-Name" to "com.p1nero.rumble.bundled.$id"))
    }
}

// Kotlin method signatures on Cobblemon's Java API use the stdlib shipped by KFF.
val kotlinApi by tasks.registering(Copy::class) {
    from(developmentMods.files.filter { it.name.startsWith("kotlinforforge-") }.map { zipTree(it) }) {
        include("META-INF/jarjar/kotlin-stdlib-*.jar")
        exclude("**/kotlin-stdlib-jdk*.jar")
        eachFile { path = name }
        includeEmptyDirs = false
    }
    into(layout.buildDirectory.dir("kotlin-api"))
}

dependencies {
    compileOnly(fileTree(layout.buildDirectory.dir("kotlin-api")) { include("*.jar") }.builtBy(kotlinApi))
    add("jarJar", files(bundledModInputs))
    developmentMods.files.sortedBy { it.name }.forEach {
        val jar = mapOf("name" to it.nameWithoutExtension, "ext" to "jar")
        runtimeOnly(jar)
        if (it.name.startsWith("Cobblemon-neoforge-") || it.name.startsWith("realtimecobblemon-")
            || it.name.startsWith("legendarymonuments-") || it.name.startsWith("CobbleDollars-")
            || it.name.startsWith("cobblemon_smartphone-") || it.name.startsWith("arc_quest-") || it.name.startsWith("mine_chat-") || it.name.startsWith("beyonddimensions-")) {
            compileOnly(jar)
        }
    }
}

tasks.processResources {
    val modVersion = project.version.toString()
    inputs.property("modVersion", modVersion)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("modVersion" to modVersion)
    }
}
