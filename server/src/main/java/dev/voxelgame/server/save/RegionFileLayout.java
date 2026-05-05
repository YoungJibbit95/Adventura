package dev.voxelgame.server.save;

import java.nio.file.Path;
import java.util.Objects;

public record RegionFileLayout(
        int regionSizeChunks,
        int formatVersion,
        String fileExtension
) {
    public static final int DEFAULT_REGION_SIZE_CHUNKS = 32;
    public static final int CURRENT_FORMAT_VERSION = 1;
    public static final String DEFAULT_FILE_EXTENSION = ".advregion";
    public static final RegionFileLayout DEFAULT = new RegionFileLayout(
            DEFAULT_REGION_SIZE_CHUNKS,
            CURRENT_FORMAT_VERSION,
            DEFAULT_FILE_EXTENSION
    );

    public RegionFileLayout {
        if (regionSizeChunks <= 0) {
            throw new IllegalArgumentException("regionSizeChunks must be > 0");
        }
        if (formatVersion <= 0) {
            throw new IllegalArgumentException("formatVersion must be > 0");
        }
        fileExtension = fileExtension == null || fileExtension.isBlank() ? DEFAULT_FILE_EXTENSION : fileExtension.strip();
        if (!fileExtension.startsWith(".")) {
            throw new IllegalArgumentException("fileExtension must start with '.'");
        }
    }

    public RegionPos regionForChunk(int chunkX, int chunkZ) {
        return new RegionPos(Math.floorDiv(chunkX, regionSizeChunks), Math.floorDiv(chunkZ, regionSizeChunks));
    }

    public ChunkSlot chunkSlot(int chunkX, int chunkZ) {
        int localX = Math.floorMod(chunkX, regionSizeChunks);
        int localZ = Math.floorMod(chunkZ, regionSizeChunks);
        return new ChunkSlot(localX, localZ, localZ * regionSizeChunks + localX);
    }

    public Path regionPath(Path rootDirectory, int chunkX, int chunkZ) {
        Objects.requireNonNull(rootDirectory, "rootDirectory");
        RegionPos region = regionForChunk(chunkX, chunkZ);
        return rootDirectory.resolve("regions").resolve(region.fileName(fileExtension));
    }

    public String saveKey(Path rootDirectory, int chunkX, int chunkZ) {
        return "region:" + regionPath(rootDirectory.toAbsolutePath().normalize(), chunkX, chunkZ);
    }

    public record RegionPos(int regionX, int regionZ) {
        public String key() {
            return regionX + "," + regionZ;
        }

        public String fileName(String fileExtension) {
            String extension = fileExtension == null || fileExtension.isBlank()
                    ? DEFAULT_FILE_EXTENSION
                    : fileExtension.strip();
            return "r." + regionX + "." + regionZ + extension;
        }
    }

    public record ChunkSlot(int localX, int localZ, int linearIndex) {
        public ChunkSlot {
            if (localX < 0 || localZ < 0 || linearIndex < 0) {
                throw new IllegalArgumentException("Chunk slot coordinates must be non-negative");
            }
        }
    }
}
