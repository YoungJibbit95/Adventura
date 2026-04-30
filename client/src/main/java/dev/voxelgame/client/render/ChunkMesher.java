package dev.voxelgame.client.render;

import dev.voxelgame.common.block.BlockRenderLayer;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.world.Chunk;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkSection;
import dev.voxelgame.common.world.WorldView;

public final class ChunkMesher {
    /**
     * Chunk vertex format, GL location order:
     * 0 position xyz, 1 normal xyz, 2 material index, 3 light, 4 AO, 5 local/tiled face UV.
     */
    public static final int FLOATS_PER_VERTEX = 11;
    public static final int POSITION_OFFSET = 0;
    public static final int NORMAL_OFFSET = 3;
    public static final int MATERIAL_INDEX_OFFSET = 6;
    public static final int LIGHT_OFFSET = 7;
    public static final int AO_OFFSET = 8;
    public static final int FACE_UV_OFFSET = 9;
    public static final int VERTEX_BYTES = FLOATS_PER_VERTEX * Float.BYTES;

    private static final Face[] FACES = {
            new Face(1, 0, 0, new float[][]{{1, 0, 0}, {1, 1, 0}, {1, 1, 1}, {1, 0, 1}}),
            new Face(-1, 0, 0, new float[][]{{0, 0, 1}, {0, 1, 1}, {0, 1, 0}, {0, 0, 0}}),
            new Face(0, 1, 0, new float[][]{{0, 1, 1}, {1, 1, 1}, {1, 1, 0}, {0, 1, 0}}),
            new Face(0, -1, 0, new float[][]{{0, 0, 0}, {1, 0, 0}, {1, 0, 1}, {0, 0, 1}}),
            new Face(0, 0, 1, new float[][]{{1, 0, 1}, {1, 1, 1}, {0, 1, 1}, {0, 0, 1}}),
            new Face(0, 0, -1, new float[][]{{0, 0, 0}, {0, 1, 0}, {1, 1, 0}, {1, 0, 0}})
    };
    private static final int[] FULL_AO = {255, 255, 255, 255};

    private final FloatMeshBuffer vertices = new FloatMeshBuffer(4096);
    private final IntMeshBuffer indices = new IntMeshBuffer(2048);
    private boolean greedyMeshingEnabled = true;
    private MeshBuildStats lastBuildStats = MeshBuildStats.empty();

    public void setGreedyMeshingEnabled(boolean greedyMeshingEnabled) {
        this.greedyMeshingEnabled = greedyMeshingEnabled;
    }

    public boolean greedyMeshingEnabled() {
        return greedyMeshingEnabled;
    }

    public MeshBuildStats lastBuildStats() {
        return lastBuildStats;
    }

    public ChunkMesh buildTerrainMesh(WorldView world, Chunk chunk) {
        return buildTerrainMesh(world, chunk, true);
    }

    public ChunkMesh buildTerrainMesh(WorldView world, Chunk chunk, boolean ambientOcclusion) {
        return buildMesh(world, chunk, false, BlockRenderLayer.SOLID, ambientOcclusion);
    }

    public ChunkMesh buildVisibleFaceMesh(WorldView world, Chunk chunk, BlockRenderLayer layer) {
        return buildMesh(world, chunk, true, layer, true);
    }

    public ChunkMesh buildVisibleFaceMesh(WorldView world, Chunk chunk, BlockRenderLayer layer, boolean ambientOcclusion) {
        return buildMesh(world, chunk, true, layer, ambientOcclusion);
    }

    private synchronized ChunkMesh buildMesh(WorldView world, Chunk chunk, boolean filterLayer, BlockRenderLayer layer, boolean ambientOcclusion) {
        vertices.reset();
        indices.reset();
        boolean usedGreedyMeshing = layer == BlockRenderLayer.SOLID && greedyMeshingEnabled;
        if (usedGreedyMeshing) {
            appendGreedySolidFaces(vertices, indices, world, chunk, ambientOcclusion);
            appendSimpleFaces(vertices, indices, world, chunk, filterLayer, BlockRenderLayer.SOLID, ambientOcclusion, true);
        } else {
            appendSimpleFaces(vertices, indices, world, chunk, filterLayer, layer, ambientOcclusion, false);
        }
        float[] vertexArray = vertices.toArray();
        int[] indexArray = indices.toArray();
        ChunkMesh mesh = new ChunkMesh(vertexArray, indexArray);
        lastBuildStats = new MeshBuildStats(
                mesh.vertexCount(),
                mesh.indexCount(),
                mesh.estimatedBytes(),
                vertices.growthBytesSinceReset() + indices.growthBytesSinceReset(),
                vertices.capacityBytes() + indices.capacityBytes(),
                usedGreedyMeshing
        );
        return mesh;
    }

    private static void appendSimpleFaces(
            FloatMeshBuffer vertices,
            IntMeshBuffer indices,
            WorldView world,
            Chunk chunk,
            boolean filterLayer,
            BlockRenderLayer layer,
            boolean ambientOcclusion,
            boolean skipGreedyBlocks
    ) {
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
                        if (skipGreedyBlocks && isGreedyBlock(block)) {
                            continue;
                        }
                        if (block.renderLayer() == BlockRenderLayer.CUTOUT && !block.collidable()) {
                            addCrossSprite(vertices, indices, world, x, y, z, block.id(), light(world, x, y + 1, z), ambientOcclusion);
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
    }

    private static void appendGreedySolidFaces(FloatMeshBuffer vertices, IntMeshBuffer indices, WorldView world, Chunk chunk, boolean ambientOcclusion) {
        int baseX = chunk.pos().x() * ChunkPos.SIZE;
        int baseZ = chunk.pos().z() * ChunkPos.SIZE;
        int minY = chunk.dimension().minY();
        int height = chunk.dimension().height();
        for (Face face : FACES) {
            int fixedCount = fixedCount(face, height);
            int uCount = ChunkPos.SIZE;
            int vCount = vCount(face, height);
            GreedyCell[] mask = new GreedyCell[uCount * vCount];
            for (int fixed = 0; fixed < fixedCount; fixed++) {
                fillGreedyMask(mask, face, fixed, uCount, vCount, baseX, baseZ, minY, world, ambientOcclusion);
                emitGreedyMask(vertices, indices, mask, face, fixed, uCount, vCount, baseX, baseZ, minY, ambientOcclusion);
            }
        }
    }

    private static void fillGreedyMask(
            GreedyCell[] mask,
            Face face,
            int fixed,
            int uCount,
            int vCount,
            int baseX,
            int baseZ,
            int minY,
            WorldView world,
            boolean ambientOcclusion
    ) {
        for (int i = 0; i < mask.length; i++) {
            mask[i] = null;
        }
        for (int v = 0; v < vCount; v++) {
            for (int u = 0; u < uCount; u++) {
                int x = blockX(face, fixed, u, v, baseX);
                int y = blockY(face, fixed, v, minY);
                int z = blockZ(face, fixed, u, v, baseZ);
                BlockType block = world.blockType(world.blockId(x, y, z));
                if (!isGreedyBlock(block)) {
                    continue;
                }
                BlockType neighbor = world.blockType(world.blockId(x + face.nx, y + face.ny, z + face.nz));
                if (neighbor.opaque() && neighbor.renderLayer() == BlockRenderLayer.SOLID) {
                    continue;
                }
                int[] ao = ambientOcclusion ? ambientOcclusionCodes(world, x, y, z, face) : FULL_AO;
                mask[v * uCount + u] = new GreedyCell(
                        block.id(),
                        lightCode(light(world, x + face.nx, y + face.ny, z + face.nz)),
                        ao[0],
                        ao[1],
                        ao[2],
                        ao[3]
                );
            }
        }
    }

    private static void emitGreedyMask(
            FloatMeshBuffer vertices,
            IntMeshBuffer indices,
            GreedyCell[] mask,
            Face face,
            int fixed,
            int uCount,
            int vCount,
            int baseX,
            int baseZ,
            int minY,
            boolean ambientOcclusion
    ) {
        for (int v = 0; v < vCount; v++) {
            for (int u = 0; u < uCount; ) {
                int index = v * uCount + u;
                GreedyCell cell = mask[index];
                if (cell == null) {
                    u++;
                    continue;
                }
                int width = greedyWidth(mask, cell, u, v, uCount);
                int height = greedyHeight(mask, cell, u, v, width, uCount, vCount);
                int x = blockX(face, fixed, u, v, baseX);
                int y = blockY(face, fixed, v, minY);
                int z = blockZ(face, fixed, u, v, baseZ);
                addMergedFace(vertices, indices, x, y, z, face, cell.blockId(), cell.light(), width, height, ambientOcclusion ? cell.aoValues() : null);
                clearMask(mask, u, v, width, height, uCount);
                u += width;
            }
        }
    }

    private static int greedyWidth(GreedyCell[] mask, GreedyCell cell, int u, int v, int uCount) {
        int width = 1;
        while (u + width < uCount && cell.equals(mask[v * uCount + u + width])) {
            width++;
        }
        return width;
    }

    private static int greedyHeight(GreedyCell[] mask, GreedyCell cell, int u, int v, int width, int uCount, int vCount) {
        int height = 1;
        while (v + height < vCount) {
            for (int du = 0; du < width; du++) {
                if (!cell.equals(mask[(v + height) * uCount + u + du])) {
                    return height;
                }
            }
            height++;
        }
        return height;
    }

    private static void clearMask(GreedyCell[] mask, int u, int v, int width, int height, int uCount) {
        for (int dv = 0; dv < height; dv++) {
            for (int du = 0; du < width; du++) {
                mask[(v + dv) * uCount + u + du] = null;
            }
        }
    }

    private static int fixedCount(Face face, int worldHeight) {
        return face.ny != 0 ? worldHeight : ChunkPos.SIZE;
    }

    private static int vCount(Face face, int worldHeight) {
        return face.ny != 0 ? ChunkPos.SIZE : worldHeight;
    }

    private static int blockX(Face face, int fixed, int u, int v, int baseX) {
        if (face.nx != 0) {
            return baseX + fixed;
        }
        return baseX + u;
    }

    private static int blockY(Face face, int fixed, int v, int minY) {
        if (face.ny != 0) {
            return minY + fixed;
        }
        return minY + v;
    }

    private static int blockZ(Face face, int fixed, int u, int v, int baseZ) {
        if (face.nz != 0) {
            return baseZ + fixed;
        }
        return face.nx != 0 ? baseZ + u : baseZ + v;
    }

    private static float light(WorldView world, int x, int y, int z) {
        if (world instanceof dev.voxelgame.common.world.InMemoryWorld memoryWorld) {
            float sky = memoryWorld.skyLight(x, y, z) / 15.0f;
            float block = memoryWorld.blockLight(x, y, z) / 15.0f;
            float combined = sky * 0.65f + block * 0.85f;
            return Math.max(0.12f, Math.min(1.0f, combined));
        }
        return 1.0f;
    }

    private static int lightCode(float light) {
        return Math.max(0, Math.min(255, Math.round(light * 255.0f)));
    }

    private static float light(int lightCode) {
        return Math.max(0, Math.min(255, lightCode)) / 255.0f;
    }

    private static int[] ambientOcclusionCodes(WorldView world, int x, int y, int z, Face face) {
        int[] values = new int[4];
        for (int i = 0; i < face.corners.length; i++) {
            values[i] = Math.max(0, Math.min(255, Math.round(ambientOcclusion(world, x, y, z, face, face.corners[i]) * 255.0f)));
        }
        return values;
    }

    private static void addFace(FloatMeshBuffer vertices, IntMeshBuffer indices, WorldView world, int x, int y, int z, Face face, short blockId, float light, boolean ambientOcclusion) {
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
            float[] uv = faceUv(face, corner);
            vertices.add(uv[0]);
            vertices.add(uv[1]);
        }
        indices.add(baseVertex);
        indices.add(baseVertex + 1);
        indices.add(baseVertex + 2);
        indices.add(baseVertex);
        indices.add(baseVertex + 2);
        indices.add(baseVertex + 3);
    }

    private static void addMergedFace(FloatMeshBuffer vertices, IntMeshBuffer indices, int x, int y, int z, Face face, short blockId, float light, int width, int height, int[] aoValues) {
        int baseVertex = vertices.size() / FLOATS_PER_VERTEX;
        int uAxis = uAxis(face);
        int vAxis = vAxis(face);
        int cornerIndex = 0;
        for (float[] corner : face.corners) {
            float[] mergedCorner = corner.clone();
            if (corner[uAxis] == 1.0f) {
                mergedCorner[uAxis] = width;
            }
            if (corner[vAxis] == 1.0f) {
                mergedCorner[vAxis] = height;
            }
            vertices.add(x + mergedCorner[0]);
            vertices.add(y + mergedCorner[1]);
            vertices.add(z + mergedCorner[2]);
            vertices.add((float) face.nx);
            vertices.add((float) face.ny);
            vertices.add((float) face.nz);
            vertices.add((float) blockId);
            vertices.add(light);
            vertices.add(aoValues == null ? 1.0f : aoValues[cornerIndex] / 255.0f);
            float[] uv = faceUv(face, mergedCorner);
            vertices.add(uv[0]);
            vertices.add(uv[1]);
            cornerIndex++;
        }
        indices.add(baseVertex);
        indices.add(baseVertex + 1);
        indices.add(baseVertex + 2);
        indices.add(baseVertex);
        indices.add(baseVertex + 2);
        indices.add(baseVertex + 3);
    }


    private static void addCrossSprite(FloatMeshBuffer vertices, IntMeshBuffer indices, WorldView world, int x, int y, int z, short blockId, float light, boolean ambientOcclusion) {
        addSpriteQuad(vertices, indices, world, x, y, z, blockId, light, ambientOcclusion, new Face(0, 0, 1, new float[][]{
                {0.0f, 0.0f, 0.5f},
                {1.0f, 0.0f, 0.5f},
                {1.0f, 1.0f, 0.5f},
                {0.0f, 1.0f, 0.5f}
        }));
        addSpriteQuad(vertices, indices, world, x, y, z, blockId, light, ambientOcclusion, new Face(1, 0, 0, new float[][]{
                {0.5f, 0.0f, 1.0f},
                {0.5f, 0.0f, 0.0f},
                {0.5f, 1.0f, 0.0f},
                {0.5f, 1.0f, 1.0f}
        }));
    }

    private static void addSpriteQuad(FloatMeshBuffer vertices, IntMeshBuffer indices, WorldView world, int x, int y, int z, short blockId, float light, boolean ambientOcclusion, Face face) {
        addFace(vertices, indices, world, x, y, z, face, blockId, light, ambientOcclusion);
        int firstBase = indices.get(indices.size() - 6);
        indices.add(firstBase + 2);
        indices.add(firstBase + 1);
        indices.add(firstBase);
        indices.add(firstBase + 3);
        indices.add(firstBase + 2);
        indices.add(firstBase);
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

    private static boolean isGreedyBlock(BlockType block) {
        return block.renderLayer() == BlockRenderLayer.SOLID && block.opaque() && block.collidable();
    }

    private static float[] faceUv(Face face, float[] corner) {
        if (face.ny != 0) {
            return new float[]{corner[0], corner[2]};
        }
        if (face.nx != 0) {
            return new float[]{corner[2], corner[1]};
        }
        return new float[]{corner[0], corner[1]};
    }

    private static int uAxis(Face face) {
        return face.nx != 0 ? 2 : 0;
    }

    private static int vAxis(Face face) {
        return face.ny != 0 ? 2 : 1;
    }

    private record GreedyCell(short blockId, int lightCode, int ao0, int ao1, int ao2, int ao3) {
        float light() {
            return ChunkMesher.light(lightCode);
        }

        int[] aoValues() {
            return new int[]{ao0, ao1, ao2, ao3};
        }
    }

    private record Face(int nx, int ny, int nz, float[][] corners) {
    }

    private static final class FloatMeshBuffer {
        private float[] values;
        private int size;
        private long growthBytesSinceReset;

        FloatMeshBuffer(int initialCapacity) {
            values = new float[Math.max(1, initialCapacity)];
        }

        void reset() {
            size = 0;
            growthBytesSinceReset = 0L;
        }

        void add(float value) {
            ensureCapacity(size + 1);
            values[size++] = value;
        }

        int size() {
            return size;
        }

        float[] toArray() {
            float[] copy = new float[size];
            System.arraycopy(values, 0, copy, 0, size);
            return copy;
        }

        long growthBytesSinceReset() {
            return growthBytesSinceReset;
        }

        long capacityBytes() {
            return (long) values.length * Float.BYTES;
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) {
                return;
            }
            int old = values.length;
            int next = values.length;
            while (next < required) {
                next *= 2;
            }
            float[] grown = new float[next];
            System.arraycopy(values, 0, grown, 0, size);
            values = grown;
            growthBytesSinceReset += (long) (next - old) * Float.BYTES;
        }
    }

    private static final class IntMeshBuffer {
        private int[] values;
        private int size;
        private long growthBytesSinceReset;

        IntMeshBuffer(int initialCapacity) {
            values = new int[Math.max(1, initialCapacity)];
        }

        void reset() {
            size = 0;
            growthBytesSinceReset = 0L;
        }

        void add(int value) {
            ensureCapacity(size + 1);
            values[size++] = value;
        }

        int get(int index) {
            if (index < 0 || index >= size) {
                throw new IndexOutOfBoundsException("Index outside mesh index buffer: " + index);
            }
            return values[index];
        }

        int size() {
            return size;
        }

        int[] toArray() {
            int[] copy = new int[size];
            System.arraycopy(values, 0, copy, 0, size);
            return copy;
        }

        long growthBytesSinceReset() {
            return growthBytesSinceReset;
        }

        long capacityBytes() {
            return (long) values.length * Integer.BYTES;
        }

        private void ensureCapacity(int required) {
            if (required <= values.length) {
                return;
            }
            int old = values.length;
            int next = values.length;
            while (next < required) {
                next *= 2;
            }
            int[] grown = new int[next];
            System.arraycopy(values, 0, grown, 0, size);
            values = grown;
            growthBytesSinceReset += (long) (next - old) * Integer.BYTES;
        }
    }

    public record MeshBuildStats(
            int vertices,
            int indices,
            long outputBytes,
            long temporaryBufferGrowthBytes,
            long retainedBufferBytes,
            boolean greedyMeshing
    ) {
        public static MeshBuildStats empty() {
            return new MeshBuildStats(0, 0, 0L, 0L, 0L, false);
        }
    }
}
