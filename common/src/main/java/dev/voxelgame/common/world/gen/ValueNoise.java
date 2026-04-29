package dev.voxelgame.common.world.gen;

public final class ValueNoise {
    private ValueNoise() {
    }

    public static double fbm(long seed, double x, double z, int octaves, double startFrequency, double persistence) {
        double value = 0.0;
        double amplitude = 1.0;
        double max = 0.0;
        double frequency = startFrequency;
        for (int i = 0; i < octaves; i++) {
            value += smooth(seed + i * 10_007L, x * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return value / max;
    }

    public static double smooth(long seed, double x, double z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        double tx = fade(x - x0);
        double tz = fade(z - z0);

        double a = lerp(hashUnit(seed, x0, z0), hashUnit(seed, x1, z0), tx);
        double b = lerp(hashUnit(seed, x0, z1), hashUnit(seed, x1, z1), tx);
        return lerp(a, b, tz);
    }

    public static double hashUnit(long seed, int x, int z) {
        long h = seed;
        h ^= x * 0x9E3779B97F4A7C15L;
        h = Long.rotateLeft(h, 27);
        h ^= z * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 33;
        h *= 0xff51afd7ed558ccdL;
        h ^= h >>> 33;
        h *= 0xc4ceb9fe1a85ec53L;
        h ^= h >>> 33;
        return ((h >>> 11) * 0x1.0p-53) * 2.0 - 1.0;
    }

    public static long columnSeed(long seed, int x, int z) {
        long h = seed ^ 0xD1B54A32D192ED03L;
        h ^= x * 0xABC98388FB8FAC03L;
        h ^= z * 0x8CB92BA72F3D8DD7L;
        h ^= h >>> 30;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 27;
        h *= 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    private static int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static double fade(double t) {
        return t * t * t * (t * (t * 6.0 - 15.0) + 10.0);
    }

    private static double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}
