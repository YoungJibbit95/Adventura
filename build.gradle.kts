plugins {
    java
}

tasks.register("buildGame") {
    group = "voxel"
    description = "Builds all game modules and runs tests."
    dependsOn(subprojects.map { "${it.path}:build" })
}

tasks.register<JavaExec>("runClient") {
    group = "voxel"
    description = "Runs the client and opens the main menu."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    jvmArgs("-Dorg.lwjgl.system.allocator=jemalloc")
}

tasks.register<JavaExec>("runSingleplayer") {
    group = "voxel"
    description = "Runs the client directly in a generated singleplayer world."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-singleplayer", "--preview-radius", "3", "--render-distance", "8")
    jvmArgs("-Dorg.lwjgl.system.allocator=jemalloc")
}

tasks.register<JavaExec>("runServer") {
    group = "voxel"
    description = "Runs the dedicated server on port 25565."
    dependsOn(":server:classes")
    classpath = project(":server").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.server.GameServerMain")
    args("--port", "25565", "--seed", "1337")
}

tasks.register<JavaExec>("joinLocal") {
    group = "voxel"
    description = "Runs the client and auto-joins a local server on port 25565."
    dependsOn(":client:classes")
    classpath = project(":client").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.client.ClientMain")
    args("--auto-join", "--connect", "127.0.0.1", "--port", "25565", "--username", "Player", "--render-distance", "8")
    jvmArgs("-Dorg.lwjgl.system.allocator=jemalloc")
}

tasks.register<JavaExec>("runLauncher") {
    group = "voxel"
    description = "Runs the Swing launcher for client/server startup."
    dependsOn(":launcher:classes")
    classpath = project(":launcher").sourceSets.main.get().runtimeClasspath
    mainClass.set("dev.voxelgame.launcher.GameLauncherMain")
}

allprojects {
    group = "dev.voxelgame"
    version = "0.1.0-SNAPSHOT"

    repositories {
        mavenCentral()
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

    dependencies {
        add("testImplementation", "org.junit.jupiter:junit-jupiter:${property("junitVersion")}")
        add("testRuntimeOnly", "org.junit.platform:junit-platform-launcher")
    }

    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
    }
}
