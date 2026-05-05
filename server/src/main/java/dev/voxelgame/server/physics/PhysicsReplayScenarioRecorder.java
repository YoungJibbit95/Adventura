package dev.voxelgame.server.physics;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.physics.PhysicsReplayFrame;
import dev.voxelgame.common.physics.PhysicsReplayRecorder;
import dev.voxelgame.common.physics.PhysicsStepContext;
import dev.voxelgame.common.physics.PlayerInput;
import dev.voxelgame.common.physics.PlayerState;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.physics.ProjectileHit;
import dev.voxelgame.common.physics.ProjectileState;
import dev.voxelgame.common.world.gen.OverworldGenerator;
import dev.voxelgame.server.entity.ServerEntityTracker;
import dev.voxelgame.server.world.ServerWorld;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

public final class PhysicsReplayScenarioRecorder {
    public static final String PROJECTILE_IMPACT_SCENARIO = "projectile-impact";
    public static final long DEFAULT_SEED = 424_242L;
    public static final int DEFAULT_TICKS = 8;
    public static final double SERVER_TICK_SECONDS = 0.05;

    private PhysicsReplayScenarioRecorder() {
    }

    public static void main(String[] args) throws IOException {
        Options options = Options.fromArgs(args);
        if (options.help()) {
            System.out.println(Options.usage());
            return;
        }
        Result result = recordToFile(options);
        System.out.println("Recorded " + result.frames().size()
                + " physics replay frames for " + result.scenario()
                + " -> " + options.outputPath().toAbsolutePath());
    }

    public static Result recordToFile(Options options) throws IOException {
        Options safeOptions = Objects.requireNonNull(options, "options");
        Result result = record(safeOptions);
        PhysicsReplayRecorder.write(safeOptions.outputPath(), result.frames());
        return result;
    }

    public static Result record(Options options) {
        Options safeOptions = Objects.requireNonNull(options, "options");
        return switch (safeOptions.scenario()) {
            case PROJECTILE_IMPACT_SCENARIO -> recordProjectileImpact(safeOptions);
            default -> throw new IllegalArgumentException("Unknown physics replay scenario: " + safeOptions.scenario());
        };
    }

    private static Result recordProjectileImpact(Options options) {
        ServerWorld world = new ServerWorld(options.seed());
        ServerEntityTracker tracker = new ServerEntityTracker();
        PhysicsReplayRecorder recorder = PhysicsReplayRecorder.create();
        OverworldGenerator.SpawnPoint spawn = world.spawnPoint();
        UUID playerId = UUID.nameUUIDFromBytes(("physics-replay:" + options.seed()).getBytes(StandardCharsets.UTF_8));

        double eyeX = spawn.eyeX();
        double eyeY = spawn.eyeY();
        double eyeZ = spawn.eyeZ();
        int targetX = spawn.blockX() + 5;
        int targetY = floor(eyeY);
        int targetZ = spawn.blockZ();
        prepareProjectileLane(world, spawn.blockX(), targetX, targetY, targetZ);
        world.setBlock(targetX, targetY, targetZ, Blocks.GARDEN_FENCE);

        tracker.updatePlayer(playerId, eyeX, eyeY, eyeZ, 0.0f, 0.0f);
        PlayerWaterState waterState = world.playerWaterState(eyeX, eyeY, eyeZ, dev.voxelgame.common.physics.PlayerBounds.DEFAULT);
        PlayerState playerState = new PlayerState(eyeX, eyeY, eyeZ, 0.0f, 0.0f, 0.0f, true, waterState.headUnderwater(), 0.0f);
        tracker.spawnArrowProjectile(playerId, eyeX, eyeY, eyeZ, 1.0, 0.0, 0.0);

        List<BlockSampleSpec> samples = List.of(
                new BlockSampleSpec(targetX, targetY, targetZ),
                new BlockSampleSpec(targetX - 1, targetY, targetZ),
                new BlockSampleSpec(spawn.blockX(), targetY, targetZ)
        );
        recordFrame(
                recorder,
                world,
                tracker,
                0L,
                playerState,
                waterState,
                samples,
                List.of()
        );

        List<ProjectileHit> terminalHits = new ArrayList<>();
        for (int tick = 1; tick <= options.ticks(); tick++) {
            List<ProjectileHit> hits = tracker.tickProjectiles(
                    SERVER_TICK_SECONDS,
                    world::projectileImpact,
                    world::fluidSample,
                    tick * SERVER_TICK_SECONDS
            );
            List<ProjectileHit> terminal = hits.stream()
                    .filter(ProjectileHit::terminal)
                    .toList();
            terminalHits.addAll(terminal);
            recordFrame(
                    recorder,
                    world,
                    tracker,
                    tick,
                    playerState,
                    waterState,
                    samples,
                    terminal
            );
            if (!terminal.isEmpty()) {
                break;
            }
        }
        return new Result(options.scenario(), options.seed(), options.ticks(), terminalHits, recorder.frames());
    }

    private static void recordFrame(
            PhysicsReplayRecorder recorder,
            ServerWorld world,
            ServerEntityTracker tracker,
            long tick,
            PlayerState playerState,
            PlayerWaterState waterState,
            List<BlockSampleSpec> samples,
            List<ProjectileHit> terminalHits
    ) {
        PhysicsStepContext context = PhysicsStepContext.projectile(SERVER_TICK_SECONDS, tick, world.dimension(), "server-replay");
        recorder.recordStep(context, builder -> {
            builder.input(tick, PlayerInput.idle(), waterState);
            builder.playerState(playerState);
            tracker.snapshots().stream()
                    .filter(snapshot -> !EntitySnapshot.STATE_PROJECTILE.equals(snapshot.stateKey()))
                    .forEach(builder::entity);
            for (ProjectileState projectile : tracker.projectileStates()) {
                builder.projectile(projectile);
            }
            for (BlockSampleSpec sample : samples) {
                builder.blockSample(sample.x(), sample.y(), sample.z(), blockId(world, sample));
            }
            for (ProjectileHit hit : terminalHits) {
                builder.projectileHit(hit);
            }
            builder.stat("entitySnapshots", tracker.snapshots().stream()
                    .filter(snapshot -> !EntitySnapshot.STATE_PROJECTILE.equals(snapshot.stateKey()))
                    .count());
            builder.stat("projectileCount", tracker.projectileCount());
            builder.stat("terminalHits", terminalHits.size());
            builder.stat("worldSampleHash", worldSampleHash(world, samples));
        });
    }

    private static void prepareProjectileLane(ServerWorld world, int startX, int targetX, int y, int z) {
        int minX = Math.min(startX, targetX);
        int maxX = Math.max(startX, targetX);
        for (int x = minX; x < maxX; x++) {
            world.setBlock(x, y - 1, z, Blocks.AIR);
            world.setBlock(x, y, z, Blocks.AIR);
            world.setBlock(x, y + 1, z, Blocks.AIR);
        }
    }

    private static long worldSampleHash(ServerWorld world, List<BlockSampleSpec> samples) {
        long hash = 1_469_598_103_934_665_603L;
        for (BlockSampleSpec sample : samples) {
            hash ^= sample.x();
            hash *= 1_099_511_628_211L;
            hash ^= sample.y();
            hash *= 1_099_511_628_211L;
            hash ^= sample.z();
            hash *= 1_099_511_628_211L;
            hash ^= blockId(world, sample);
            hash *= 1_099_511_628_211L;
        }
        return hash & Long.MAX_VALUE;
    }

    private static short blockId(ServerWorld world, BlockSampleSpec sample) {
        return world.blockAt(sample.x(), sample.y(), sample.z())
                .map(BlockType::id)
                .orElse(Blocks.AIR);
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private record BlockSampleSpec(int x, int y, int z) {
    }

    public record Result(
            String scenario,
            long seed,
            int requestedTicks,
            List<ProjectileHit> terminalHits,
            List<PhysicsReplayFrame> frames
    ) {
        public Result {
            scenario = requireScenario(scenario);
            terminalHits = List.copyOf(terminalHits == null ? List.of() : terminalHits);
            frames = List.copyOf(frames == null ? List.of() : frames);
        }
    }

    public record Options(String scenario, long seed, int ticks, Path outputPath, boolean help) {
        public Options {
            scenario = requireScenario(scenario);
            if (ticks <= 0) {
                throw new IllegalArgumentException("Physics replay ticks must be positive");
            }
            outputPath = outputPath == null ? defaultOutputPath(scenario) : outputPath;
        }

        public static Options defaults() {
            return new Options(PROJECTILE_IMPACT_SCENARIO, DEFAULT_SEED, DEFAULT_TICKS, null, false);
        }

        public static Options fromArgs(String[] args) {
            String scenario = PROJECTILE_IMPACT_SCENARIO;
            long seed = DEFAULT_SEED;
            int ticks = DEFAULT_TICKS;
            Path outputPath = null;
            boolean help = false;
            for (int index = 0; index < args.length; index++) {
                String arg = args[index];
                switch (arg) {
                    case "--help", "-h" -> help = true;
                    case "--scenario" -> scenario = requireValue(args, ++index, arg);
                    case "--seed" -> seed = Long.parseLong(requireValue(args, ++index, arg));
                    case "--ticks" -> ticks = Integer.parseInt(requireValue(args, ++index, arg));
                    case "--out" -> outputPath = Path.of(requireValue(args, ++index, arg));
                    default -> throw new IllegalArgumentException("Unknown physics replay recorder argument: " + arg);
                }
            }
            return new Options(scenario, seed, ticks, outputPath, help);
        }

        public static String usage() {
            return "Usage: ./gradlew recordPhysicsReplay -Pscenario=projectile-impact [-Pseed=424242] [-Pticks=8] [-Pout=build/physics-replays/projectile-impact.jsonl]";
        }

        private static String requireValue(String[] args, int index, String option) {
            if (index >= args.length || args[index].isBlank()) {
                throw new IllegalArgumentException(option + " requires a value");
            }
            return args[index];
        }
    }

    private static Path defaultOutputPath(String scenario) {
        return Path.of("build", "physics-replays", scenario + ".jsonl");
    }

    private static String requireScenario(String scenario) {
        if (scenario == null || scenario.isBlank()) {
            throw new IllegalArgumentException("Physics replay scenario must not be blank");
        }
        return scenario.toLowerCase(Locale.ROOT);
    }
}
