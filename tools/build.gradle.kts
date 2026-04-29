plugins {
    application
}

dependencies {
    implementation(project(":common"))
}

application {
    mainClass.set("dev.voxelgame.tools.AssetToolMain")
}
