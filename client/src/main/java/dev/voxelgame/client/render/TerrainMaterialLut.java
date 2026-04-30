package dev.voxelgame.client.render;

import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.registry.Registry;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.Objects;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_NEAREST;
import static org.lwjgl.opengl.GL11.GL_RGBA;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_2D;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MAG_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_MIN_FILTER;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_S;
import static org.lwjgl.opengl.GL11.GL_TEXTURE_WRAP_T;
import static org.lwjgl.opengl.GL11.glBindTexture;
import static org.lwjgl.opengl.GL11.glDeleteTextures;
import static org.lwjgl.opengl.GL11.glGenTextures;
import static org.lwjgl.opengl.GL11.glTexImage2D;
import static org.lwjgl.opengl.GL11.glTexParameteri;
import static org.lwjgl.opengl.GL12.GL_CLAMP_TO_EDGE;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.GL_RGBA32F;

public final class TerrainMaterialLut implements AutoCloseable {
    public static final int TEXEL_ROWS = RenderMaterial.TEXELS_PER_MATERIAL;
    public static final int ROW_COLOR_ALPHA = 0;
    public static final int ROW_EFFECTS = 1;
    public static final int ROW_SIDE_UV = 2;
    public static final int ROW_TOP_UV = 3;
    public static final int ROW_BOTTOM_UV = 4;
    public static final int ROW_STYLE = 5;

    private final int textureId;
    private final int materialCount;
    private final int missingMaterialCount;
    private final long estimatedBytes;
    private boolean closed;

    private TerrainMaterialLut(int textureId, int materialCount, int missingMaterialCount, long estimatedBytes) {
        this.textureId = textureId;
        this.materialCount = materialCount;
        this.missingMaterialCount = missingMaterialCount;
        this.estimatedBytes = estimatedBytes;
    }

    public static TerrainMaterialLut upload(Registry<BlockType> blocks, BlockTextureAtlas atlas) {
        Objects.requireNonNull(atlas, "atlas");
        RenderMaterial.Table materialTable = RenderMaterial.tableFor(blocks, atlas);
        RenderMaterial[] materials = materialTable.materials();
        float[] pixels = pixels(materials);
        FloatBuffer buffer = BufferUtils.createFloatBuffer(pixels.length);
        buffer.put(pixels).flip();

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA32F, materials.length, TEXEL_ROWS, 0, GL_RGBA, GL_FLOAT, buffer);
        glBindTexture(GL_TEXTURE_2D, 0);

        long estimatedBytes = (long) pixels.length * Float.BYTES;
        RenderResourceTracker.registerTexture(estimatedBytes);
        return new TerrainMaterialLut(textureId, materials.length, materialTable.missingMaterialCount(), estimatedBytes);
    }

    static float[] pixels(RenderMaterial[] materials) {
        Objects.requireNonNull(materials, "materials");
        float[] pixels = new float[materials.length * TEXEL_ROWS * 4];
        for (int materialIndex = 0; materialIndex < materials.length; materialIndex++) {
            RenderMaterial material = materials[materialIndex] == null ? RenderMaterial.fallback(materialIndex) : materials[materialIndex];
            put(pixels, materials.length, materialIndex, ROW_COLOR_ALPHA,
                    material.tintR(), material.tintG(), material.tintB(), material.alpha());
            put(pixels, materials.length, materialIndex, ROW_EFFECTS,
                    material.emissive(),
                    material.animatedFluid() ? 1.0f : 0.0f,
                    material.flags(),
                    material.cutoutThreshold());
            put(pixels, materials.length, materialIndex, ROW_SIDE_UV,
                    material.sideU0(), material.sideV0(), material.sideU1(), material.sideV1());
            put(pixels, materials.length, materialIndex, ROW_TOP_UV,
                    material.topU0(), material.topV0(), material.topU1(), material.topV1());
            put(pixels, materials.length, materialIndex, ROW_BOTTOM_UV,
                    material.bottomU0(), material.bottomV0(), material.bottomU1(), material.bottomV1());
            put(pixels, materials.length, materialIndex, ROW_STYLE,
                    material.biomeTintMode().ordinal(),
                    material.fogAffectMode().ordinal(),
                    material.renderLayer().ordinal(),
                    material.roughness());
        }
        return pixels;
    }

    public int materialCount() {
        return materialCount;
    }

    public int texelCount() {
        return materialCount * TEXEL_ROWS;
    }

    public int missingMaterialCount() {
        return missingMaterialCount;
    }

    public long estimatedBytes() {
        return estimatedBytes;
    }

    public void bindAndApply(ShaderProgram shader, int unit) {
        shader.setInt("uMaterialLut", unit);
        shader.setInt("uMaterialCount", materialCount);
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        glDeleteTextures(textureId);
        RenderResourceTracker.releaseTexture(estimatedBytes);
    }

    private static void put(float[] pixels, int materialCount, int materialIndex, int row, float x, float y, float z, float w) {
        int offset = (row * materialCount + materialIndex) * 4;
        pixels[offset] = x;
        pixels[offset + 1] = y;
        pixels[offset + 2] = z;
        pixels[offset + 3] = w;
    }
}
