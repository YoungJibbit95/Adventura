package dev.voxelgame.client.render;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

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
    private final int programId;

    private ShaderProgram(int programId) {
        this.programId = programId;
    }

    public static ShaderProgram fromResources(String vertexPath, String fragmentPath) {
        int vertexShader = compile(GL_VERTEX_SHADER, loadResource(vertexPath));
        int fragmentShader = compile(GL_FRAGMENT_SHADER, loadResource(fragmentPath));
        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, fragmentShader);
        glLinkProgram(program);
        if (glGetProgrami(program, GL_LINK_STATUS) == 0) {
            throw new IllegalStateException("Shader link failed: " + glGetProgramInfoLog(program));
        }
        glDeleteShader(vertexShader);
        glDeleteShader(fragmentShader);
        return new ShaderProgram(program);
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
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(values.length);
            buffer.put(values).flip();
            glUniform4fv(glGetUniformLocation(programId, uniformName), buffer);
        }
    }

    @Override
    public void close() {
        glDeleteProgram(programId);
    }

    private static int compile(int type, String source) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);
        if (glGetShaderi(shader, GL_COMPILE_STATUS) == 0) {
            throw new IllegalStateException("Shader compile failed: " + glGetShaderInfoLog(shader));
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
