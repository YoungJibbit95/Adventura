package dev.voxelgame.client.render;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ShaderRegistry {
    private static final Set<ShaderProgram> PROGRAMS = new LinkedHashSet<>();

    private ShaderRegistry() {
    }

    static synchronized void register(ShaderProgram shader) {
        PROGRAMS.add(shader);
    }

    static synchronized void unregister(ShaderProgram shader) {
        PROGRAMS.remove(shader);
    }

    public static ReloadReport reloadAll() {
        List<ShaderProgram> shaders;
        synchronized (ShaderRegistry.class) {
            shaders = new ArrayList<>(PROGRAMS);
        }
        long startNanos = System.nanoTime();
        int reloaded = 0;
        int failed = 0;
        String lastError = "";
        for (ShaderProgram shader : shaders) {
            try {
                shader.reloadFromResources();
                reloaded++;
            } catch (RuntimeException e) {
                failed++;
                lastError = shader.resourceLabel() + ": " + e.getMessage();
            }
        }
        double elapsedMilliseconds = (System.nanoTime() - startNanos) / 1_000_000.0;
        RenderResourceTracker.recordShaderReload(elapsedMilliseconds, failed);
        return new ReloadReport(shaders.size(), reloaded, failed, elapsedMilliseconds, lastError);
    }

    public static synchronized int registeredShaderCount() {
        return PROGRAMS.size();
    }

    static synchronized void resetForTests() {
        PROGRAMS.clear();
    }

    public record ReloadReport(
            int attemptedPrograms,
            int reloadedPrograms,
            int failedPrograms,
            double elapsedMilliseconds,
            String lastError
    ) {
        public ReloadReport {
            attemptedPrograms = Math.max(0, attemptedPrograms);
            reloadedPrograms = Math.max(0, reloadedPrograms);
            failedPrograms = Math.max(0, failedPrograms);
            if (!Double.isFinite(elapsedMilliseconds) || elapsedMilliseconds < 0.0) {
                elapsedMilliseconds = 0.0;
            }
            lastError = lastError == null ? "" : lastError;
        }

        public boolean successful() {
            return failedPrograms == 0;
        }
    }
}
