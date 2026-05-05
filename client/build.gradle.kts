plugins {
    application
}

val lwjglVersion: String by project
val lwjglModules = listOf("lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-stb")
val lwjglNatives = listOf("natives-linux", "natives-windows", "natives-macos", "natives-macos-arm64")
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
    applicationDefaultJvmArgs = lwjglClientJvmArgs
}
