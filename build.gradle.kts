import java.nio.channels.FileChannel
import java.nio.channels.FileLock
import java.nio.file.StandardOpenOption
import java.util.UUID
import java.util.zip.ZipFile
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.jvm.application.tasks.CreateStartScripts
import org.gradle.jvm.tasks.Jar
import org.gradle.language.jvm.tasks.ProcessResources

plugins {
    java
}

abstract class WorkspaceMutationLockService : BuildService<WorkspaceMutationLockService.Parameters>, AutoCloseable {
    interface Parameters : BuildServiceParameters {
        val lockFile: RegularFileProperty
        val enabled: Property<Boolean>
    }

    private val channel: FileChannel?
    private val lock: FileLock?

    init {
        if (parameters.enabled.get()) {
            val file = parameters.lockFile.get().asFile
            file.parentFile.mkdirs()
            channel = FileChannel.open(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE)
            lock = channel.lock()
        } else {
            channel = null
            lock = null
        }
    }

    override fun close() {
        lock?.release()
        channel?.close()
    }
}

fun archiveContainsClassFile(file: File): Boolean {
    if (!file.isFile) {
        return false
    }
    return try {
        ZipFile(file).use { zip ->
            zip.entries().asSequence().any { entry -> !entry.isDirectory && entry.name.endsWith(".class") }
        }
    } catch (_: Exception) {
        false
    }
}

val isMacOs = System.getProperty("os.name").lowercase().contains("mac")
val supportsNativeAccessFlag = Runtime.version().feature() >= 22
val lwjglClientJvmArgs = buildList {
    add("-Dorg.lwjgl.system.allocator=jemalloc")
    if (isMacOs) {
        add("-XstartOnFirstThread")
    }
    if (supportsNativeAccessFlag) {
        add("--enable-native-access=ALL-UNNAMED")
    }
}

fun JavaExec.configureLwjglClientJvm(vararg additionalJvmArgs: String) {
    jvmArgs(lwjglClientJvmArgs + additionalJvmArgs)
}

tasks.register("buildGame") {
    group = "voxel"
    description = "Builds all game modules and runs tests."
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.register("physicsRegression") {
    group = "verification"
    description = "Runs the tagged physics regression tests across all game modules."
    dependsOn(subprojects.map { "${it.path}:physicsRegression" })
}

val isWindows = System.getProperty("os.name").lowercase().contains("windows")
val localNpmExecutable = if (isWindows) {
    null
} else {
    file("${System.getProperty("user.home")}/.nvm/versions/node")
        .takeIf { it.isDirectory }
        ?.listFiles()
        ?.filter { it.isDirectory }
        ?.sortedByDescending { it.name }
        ?.map { it.resolve("bin/npm") }
        ?.firstOrNull { it.exists() }
}
val npmCommand = if (isWindows) "npm.cmd" else localNpmExecutable?.absolutePath ?: "npm"
val launcherNpmPath = localNpmExecutable
    ?.parentFile
    ?.absolutePath
    ?.let { "$it:${System.getenv("PATH").orEmpty()}" }
val testBinaryRunId = providers.gradleProperty("adventuraTestRunId")
    .orElse(providers.systemProperty("adventura.testRunId"))
    .orElse(UUID.randomUUID().toString())
    .map { it.replace(Regex("[^A-Za-z0-9._-]"), "_") }
val workspaceMutationLockEnabled = providers.gradleProperty("adventuraWorkspaceLock")
    .orElse(providers.systemProperty("adventura.workspaceLock"))
    .map { value -> value.lowercase() !in setOf("0", "false", "no", "off") }
    .orElse(true)
val workspaceMutationLock = gradle.sharedServices.registerIfAbsent(
    "adventuraWorkspaceMutationLock",
    WorkspaceMutationLockService::class
) {
    parameters.lockFile.set(layout.projectDirectory.file(".gradle/adventura-workspace-mutation.lock"))
    parameters.enabled.set(workspaceMutationLockEnabled)
}

tasks.register<Exec>("setupLauncher") {
    group = "voxel"
    description = "Installs the Electron launcher dependencies."
    workingDir = file("launcher-electron")
    launcherNpmPath?.let { environment("PATH", it) }
    commandLine(npmCommand, "install")
}

tasks.register<Exec>("runLauncher") {
    group = "voxel"
    description = "Runs the Electron launcher from the workspace."
    workingDir = file("launcher-electron")
    launcherNpmPath?.let { environment("PATH", it) }
    commandLine(npmCommand, "run", "dev")
}

tasks.register<Exec>("runElectronLauncher") {
    group = "voxel"
    description = "Runs the Electron launcher from the workspace."
    workingDir = file("launcher-electron")
    launcherNpmPath?.let { environment("PATH", it) }
    commandLine(npmCommand, "run", "dev")
}

tasks.register<Exec>("packageLauncher") {
    group = "voxel"
    description = "Builds the Electron launcher desktop package."
    workingDir = file("launcher-electron")
    launcherNpmPath?.let { environment("PATH", it) }
    commandLine(npmCommand, "run", "dist")
}

tasks.register<JavaExec>("runClient") {
    group = "voxel"
    description = "Runs the client and opens the main menu."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    configureLwjglClientJvm()
}

tasks.register<JavaExec>("runSingleplayer") {
    group = "voxel"
    description = "Runs the client directly in a generated singleplayer world."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-singleplayer", "--preview-radius", "3", "--render-distance", "8")
    configureLwjglClientJvm()
}

tasks.register<JavaExec>("profileSingleplayerJfr") {
    group = "profiling"
    description = "Runs singleplayer with Java Flight Recorder enabled for allocation and frame-time profiling."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-singleplayer", "--preview-radius", "3", "--render-distance", "8")
    val recordingFile = layout.buildDirectory.file("reports/jfr/adventura-singleplayer.jfr")
    doFirst {
        recordingFile.get().asFile.parentFile.mkdirs()
    }
    configureLwjglClientJvm(
        "-XX:StartFlightRecording=filename=${recordingFile.get().asFile.absolutePath},settings=profile,dumponexit=true",
        "-XX:FlightRecorderOptions=stackdepth=128"
    )
}

tasks.register<JavaExec>("profileJoinLocalJfr") {
    group = "profiling"
    description = "Runs the client join-local flow with Java Flight Recorder enabled."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-join", "--connect", "127.0.0.1", "--port", "25565", "--username", "Profiler", "--render-distance", "8")
    val recordingFile = layout.buildDirectory.file("reports/jfr/adventura-join-local.jfr")
    doFirst {
        recordingFile.get().asFile.parentFile.mkdirs()
    }
    configureLwjglClientJvm(
        "-XX:StartFlightRecording=filename=${recordingFile.get().asFile.absolutePath},settings=profile,dumponexit=true",
        "-XX:FlightRecorderOptions=stackdepth=128"
    )
}

tasks.register<JavaExec>("profileLongExploreJfr") {
    group = "profiling"
    description = "Runs a higher-distance singleplayer profile for long-explore chunk, lighting and upload analysis."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-singleplayer", "--seed", "424242", "--preview-radius", "5", "--render-distance", "12")
    val recordingFile = layout.buildDirectory.file("reports/jfr/adventura-long-explore.jfr")
    doFirst {
        recordingFile.get().asFile.parentFile.mkdirs()
    }
    configureLwjglClientJvm(
        "-XX:StartFlightRecording=filename=${recordingFile.get().asFile.absolutePath},settings=profile,dumponexit=true",
        "-XX:FlightRecorderOptions=stackdepth=128"
    )
}

tasks.register<JavaExec>("runServer") {
    group = "voxel"
    description = "Runs the dedicated server on port 25565."
    dependsOn(":server:classes")
    classpath = project(":server").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.server.GameServerMain")
    args("--port", "25565", "--seed", "1337")
}

tasks.register<JavaExec>("recordPhysicsReplay") {
    group = "verification"
    description = "Records a deterministic server-side physics replay JSONL. Use -Pscenario=projectile-impact, -Pseed=424242, -Pticks=8, -Pout=build/physics-replays/projectile-impact.jsonl."
    dependsOn(":server:classes")
    classpath = project(":server").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.server.physics.PhysicsReplayScenarioRecorder")
    usesService(workspaceMutationLock)
    doFirst {
        val scenario = providers.gradleProperty("scenario").orElse("projectile-impact").get()
        val seed = providers.gradleProperty("seed").orElse("424242").get()
        val ticks = providers.gradleProperty("ticks").orElse("8").get()
        val output = providers.gradleProperty("out")
            .orElse(layout.buildDirectory.file("physics-replays/$scenario.jsonl").map { it.asFile.absolutePath })
            .get()
        args("--scenario", scenario, "--seed", seed, "--ticks", ticks, "--out", output)
    }
}

tasks.register<JavaExec>("joinLocal") {
    group = "voxel"
    description = "Runs the client and auto-joins a local server on port 25565."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-join", "--connect", "127.0.0.1", "--port", "25565", "--username", "Player", "--render-distance", "8")
    configureLwjglClientJvm()
}

allprojects {
    group = "dev.voxelgame"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
    }

    tasks.withType<JavaCompile>().configureEach {
        usesService(workspaceMutationLock)
    }

    tasks.withType<Test>().configureEach {
        usesService(workspaceMutationLock)
    }

    tasks.withType<Delete>().configureEach {
        usesService(workspaceMutationLock)
    }

    tasks.withType<ProcessResources>().configureEach {
        usesService(workspaceMutationLock)
    }

    tasks.withType<AbstractArchiveTask>().configureEach {
        usesService(workspaceMutationLock)
    }

    tasks.withType<CreateStartScripts>().configureEach {
        usesService(workspaceMutationLock)
    }
}

subprojects {
    apply(plugin = "java-library")

    extensions.configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(21)
    }

    tasks.withType<Jar>().configureEach {
        outputs.upToDateWhen {
            archiveContainsClassFile(archiveFile.get().asFile)
        }
        doLast {
            val archive = archiveFile.get().asFile
            if (!archiveContainsClassFile(archive)) {
                throw GradleException("Archive ${archive.absolutePath} contains no class files; refusing stale or incomplete jar output.")
            }
        }
    }

    dependencies {
        add("testImplementation", "org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        // Isolate Gradle's binary test-result store per invocation so parallel
        // workspace runs cannot delete another run's in-progress result file.
        binaryResultsDirectory.set(layout.buildDirectory.dir("test-binary-results/$name/${testBinaryRunId.get()}"))
        systemProperty("adventura.testRunId", testBinaryRunId.get())
        doFirst {
            binaryResultsDirectory.get().asFile.mkdirs()
        }
        // Stale XML/HTML reports from interrupted runs can break later gates.
        if (name == "test") {
            dependsOn("cleanTest")
        }
    }

    tasks.register<Test>("physicsRegression") {
        group = "verification"
        description = "Runs deterministic physics regression, fuzz and replay tests."
        testClassesDirs = project.extensions.getByType<SourceSetContainer>().named("test").get().output.classesDirs
        classpath = project.extensions.getByType<SourceSetContainer>().named("test").get().runtimeClasspath
        useJUnitPlatform {
            includeTags("physicsRegression")
        }
        shouldRunAfter(tasks.named("test"))
    }
}
