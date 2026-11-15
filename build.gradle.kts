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
            jvmArgument("-Xmx6G")
        }
        create("server") {
            server()
            gameDirectory.set(layout.projectDirectory.dir("runs/neoforge-server"))
            programArgument("--nogui")
            jvmArgument("-Xmx6G")
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

dependencies {
    developmentMods.files.sortedBy { it.name }.forEach {
        val jar = mapOf("name" to it.nameWithoutExtension, "ext" to "jar")
        runtimeOnly(jar)
        if (it.name.startsWith("Cobblemon-neoforge-") || it.name.startsWith("realtimecobblemon-")) {
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
