package dev.voxelgame.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import static org.lwjgl.opengl.GL20.GL_COMPILE_STATUS;
import static org.lwjgl.opengl.GL20.GL_FRAGMENT_SHADER;
import static org.lwjgl.opengl.GL20.GL_LINK_STATUS;
import static org.lwjgl.opengl.GL20.GL_VERTEX_SHADER;
import static org.lwjgl.opengl.GL20.glAttachShader;
import static org.lwjgl.opengl.GL20.glCompileShader;
import static org.lwjgl.opengl.GL20.glCreateProgram;
import static org.lwjgl.opengl.GL20.glCreateShader;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glDeleteShader;
import static org.lwjgl.opengl.GL20.glGetProgramInfoLog;
import static org.lwjgl.opengl.GL20.glGetProgrami;
import static org.lwjgl.opengl.GL20.glGetShaderInfoLog;
import static org.lwjgl.opengl.GL20.glGetShaderi;
import static org.lwjgl.opengl.GL20.glGetUniformLocation;
import static org.lwjgl.opengl.GL20.glLinkProgram;
import static org.lwjgl.opengl.GL20.glShaderSource;
import static org.lwjgl.opengl.GL20.glUniformMatrix4fv;
import static org.lwjgl.opengl.GL20.glUniform1f;
import static org.lwjgl.opengl.GL20.glUniform1i;
import static org.lwjgl.opengl.GL20.glUniform3f;
import static org.lwjgl.opengl.GL20.glUniform4fv;
import static org.lwjgl.opengl.GL20.glUseProgram;

public final class ShaderProgram implements AutoCloseable {
    private final String vertexPath;
    private final String fragmentPath;
    private int programId;
    private boolean closed;

    private ShaderProgram(String vertexPath, String fragmentPath, int programId) {
        this.vertexPath = Objects.requireNonNull(vertexPath, "vertexPath");
        this.fragmentPath = Objects.requireNonNull(fragmentPath, "fragmentPath");
        this.programId = programId;
        RenderResourceTracker.registerShaderProgram();
        ShaderRegistry.register(this);
    }

    public static ShaderProgram fromResources(String vertexPath, String fragmentPath) {
        return new ShaderProgram(vertexPath, fragmentPath, linkFromResources(vertexPath, fragmentPath));
    }

    public void bind() {
        glUseProgram(programId);
    }

    public void setMatrix4(String uniformName, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(glGetUniformLocation(programId, uniformName), false, buffer);
        }
    }

    public void setFloat(String uniformName, float value) {
        glUniform1f(glGetUniformLocation(programId, uniformName), value);
    }

    public void setInt(String uniformName, int value) {
        glUniform1i(glGetUniformLocation(programId, uniformName), value);
    }

    public void setVector3(String uniformName, Vector3f value) {
        glUniform3f(glGetUniformLocation(programId, uniformName), value.x, value.y, value.z);
    }

    public void setVector4Array(String uniformName, float[] values) {
        if (values.length % 4 != 0) {
            throw new IllegalArgumentException("Uniform " + uniformName + " requires a float array length divisible by 4 but was " + values.length);
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length);
            buffer.put(values).flip();
            glUniform4fv(glGetUniformLocation(programId, uniformName), buffer);
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        ShaderRegistry.unregister(this);
        glDeleteProgram(programId);
        RenderResourceTracker.releaseShaderProgram();
    }

    void reloadFromResources() {
        if (closed) {
            throw new IllegalStateException("Shader is already closed: " + resourceLabel());
        }
        int replacementProgram = linkFromResources(vertexPath, fragmentPath);
        RenderResourceTracker.registerShaderProgram();
        int previousProgram = programId;
        programId = replacementProgram;
        glDeleteProgram(previousProgram);
        RenderResourceTracker.releaseShaderProgram();
    }

    String resourceLabel() {
        return vertexPath + " + " + fragmentPath;
    }

    private static int linkFromResources(String vertexPath, String fragmentPath) {
        int vertexShader = 0;
        int fragmentShader = 0;
        int program = 0;
        try {
            vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexPath));
            fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentPath));
            program = glCreateProgram();
            glAttachShader(program, vertexShader);
            glAttachShader(program, fragmentShader);
            glLinkProgram(program);
            if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
                String log = glGetProgramInfoLog(program);
                glDeleteProgram(program);
                program = 0;
                throw new IllegalStateException("Shader link failed: " + log);
            }
            return program;
        } finally {
            if (vertexShader != 0) {
                glDeleteShader(vertexShader);
            }
            if (fragmentShader != 0) {
                glDeleteShader(fragmentShader);
            }
            if (program != 0 && glGetProgrami(program, GL_LINK_STATUS) == 0) {
                glDeleteProgram(program);
            }
        }
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            String log = glGetShaderInfoLog(shader);
            glDeleteShader(shader);
            throw new IllegalStateException("Shader compile failed: " + log);
        }
        return shader;
    }

    private static String loadResource(String path) {
        try (InputStream input = ShaderProgram.class.getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                throw new IllegalArgumentException("Missing shader resource: " + path);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load shader: " + path, e);
        }
    }
}
