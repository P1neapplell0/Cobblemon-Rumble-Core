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
            if (providers.gradleProperty("rumblePacks").orNull == "false") {
                systemProperty("rumble.disableExternalPacks", "true")
            } else {
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
            if (providers.gradleProperty("rumbleAuditArenaRosters").orNull == "true") {
                systemProperty("rumble.auditArenaRosters", "true")
            }
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
    exclude("rbrctai-*.jar")
    exclude { it.name.lowercase().let { name ->
        name.contains("autobattle") || name.contains("auto-battle") || name.contains("auto_battle") || name.startsWith("smartbattleai-")
    } }
}

// Preserve each local mod’s descriptor/resources and add a module name for Jar-in-Jar.
val playerDataJar = layout.projectDirectory.file("libs/simple_player_data_manager-1.0.0+mc1.21.1-neoforge.jar")
val bundledMods = mapOf(
    "simple_player_data_manager" to playerDataJar.asFile.name,
    "cobbreeding" to "Cobbreeding-neoforge-2.2.2.jar",
    "virtualloot" to "VirtualLoot-neoforge-0.4.1.jar",
    "fsweep_stackfix" to "fsweep_stackfix-1.0.0.jar",
    "msd_special_move_fix" to "msd_special_move_fix-1.0.0.jar",
)

val bundledModInputs = bundledMods.mapNotNull { (id, filename) ->
    val source = layout.projectDirectory.file("libs/$filename").asFile
    if (!source.isFile) {
        check(id != "simple_player_data_manager" && id != "virtualloot") { "Missing required build input: $source" }
        return@mapNotNull null
    }
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
            || it.name.startsWith("custom_instances-") || it == playerDataJar.asFile
            || it.name.startsWith("legendarymonuments-") || it.name.startsWith("CobbleDollars-")
            || it.name.startsWith("cobblemoncinematics-") || it.name.startsWith("cobbleeffortvr-") || it.name.startsWith("mega_showdown-") || it.name.startsWith("accessories-neoforge-")
            || it.name.startsWith("rctmod-") || it.name.startsWith("rctapi-") || it.name.startsWith("cobblebash-")
            || it.name.startsWith("cobblebash_screen-") || it.name.startsWith("cobblesafari-")
            || it.name.startsWith("quark-") || it.name.startsWith("zeta-")
            || it.name.startsWith("SoulHome-Mod-") || it.name.startsWith("female-gender-")
            || it.name.startsWith("waystones-") || it.name.startsWith("balm-neoforge-")
            || it.name.startsWith("cobblemon_smartphone-") || it.name.startsWith("arc_quest-") || it.name.startsWith("mine_chat-") || it.name.startsWith("beyonddimensions-")) {
            compileOnly(jar)
        }
    }
}

tasks.processResources {
    from("docs/title-screen") {
        include("arena-wordmark.png", "cobblemon-mark.png", "clean-arena.png", "game/*.png")
        eachFile { path = "assets/cobblemon_rumble_core/textures/title/$name" }
        includeEmptyDirs = false
    }
    from("docs/title-screen/game/window-icons") { into("rumble-window-icons") }
    from(layout.projectDirectory.dir("modpack_override/config")) {
        include("custom_instances/definitions/rumble_prologue.json")
        include("custom_instances/definitions/rumble_battle.json")
        include("custom_instances/definitions/rumble_champion.json")
        include("custom_instances/definitions/rumble_gym_*.json")
        include("cobblemon_rumble_core/recycling.json")
        include("cobblemon_rumble_core/mega_challenges.tsv")
        include("cobblemon_rumble_core/maps/gyms/region/r.-1.-1.mca")
        include("cobblemon_rumble_core/maps/gyms/region/r.-1.0.mca")

        into("defaults")
    }
    val modVersion = project.version.toString()
    inputs.property("modVersion", modVersion)
    filesMatching("META-INF/neoforge.mods.toml") {
        expand("modVersion" to modVersion)
    }
}

// Keep the normal, Jar-in-Jar-enabled Core intact as the payload of the release JAR.
val coreModJar = tasks.named<Jar>("jar") {
    destinationDirectory.set(layout.buildDirectory.dir("internal/core"))
}

// ImmediateWindowProvider is discovered in the SERVICE layer before normal mods.
val earlyDisplay = sourceSets.create("earlyDisplay") {
    compileClasspath += sourceSets.main.get().compileClasspath
}
val configureEarlyDisplay: Jar.() -> Unit = {
    from(earlyDisplay.output)
    from("docs/title-screen") {
        include("arena-wordmark.png", "cobblemon-mark.png", "game/runner.png", "game/arena-blur.png", "game/window-icons/*.png")
        eachFile { path = "rumble-early/$name" }
        includeEmptyDirs = false
    }
    manifest.attributes("Automatic-Module-Name" to "com.p1nero.rumble.earlydisplay")
}
val releaseJar by tasks.registering(Jar::class) {
    group = "build"
    description = "Packages the early window and Core mod into the single distributable JAR."
    configureEarlyDisplay()
    from("src/release/resources")
    from(coreModJar.flatMap { it.archiveFile }) {
        into("rumble-core")
        rename { "core.jar" }
    }
}
tasks.assemble { dependsOn(releaseJar) }

// runClient already loads Core from source; stage only the early-window service there.
val earlyDisplayJar by tasks.registering(Jar::class) {
    archiveBaseName.set("rumble-early-display")
    destinationDirectory.set(layout.buildDirectory.dir("internal/early-display"))
    configureEarlyDisplay()
    exclude("**/BundledCoreLocator.class")
}
val stageEarlyDisplay by tasks.registering(Copy::class) {
    from(earlyDisplayJar)
    into(layout.projectDirectory.dir("runs/neoforge-client/mods"))
    rename { "rumble-early-display.jar" }
}
tasks.named("runClient") { dependsOn(stageEarlyDisplay) }

val frontierBracketCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.named("testClasses"))
    classpath = files(sourceSets.test.get().output, sourceSets.main.get().output)
    mainClass.set("com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierBracketCheck")
}
tasks.named("check") { dependsOn(frontierBracketCheck) }

// Mirror only packaged overrides; never sync/delete unrelated local config, options or worlds.
tasks.register("syncRunOverrides") {
    group = "development"
    description = "Copies modpack overrides to the local run and NeoForge test directories."
    val overrideRoot = layout.projectDirectory.dir("modpack_override")
    val targets = listOf("run", "runs/neoforge-client", "runs/neoforge-server").map(layout.projectDirectory::dir)
    doLast {
        val packKey = "resourcePacks:"
        fun packs(line: String): List<String> = (groovy.json.JsonSlurper().parseText(line.removePrefix(packKey)) as List<*>)
            .filterIsInstance<String>()
        val listed = overrideRoot.file("options.txt").asFile.readLines().first { it.startsWith(packKey) }
        val packagedPacks = packs(listed)
        val modResources = packagedPacks.indexOf("mod_resources").let { if (it < 0) packagedPacks.size else it }
        targets.forEach { target ->
            project.copy {
                from(overrideRoot)
                include("config/**", "global_packs/**", "resourcepacks/**", "optional_datapacks/**")
                into(target)
            }
            if (target.asFile.name != "neoforge-server") {
                val options = target.file("options.txt").asFile
                val lines = if (options.exists()) options.readLines().toMutableList() else mutableListOf()
                val index = lines.indexOfFirst { it.startsWith(packKey) }
                val existing = if (index >= 0) packs(lines[index]) else emptyList()
                val localPacks = existing.filterNot(packagedPacks::contains)
                val order = (packagedPacks.take(modResources) + localPacks + packagedPacks.drop(modResources)).distinct()
                val line = packKey + groovy.json.JsonOutput.toJson(order)
                if (index >= 0) lines[index] = line else lines.add(line)
                options.writeText(lines.joinToString("\n", postfix = "\n"))
            }
        }
    }
}

val frontierRulesCheck by tasks.registering(JavaExec::class) {
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("com.p1nero.cobblemon.rumblecore.combat.arena.frontier.FrontierRulesCheck")
}

tasks.named("check") { dependsOn(frontierRulesCheck) }

val gymReplacementsCheck by tasks.registering(JavaExec::class) {
    group = "verification"
    dependsOn(tasks.testClasses)
    classpath = files(sourceSets.test.get().output, sourceSets.main.get().output)
    mainClass.set("com.p1nero.cobblemon.rumblecore.combat.arena.GymReplacementsCheck")
}

tasks.named("check") { dependsOn(gymReplacementsCheck) }
