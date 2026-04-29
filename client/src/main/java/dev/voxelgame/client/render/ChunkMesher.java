package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.WorldView;

import java.util.ArrayList;
import java.util.List;

public final class ChunkMesher {
    public static final int FLOATS_PER_VERTEX = 9;

    private static final Face[] FACES = {
            new Face(1, 0, 0, new float[][]{{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}),
            new Face(-1, 0, 0, new float[][]{{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}}),
            new Face(0, 1, 0, new float[][]{{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}),
            new Face(0, -1, 0, new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}),
            new Face(0, 0, 1, new float[][]{{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}}),
            new Face(0, 0, -1, new float[][]{{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}})
    };

    public ChunkMesh buildTerrainMesh(WorldView world, Chunk chunk) {
        return buildTerrainMesh(world, chunk, true);
    }

    public ChunkMesh buildTerrainMesh(WorldView world, Chunk chunk, boolean ambientOcclusion) {
        return buildMesh(world, chunk, false, BlockRenderLayer.SOLID, ambientOcclusion);
    }

    public ChunkMesh buildVisibleFaceMesh(WorldView world, Chunk chunk, BlockRenderLayer layer) {
        return buildMesh(world, chunk, true, layer, true);
    }

    private ChunkMesh buildMesh(WorldView world, Chunk chunk, boolean filterLayer, BlockRenderLayer layer, boolean ambientOcclusion) {
        List<Float> vertices = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        for (int sectionIndex = 0; sectionIndex < chunk.sectionCount(); sectionIndex++) {
            ChunkSection section = chunk.sectionByIndex(sectionIndex);
            if (section.isEmpty()) {
                continue;
            }
            int sectionBaseY = section.sectionY() * ChunkSection.SIZE;
            for (int localY = 0; localY < ChunkSection.SIZE; localY++) {
                int y = sectionBaseY + localY;
                if (!chunk.dimension().containsY(y)) {
                    continue;
                }
                for (int z = baseZ; z < baseZ + ChunkPos.SIZE; z++) {
                    int localZ = ChunkPos.localCoord(z);
                    for (int x = baseX; x < baseX + ChunkPos.SIZE; x++) {
                        int localX = ChunkPos.localCoord(x);
                        BlockType block = world.blockType(section.blockId(localX, localY, localZ));
                        if (block.id() == 0 || (!filterLayer && block.renderLayer() == BlockRenderLayer.TRANSLUCENT)) {
                            continue;
                        }
                        if (filterLayer && block.renderLayer() != layer) {
                            continue;
                        }
                        for (Face face : FACES) {
                            BlockType neighbor = world.blockType(world.blockId(x + face.nx, y + face.ny, z + face.nz));
                            if (neighbor.id() == block.id() && block.renderLayer() == BlockRenderLayer.TRANSLUCENT) {
                                continue;
                            }
                            if (neighbor.opaque() && neighbor.renderLayer() == BlockRenderLayer.SOLID) {
                                continue;
                            }
                            addFace(vertices, indices, world, x, y, z, face, block.id(), light(world, x + face.nx, y + face.ny, z + face.nz), ambientOcclusion);
                        }
                    }
                }
            }
        }

        return new ChunkMesh(toFloatArray(vertices), toIntArray(indices));
    }

    private ChunkMesh buildMesh(WorldView world, Chunk chunk, boolean filterLayer) {
        return buildMesh(world, chunk, filterLayer, BlockRenderLayer.SOLID, true);
    }

    private static float light(WorldView world, int x, int y, int z) {
        if (world instanceof dev.voxelgame.common.world.InMemoryWorld memoryWorld) {
            int packed = Math.max(memoryWorld.skyLight(x, y, z), memoryWorld.blockLight(x, y, z));
            return packed / 15.0f;
        }
        return 1.0f;
    }

    private static void addFace(List<Float> vertices, List<Integer> indices, WorldView world, int x, int y, int z, Face face, short blockId, float light, boolean ambientOcclusion) {
        int baseVertex = vertices.size() / FLOATS_PER_VERTEX;
        for (float[] corner : face.corners) {
            vertices.add(x + corner[0]);
            vertices.add(y + corner[1]);
            vertices.add(z + corner[2]);
            vertices.add((float) face.nx);
            vertices.add((float) face.ny);
            vertices.add((float) face.nz);
            vertices.add((float) blockId);
            vertices.add(light);
            vertices.add(ambientOcclusion ? ambientOcclusion(world, x, y, z, face, corner) : 1.0f);
        }
        indices.add(baseVertex);
        indices.add(baseVertex + 1);
        indices.add(baseVertex + 2);
        indices.add(baseVertex);
        indices.add(baseVertex + 2);
        indices.add(baseVertex + 3);
    }

    private static float ambientOcclusion(WorldView world, int x, int y, int z, Face face, float[] corner) {
        int sx = corner[0] == 0.0f ? -1 : 1;
        int sy = corner[1] == 0.0f ? -1 : 1;
        int sz = corner[2] == 0.0f ? -1 : 1;
        int samples = 0;
        if (face.nx != 0) {
            samples += opaque(world, x + face.nx, y + sy, z) ? 1 : 0;
            samples += opaque(world, x + face.nx, y, z + sz) ? 1 : 0;
            samples += opaque(world, x + face.nx, y + sy, z + sz) ? 1 : 0;
        } else if (face.ny != 0) {
            samples += opaque(world, x + sx, y + face.ny, z) ? 1 : 0;
            samples += opaque(world, x, y + face.ny, z + sz) ? 1 : 0;
            samples += opaque(world, x + sx, y + face.ny, z + sz) ? 1 : 0;
        } else {
            samples += opaque(world, x + sx, y, z + face.nz) ? 1 : 0;
            samples += opaque(world, x, y + sy, z + face.nz) ? 1 : 0;
            samples += opaque(world, x + sx, y + sy, z + face.nz) ? 1 : 0;
        }
        return switch (samples) {
            case 0 -> 1.0f;
            case 1 -> 0.86f;
            case 2 -> 0.70f;
            default -> 0.54f;
        };
    }

    private static boolean opaque(WorldView world, int x, int y, int z) {
        return world.dimension().containsY(y) && world.blockType(world.blockId(x, y, z)).opaque();
    }

    private static float[] toFloatArray(List<Float> values) {
        float[] array = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private static int[] toIntArray(List<Integer> values) {
        int[] array = new int[values.size()];
        for (int i = 0; i < values.size(); i++) {
            array[i] = values.get(i);
        }
        return array;
    }

    private record Face(int nx, int ny, int nz, float[][] corners) {
    }
}
