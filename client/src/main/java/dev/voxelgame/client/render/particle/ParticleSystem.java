package dev.voxelgame.client.render.particle;

import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.ChunkMesh;
import dev.voxelgame.client.render.ShaderProgram;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SplittableRandom;

import static org.lwjgl.opengl.GL11.GL_BLEND;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL11.GL_ONE_MINUS_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_SRC_ALPHA;
import static org.lwjgl.opengl.GL11.GL_TRIANGLES;
import static org.lwjgl.opengl.GL11.glBlendFunc;
import static org.lwjgl.opengl.GL11.glDepthMask;
import static org.lwjgl.opengl.GL11.glDisable;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glDrawArrays;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glBufferSubData;
import static org.lwjgl.opengl.GL15.glDeleteBuffers;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glUseProgram;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glDeleteVertexArrays;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

public final class ParticleSystem implements AutoCloseable {
    private static final int MAX_PARTICLES = 512;
    private static final int VERTICES_PER_PARTICLE = 6;
    private static final int FLOATS_PER_VERTEX = 7;
    private static final long PARTICLE_BUFFER_BYTES = (long) MAX_PARTICLES * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX * Float.BYTES;

    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(MAX_PARTICLES * VERTICES_PER_PARTICLE * FLOATS_PER_VERTEX);
    private final List<Particle> particles = new ArrayList<>();
    private final Map<Long, Double> nextCampfireSmokeTimes = new HashMap<>();
    private final Map<Long, Double> nextCampfireSparkTimes = new HashMap<>();
    private final Map<Long, Double> nextEntityGlowTimes = new HashMap<>();
    private final Map<Long, Double> nextProjectileTrailTimes = new HashMap<>();
    private final Map<Long, Double> nextCookingSteamTimes = new HashMap<>();
    private final Map<Long, Double> nextLeafTimes = new HashMap<>();
    private final Map<Long, Double> nextSporeTimes = new HashMap<>();
    private final SplittableRandom random = new SplittableRandom(42L);
    private double lastUpdateTime = Double.NaN;
    private double lastStatsTime = Double.NaN;
    private int spawnedSinceLastStats;
    private long evictedSinceLastStats;
    private double particleQuality = 1.0;
    private boolean closed;

    public ParticleSystem() {
        this.shader = ShaderProgram.fromResources(
                "assets/voxelgame/shaders/particle.vert",
                "assets/voxelgame/shaders/particle.frag"
        );
        this.vao = glGenVertexArrays();
        this.vbo = glGenBuffers();

        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, PARTICLE_BUFFER_BYTES, GL_DYNAMIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 4, GL_FLOAT, false, FLOATS_PER_VERTEX * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        RenderResourceTracker.registerParticleBuffers(PARTICLE_BUFFER_BYTES);
    }

    public void spawnBlockBreak(BlockType block, Raycast.Hit hit, double now) {
        Vector3f color = colorFor(block);
        Vector3f origin = new Vector3f(hit.x() + 0.5f, hit.y() + 0.55f, hit.z() + 0.5f);
        for (int i = 0; i < 14; i++) {
            spawn(origin, jitterVelocity(0.90f, 1.25f), color, 0.11f, 0.42f, now, 3.6f, 0.94f);
        }
    }

    public void spawnHarvestSparkle(Raycast.Hit hit, double now) {
        Vector3f origin = new Vector3f(hit.x() + 0.5f, hit.y() + 0.72f, hit.z() + 0.5f);
        Vector3f color = new Vector3f(0.90f, 0.78f, 0.36f);
        for (int i = 0; i < 8; i++) {
            spawn(origin, jitterVelocity(0.45f, 0.80f), color, 0.075f, 0.36f, now, 0.7f, 0.96f);
        }
    }

    public void spawnCampfireAmbient(int x, int y, int z, double now) {
        long key = campfireKey(x, y, z);
        if (now >= nextCampfireSmokeTimes.getOrDefault(key, 0.0)) {
            Vector3f origin = new Vector3f(x + 0.5f, y + 0.82f, z + 0.5f);
            Vector3f color = new Vector3f(0.48f, 0.44f, 0.38f);
            spawn(origin, new Vector3f(randomRange(-0.08f, 0.08f), randomRange(0.42f, 0.72f), randomRange(-0.08f, 0.08f)), color, 0.16f, 1.25f, now, -0.18f, 0.98f);
            nextCampfireSmokeTimes.put(key, now + randomRange(0.09f, 0.16f));
        }
        if (now >= nextCampfireSparkTimes.getOrDefault(key, 0.0)) {
            Vector3f origin = new Vector3f(x + 0.5f, y + 0.62f, z + 0.5f);
            Vector3f color = new Vector3f(1.0f, 0.60f, 0.22f);
            for (int i = 0; i < 2; i++) {
                spawn(origin, new Vector3f(randomRange(-0.18f, 0.18f), randomRange(0.65f, 1.05f), randomRange(-0.18f, 0.18f)), color, 0.045f, 0.42f, now, 1.1f, 0.96f);
            }
            nextCampfireSparkTimes.put(key, now + randomRange(0.22f, 0.40f));
        }
    }

    public void spawnCookingSteam(int x, int y, int z, double now) {
        long key = sourceKey(11, x, y, z);
        if (now < nextCookingSteamTimes.getOrDefault(key, 0.0)) {
            return;
        }
        Vector3f origin = new Vector3f(x + 0.5f, y + 0.96f, z + 0.5f);
        Vector3f color = new Vector3f(0.82f, 0.78f, 0.66f);
        spawn(origin, new Vector3f(randomRange(-0.07f, 0.07f), randomRange(0.32f, 0.56f), randomRange(-0.07f, 0.07f)), color, 0.13f, 0.90f, now, -0.10f, 0.985f);
        nextCookingSteamTimes.put(key, now + randomRange(0.13f, 0.24f));
    }

    public void spawnLeafDrift(int x, int y, int z, double now) {
        long key = sourceKey(23, x, y, z);
        if (now < nextLeafTimes.getOrDefault(key, 0.0)) {
            return;
        }
        Vector3f origin = new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f);
        Vector3f color = new Vector3f(0.44f, 0.66f, 0.30f);
        spawn(origin, new Vector3f(randomRange(-0.16f, 0.16f), randomRange(-0.02f, 0.10f), randomRange(-0.16f, 0.16f)), color, 0.055f, 1.10f, now, 0.16f, 0.992f);
        nextLeafTimes.put(key, now + randomRange(0.55f, 1.15f));
    }

    public void spawnGlowSpores(int x, int y, int z, double now) {
        long key = sourceKey(37, x, y, z);
        if (now < nextSporeTimes.getOrDefault(key, 0.0)) {
            return;
        }
        Vector3f origin = new Vector3f(x + 0.5f, y + 0.55f, z + 0.5f);
        Vector3f color = new Vector3f(0.46f, 0.92f, 0.68f);
        int count = random.nextInt(1, 3);
        for (int i = 0; i < count; i++) {
            spawn(origin, new Vector3f(randomRange(-0.07f, 0.07f), randomRange(0.02f, 0.16f), randomRange(-0.07f, 0.07f)), color, randomRange(0.045f, 0.070f), 0.85f, now, 0.0f, 0.99f);
        }
        nextSporeTimes.put(key, now + randomRange(0.18f, 0.34f));
    }

    public void spawnWaterSplash(Vector3f position, double now) {
        Vector3f origin = new Vector3f(position);
        Vector3f color = new Vector3f(0.42f, 0.66f, 0.82f);
        for (int i = 0; i < 12; i++) {
            spawn(origin, new Vector3f(randomRange(-0.34f, 0.34f), randomRange(0.35f, 0.92f), randomRange(-0.34f, 0.34f)), color, 0.070f, 0.45f, now, 2.2f, 0.95f);
        }
    }

    public void spawnLandingDust(Vector3f position, double impactSpeed, double now) {
        Vector3f origin = new Vector3f(position.x, (float) Math.floor(position.y - 1.55f) + 1.03f, position.z);
        Vector3f color = new Vector3f(0.58f, 0.52f, 0.42f);
        int count = Math.min(18, Math.max(6, (int) Math.round(impactSpeed * 0.65)));
        for (int i = 0; i < count; i++) {
            spawn(origin, new Vector3f(randomRange(-0.42f, 0.42f), randomRange(0.05f, 0.22f), randomRange(-0.42f, 0.42f)), color, randomRange(0.055f, 0.085f), 0.42f, now, 0.35f, 0.93f);
        }
    }

    public void spawnEntityGlow(EntitySnapshot snapshot, double now) {
        String typeKey = snapshot.typeKey();
        if (!"voxel:firefly_swarm".equals(typeKey) && !"voxel:mire_wisp".equals(typeKey)) {
            return;
        }
        long key = snapshot.entityId();
        if (now < nextEntityGlowTimes.getOrDefault(key, 0.0)) {
            return;
        }
        EntityBounds bounds = EntityBounds.forType(typeKey);
        Vector3f origin = new Vector3f(
                (float) snapshot.x(),
                EntityBounds.baseY(snapshot) + bounds.height() * 0.5f,
                (float) snapshot.z()
        );
        Vector3f color = "voxel:mire_wisp".equals(typeKey)
                ? new Vector3f(0.28f, 0.92f, 0.70f)
                : new Vector3f(1.0f, 0.88f, 0.30f);
        int count = "voxel:mire_wisp".equals(typeKey) ? 1 : 2;
        for (int i = 0; i < count; i++) {
            Vector3f velocity = new Vector3f(
                    randomRange(-0.08f, 0.08f),
                    randomRange(-0.03f, 0.10f),
                    randomRange(-0.08f, 0.08f)
            );
            spawn(origin, velocity, color, randomRange(0.045f, 0.075f), 0.62f, now, 0.0f, 0.985f);
        }
        nextEntityGlowTimes.put(key, now + randomRange(0.10f, 0.18f));
    }

    public void spawnProjectileTrail(EntitySnapshot snapshot, double now) {
        if (!EntitySnapshot.STATE_PROJECTILE.equals(snapshot.stateKey())) {
            return;
        }
        long key = snapshot.entityId();
        if (now < nextProjectileTrailTimes.getOrDefault(key, 0.0)) {
            return;
        }
        Vector3f origin = new Vector3f((float) snapshot.x(), (float) snapshot.y(), (float) snapshot.z());
        Vector3f color = new Vector3f(0.92f, 0.86f, 0.68f);
        Vector3f drift = new Vector3f((float) -snapshot.velocityX(), (float) -snapshot.velocityY(), (float) -snapshot.velocityZ());
        if (drift.lengthSquared() > 0.0001f) {
            drift.normalize(0.20f);
        }
        for (int i = 0; i < 2; i++) {
            spawn(origin, new Vector3f(drift).add(randomRange(-0.04f, 0.04f), randomRange(-0.02f, 0.04f), randomRange(-0.04f, 0.04f)), color, 0.045f, 0.24f, now, 0.0f, 0.95f);
        }
        nextProjectileTrailTimes.put(key, now + 0.035);
    }

    public void spawnProjectileImpact(GamePacket.ProjectileImpact impact, double now) {
        Vector3f origin = new Vector3f((float) impact.x(), (float) impact.y(), (float) impact.z());
        boolean entityHit = impact.hitType() == dev.voxelgame.common.physics.ProjectileHit.Type.ENTITY;
        Vector3f color = entityHit ? new Vector3f(0.92f, 0.36f, 0.28f) : new Vector3f(0.82f, 0.72f, 0.50f);
        int count = entityHit ? 12 : 9;
        Vector3f normal = new Vector3f(impact.blockFace().normalX(), impact.blockFace().normalY(), impact.blockFace().normalZ());
        if (normal.lengthSquared() > 0.0001f) {
            normal.normalize(0.28f);
        }
        for (int i = 0; i < count; i++) {
            Vector3f velocity = new Vector3f(normal)
                    .add(randomRange(-0.22f, 0.22f), randomRange(0.04f, 0.30f), randomRange(-0.22f, 0.22f));
            spawn(origin, velocity, color, randomRange(0.050f, 0.080f), 0.36f, now, 1.2f, 0.94f);
        }
    }

    public RenderStats render(Matrix4f projection, Matrix4f view, double now) {
        update(now);
        if (particles.isEmpty()) {
            return statsSnapshot(now, 0, 0);
        }
        int vertexCount = buildVertices(view, now);
        if (vertexCount == 0) {
            return statsSnapshot(now, 0, 0);
        }
        int usedFloats = vertexCount * FLOATS_PER_VERTEX;
        vertexBuffer.position(0);
        vertexBuffer.limit(usedFloats);

        shader.bind();
        shader.setMatrix4("uProjection", projection);
        shader.setMatrix4("uView", view);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDepthMask(false);
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0L, vertexBuffer);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
        glDepthMask(true);
        glDisable(GL_BLEND);
        glUseProgram(0);
        return statsSnapshot(now, 1, vertexCount / 3);
    }

    public int liveCount() {
        return particles.size();
    }

    public void setQuality(double quality) {
        if (!Double.isFinite(quality)) {
            return;
        }
        particleQuality = Math.max(0.25, Math.min(1.0, quality));
    }

    public double quality() {
        return particleQuality;
    }

    public List<ChunkMesh.Bounds> particleBounds() {
        List<ChunkMesh.Bounds> bounds = new ArrayList<>(particles.size());
        for (Particle particle : particles) {
            float half = Math.max(0.025f, particle.size);
            bounds.add(new ChunkMesh.Bounds(
                    particle.position.x - half,
                    particle.position.y - half,
                    particle.position.z - half,
                    particle.position.x + half,
                    particle.position.y + half,
                    particle.position.z + half
            ));
        }
        return bounds;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        glDeleteBuffers(vbo);
        glDeleteVertexArrays(vao);
        RenderResourceTracker.releaseParticleBuffers(PARTICLE_BUFFER_BYTES);
        shader.close();
    }

    private void spawn(Vector3f origin, Vector3f velocity, Vector3f color, float size, float lifetime, double now, float gravity, float damping) {
        int limit = particleLimit();
        while (particles.size() >= limit) {
            particles.remove(0);
            evictedSinceLastStats++;
        }
        Vector3f position = new Vector3f(origin)
                .add(randomRange(-0.20f, 0.20f), randomRange(-0.12f, 0.16f), randomRange(-0.20f, 0.20f));
        particles.add(new Particle(position, velocity, new Vector3f(color), size, gravity, damping, now, now + lifetime));
        spawnedSinceLastStats++;
    }

    private RenderStats statsSnapshot(double now, int drawCalls, int triangles) {
        double elapsedSeconds = Double.isNaN(lastStatsTime) ? 0.0 : Math.max(0.0, now - lastStatsTime);
        double spawnRate = elapsedSeconds > 0.0 ? spawnedSinceLastStats / elapsedSeconds : 0.0;
        long evictedParticles = evictedSinceLastStats;
        spawnedSinceLastStats = 0;
        evictedSinceLastStats = 0L;
        lastStatsTime = now;
        return new RenderStats(
                particles.size(),
                spawnRate,
                particles.size() / (double) particleLimit(),
                evictedParticles,
                drawCalls,
                triangles
        );
    }

    private void update(double now) {
        if (Double.isNaN(lastUpdateTime)) {
            lastUpdateTime = now;
        }
        float dt = (float) Math.min(0.05, Math.max(0.0, now - lastUpdateTime));
        lastUpdateTime = now;
        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle particle = iterator.next();
            if (now >= particle.endTime) {
                iterator.remove();
                continue;
            }
            particle.velocity.y -= particle.gravity * dt;
            particle.position.fma(dt, particle.velocity);
            particle.velocity.mul(particle.damping);
        }
        int limit = particleLimit();
        while (particles.size() > limit) {
            particles.remove(0);
            evictedSinceLastStats++;
        }
        pruneSourceTimers(now);
    }

    private int particleLimit() {
        return Math.max(96, (int) Math.round(MAX_PARTICLES * particleQuality));
    }

    private int buildVertices(Matrix4f view, double now) {
        vertexBuffer.clear();
        float rightX = view.m00();
        float rightY = view.m10();
        float rightZ = view.m20();
        float rightLength = (float) Math.sqrt(rightX * rightX + rightY * rightY + rightZ * rightZ);
        if (rightLength > 0.000001f) {
            rightX /= rightLength;
            rightY /= rightLength;
            rightZ /= rightLength;
        }
        float upX = view.m01();
        float upY = view.m11();
        float upZ = view.m21();
        float upLength = (float) Math.sqrt(upX * upX + upY * upY + upZ * upZ);
        if (upLength > 0.000001f) {
            upX /= upLength;
            upY /= upLength;
            upZ /= upLength;
        }
        int offset = 0;
        for (Particle particle : particles) {
            float ageRatio = (float) ((now - particle.startTime) / (particle.endTime - particle.startTime));
            float alpha = Math.max(0.0f, 1.0f - ageRatio);
            float size = particle.size * (0.55f + ageRatio * 0.75f);
            float rx = rightX * size;
            float ry = rightY * size;
            float rz = rightZ * size;
            float ux = upX * size;
            float uy = upY * size;
            float uz = upZ * size;
            float px = particle.position.x;
            float py = particle.position.y;
            float pz = particle.position.z;
            offset = writeVertex(vertexBuffer, offset, px - rx - ux, py - ry - uy, pz - rz - uz, particle.color, alpha);
            offset = writeVertex(vertexBuffer, offset, px + rx - ux, py + ry - uy, pz + rz - uz, particle.color, alpha);
            offset = writeVertex(vertexBuffer, offset, px + rx + ux, py + ry + uy, pz + rz + uz, particle.color, alpha);
            offset = writeVertex(vertexBuffer, offset, px - rx - ux, py - ry - uy, pz - rz - uz, particle.color, alpha);
            offset = writeVertex(vertexBuffer, offset, px + rx + ux, py + ry + uy, pz + rz + uz, particle.color, alpha);
            offset = writeVertex(vertexBuffer, offset, px - rx + ux, py - ry + uy, pz - rz + uz, particle.color, alpha);
        }
        return offset / FLOATS_PER_VERTEX;
    }

    private static int writeVertex(FloatBuffer vertices, int offset, float x, float y, float z, Vector3f color, float alpha) {
        vertices.put(offset++, x);
        vertices.put(offset++, y);
        vertices.put(offset++, z);
        vertices.put(offset++, color.x);
        vertices.put(offset++, color.y);
        vertices.put(offset++, color.z);
        vertices.put(offset++, alpha);
        return offset;
    }

    private Vector3f jitterVelocity(float horizontal, float vertical) {
        return new Vector3f(
                randomRange(-horizontal, horizontal),
                randomRange(vertical * 0.35f, vertical),
                randomRange(-horizontal, horizontal)
        );
    }

    private float randomRange(float min, float max) {
        return (float) random.nextDouble(min, max);
    }

    private static Vector3f colorFor(BlockType block) {
        String key = block.key();
        if (key.contains("grass") || key.contains("leaf") || key.contains("moss")) {
            return new Vector3f(0.34f, 0.58f, 0.30f);
        }
        if (key.contains("flower") || key.contains("berry")) {
            return new Vector3f(0.82f, 0.44f, 0.46f);
        }
        if (key.contains("sand") || key.contains("clay")) {
            return new Vector3f(0.72f, 0.56f, 0.36f);
        }
        if (key.contains("log") || key.contains("wood")) {
            return new Vector3f(0.50f, 0.32f, 0.20f);
        }
        if (key.contains("water")) {
            return new Vector3f(0.36f, 0.58f, 0.72f);
        }
        return new Vector3f(0.54f, 0.50f, 0.42f);
    }

    private static long campfireKey(int x, int y, int z) {
        return sourceKey(5, x, y, z);
    }

    private static long sourceKey(int salt, int x, int y, int z) {
        long key = 1469598103934665603L;
        key = (key ^ salt) * 1099511628211L;
        key = (key ^ x) * 1099511628211L;
        key = (key ^ y) * 1099511628211L;
        return (key ^ z) * 1099511628211L;
    }

    private void pruneSourceTimers(double now) {
        pruneSourceTimers(nextCampfireSmokeTimes, now);
        pruneSourceTimers(nextCampfireSparkTimes, now);
        pruneSourceTimers(nextEntityGlowTimes, now);
        pruneSourceTimers(nextProjectileTrailTimes, now);
        pruneSourceTimers(nextCookingSteamTimes, now);
        pruneSourceTimers(nextLeafTimes, now);
        pruneSourceTimers(nextSporeTimes, now);
    }

    private static void pruneSourceTimers(Map<Long, Double> timers, double now) {
        if (timers.size() <= 256) {
            return;
        }
        Iterator<Map.Entry<Long, Double>> iterator = timers.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() < now - 5.0) {
                iterator.remove();
            }
        }
    }

    public record RenderStats(int liveParticles, double spawnRate, double budgetUsage, long evictedParticles, int drawCalls, int triangles) {
        public static RenderStats empty() {
            return new RenderStats(0, 0.0, 0.0, 0L, 0, 0);
        }
    }

    private static final class Particle {
        private final Vector3f position;
        private final Vector3f velocity;
        private final Vector3f color;
        private final float size;
        private final float gravity;
        private final float damping;
        private final double startTime;
        private final double endTime;

        private Particle(Vector3f position, Vector3f velocity, Vector3f color, float size, float gravity, float damping, double startTime, double endTime) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.size = size;
            this.gravity = gravity;
            this.damping = damping;
            this.startTime = startTime;
            this.endTime = endTime;
        }
    }
}
