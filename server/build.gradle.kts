plugins {
    application
}

dependencies {
    implementation(project(":common"))
    implementation("io.netty:netty-all:${property("nettyVersion")}")
}

application {
    mainClass.set("dev.voxelgame.server.GameServerMain")
}
