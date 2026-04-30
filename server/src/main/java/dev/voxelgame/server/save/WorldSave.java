package dev.voxelgame.server.save;

import dev.voxelgame.server.world.ServerWorld;

import java.util.List;
import java.util.Objects;

public record WorldSave(
        SaveMetadata metadata,
        List<ServerWorld.BlockChange> blockChanges,
        ServerWorld.BlockEntitySnapshot blockEntities
) {
    public WorldSave {
        metadata = Objects.requireNonNull(metadata, "metadata");
        blockChanges = blockChanges == null ? List.of() : List.copyOf(blockChanges);
        blockEntities = blockEntities == null
                ? new ServerWorld.BlockEntitySnapshot(List.of(), List.of(), List.of())
                : blockEntities;
    }
}
