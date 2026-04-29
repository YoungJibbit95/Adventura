plugins {
    application
}

val lwjglVersion: String by project
val lwjglModules = listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")
val lwjglNatives = listOf("natives-linux", "natives-windows", "natives-macos", "natives-macos-arm64")

dependencies {
    implementation(project(":common"))
    implementation("io.netty:netty-all:${property("nettyVersion")}")

    lwjglModules.forEach { module ->
        implementation("org.lwjgl:$module:$lwjglVersion")
        lwjglNatives.forEach { native ->
            runtimeOnly("org.lwjgl:$module:$lwjglVersion:$native")
        }
    }
}

application {
    mainClass.set("dev.voxelgame.client.ClientMain")
}

tasks.named<JavaExec>("run") {
    jvmArgs("-Dorg.lwjgl.system.allocator=jemalloc")
}
