plugins {
    application
}

dependencies {
    implementation(project(":client"))
    implementation(project(":server"))
}

application {
    mainClass.set("dev.voxelgame.launcher.GameLauncherMain")
}
