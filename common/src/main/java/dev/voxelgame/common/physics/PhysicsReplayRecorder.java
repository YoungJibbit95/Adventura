package dev.voxelgame.common.physics;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class PhysicsReplayRecorder {
    private final ArrayList<PhysicsReplayFrame> frames = new ArrayList<>();
    private long lastTick = -1L;

    public static PhysicsReplayRecorder create() {
        return new PhysicsReplayRecorder();
    }

    public static PhysicsReplayRecorder fromJsonl(String jsonl) {
        PhysicsReplayRecorder recorder = create();
        for (PhysicsReplayFrame frame : PhysicsReplayCodec.readJsonl(jsonl)) {
            recorder.record(frame);
        }
        return recorder;
    }

    public static PhysicsReplayRecorder read(Path path) throws IOException {
        return fromJsonl(Files.readString(path));
    }

    public static void write(Path path, Collection<PhysicsReplayFrame> frames) throws IOException {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, PhysicsReplayCodec.writeJsonl(frames));
    }

    public PhysicsReplayFrame recordStep(PhysicsStepContext context, Consumer<PhysicsReplayFrame.Builder> capture) {
        Objects.requireNonNull(capture, "capture");
        PhysicsReplayFrame.Builder builder = PhysicsReplayFrame.fromContext(context);
        capture.accept(builder);
        PhysicsReplayFrame frame = builder.build();
        record(frame);
        return frame;
    }

    public void record(PhysicsReplayFrame frame) {
        Objects.requireNonNull(frame, "frame");
        if (frame.tick() <= lastTick) {
            throw new IllegalArgumentException("Physics replay ticks must be strictly increasing");
        }
        frames.add(frame);
        lastTick = frame.tick();
    }

    public List<PhysicsReplayFrame> frames() {
        return List.copyOf(frames);
    }

    public String toJsonl() {
        return PhysicsReplayCodec.writeJsonl(frames);
    }

    public boolean isEmpty() {
        return frames.isEmpty();
    }
}
