package dev.voxelgame.client;

import dev.voxelgame.client.audio.AudioCue;
import dev.voxelgame.client.audio.AudioCueRules;
import dev.voxelgame.client.audio.GameAudio;
import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.net.GameClientConnection;
import dev.voxelgame.client.render.BlockRenderProperties;
import dev.voxelgame.client.render.ChunkBorderRenderer;
import dev.voxelgame.client.render.RenderSettings;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.ui.BitmapFont;
import dev.voxelgame.client.ui.GameSprites;
import dev.voxelgame.client.ui.UiButton;
import dev.voxelgame.client.ui.UiColor;
import dev.voxelgame.client.ui.UiRenderer;
import dev.voxelgame.client.ui.UiSpriteRenderer;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.CraftingCategory;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.ChunkPos;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;

import static org.lwjgl.glfw.GLFW.GLFW_CURSOR;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_DISABLED;
import static org.lwjgl.glfw.GLFW.GLFW_CURSOR_NORMAL;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MAJOR;
import static org.lwjgl.glfw.GLFW.GLFW_CONTEXT_VERSION_MINOR;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_BACKSPACE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_E;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F1;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F3;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F4;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_O;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_SHIFT;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_SLASH;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT;
import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_RIGHT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_CORE_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_FORWARD_COMPAT;
import static org.lwjgl.glfw.GLFW.GLFW_OPENGL_PROFILE;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetCharCallback;
import static org.lwjgl.glfw.GLFW.glfwSetInputMode;
import static org.lwjgl.glfw.GLFW.glfwSetScrollCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowTitle;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_TEST;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glEnable;
import static org.lwjgl.opengl.GL11.glViewport;

public final class GameClient {
    private static final double LOCAL_DAY_LENGTH_SECONDS = 900.0;
    private static final int DAY_START_MINUTES = 6 * 60;

    private final ConnectionOptions connectionOptions;
    private final GameSettings settings;
    private final Camera camera = new Camera();
    private final Hotbar hotbar = new Hotbar();
    private final ChatLog chatLog = new ChatLog();
    private final FeedbackLog feedbackLog = new FeedbackLog();
    private final EnumSet<CraftingStationType> discoveredRecipeStations = EnumSet.of(CraftingStationType.INVENTORY);
    private final PlayerStats playerStats = new PlayerStats();
    private final BlockBreakAnimation blockBreakAnimation = new BlockBreakAnimation();
    private final GameAudio audio = new GameAudio();
    private final Registry<BiomeType> biomes = Biomes.createDefaultRegistry();
    private final StringBuilder chatDraft = new StringBuilder();
    private final StringBuilder craftingSearch = new StringBuilder();
    private final Set<String> announcedRecipeUnlocks = new HashSet<>();
    private GameMode gameMode = GameMode.SURVIVAL;
    private GameState gameState = GameState.MAIN_MENU;
    private ClientWorld world;
    private WorldRenderer worldRenderer;
    private ChunkBorderRenderer chunkBorderRenderer;
    private EntityRenderer entityRenderer;
    private ParticleSystem particleSystem;
    private UiRenderer uiRenderer;
    private UiSpriteRenderer backgroundSpriteRenderer;
    private UiSpriteRenderer spriteRenderer;
    private GameSprites sprites;
    private GameClientConnection connection;
    private long window;
    private int framebufferWidth = 1280;
    private int framebufferHeight = 720;
    private boolean previousLeftMouse;
    private boolean previousRightMouse;
    private boolean previousEscape;
    private boolean previousChat;
    private boolean previousSlash;
    private boolean previousEnter;
    private boolean previousBackspace;
    private boolean previousF3;
    private boolean previousCrafting;
    private boolean previousHudToggle;
    private boolean previousModeCycle;
    private boolean previousSettingsKey;
    private boolean previousSpawnKey;
    private boolean onlineMode;
    private double nextMoveSendTime;
    private double nextFpsSampleTime;
    private int framesThisSecond;
    private int lastFps;
    private WorldRenderer.RenderStats lastRenderStats = new WorldRenderer.RenderStats(0, 0);
    private String statusMessage = "Ready";
    private GameState settingsReturnState = GameState.MAIN_MENU;
    private double nextBlockActionTime;
    private double pendingScrollY;
    private double frameTimeSeconds;
    private double worldStartTimeSeconds;
    private String pickupAnimationItemKey = "";
    private double pickupAnimationUntil;
    private double lastFrameMilliseconds;
    private double lastWorldRenderMilliseconds;
    private int lastMeshBuilds;
    private int lastRenderedEntities;
    private EntityRenderer.RenderStats lastEntityRenderStats = EntityRenderer.RenderStats.empty();
    private ParticleSystem.RenderStats lastParticleRenderStats = ParticleSystem.RenderStats.empty();
    private int lastRenderedEntityHitboxes;
    private int lastChunkBorderDebugChunks;
    private CraftingCategory craftingCategoryFilter;
    private boolean craftableRecipesOnly;
    private boolean craftingSearchFocused;
    private boolean inventoryTrashMode;
    private int draggedInventorySlot = -1;
    private int storageTransactionId;
    private boolean previousUnderwater;
    private boolean headUnderwaterNow;
    private double nextAmbientParticleSourceScanTime;
    private double nextStepAudioTime;
    private double nextAmbientAudioTime;
    private double nextToolHintTime;
    private double nextComfortScanTime;
    private double nextRecipeUnlockScanTime;
    private int lastComfortFeedbackValue = -1;
    private List<ClientWorld.BlockPos> ambientLeafParticleSources = List.of();
    private List<ClientWorld.BlockPos> ambientSporeParticleSources = List.of();

    public GameClient(ConnectionOptions connectionOptions) {
        this.connectionOptions = connectionOptions;
        this.settings = GameSettings.fromOptions(connectionOptions);
    }

    public void run() {
        init();
        loop();
        shutdown();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(1280, 720, "Adventura", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        glfwShowWindow(window);
        GL.createCapabilities();
        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            framebufferWidth = Math.max(1, width);
            framebufferHeight = Math.max(1, height);
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        });
        glfwSetCharCallback(window, (handle, codepoint) -> appendChatCharacter(codepoint));
        glfwSetScrollCallback(window, (handle, xoffset, yoffset) -> {
            if (gameState == GameState.PLAYING) {
                pendingScrollY += yoffset;
            }
        });
        worldRenderer = new WorldRenderer();
        chunkBorderRenderer = new ChunkBorderRenderer();
        entityRenderer = new EntityRenderer();
        particleSystem = new ParticleSystem();
        uiRenderer = new UiRenderer();
        backgroundSpriteRenderer = new UiSpriteRenderer();
        spriteRenderer = new UiSpriteRenderer();
        sprites = GameSprites.loadDefault();
        setCursorForState();
        updateWindowTitle();
        if (connectionOptions.autoJoin()) {
            startMultiplayer();
        } else if (connectionOptions.autoSingleplayer()) {
            startSingleplayer();
        }
    }

    private void loop() {
        double lastTime = System.nanoTime() / 1_000_000_000.0;
        while (!glfwWindowShouldClose(window)) {
            double now = System.nanoTime() / 1_000_000_000.0;
            frameTimeSeconds = now;
            double rawDeltaSeconds = now - lastTime;
            float deltaSeconds = (float) Math.min(0.05, rawDeltaSeconds);
            lastTime = now;
            lastFrameMilliseconds = rawDeltaSeconds * 1000.0;
            updateFps(now);

            boolean leftMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
            boolean rightMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;
            boolean leftClicked = leftMouse && !previousLeftMouse;
            boolean leftReleased = !leftMouse && previousLeftMouse;
            boolean rightClicked = rightMouse && !previousRightMouse;
            MousePosition mouse = mousePosition();

            handleEscape();
            handleGlobalKeys();

            if (gameState == GameState.PLAYING) {
                boolean moving = camera.hasMovementInput(window);
                boolean sprinting = camera.wantsSprint(window) && moving && playerStats.canSprint();
                camera.update(window, deltaSeconds, settings.mouseSensitivity(), world, gameMode, playerStats.canSprint());
                float fallImpact = camera.consumeFallImpactSpeed();
                if (gameMode == GameMode.SURVIVAL && fallImpact > 13.0f) {
                    playerStats.hurt(Math.round((fallImpact - 12.0f) * 0.55f));
                }
                PlayerWaterState water = world == null ? new PlayerWaterState(false, false, false) : world.playerWaterState(camera.position());
                headUnderwaterNow = water.headUnderwater();
                refreshLocalComfort(now);
                playerStats.tick(deltaSeconds, gameMode, headUnderwaterNow, sprinting, moving);
                emitComfortFeedback();
                emitRecipeUnlockFeedback(now);
                handleDeathIfNeeded();
                emitWaterSplashIfNeeded(water.movementAffected(), now);
                emitMovementAudio(moving, sprinting, water.movementAffected(), now);
                if (!onlineMode) {
                    world.tickCampfires(now);
                    world.ensurePreviewAround(camera.position(), settings.previewRadiusChunks(), settings.meshBuildBudgetChunks());
                } else {
                    sendMovementIfDue(now);
                }
                if (hotbar.updateSelection(window) || consumeHotbarScroll()) {
                    audio.play(AudioCue.INVENTORY_CLICK);
                    updateWindowTitle();
                }
                handleBlockInteraction(leftMouse, rightClicked, now);
            } else if (gameState == GameState.CHAT) {
                handleChatInput();
            } else if (gameState == GameState.CRAFTING) {
                handleCraftingSearchInput();
            }
            if (gameState == GameState.PLAYING || gameState == GameState.CRAFTING) {
                emitAmbientParticles(now);
                emitAmbientAudio(now);
            }

            glClearColor(0.52f, 0.72f, 0.95f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            if (world != null) {
                long renderStartNanos = System.nanoTime();
                lastMeshBuilds = worldRenderer.rebuildDirty(world, settings.ambientOcclusionEnabled(), settings.transparentWaterEnabled(), settings.meshBuildBudgetChunks(), camera.position());
                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(settings.fieldOfViewDegrees()),
                        (float) framebufferWidth / framebufferHeight,
                        0.05f,
                        1200.0f
                );
                Matrix4f view = camera.viewMatrix();
                List<EntitySnapshot> visibleEntities = world.visibleEntities(now);
                lastRenderStats = worldRenderer.render(projection, view, world, camera.position(), currentRenderSettings(), now);
                lastEntityRenderStats = entityRenderer.renderDetailed(projection, view, visibleEntities, now);
                emitEntityParticles(visibleEntities, now);
                lastParticleRenderStats = particleSystem.render(projection, view, now);
                lastRenderedEntities = lastEntityRenderStats.renderedEntities();
                lastChunkBorderDebugChunks = settings.debugChunkBordersEnabled()
                        ? chunkBorderRenderer.render(projection, view, camera.position(), world.dimension(), settings.renderDistanceChunks())
                        : 0;
                lastRenderedEntityHitboxes = settings.debugOverlayEnabled()
                        ? chunkBorderRenderer.renderEntityHitboxes(projection, view, visibleEntities)
                        : 0;
                lastWorldRenderMilliseconds = (System.nanoTime() - renderStartNanos) / 1_000_000.0;
            } else {
                lastMeshBuilds = 0;
                lastRenderedEntities = 0;
                lastEntityRenderStats = EntityRenderer.RenderStats.empty();
                lastParticleRenderStats = ParticleSystem.RenderStats.empty();
                lastRenderedEntityHitboxes = 0;
                lastChunkBorderDebugChunks = 0;
                lastWorldRenderMilliseconds = 0.0;
                lastRenderStats = new WorldRenderer.RenderStats(0, 0);
                previousUnderwater = false;
            }

            uiRenderer.begin();
            backgroundSpriteRenderer.begin();
            spriteRenderer.begin();
            if (gameState == GameState.MAIN_MENU) {
                renderMainMenu(mouse, leftClicked);
            } else if (gameState == GameState.PAUSED) {
                renderPauseMenu(mouse, leftClicked);
            } else if (gameState == GameState.SETTINGS) {
                renderSettingsMenu(mouse, leftClicked);
            } else if (gameState == GameState.CRAFTING) {
                renderCraftingScreen(mouse, leftClicked, leftReleased, rightClicked);
            } else if (gameState == GameState.STORAGE) {
                renderStorageScreen(mouse, leftClicked, rightClicked);
            } else if (gameState == GameState.DEAD) {
                renderDeathScreen(mouse, leftClicked);
            }
            renderHud();
            renderChatOverlay();
            backgroundSpriteRenderer.flush(framebufferWidth, framebufferHeight);
            uiRenderer.flush(framebufferWidth, framebufferHeight);
            spriteRenderer.flush(framebufferWidth, framebufferHeight);

            glfwSwapBuffers(window);
            glfwPollEvents();

            previousLeftMouse = leftMouse;
            previousRightMouse = rightMouse;
        }
    }

    private void shutdown() {
        if (uiRenderer != null) {
            uiRenderer.close();
        }
        if (backgroundSpriteRenderer != null) {
            backgroundSpriteRenderer.close();
        }
        if (spriteRenderer != null) {
            spriteRenderer.close();
        }
        if (sprites != null) {
            sprites.close();
        }
        if (worldRenderer != null) {
            worldRenderer.close();
        }
        if (chunkBorderRenderer != null) {
            chunkBorderRenderer.close();
        }
        if (entityRenderer != null) {
            entityRenderer.close();
        }
        if (particleSystem != null) {
            particleSystem.close();
        }
        if (connection != null) {
            connection.close();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void handleBlockInteraction(boolean leftMouse, boolean rightClicked, double now) {
        if (leftMouse && now >= nextBlockActionTime) {
            handleBlockBreakHold(now);
        } else if (!leftMouse) {
            blockBreakAnimation.clear();
        }
        if (rightClicked && now < nextBlockActionTime) {
            return;
        }
        if (rightClicked) {
            blockBreakAnimation.clear();
            Optional<dev.voxelgame.common.math.Raycast.Hit> pickedHit = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
            if (pickedHit.isPresent() && targetIsStorageCrate(pickedHit.get())) {
                openStorageCrate(pickedHit.get(), now);
                return;
            }
            if (pickedHit.isPresent() && targetIsSleepingMat(pickedHit.get())) {
                requestSleep(pickedHit.get(), now);
                return;
            }
            Optional<EntitySnapshot> pickedEntity = nearestEntityTarget(
                    world.visibleEntities(now),
                    camera.position(),
                    camera.forward(),
                    pickedHit.map(dev.voxelgame.common.math.Raycast.Hit::distance).orElse(InteractionRules.BLOCK_REACH)
            );
            if (pickedEntity.isPresent()) {
                handleEntityInteract(pickedEntity.get(), now);
                return;
            }
            String foodLabel = hotbar.selectedLabel();
            if (hotbar.useSelectedFood(playerStats)) {
                setStatus("Ate " + foodLabel);
                nextBlockActionTime = now + 0.22;
                audio.play(AudioCue.EAT);
                return;
            }
            Optional<Short> placeBlockId = hotbar.selectedPlaceBlockId();
            if (pickedHit.isEmpty()) {
                showTooFarAwayIfFarTarget(now);
                return;
            }
            pickedHit.ifPresent(hit -> {
                if (targetIsCampfire(hit) && selectedItemIsCampfireFuel()) {
                    if (onlineMode) {
                        connection.send(new GamePacket.BlockInteract(hotbar.selectedIndex(), hit.x(), hit.y(), hit.z()));
                        setStatus("Campfire fuel requested");
                        nextBlockActionTime = now + 0.15;
                        updateWindowTitle();
                    } else {
                        handleLocalBlockInteract(hit, now);
                    }
                    return;
                }
                if (placeBlockId.isEmpty()) {
                    if (onlineMode) {
                        connection.send(new GamePacket.BlockInteract(hotbar.selectedIndex(), hit.x(), hit.y(), hit.z()));
                        setStatus("Interaction requested");
                        nextBlockActionTime = now + 0.15;
                        updateWindowTitle();
                    } else {
                        handleLocalBlockInteract(hit, now);
                    }
                    return;
                }
                short blockId = placeBlockId.get();
                org.joml.Vector3f eyePosition = camera.position();
                if (PlayerBounds.DEFAULT.intersectsBlock(eyePosition.x, eyePosition.y, eyePosition.z, hit.placeX(), hit.placeY(), hit.placeZ())) {
                    setStatus("Too close to place");
                    nextBlockActionTime = now + 0.10;
                    return;
                }
                if (onlineMode) {
                    connection.send(new GamePacket.BlockAction(
                            GamePacket.BlockAction.Action.PLACE,
                            hotbar.selectedIndex(),
                            hit.x(),
                            hit.y(),
                            hit.z(),
                            hit.placeX(),
                            hit.placeY(),
                            hit.placeZ(),
                            blockId
                    ));
                    nextBlockActionTime = now + 0.15;
                } else {
                    if (world.placeBlock(hit, blockId, eyePosition)) {
                        hotbar.consumeSelectedOne();
                        nextBlockActionTime = now + 0.12;
                        audio.play(AudioCue.BLOCK_PLACE);
                    }
                }
            });
        }
    }

    private void handleBlockBreakHold(double now) {
        Optional<dev.voxelgame.common.math.Raycast.Hit> picked = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (picked.isEmpty()) {
            showTooFarAwayIfFarTarget(now);
            blockBreakAnimation.clear();
            return;
        }
        dev.voxelgame.common.math.Raycast.Hit hit = picked.get();
        Optional<BlockType> target = world.targetBlock(hit);
        if (target.isEmpty()) {
            blockBreakAnimation.clear();
            return;
        }
        BlockType block = target.get();
        float multiplier = hotbar.selectedBreakMultiplier(block);
        boolean canHarvest = gameMode == GameMode.CREATIVE || hotbar.canHarvestSelected(block);
        emitToolRequirementHint(block, canHarvest, multiplier, now);
        if (!canHarvest) {
            blockBreakAnimation.clear();
            return;
        }
        double duration = gameMode == GameMode.CREATIVE ? 0.08 : Math.max(0.16, breakDelay(block, multiplier));
        blockBreakAnimation.startOrContinue(hit, block, duration, now);
        if (!blockBreakAnimation.complete(now)) {
            return;
        }
        performBlockBreak(hit, block, multiplier, now);
        blockBreakAnimation.clear();
    }

    private void performBlockBreak(dev.voxelgame.common.math.Raycast.Hit hit, BlockType target, float multiplier, double now) {
        if (onlineMode) {
            connection.send(new GamePacket.BlockAction(
                    GamePacket.BlockAction.Action.BREAK,
                    hotbar.selectedIndex(),
                    hit.x(),
                    hit.y(),
                    hit.z(),
                    hit.placeX(),
                    hit.placeY(),
                    hit.placeZ(),
                    Blocks.AIR
            ));
            nextBlockActionTime = now + 0.12;
            blockBreakAnimation.hit(now);
            return;
        }
        if (world.breakBlock(hit)) {
            particleSystem.spawnBlockBreak(target, hit, now);
            collectDrops(target, multiplier, now);
            hotbar.damageSelectedTool(target);
            nextBlockActionTime = now + 0.10;
            announceNewRecipeUnlocks();
            audio.play(breakCueFor(target));
            blockBreakAnimation.hit(now);
        }
    }

    private void handleLocalBlockInteract(dev.voxelgame.common.math.Raycast.Hit hit, double now) {
        Optional<BlockType> target = world.targetBlock(hit);
        if (target.isEmpty()) {
            return;
        }
        if (CampfireRules.isCampfire(target.get().id())) {
            Optional<String> selectedItemKey = hotbar.selectedItemKey();
            OptionalDouble fuelSeconds = selectedItemKey.isPresent()
                    ? CampfireRules.fuelSeconds(selectedItemKey.get())
                    : OptionalDouble.empty();
            if (fuelSeconds.isPresent() && world.fuelCampfire(hit, now, fuelSeconds.getAsDouble())) {
                hotbar.consumeSelectedOne();
                setStatus("Campfire fueled");
                nextBlockActionTime = now + 0.25;
                audio.play(AudioCue.CRAFT);
                updateWindowTitle();
            } else {
                setStatus("Campfire needs fuel");
                nextBlockActionTime = now + 0.15;
                updateWindowTitle();
            }
            return;
        }
        InteractionRules.blockInteraction(target.get()).ifPresent(interaction -> {
            if (hotbar.addItem(interaction.itemKey(), interaction.count())) {
                particleSystem.spawnHarvestSparkle(hit, now);
                setStatus(interaction.message());
                announceNewRecipeUnlocks();
                nextBlockActionTime = now + interaction.cooldownSeconds();
                audio.play(AudioCue.COLLECT_ITEM);
                updateWindowTitle();
            } else {
                setStatus("Inventory full");
                updateWindowTitle();
            }
        });
    }

    private void openStorageCrate(dev.voxelgame.common.math.Raycast.Hit hit, double now) {
        hotbar.openStorage(hit.x(), hit.y(), hit.z());
        if (onlineMode) {
            connection.send(new GamePacket.StorageOpenRequest(hit.x(), hit.y(), hit.z()));
            setStatus("Opening storage crate");
        } else {
            setStatus("Storage crate opened");
        }
        gameState = GameState.STORAGE;
        nextBlockActionTime = now + 0.12;
        audio.play(AudioCue.INVENTORY_CLICK);
        setCursorForState();
        updateWindowTitle();
    }

    private boolean targetIsCampfire(dev.voxelgame.common.math.Raycast.Hit hit) {
        return world.targetBlock(hit).map(block -> CampfireRules.isCampfire(block.id())).orElse(false);
    }

    private boolean targetIsStorageCrate(dev.voxelgame.common.math.Raycast.Hit hit) {
        return world.targetBlock(hit).map(block -> block.id() == Blocks.STORAGE_CRATE).orElse(false);
    }

    private boolean targetIsSleepingMat(dev.voxelgame.common.math.Raycast.Hit hit) {
        return world.targetBlock(hit).map(block -> block.id() == Blocks.SLEEPING_MAT).orElse(false);
    }

    private void requestSleep(dev.voxelgame.common.math.Raycast.Hit hit, double now) {
        if (onlineMode) {
            connection.send(new GamePacket.SleepRequest(hit.x(), hit.y(), hit.z()));
            setStatus("Sleep requested");
        } else {
            setStatus("Sleeping mat needs the server clock");
        }
        nextBlockActionTime = now + 0.25;
        updateWindowTitle();
    }

    private void handleEntityInteract(EntitySnapshot target, double now) {
        GamePacket.EntityInteract.Action action = hotbar.selectedItemIsFood()
                ? GamePacket.EntityInteract.Action.FEED
                : GamePacket.EntityInteract.Action.OBSERVE;
        if (onlineMode) {
            connection.send(new GamePacket.EntityInteract(target.entityId(), hotbar.selectedIndex(), action));
            setStatus(action == GamePacket.EntityInteract.Action.FEED ? "Feed requested" : "Entity observed");
        } else {
            setStatus(action == GamePacket.EntityInteract.Action.FEED
                    ? cozyName(target.typeKey()) + " seems interested"
                    : cozyName(target.typeKey()));
        }
        nextBlockActionTime = now + 0.22;
        updateWindowTitle();
    }

    private boolean selectedItemIsCampfireFuel() {
        return hotbar.selectedItemKey().map(key -> CampfireRules.fuelSeconds(key).isPresent()).orElse(false);
    }

    private boolean showTooFarAwayIfFarTarget(double now) {
        if (world == null || world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH + 3.0).isEmpty()) {
            return false;
        }
        setStatus("Too far away");
        nextBlockActionTime = now + 0.35;
        return true;
    }

    private void emitToolRequirementHint(BlockType target, boolean canHarvest, float multiplier, double now) {
        if (gameMode == GameMode.CREATIVE
                || target.preferredTool() == ToolType.NONE
                || now < nextToolHintTime) {
            return;
        }
        if (!canHarvest) {
            int requiredLevel = InteractionRules.requiredToolLevel(target);
            if (requiredLevel > 0) {
                setStatus("Need Level " + requiredLevel + " " + toolLabel(target.preferredTool()));
            } else {
                setStatus("Need " + articleFor(target.preferredTool()) + " " + toolLabel(target.preferredTool()));
            }
            nextToolHintTime = now + 2.0;
            return;
        }
        if (multiplier >= 1.0f) {
            return;
        }
        setStatus("Need " + articleFor(target.preferredTool()) + " " + toolLabel(target.preferredTool()));
        nextToolHintTime = now + 2.0;
    }

    private void refreshLocalComfort(double now) {
        if (onlineMode || world == null || now < nextComfortScanTime) {
            return;
        }
        playerStats.applyComfort(world.comfortAt(camera.position()));
        nextComfortScanTime = now + 1.0;
    }

    private void handleDeathIfNeeded() {
        if (gameMode != GameMode.SURVIVAL || !playerStats.dead() || gameState == GameState.DEAD) {
            return;
        }
        gameState = GameState.DEAD;
        setStatus("You died");
        setCursorForState();
        updateWindowTitle();
    }

    private void respawnPlayer() {
        playerStats.respawn();
        setCameraToSpawn();
        gameState = GameState.PLAYING;
        setStatus("Respawned");
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void emitComfortFeedback() {
        int comfort = playerStats.comfort();
        if (lastComfortFeedbackValue < 0) {
            lastComfortFeedbackValue = comfort;
            return;
        }
        if (comfort >= 5 && lastComfortFeedbackValue < 5) {
            setStatus("You feel cozy");
        }
        lastComfortFeedbackValue = comfort;
    }

    private void emitRecipeUnlockFeedback(double now) {
        if (world == null || now < nextRecipeUnlockScanTime) {
            return;
        }
        nextRecipeUnlockScanTime = now + 1.0;
        CraftingStationType stationType = currentCraftingStation();
        if (stationType != CraftingStationType.INVENTORY && discoveredRecipeStations.add(stationType)) {
            setStatus("New recipe unlocked");
        }
    }

    private static String articleFor(ToolType toolType) {
        return toolType == ToolType.AXE ? "an" : "a";
    }

    private static String toolLabel(ToolType toolType) {
        return switch (toolType) {
            case PICKAXE -> "pickaxe";
            case SHOVEL -> "shovel";
            case AXE -> "axe";
            case KNIFE -> "knife";
            case NONE -> "tool";
        };
    }

    private boolean consumeHotbarScroll() {
        double scroll = pendingScrollY;
        pendingScrollY = 0.0;
        if (Math.abs(scroll) < 0.01) {
            return false;
        }
        return hotbar.scroll(scroll > 0.0 ? -1 : 1);
    }

    private void collectDrops(BlockType target, float multiplier, double now) {
        String drop = target.dropItemKey();
        if (drop == null || drop.isBlank()) {
            setStatus("Broke " + cozyName(target.key()));
            return;
        }
        int count = InteractionRules.dropCount(target, multiplier);
        if (hotbar.addItem(drop, count)) {
            pickupAnimationItemKey = drop;
            pickupAnimationUntil = now + 0.48;
            setStatus("Gathered " + cozyName(drop));
            audio.play(AudioCue.COLLECT_ITEM);
        } else {
            setStatus("Inventory full");
        }
    }

    private void emitAmbientParticles(double now) {
        if (world == null || particleSystem == null) {
            return;
        }
        for (ClientWorld.BlockPos campfire : world.activeCampfiresWithin(camera.position(), 16, 3, now)) {
            particleSystem.spawnCampfireAmbient(campfire.x(), campfire.y(), campfire.z(), now);
            if (gameState == GameState.CRAFTING) {
                particleSystem.spawnCookingSteam(campfire.x(), campfire.y(), campfire.z(), now);
            }
        }
        if (now >= nextAmbientParticleSourceScanTime) {
            ClientWorld.AmbientParticleSources sources = world.ambientParticleSourcesWithin(camera.position(), 9, 4, 3);
            ambientLeafParticleSources = sources.leafSources();
            ambientSporeParticleSources = sources.sporeSources();
            nextAmbientParticleSourceScanTime = now + 0.35;
        }
        for (ClientWorld.BlockPos source : ambientLeafParticleSources) {
            particleSystem.spawnLeafDrift(source.x(), source.y(), source.z(), now);
        }
        for (ClientWorld.BlockPos source : ambientSporeParticleSources) {
            particleSystem.spawnGlowSpores(source.x(), source.y(), source.z(), now);
        }
    }

    private void emitEntityParticles(List<EntitySnapshot> visibleEntities, double now) {
        if (particleSystem == null) {
            return;
        }
        for (EntitySnapshot entity : visibleEntities) {
            particleSystem.spawnEntityGlow(entity, now);
        }
    }

    private void emitWaterSplashIfNeeded(boolean underwater, double now) {
        if (particleSystem != null && underwater != previousUnderwater) {
            Vector3f splashPosition = new Vector3f(camera.position());
            splashPosition.y = (float) Math.floor(splashPosition.y);
            particleSystem.spawnWaterSplash(splashPosition, now);
            audio.play(AudioCue.WATER_SPLASH);
        }
        previousUnderwater = underwater;
    }

    private void emitMovementAudio(boolean moving, boolean sprinting, boolean waterAffected, double now) {
        if (!moving || waterAffected || !camera.onGround() || world == null || now < nextStepAudioTime) {
            return;
        }
        Optional<BlockType> footing = world.blockBelowPlayer(camera.position());
        if (footing.isEmpty()) {
            nextStepAudioTime = now + 0.15;
            return;
        }
        audio.play(AudioCueRules.stepCueFor(footing.get()));
        nextStepAudioTime = now + (sprinting ? 0.30 : 0.46);
    }

    private void emitAmbientAudio(double now) {
        if (world == null || now < nextAmbientAudioTime) {
            return;
        }
        Vector3f position = camera.position();
        if (world.nearestActiveCampfireWithin(position, 10, now).isPresent()) {
            audio.play(AudioCue.CAMPFIRE_CRACKLE);
            nextAmbientAudioTime = now + 4.0;
            return;
        }
        int x = (int) Math.floor(position.x);
        int y = (int) Math.floor(position.y);
        int z = (int) Math.floor(position.z);
        if (y < 58 && world.skyLightAt(x, y, z) < 4) {
            audio.play(AudioCue.CAVE_DRIP);
            nextAmbientAudioTime = now + 9.0;
            return;
        }
        String biomeKey = world.biomeKeyAt(x, z);
        if (isNightTime()) {
            audio.play(AudioCue.NIGHT_AMBIENCE);
            nextAmbientAudioTime = now + 12.0;
        } else if (biomeKey.contains("meadow") || biomeKey.contains("flower")) {
            audio.play(AudioCue.MEADOW_BIRDS);
            nextAmbientAudioTime = now + 10.0;
        } else {
            audio.play(AudioCue.DAY_AMBIENCE);
            nextAmbientAudioTime = now + 14.0;
        }
    }

    private static double breakDelay(BlockType target, float multiplier) {
        return InteractionRules.breakDelaySeconds(target, multiplier);
    }

    private static AudioCue breakCueFor(BlockType target) {
        return AudioCueRules.breakCueFor(target);
    }

    private CraftingStationType currentCraftingStation() {
        if (world != null && world.hasActiveCampfireWithin(camera.position(), CampfireRules.STATION_RADIUS_BLOCKS, frameTimeSeconds)) {
            return CraftingStationType.CAMPFIRE;
        }
        return CraftingStationType.INVENTORY;
    }

    private void handleGlobalKeys() {
        boolean chat = glfwGetKey(window, GLFW_KEY_T) == GLFW_PRESS;
        boolean slash = glfwGetKey(window, GLFW_KEY_SLASH) == GLFW_PRESS;
        boolean f3 = glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS;
        boolean crafting = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        boolean hudToggle = glfwGetKey(window, GLFW_KEY_F1) == GLFW_PRESS;
        boolean modeCycle = glfwGetKey(window, GLFW_KEY_F4) == GLFW_PRESS;
        boolean settingsKey = glfwGetKey(window, GLFW_KEY_O) == GLFW_PRESS;
        boolean spawnKey = glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS;
        boolean craftingTextInput = gameState == GameState.CRAFTING && craftingSearchFocused;
        boolean gameplayHotkeys = gameState == GameState.PLAYING || (gameState == GameState.CRAFTING && !craftingTextInput);

        if (gameState == GameState.DEAD && spawnKey && !previousSpawnKey) {
            respawnPlayer();
        }
        if (gameState == GameState.PLAYING && settings.chatEnabled()) {
            if (chat && !previousChat) {
                openChat("");
            } else if (slash && !previousSlash) {
                openChat("/");
            }
        }
        if (gameState == GameState.STORAGE && crafting && !previousCrafting) {
            closeStorageScreen();
        } else if (gameplayHotkeys && crafting && !previousCrafting) {
            toggleCrafting();
        }
        if (!craftingTextInput && f3 && !previousF3) {
            settings.toggleDebugOverlay();
        }
        if (gameplayHotkeys && hudToggle && !previousHudToggle) {
            settings.toggleHud();
        }
        if (gameplayHotkeys && modeCycle && !previousModeCycle) {
            cycleGameMode();
        }
        if (gameplayHotkeys && settingsKey && !previousSettingsKey) {
            openSettings(gameState);
        }
        if (gameplayHotkeys && spawnKey && !previousSpawnKey) {
            setCameraToSpawn();
            chatLog.add("Teleported to spawn");
        }

        previousChat = chat;
        previousSlash = slash;
        previousF3 = f3;
        previousCrafting = crafting;
        previousHudToggle = hudToggle;
        previousModeCycle = modeCycle;
        previousSettingsKey = settingsKey;
        previousSpawnKey = spawnKey;
    }

    private void handleChatInput() {
        boolean enter = glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_ENTER) == GLFW_PRESS;
        boolean backspace = glfwGetKey(window, GLFW_KEY_BACKSPACE) == GLFW_PRESS;
        if (enter && !previousEnter) {
            submitChat();
        }
        if (backspace && !previousBackspace && !chatDraft.isEmpty()) {
            chatDraft.deleteCharAt(chatDraft.length() - 1);
        }
        previousEnter = enter;
        previousBackspace = backspace;
    }

    private void handleCraftingSearchInput() {
        boolean enter = glfwGetKey(window, GLFW_KEY_ENTER) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_KP_ENTER) == GLFW_PRESS;
        boolean backspace = glfwGetKey(window, GLFW_KEY_BACKSPACE) == GLFW_PRESS;
        if (craftingSearchFocused && backspace && !previousBackspace && !craftingSearch.isEmpty()) {
            craftingSearch.deleteCharAt(craftingSearch.length() - 1);
        }
        if (craftingSearchFocused && enter && !previousEnter) {
            craftingSearchFocused = false;
        }
        previousEnter = enter;
        previousBackspace = backspace;
    }

    private void openChat(String initialText) {
        chatDraft.setLength(0);
        chatDraft.append(initialText);
        gameState = GameState.CHAT;
        setCursorForState();
        updateWindowTitle();
    }

    private void closeChat() {
        chatDraft.setLength(0);
        gameState = world == null ? GameState.MAIN_MENU : GameState.PLAYING;
        previousEnter = false;
        previousBackspace = false;
        setCursorForState();
        updateWindowTitle();
    }

    private void submitChat() {
        String line = chatDraft.toString().trim();
        closeChat();
        if (line.isEmpty()) {
            return;
        }
        if (line.startsWith("/")) {
            executeCommand(line.substring(1));
        } else if (onlineMode && connection != null) {
            connection.send(new GamePacket.Chat(connectionOptions.username() + ": " + line));
        } else {
            chatLog.add("<local> " + line);
        }
    }

    private void appendChatCharacter(int codepoint) {
        if (codepoint < 32 || codepoint > 126) {
            return;
        }
        if (gameState == GameState.CHAT && chatDraft.length() < 96) {
            chatDraft.append((char) codepoint);
        } else if (gameState == GameState.CRAFTING && craftingSearchFocused && craftingSearch.length() < 28) {
            craftingSearch.append((char) codepoint);
        }
    }

    private void executeCommand(String commandLine) {
        String[] parts = commandLine.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return;
        }
        String command = parts[0].toLowerCase(Locale.ROOT);
        try {
            switch (command) {
                case "help" -> chatLog.add("Commands: /help /keys /seed /pos /tp x y z /spawn /gamemode survival|creative|spectator /preset low|medium|high /renderdistance n /preview n /meshbudget n /fov n /fog /ao /shadows /bloom /hud /debug /debugchunks /debuglight /debugbiome /water /settings /clear /say text");
                case "keys", "keybinds" -> showKeybinds();
                case "seed" -> chatLog.add("Seed: " + connectionOptions.seed());
                case "pos" -> chatLog.add(positionLine());
                case "tp" -> teleport(parts);
                case "spawn" -> {
                    setCameraToSpawn();
                    chatLog.add("Teleported to spawn");
                }
                case "gamemode", "gm" -> setGameMode(parts);
                case "preset" -> applyRenderPreset(parts);
                case "renderdistance", "rd" -> {
                    settings.setRenderDistanceChunks(parseInt(parts, 1));
                    chatLog.add("Render distance: " + settings.renderDistanceChunks());
                }
                case "preview" -> {
                    settings.setPreviewRadiusChunks(parseInt(parts, 1));
                    refreshPreview();
                    chatLog.add("Preview radius: " + settings.previewRadiusChunks());
                }
                case "fov" -> {
                    settings.setFieldOfViewDegrees(parseInt(parts, 1));
                    chatLog.add("FOV: " + settings.fieldOfViewDegrees());
                }
                case "meshbudget" -> {
                    settings.setMeshBuildBudgetChunks(parseInt(parts, 1));
                    chatLog.add("Mesh budget: " + settings.meshBuildBudgetChunks() + " chunks/frame");
                }
                case "fog" -> toggleCommand("Fog", settings.fogEnabled(), settings::toggleFog);
                case "ao" -> {
                    settings.toggleAmbientOcclusion();
                    if (world != null) {
                        world.markAllLoadedDirty();
                    }
                    chatLog.add("Ambient AO: " + onOff(settings.ambientOcclusionEnabled()));
                }
                case "shadows" -> toggleCommand("Soft shadows", settings.softShadowsEnabled(), settings::toggleSoftShadows);
                case "bloom" -> toggleCommand("Bloom", settings.bloomEnabled(), settings::toggleBloom);
                case "hud" -> toggleCommand("HUD", settings.hudEnabled(), settings::toggleHud);
                case "debug" -> toggleCommand("Debug overlay", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
                case "debugchunks" -> toggleCommand("Chunk borders", settings.debugChunkBordersEnabled(), settings::toggleDebugChunkBorders);
                case "debuglight" -> chatLog.add(lightDebugLine());
                case "debugbiome" -> chatLog.add(biomeDebugLine());
                case "water" -> {
                    settings.toggleTransparentWater();
                    if (world != null) {
                        world.markAllLoadedDirty();
                    }
                    chatLog.add("Transparent water: " + onOff(settings.transparentWaterEnabled()));
                }
                case "settings" -> openSettings(GameState.PLAYING);
                case "clear" -> chatLog.clear();
                case "say" -> sendSay(commandLine);
                default -> chatLog.add("Unknown command. Try /help");
            }
        } catch (IllegalArgumentException e) {
            chatLog.add(e.getMessage());
        }
    }

    private void applyRenderPreset(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Usage: /preset low|medium|high");
        }
        RenderPreset preset = RenderPreset.parse(parts[1]);
        applyRenderPreset(preset);
        chatLog.add("Render preset: " + preset.label());
    }

    private void applyRenderPreset(RenderPreset preset) {
        settings.applyPreset(preset);
        if (world != null) {
            world.markAllLoadedDirty();
            refreshPreview();
        }
    }

    private void sendSay(String commandLine) {
        String message = commandLine.length() > 4 ? commandLine.substring(4).trim() : "";
        if (message.isBlank()) {
            throw new IllegalArgumentException("Usage: /say text");
        }
        if (onlineMode && connection != null) {
            connection.send(new GamePacket.Chat(connectionOptions.username() + ": " + message));
        } else {
            chatLog.add("<local> " + message);
        }
    }

    private String lightDebugLine() {
        if (world == null) {
            return "Light: no world";
        }
        int x = (int) Math.floor(camera.position().x);
        int y = (int) Math.floor(camera.position().y);
        int z = (int) Math.floor(camera.position().z);
        Optional<dev.voxelgame.common.math.Raycast.Hit> target = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (target.isPresent()) {
            x = target.get().x();
            y = target.get().y();
            z = target.get().z();
        }
        int sky = world.skyLightAt(x, y, z);
        int block = world.blockLightAt(x, y, z);
        short blockId = world.blockIdAt(x, y, z);
        BlockRenderProperties properties = BlockRenderProperties.forBlock(blockId);
        String emissive = properties.emissive() > 0.0f ? String.format(Locale.ROOT, "%.2f", properties.emissive()) : "0";
        return "Light @ " + x + " " + y + " " + z
                + ": combined " + Math.max(sky, block)
                + " sky " + sky
                + " block " + block
                + " emissive " + emissive;
    }

    private String biomeDebugLine() {
        if (world == null) {
            return "Biome: no world";
        }
        int x = (int) Math.floor(camera.position().x);
        int z = (int) Math.floor(camera.position().z);
        return "Biome @ " + x + " " + z + ": " + biomeLabel(world.biomeKeyAt(x, z));
    }

    private void showKeybinds() {
        chatLog.add("Keys: WASD move, Space jump/up, Ctrl down, Shift sprint");
        chatLog.add("Keys: E crafting, O settings, R spawn, F1 HUD, F3 debug, F4 mode");
        chatLog.add("Keys: T chat, / command, 1-9 or mouse wheel hotbar, mouse break/place");
    }

    private void teleport(String[] parts) {
        if (parts.length != 4) {
            throw new IllegalArgumentException("Usage: /tp x y z");
        }
        camera.setPosition(Float.parseFloat(parts[1]), Float.parseFloat(parts[2]), Float.parseFloat(parts[3]));
        chatLog.add(positionLine());
    }

    private void setGameMode(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Usage: /gamemode survival|creative|spectator");
        }
        GameMode.parse(parts[1]).ifPresentOrElse(mode -> {
            applyGameMode(mode);
        }, () -> {
            throw new IllegalArgumentException("Unknown gamemode: " + parts[1]);
        });
    }

    private void cycleGameMode() {
        GameMode[] modes = GameMode.values();
        applyGameMode(modes[(gameMode.ordinal() + 1) % modes.length]);
    }

    private void applyGameMode(GameMode mode) {
        gameMode = mode;
        playerStats.resetForMode(mode);
        chatLog.add("Game mode: " + mode.name());
        updateWindowTitle();
    }

    private int parseInt(String[] parts, int index) {
        if (parts.length <= index) {
            throw new IllegalArgumentException("Missing number");
        }
        return Integer.parseInt(parts[index]);
    }

    private void toggleCommand(String label, boolean before, Runnable toggle) {
        toggle.run();
        chatLog.add(label + ": " + onOff(!before));
    }

    private String onOff(boolean value) {
        return value ? "ON" : "OFF";
    }

    private static String formatMilliseconds(double milliseconds) {
        return String.format(Locale.ROOT, "%.1fMS", milliseconds);
    }

    private static String formatMegabytes(long bytes) {
        return String.format(Locale.ROOT, "%.1fMB", bytes / (1024.0 * 1024.0));
    }

    private static String formatCount(long value) {
        if (value >= 1_000_000) {
            return String.format(Locale.ROOT, "%.1fM", value / 1_000_000.0);
        }
        if (value >= 1_000) {
            return String.format(Locale.ROOT, "%.1fK", value / 1_000.0);
        }
        return Long.toString(value);
    }

    private static String formatRate(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return "0.0";
        }
        return String.format(Locale.ROOT, "%.1f", value);
    }

    private String positionLine() {
        org.joml.Vector3f position = camera.position();
        return "Position: " + Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z);
    }

    private void setCameraToSpawn() {
        if (world == null) {
            camera.setPosition(8.0f, 118.0f, 8.0f);
            return;
        }
        org.joml.Vector3f spawn = world.spawnPosition();
        camera.setPosition(spawn.x, spawn.y, spawn.z);
    }

    private static String meter(int value, int max) {
        int filled = Math.max(0, Math.min(10, Math.round(value / (float) max * 10.0f)));
        return "[" + "#".repeat(filled) + "-".repeat(10 - filled) + "]";
    }

    private static String cozyName(String key) {
        if (key == null || key.isBlank()) {
            return "something";
        }
        int colon = key.indexOf(':');
        String value = colon >= 0 ? key.substring(colon + 1) : key;
        return value.replace('_', ' ');
    }

    static Optional<EntitySnapshot> nearestEntityTarget(Collection<EntitySnapshot> snapshots, Vector3f origin, Vector3f direction, double maxDistance) {
        Vector3f rayDirection = new Vector3f(direction);
        if (rayDirection.lengthSquared() <= 0.000001f || maxDistance <= 0.0) {
            return Optional.empty();
        }
        rayDirection.normalize();
        EntitySnapshot best = null;
        double bestDistance = maxDistance;
        for (EntitySnapshot snapshot : snapshots) {
            double distance = rayDistanceToEntity(snapshot, origin, rayDirection, maxDistance);
            if (distance >= 0.0 && distance < bestDistance) {
                bestDistance = distance;
                best = snapshot;
            }
        }
        return Optional.ofNullable(best);
    }

    private static double rayDistanceToEntity(EntitySnapshot snapshot, Vector3f origin, Vector3f direction, double maxDistance) {
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        float centerX = (float) snapshot.x();
        float centerY = EntityBounds.baseY(snapshot) + bounds.height() * 0.5f;
        float centerZ = (float) snapshot.z();
        float toX = centerX - origin.x;
        float toY = centerY - origin.y;
        float toZ = centerZ - origin.z;
        double projected = toX * direction.x + toY * direction.y + toZ * direction.z;
        if (projected < 0.0 || projected > maxDistance) {
            return -1.0;
        }
        double centerDistanceSquared = toX * toX + toY * toY + toZ * toZ;
        double perpendicularSquared = centerDistanceSquared - projected * projected;
        double radius = Math.max(bounds.width(), Math.max(bounds.height(), bounds.depth())) * 0.5 + 0.12;
        if (perpendicularSquared > radius * radius) {
            return -1.0;
        }
        return Math.max(0.0, projected - Math.sqrt(radius * radius - perpendicularSquared));
    }

    private void sendMovementIfDue(double now) {
        if (connection == null || now < nextMoveSendTime) {
            return;
        }
        nextMoveSendTime = now + 0.10;
        org.joml.Vector3f position = camera.position();
        connection.send(new GamePacket.PlayerMove(
                position.x,
                position.y,
                position.z,
                camera.yaw(),
                camera.pitch(),
                false
        ));
    }

    private void updateWindowTitle() {
        if (window != 0) {
            String suffix = switch (gameState) {
                case MAIN_MENU -> "Main Menu";
                case PAUSED -> "Paused";
                case SETTINGS -> "Settings";
                case CHAT -> "Chat";
                case CRAFTING -> "Crafting";
                case STORAGE -> "Storage Crate";
                case DEAD -> "You Died";
                case PLAYING -> hotbar.selectedLabel();
            };
            glfwSetWindowTitle(window, "Adventura - " + suffix);
        }
    }

    private void handleEscape() {
        boolean escape = glfwGetKey(window, GLFW_KEY_ESCAPE) == GLFW_PRESS;
        if (escape && !previousEscape) {
            if (gameState == GameState.PLAYING) {
                gameState = GameState.PAUSED;
                setCursorForState();
                updateWindowTitle();
            } else if (gameState == GameState.PAUSED) {
                resumeGame();
            } else if (gameState == GameState.SETTINGS) {
                gameState = settingsReturnState;
                setCursorForState();
                updateWindowTitle();
            } else if (gameState == GameState.CHAT) {
                closeChat();
            } else if (gameState == GameState.CRAFTING) {
                craftingSearchFocused = false;
                draggedInventorySlot = -1;
                resumeGame();
            } else if (gameState == GameState.STORAGE) {
                closeStorageScreen();
            } else if (gameState == GameState.DEAD) {
                returnToMainMenu();
            }
        }
        previousEscape = escape;
    }

    private void renderMainMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.03f, 0.05f, 0.05f, 1.0f));
        uiRenderer.rect(0, framebufferHeight * 0.60f, framebufferWidth, framebufferHeight * 0.40f, new UiColor(0.07f, 0.12f, 0.10f, 0.9f));
        uiRenderer.centeredText("ADVENTURA", framebufferWidth * 0.5f, 88.0f, 7.0f, UiColor.WHITE);
        uiRenderer.centeredText("COZY VOXEL SURVIVAL", framebufferWidth * 0.5f, 154.0f, 2.0f, UiColor.MUTED);

        float buttonWidth = Math.min(360.0f, framebufferWidth - 80.0f);
        float buttonHeight = 48.0f;
        float x = framebufferWidth * 0.5f - buttonWidth * 0.5f;
        float y = framebufferHeight * 0.5f - 80.0f;
        UiButton singleplayer = new UiButton(x, y, buttonWidth, buttonHeight, "SINGLEPLAYER", true);
        UiButton multiplayer = new UiButton(x, y + 58.0f, buttonWidth, buttonHeight, "JOIN SERVER", true);
        UiButton settingsButton = new UiButton(x, y + 116.0f, buttonWidth, buttonHeight, "SETTINGS", true);
        UiButton quit = new UiButton(x, y + 174.0f, buttonWidth, buttonHeight, "QUIT", true);

        drawButton(singleplayer, mouse, clicked, this::startSingleplayer);
        drawButton(multiplayer, mouse, clicked, this::startMultiplayer);
        drawButton(settingsButton, mouse, clicked, () -> openSettings(GameState.MAIN_MENU));
        drawButton(quit, mouse, clicked, () -> glfwSetWindowShouldClose(window, true));
        uiRenderer.centeredText(statusMessage, framebufferWidth * 0.5f, y + 246.0f, 2.0f, UiColor.MUTED);
    }

    private void renderPauseMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, UiColor.PANEL);
        uiRenderer.centeredText("PAUSED", framebufferWidth * 0.5f, 110.0f, 6.0f, UiColor.WHITE);

        float buttonWidth = Math.min(320.0f, framebufferWidth - 80.0f);
        float buttonHeight = 46.0f;
        float x = framebufferWidth * 0.5f - buttonWidth * 0.5f;
        float y = framebufferHeight * 0.5f - 78.0f;
        drawButton(new UiButton(x, y, buttonWidth, buttonHeight, "RESUME", true), mouse, clicked, this::resumeGame);
        drawButton(new UiButton(x, y + 58.0f, buttonWidth, buttonHeight, "SETTINGS", true), mouse, clicked, () -> openSettings(GameState.PAUSED));
        drawButton(new UiButton(x, y + 116.0f, buttonWidth, buttonHeight, "MAIN MENU", true), mouse, clicked, this::returnToMainMenu);
        drawButton(new UiButton(x, y + 174.0f, buttonWidth, buttonHeight, "QUIT", true), mouse, clicked, () -> glfwSetWindowShouldClose(window, true));
    }

    private void renderDeathScreen(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.05f, 0.01f, 0.012f, 0.72f));
        float panelWidth = Math.min(420.0f, framebufferWidth - 72.0f);
        float panelHeight = 246.0f;
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float y = framebufferHeight * 0.5f - panelHeight * 0.5f;
        drawAssetPanel("panel_journal", x, y, panelWidth, panelHeight, new UiColor(0.04f, 0.025f, 0.025f, 0.88f));
        uiRenderer.rect(x + 18.0f, y + 18.0f, panelWidth - 36.0f, panelHeight - 36.0f, new UiColor(0.045f, 0.018f, 0.016f, 0.42f));
        uiRenderer.centeredText("YOU DIED", framebufferWidth * 0.5f, y + 52.0f, 5.2f, UiColor.HEART);
        uiRenderer.centeredText("DAY " + Math.max(1, (int) ((frameTimeSeconds - worldStartTimeSeconds) / LOCAL_DAY_LENGTH_SECONDS) + 1), framebufferWidth * 0.5f, y + 104.0f, 1.55f, UiColor.MUTED);
        float buttonWidth = Math.min(260.0f, panelWidth - 80.0f);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, y + 136.0f, buttonWidth, 42.0f, "RESPAWN", true), mouse, clicked, this::respawnPlayer);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, y + 188.0f, buttonWidth, 34.0f, "MAIN MENU", true), mouse, clicked, this::returnToMainMenu);
    }

    private void renderCraftingScreen(MousePosition mouse, boolean clicked, boolean released, boolean rightClicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.72f));
        uiRenderer.centeredText("CRAFTING", framebufferWidth * 0.5f, 70.0f, 5.0f, UiColor.WHITE);
        uiRenderer.centeredText("E CLOSE  CLICK RECIPE TO CRAFT  RIGHT-CLICK SPLIT  O SETTINGS", framebufferWidth * 0.5f, 116.0f, 1.7f, UiColor.MUTED);

        float contentWidth = Math.min(980.0f, framebufferWidth - 64.0f);
        float x = framebufferWidth * 0.5f - contentWidth * 0.5f;
        float y = Math.max(142.0f, framebufferHeight * 0.20f);
        renderInventoryTabs(x, y - 52.0f, contentWidth);
        CraftingStationType stationType = currentCraftingStation();
        renderWorkbenchPreview(previewCraftingRecipe(stationType), x, y, stationType);
        renderCraftingMenu(mouse, clicked, x + 360.0f, y, stationType);
        renderInventoryGridCompact(mouse, clicked, released, rightClicked, x, y + 266.0f);
    }

    private void renderStorageScreen(MousePosition mouse, boolean clicked, boolean rightClicked) {
        if (!hotbar.storageOpen()) {
            resumeGame();
            return;
        }
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.74f));
        uiRenderer.centeredText("STORAGE CRATE", framebufferWidth * 0.5f, 62.0f, 4.8f, UiColor.WHITE);
        uiRenderer.centeredText("CLICK MOVE  RIGHT-CLICK SPLIT  E OR ESC CLOSE", framebufferWidth * 0.5f, 106.0f, 1.55f, UiColor.MUTED);

        float slot = framebufferWidth < 820 ? 36.0f : 42.0f;
        float gap = 6.0f;
        int columns = 9;
        float gridWidth = columns * slot + (columns - 1) * gap;
        float panelWidth = Math.min(gridWidth + 56.0f, framebufferWidth - 42.0f);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f + 28.0f;
        float y = Math.max(132.0f, framebufferHeight * 0.20f);

        drawAssetPanel("panel_inventory", x - 24.0f, y - 40.0f, panelWidth, 130.0f, new UiColor(0.04f, 0.06f, 0.055f, 0.78f));
        uiRenderer.text("CRATE", x, y - 26.0f, 2.2f, UiColor.WHITE);
        uiRenderer.text("POS " + hotbar.storageX() + " " + hotbar.storageY() + " " + hotbar.storageZ(), x + panelWidth - 194.0f, y - 21.0f, 1.15f, UiColor.MUTED);
        String hoverHint = "";
        for (int i = 0; i < Math.min(hotbar.storageSlotCount(), 18); i++) {
            int slotIndex = i;
            int column = i % columns;
            int row = i / columns;
            float sx = x + column * (slot + gap);
            float sy = y + row * (slot + gap);
            Hotbar.SlotView slotView = hotbar.storageSlotView(i);
            String slotHint = renderTransferSlot(mouse, clicked, rightClicked, sx, sy, slot, slotView, false, false, () -> transferStorageSlot(true, slotIndex, Integer.MAX_VALUE), () -> transferStorageSlot(true, slotIndex, splitCount(slotView)));
            if (!slotHint.isBlank()) {
                hoverHint = slotHint;
            }
        }

        float inventoryY = y + 178.0f;
        drawAssetPanel("panel_inventory", x - 24.0f, inventoryY - 40.0f, panelWidth, 238.0f, new UiColor(0.04f, 0.06f, 0.055f, 0.78f));
        uiRenderer.text("BACKPACK", x, inventoryY - 26.0f, 2.2f, UiColor.WHITE);
        for (int i = 0; i < Math.min(hotbar.inventorySlotCount(), 36); i++) {
            int slotIndex = i;
            int column = i % columns;
            int row = i / columns;
            float sx = x + column * (slot + gap);
            float sy = inventoryY + row * (slot + gap);
            boolean selected = i == hotbar.selectedIndex();
            Hotbar.SlotView slotView = hotbar.slotView(i);
            String slotHint = renderTransferSlot(mouse, clicked, rightClicked, sx, sy, slot, slotView, selected, i < Hotbar.HOTBAR_SLOTS, () -> transferStorageSlot(false, slotIndex, Integer.MAX_VALUE), () -> transferStorageSlot(false, slotIndex, splitCount(slotView)));
            if (!slotHint.isBlank()) {
                hoverHint = slotHint;
            }
        }

        float buttonWidth = Math.min(220.0f, panelWidth);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, Math.min(framebufferHeight - 62.0f, inventoryY + 224.0f), buttonWidth, 40.0f, "CLOSE", true), mouse, clicked, this::closeStorageScreen);
        if (!hoverHint.isBlank()) {
            renderSlotHoverHint(mouse, hoverHint);
        }
    }

    private String renderTransferSlot(MousePosition mouse, boolean clicked, boolean rightClicked, float x, float y, float size, Hotbar.SlotView slotView, boolean selected, boolean hotbarSlot, Runnable transfer, Runnable splitTransfer) {
        boolean hovered = contains(mouse, x, y, size, size);
        if (hovered) {
            uiRenderer.rect(x - 3.0f, y - 3.0f, size + 6.0f, size + 6.0f, UiColor.BUTTON_HOVER);
        }
        drawAssetSlot(x, y, size, selected, hotbarSlot);
        if (!slotView.isEmpty()) {
            drawItemIcon(slotView.itemKey(), x + 5.0f, y + 4.0f, size - 12.0f);
            if (slotView.count() > 1) {
                uiRenderer.text(String.valueOf(slotView.count()), x + size - 15.0f, y + size - 14.0f, 1.0f, UiColor.WHITE);
            }
            if (slotView.hasDurability()) {
                drawDurabilityBar(x + 6.0f, y + size - 6.0f, size - 12.0f, slotView.durabilityLeft(), slotView.maxDurability());
            }
        }
        if (hovered && clicked && !slotView.isEmpty()) {
            transfer.run();
        } else if (hovered && rightClicked && !slotView.isEmpty()) {
            splitTransfer.run();
        }
        return hovered && !slotView.isEmpty() ? slotHoverText(slotView) : "";
    }

    private int splitCount(Hotbar.SlotView slotView) {
        return Math.max(1, (slotView.count() + 1) / 2);
    }

    private void transferStorageSlot(boolean fromStorage, int slot, int count) {
        if (!hotbar.storageOpen()) {
            return;
        }
        if (onlineMode && connection != null) {
            connection.send(new GamePacket.StorageTransfer(
                    hotbar.storageX(),
                    hotbar.storageY(),
                    hotbar.storageZ(),
                    fromStorage,
                    slot,
                    GamePacket.StorageTransfer.AUTO_TARGET_SLOT,
                    count,
                    nextStorageTransactionId()
            ));
            setStatus("Storage transfer requested");
        } else if (hotbar.transferStorage(fromStorage, slot, count)) {
            setStatus(fromStorage ? "Moved from crate" : "Stored in crate");
            if (fromStorage) {
                announceNewRecipeUnlocks();
            }
            audio.play(AudioCue.INVENTORY_CLICK);
        }
        updateWindowTitle();
    }

    private int nextStorageTransactionId() {
        storageTransactionId = storageTransactionId == Integer.MAX_VALUE ? 1 : storageTransactionId + 1;
        return storageTransactionId;
    }

    private void closeStorageScreen() {
        hotbar.closeStorage();
        resumeGame();
    }

    private void renderInventoryTabs(float x, float y, float width) {
        float tabWidth = Math.min(170.0f, width / 3.0f - 8.0f);
        String[] labels = {"CRAFT", "PACK", "COZY LOG"};
        String[] icons = {"crafting_icon", "backpack_icon", "book_icon"};
        for (int i = 0; i < labels.length; i++) {
            float tx = x + i * (tabWidth + 8.0f);
            drawAssetPanel(i == 0 ? "frame_moss" : "frame_wood", tx, y, tabWidth, 32.0f, i == 0 ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            drawHudBackdropSprite(icons[i], tx + 9.0f, y + 6.0f, 26.0f, 18.0f, new UiColor(1.0f, 1.0f, 1.0f, i == 0 ? 0.72f : 0.42f));
            uiRenderer.rect(tx, y + 30.0f, tabWidth, 2.0f, i == 0 ? UiColor.ACCENT : UiColor.BUTTON);
            uiRenderer.centeredText(labels[i], tx + tabWidth * 0.5f + 10.0f, y + 9.0f, 1.35f, i == 0 ? UiColor.WHITE : UiColor.MUTED);
        }
        uiRenderer.text("Selected: " + clampText(hotbar.selectedTooltip(), 42), x + Math.min(width - 360.0f, 540.0f), y + 9.0f, 1.35f, UiColor.MUTED);
    }

    private void renderSettingsMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.035f, 0.047f, 0.045f, 0.96f));
        uiRenderer.centeredText("SETTINGS", framebufferWidth * 0.5f, 54.0f, 5.2f, UiColor.WHITE);
        uiRenderer.centeredText("BALANCE PERFORMANCE, LOOKS AND COMFORT", framebufferWidth * 0.5f, 96.0f, 1.45f, UiColor.MUTED);

        float panelWidth = Math.min(920.0f, framebufferWidth - 72.0f);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float y = 128.0f;
        float gap = 22.0f;
        float columnWidth = (panelWidth - gap) * 0.5f;
        float leftX = x;
        float rightX = x + columnWidth + gap;

        settingsSection(leftX, y, columnWidth, 330.0f, "WORLD & RENDERING");
        settingStepperCompact(mouse, clicked, leftX + 18.0f, y + 52.0f, columnWidth - 36.0f, "Render distance", settings.renderDistanceChunks() + " chunks",
                () -> settings.adjustRenderDistance(-1),
                () -> settings.adjustRenderDistance(1));
        settingStepperCompact(mouse, clicked, leftX + 18.0f, y + 104.0f, columnWidth - 36.0f, "World preview", settings.previewRadiusChunks() + " chunks",
                () -> {
                    settings.adjustPreviewRadius(-1);
                    refreshPreview();
                },
                () -> {
                    settings.adjustPreviewRadius(1);
                    refreshPreview();
                });
        settingStepperCompact(mouse, clicked, leftX + 18.0f, y + 156.0f, columnWidth - 36.0f, "Mesh budget", settings.meshBuildBudgetChunks() + " chunks",
                () -> settings.adjustMeshBuildBudget(-1),
                () -> settings.adjustMeshBuildBudget(1));
        settingStepperCompact(mouse, clicked, leftX + 18.0f, y + 208.0f, columnWidth - 36.0f, "Field of view", settings.fieldOfViewDegrees() + " deg",
                () -> settings.adjustFieldOfView(-5),
                () -> settings.adjustFieldOfView(5));
        settingToggleCompact(mouse, clicked, leftX + 18.0f, y + 266.0f, columnWidth * 0.5f - 26.0f, "Fog", settings.fogEnabled(), settings::toggleFog);
        settingToggleCompact(mouse, clicked, leftX + columnWidth * 0.5f + 6.0f, y + 266.0f, columnWidth * 0.5f - 24.0f, "Water", settings.transparentWaterEnabled(), () -> {
            settings.toggleTransparentWater();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });

        settingsSection(rightX, y, columnWidth, 330.0f, "QUALITY & INTERFACE");
        settingStepperCompact(mouse, clicked, rightX + 18.0f, y + 52.0f, columnWidth - 36.0f, "Mouse speed", settings.mouseSensitivityPercent() + "%",
                () -> settings.adjustMouseSensitivity(-10),
                () -> settings.adjustMouseSensitivity(10));
        settingToggleCompact(mouse, clicked, rightX + 18.0f, y + 108.0f, columnWidth * 0.5f - 26.0f, "Ambient AO", settings.ambientOcclusionEnabled(), () -> {
            settings.toggleAmbientOcclusion();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });
        settingToggleCompact(mouse, clicked, rightX + columnWidth * 0.5f + 6.0f, y + 108.0f, columnWidth * 0.5f - 24.0f, "Soft shadows", settings.softShadowsEnabled(), settings::toggleSoftShadows);
        settingToggleCompact(mouse, clicked, rightX + 18.0f, y + 168.0f, columnWidth * 0.5f - 26.0f, "HUD", settings.hudEnabled(), settings::toggleHud);
        settingToggleCompact(mouse, clicked, rightX + columnWidth * 0.5f + 6.0f, y + 168.0f, columnWidth * 0.5f - 24.0f, "Bloom", settings.bloomEnabled(), settings::toggleBloom);
        settingToggleCompact(mouse, clicked, rightX + 18.0f, y + 228.0f, columnWidth * 0.5f - 26.0f, "Debug", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
        settingToggleCompact(mouse, clicked, rightX + columnWidth * 0.5f + 6.0f, y + 228.0f, columnWidth * 0.5f - 24.0f, "VSync", settings.vsyncEnabled(), () -> {
            settings.toggleVsync();
            glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        });
        settingStepperCompact(mouse, clicked, rightX + 18.0f, y + 280.0f, columnWidth - 36.0f, "UI scale", settings.uiScalePercent() + "%",
                () -> settings.adjustUiScale(-10),
                () -> settings.adjustUiScale(10));

        drawAssetPanel("frame_moss", x, y + 352.0f, panelWidth, 54.0f, new UiColor(0.045f, 0.058f, 0.052f, 0.72f));
        uiRenderer.rect(x + 8.0f, y + 360.0f, panelWidth - 16.0f, 38.0f, new UiColor(0.02f, 0.032f, 0.030f, 0.26f));
        uiRenderer.text("PRESET", x + 18.0f, y + 372.0f, 1.45f, UiColor.MUTED);
        float presetButtonWidth = 106.0f;
        float presetX = x + 112.0f;
        drawButton(new UiButton(presetX, y + 362.0f, presetButtonWidth, 34.0f, "LOW", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.LOW));
        drawButton(new UiButton(presetX + presetButtonWidth + 10.0f, y + 362.0f, presetButtonWidth, 34.0f, "MEDIUM", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.MEDIUM));
        drawButton(new UiButton(presetX + (presetButtonWidth + 10.0f) * 2.0f, y + 362.0f, presetButtonWidth, 34.0f, "HIGH", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.HIGH));

        float buttonWidth = Math.min(280.0f, framebufferWidth - 80.0f);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, framebufferHeight - 86.0f, buttonWidth, 46.0f, "BACK", true), mouse, clicked, () -> {
            gameState = settingsReturnState;
            setCursorForState();
            updateWindowTitle();
        });
    }

    private void settingsSection(float x, float y, float width, float height, String title) {
        drawAssetPanel("panel_settings", x, y, width, height, UiColor.PANEL);
        uiRenderer.rect(x + 8.0f, y + 7.0f, width - 16.0f, 3.0f, UiColor.ACCENT);
        uiRenderer.text(title, x + 18.0f, y + 18.0f, 2.0f, UiColor.WHITE);
    }

    private void settingStepperCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, String value, Runnable minus, Runnable plus) {
        drawAssetPanel("frame_stone", x, y, width, 40.0f, UiColor.SLOT);
        uiRenderer.rect(x + 5.0f, y + 5.0f, width - 10.0f, 30.0f, new UiColor(0.02f, 0.032f, 0.030f, 0.34f));
        uiRenderer.text(label.toUpperCase(Locale.ROOT), x + 10.0f, y + 7.0f, 1.15f, UiColor.MUTED);
        uiRenderer.text(value.toUpperCase(Locale.ROOT), x + 10.0f, y + 23.0f, 1.2f, UiColor.WHITE);
        drawButton(new UiButton(x + width - 78.0f, y + 5.0f, 30.0f, 30.0f, "-", true), mouse, clicked, minus);
        drawButton(new UiButton(x + width - 38.0f, y + 5.0f, 30.0f, 30.0f, "+", true), mouse, clicked, plus);
    }

    private void settingToggleCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, boolean enabled, Runnable toggle) {
        drawAssetPanel("frame_wood", x, y, width, 44.0f, UiColor.SLOT);
        uiRenderer.rect(x + 5.0f, y + 5.0f, width - 10.0f, 34.0f, new UiColor(0.02f, 0.032f, 0.030f, 0.30f));
        uiRenderer.text(label.toUpperCase(Locale.ROOT), x + 10.0f, y + 8.0f, 1.2f, UiColor.WHITE);
        drawButton(new UiButton(x + width - 80.0f, y + 7.0f, 68.0f, 30.0f, enabled ? "ON" : "OFF", true), mouse, clicked, toggle);
    }

    private void renderCraftingMenu(MousePosition mouse, boolean clicked, float x, float y, CraftingStationType stationType) {
        float panelWidth = Math.min(560.0f, framebufferWidth - x - 32.0f);
        if (panelWidth < 260.0f) {
            return;
        }
        List<CraftingRecipe> recipes = filteredCraftingRecipes(stationType);
        int visibleRecipes = Math.min(6, recipes.size());
        float rowHeight = 50.0f;
        float panelHeight = 142.0f + Math.max(1, visibleRecipes) * rowHeight + (recipes.size() > visibleRecipes ? 24.0f : 0.0f);
        drawAssetPanel("panel_crafting", x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.72f));
        uiRenderer.rect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, panelHeight - 56.0f, new UiColor(0.018f, 0.030f, 0.028f, 0.20f));
        uiRenderer.text("RECIPES", x, y - 30.0f, 2.6f, UiColor.WHITE);
        uiRenderer.text("STATION " + Hotbar.stationLabel(stationType).toUpperCase(Locale.ROOT), x + panelWidth - 210.0f, y - 25.0f, 1.2f, UiColor.MUTED);
        renderCraftingFilters(mouse, clicked, x, y + 2.0f, panelWidth);
        renderCraftingSearchField(mouse, clicked, x, y + 34.0f, panelWidth);
        float buttonHeight = 34.0f;
        for (int index = 0; index < visibleRecipes; index++) {
            CraftingRecipe recipe = recipes.get(index);
            boolean unlocked = recipeUnlockedForUi(recipe, stationType);
            boolean canCraft = unlocked && hotbar.canCraft(recipe, stationType);
            float rowY = y + 76.0f + index * rowHeight;
            String buttonLabel = recipe.label().toUpperCase(Locale.ROOT) + " [" + recipe.category().name() + "]";
            UiButton button = new UiButton(x, rowY, panelWidth, buttonHeight, buttonLabel, canCraft);
            drawButton(button, mouse, clicked, () -> {
                if (onlineMode) {
                    connection.send(craftRequestFor(recipe));
                    setStatus("Crafting requested");
                    updateWindowTitle();
                } else if (hotbar.craft(recipe, stationType)) {
                    setStatus("Crafted " + recipe.label());
                    announceNewRecipeUnlocks();
                    audio.play(AudioCue.CRAFT_SUCCESS);
                    updateWindowTitle();
                }
            });
            if (!canCraft && clicked && button.contains(mouse.x(), mouse.y())) {
                setStatus(craftFailMessage(recipe, stationType));
                audio.play(AudioCue.CRAFT_FAIL);
            }
            UiColor stationColor = recipe.isAvailableAt(stationType) ? UiColor.MUTED : UiColor.BUTTON_DISABLED;
            UiColor statusColor = canCraft ? UiColor.ACCENT : unlocked && recipe.isAvailableAt(stationType) ? UiColor.HEART : UiColor.BUTTON_DISABLED;
            uiRenderer.text(clampText(stationRequirementLine(recipe), 42), x + 42.0f, rowY + 23.0f, 0.95f, stationColor);
            uiRenderer.text(clampText(hotbar.recipeSummary(recipe), 42), x + 42.0f, rowY + 36.0f, 0.95f, unlocked ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            uiRenderer.text(recipeStatusLine(recipe, stationType).toUpperCase(Locale.ROOT), x + panelWidth - 168.0f, rowY + 36.0f, 0.9f, statusColor);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), x + 8.0f, rowY + 4.0f, 26.0f);
        }
        if (recipes.isEmpty()) {
            uiRenderer.text("NO MATCHING RECIPES", x + 12.0f, y + 92.0f, 1.55f, UiColor.MUTED);
            if (clicked && contains(mouse, x, y + 76.0f, panelWidth, 42.0f)) {
                setStatus("No matching recipe");
                audio.play(AudioCue.CRAFT_FAIL);
            }
        }
        if (recipes.size() > visibleRecipes) {
            uiRenderer.text("MORE RECIPES IN THIS FILTER", x, y + 76.0f + visibleRecipes * rowHeight + 10.0f, 1.15f, UiColor.MUTED);
        }
    }

    private GamePacket craftRequestFor(CraftingRecipe recipe) {
        if (recipe.stationType() == CraftingStationType.CAMPFIRE && world != null) {
            Optional<ClientWorld.BlockPos> station = world.nearestActiveCampfireWithin(camera.position(), CampfireRules.STATION_RADIUS_BLOCKS, frameTimeSeconds);
            Optional<List<Integer>> inputSlots = hotbar.inputSlotsFor(recipe);
            if (station.isPresent() && inputSlots.isPresent()) {
                ClientWorld.BlockPos pos = station.get();
                return new GamePacket.CookRequest(pos.x(), pos.y(), pos.z(), recipe.key(), inputSlots.get());
            }
        }
        return new GamePacket.CraftRequest(recipe.key());
    }

    private boolean recipeUnlockedForUi(CraftingRecipe recipe, CraftingStationType stationType) {
        return switch (recipe.unlockCondition()) {
            case ALWAYS -> true;
            case NEAR_STATION -> recipe.isAvailableAt(stationType);
            case DISCOVERED_ITEM, FOUND_LORE_NOTE, BIOME_DISCOVERED -> false;
        };
    }

    private String recipeStatusLine(CraftingRecipe recipe, CraftingStationType stationType) {
        if (!recipeUnlockedForUi(recipe, stationType)) {
            return "Locked: " + unlockRequirementLine(recipe);
        }
        return hotbar.recipeStatus(recipe, stationType);
    }

    private String craftFailMessage(CraftingRecipe recipe, CraftingStationType stationType) {
        if (!recipe.isAvailableAt(stationType)) {
            return "This needs a " + Hotbar.stationLabel(recipe.stationType()).toLowerCase(Locale.ROOT);
        }
        if (!recipeUnlockedForUi(recipe, stationType)) {
            return "Locked: " + unlockRequirementLine(recipe);
        }
        return "Missing ingredients";
    }

    private String stationRequirementLine(CraftingRecipe recipe) {
        return "Station: " + Hotbar.stationLabel(recipe.stationType());
    }

    private String unlockRequirementLine(CraftingRecipe recipe) {
        return switch (recipe.unlockCondition()) {
            case ALWAYS -> "Unlocked";
            case NEAR_STATION -> "Near " + Hotbar.stationLabel(recipe.stationType());
            case DISCOVERED_ITEM -> "Discover item";
            case FOUND_LORE_NOTE -> "Find lore note";
            case BIOME_DISCOVERED -> "Discover biome";
        };
    }

    private void renderCraftingFilters(MousePosition mouse, boolean clicked, float x, float y, float width) {
        float gap = 6.0f;
        float chipHeight = 26.0f;
        float chipWidth = Math.min(66.0f, (width - gap * 6.0f) / 7.0f);
        float cx = x;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "ALL", craftingCategoryFilter == null, () -> craftingCategoryFilter = null);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "TOOLS", craftingCategoryFilter == CraftingCategory.TOOLS, () -> craftingCategoryFilter = CraftingCategory.TOOLS);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "FOOD", craftingCategoryFilter == CraftingCategory.FOOD, () -> craftingCategoryFilter = CraftingCategory.FOOD);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "BUILD", craftingCategoryFilter == CraftingCategory.BUILDING, () -> craftingCategoryFilter = CraftingCategory.BUILDING);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "DECOR", craftingCategoryFilter == CraftingCategory.DECOR, () -> craftingCategoryFilter = CraftingCategory.DECOR);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "ADV", craftingCategoryFilter == CraftingCategory.ADVENTURE, () -> craftingCategoryFilter = CraftingCategory.ADVENTURE);
        cx += chipWidth + gap;
        renderCraftingFilterChip(mouse, clicked, cx, y, chipWidth, chipHeight, "READY", craftableRecipesOnly, () -> craftableRecipesOnly = !craftableRecipesOnly);
    }

    private void renderCraftingFilterChip(MousePosition mouse, boolean clicked, float x, float y, float width, float height, String label, boolean selected, Runnable action) {
        boolean hovered = contains(mouse, x, y, width, height);
        UiColor color = selected ? UiColor.SLOT_ACTIVE : hovered ? UiColor.BUTTON_HOVER : UiColor.SLOT;
        uiRenderer.rect(x, y, width, height, color);
        if (selected) {
            uiRenderer.rect(x, y + height - 3.0f, width, 3.0f, UiColor.ACCENT);
        }
        float textScale = Math.min(1.15f, (width - 6.0f) / Math.max(1.0f, BitmapFont.textWidth(label, 1.0f)));
        uiRenderer.centeredText(
                label,
                x + width * 0.5f,
                y + (height - BitmapFont.textHeight(textScale)) * 0.5f,
                textScale,
                selected ? UiColor.WHITE : UiColor.MUTED
        );
        if (hovered && clicked) {
            action.run();
            audio.play(AudioCue.INVENTORY_CLICK);
        }
    }

    private void renderCraftingSearchField(MousePosition mouse, boolean clicked, float x, float y, float width) {
        boolean hovered = contains(mouse, x, y, width, 28.0f);
        if (clicked) {
            craftingSearchFocused = hovered;
        }
        UiColor color = craftingSearchFocused ? UiColor.SLOT_ACTIVE : hovered ? UiColor.BUTTON_HOVER : UiColor.SLOT;
        uiRenderer.rect(x, y, width, 28.0f, color);
        uiRenderer.rect(x, y + 26.0f, width, 2.0f, craftingSearchFocused ? UiColor.ACCENT : UiColor.BUTTON);
        String value = craftingSearch.isEmpty() ? "SEARCH" : craftingSearch.toString().toUpperCase(Locale.ROOT);
        UiColor textColor = craftingSearch.isEmpty() ? UiColor.MUTED : UiColor.WHITE;
        uiRenderer.text(clampText(value, 50), x + 10.0f, y + 9.0f, 1.15f, textColor);
        if (!craftingSearch.isEmpty()) {
            float clearWidth = 24.0f;
            float clearX = x + width - clearWidth - 5.0f;
            boolean clearHovered = contains(mouse, clearX, y + 2.0f, clearWidth, 24.0f);
            uiRenderer.rect(clearX, y + 2.0f, clearWidth, 24.0f, clearHovered ? UiColor.BUTTON_HOVER : UiColor.BUTTON);
            uiRenderer.centeredText("X", clearX + clearWidth * 0.5f, y + 8.0f, 1.15f, UiColor.WHITE);
            if (clearHovered && clicked) {
                craftingSearch.setLength(0);
                craftingSearchFocused = true;
                audio.play(AudioCue.INVENTORY_CLICK);
            }
        }
    }

    private void renderWorkbenchPreview(CraftingRecipe recipe, float x, float y, CraftingStationType stationType) {
        float panelWidth = 316.0f;
        float panelHeight = 232.0f;
        String panelSprite = stationType == CraftingStationType.CAMPFIRE ? "panel_campfire" : "panel_crafting";
        drawAssetPanel(panelSprite, x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.76f));
        uiRenderer.rect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, 136.0f, new UiColor(0.018f, 0.030f, 0.028f, 0.22f));
        uiRenderer.text(Hotbar.stationLabel(stationType).toUpperCase(Locale.ROOT), x, y - 30.0f, 2.6f, UiColor.WHITE);
        uiRenderer.text("3 x 3", x + panelWidth - 62.0f, y - 25.0f, 1.25f, UiColor.MUTED);

        float slot = 42.0f;
        float gap = 7.0f;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                float sx = x + column * (slot + gap);
                float sy = y + row * (slot + gap);
                uiRenderer.rect(sx, sy, slot, slot, UiColor.SLOT);
            }
        }

        if (recipe != null) {
            for (int i = 0; i < recipe.ingredients().size() && i < 9; i++) {
                CraftingRecipe.Ingredient ingredient = recipe.ingredients().get(i);
                int column = i % 3;
                int row = i / 3;
                float sx = x + column * (slot + gap);
                float sy = y + row * (slot + gap);
                int owned = hotbar.itemCount(ingredient.itemId());
                boolean hasIngredient = owned >= ingredient.count();
                if (!hasIngredient) {
                    uiRenderer.rect(sx + 3.0f, sy + 3.0f, slot - 6.0f, slot - 6.0f, new UiColor(0.55f, 0.10f, 0.12f, 0.34f));
                }
                drawItemIcon(hotbar.itemKey(ingredient.itemId()), sx + 6.0f, sy + 5.0f, 30.0f);
                uiRenderer.text(owned + "/" + ingredient.count(), sx + 8.0f, sy + 28.0f, 0.95f, hasIngredient ? UiColor.WHITE : UiColor.HEART);
            }
            float arrowX = x + 164.0f;
            float arrowY = y + 58.0f;
            uiRenderer.rect(arrowX, arrowY + 9.0f, 34.0f, 5.0f, UiColor.MUTED);
            uiRenderer.rect(arrowX + 28.0f, arrowY + 4.0f, 10.0f, 15.0f, UiColor.MUTED);
            float outX = x + 222.0f;
            float outY = y + 48.0f;
            boolean canCraft = hotbar.canCraft(recipe, stationType);
            uiRenderer.rect(outX - 4.0f, outY - 4.0f, 58.0f, 58.0f, canCraft ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), outX + 4.0f, outY + 4.0f, 42.0f);
            if (recipe.result().count() > 1) {
                uiRenderer.text(String.valueOf(recipe.result().count()), outX + 38.0f, outY + 39.0f, 1.15f, UiColor.WHITE);
            }
            uiRenderer.text(clampText(recipe.label(), 28), x, y + 158.0f, 1.55f, UiColor.WHITE);
            uiRenderer.text(stationRequirementLine(recipe).toUpperCase(Locale.ROOT), x, y + 178.0f, 1.05f, recipe.isAvailableAt(stationType) ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            uiRenderer.text(recipeStatusLine(recipe, stationType).toUpperCase(Locale.ROOT), x, y + 196.0f, 1.1f, canCraft ? UiColor.ACCENT : recipeUnlockedForUi(recipe, stationType) ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
        }
    }

    private CraftingRecipe previewCraftingRecipe(CraftingStationType stationType) {
        List<CraftingRecipe> recipes = filteredCraftingRecipes(stationType);
        for (CraftingRecipe recipe : recipes) {
            if (hotbar.canCraft(recipe, stationType)) {
                return recipe;
            }
        }
        if (!recipes.isEmpty()) {
            return recipes.get(0);
        }
        return null;
    }

    private List<CraftingRecipe> filteredCraftingRecipes(CraftingStationType stationType) {
        return prioritizedRecipes(stationType).stream()
                .filter(recipe -> craftingCategoryFilter == null || recipe.category() == craftingCategoryFilter)
                .filter(recipe -> !craftableRecipesOnly || recipeUnlockedForUi(recipe, stationType) && hotbar.canCraft(recipe, stationType))
                .filter(recipe -> craftingSearch.isEmpty() || recipeMatchesSearch(recipe))
                .toList();
    }

    private boolean recipeMatchesSearch(CraftingRecipe recipe) {
        String search = craftingSearch.toString().toLowerCase(Locale.ROOT);
        return recipe.label().toLowerCase(Locale.ROOT).contains(search)
                || recipe.key().toLowerCase(Locale.ROOT).contains(search)
                || recipe.category().name().toLowerCase(Locale.ROOT).contains(search)
                || hotbar.recipeSummary(recipe).toLowerCase(Locale.ROOT).contains(search)
                || Hotbar.stationLabel(recipe.stationType()).toLowerCase(Locale.ROOT).contains(search);
    }

    private List<CraftingRecipe> prioritizedRecipes(CraftingStationType stationType) {
        return hotbar.recipes().stream()
                .sorted(Comparator
                        .comparingInt((CraftingRecipe recipe) -> recipeUnlockedForUi(recipe, stationType) ? 0 : 1)
                        .thenComparingInt(recipe -> hotbar.canCraft(recipe, stationType) ? 0 : 1)
                        .thenComparingInt(recipe -> recipe.isAvailableAt(stationType) ? 0 : 1)
                        .thenComparing(CraftingRecipe::category)
                        .thenComparing(CraftingRecipe::label))
                .toList();
    }

    private void renderInventoryGrid(float x, float y) {
        float slotWidth = 98.0f;
        float slotHeight = 34.0f;
        float gap = 6.0f;
        float gridWidth = slotWidth * 4 + gap * 3;
        if (x + gridWidth > framebufferWidth - 24.0f) {
            return;
        }
        uiRenderer.rect(x - 14.0f, y - 44.0f, gridWidth + 28.0f, 360.0f, new UiColor(0.04f, 0.06f, 0.06f, 0.72f));
        uiRenderer.text("INVENTORY", x, y - 30.0f, 3.0f, UiColor.WHITE);
        for (int i = 0; i < hotbar.inventorySlotCount(); i++) {
            int column = i % 4;
            int row = i / 4;
            float sx = x + column * (slotWidth + gap);
            float sy = y + row * (slotHeight + gap);
            boolean hotbarSlot = i < Hotbar.HOTBAR_SLOTS;
            Hotbar.SlotView slot = hotbar.slotView(i);
            uiRenderer.rect(sx, sy, slotWidth, slotHeight, hotbarSlot ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            if (!slot.isEmpty()) {
                drawItemIcon(slot.itemKey(), sx + 5.0f, sy + 4.0f, 26.0f);
                if (slot.hasDurability()) {
                    drawDurabilityBar(sx + 5.0f, sy + slotHeight - 5.0f, 26.0f, slot.durabilityLeft(), slot.maxDurability());
                }
            }
            uiRenderer.text(clampText(slot.isEmpty() ? (i + 1) + " Empty" : slot.label() + " x" + slot.count(), 12), sx + 36.0f, sy + 11.0f, 1.0f, UiColor.WHITE);
        }
    }

    private void renderInventoryGridCompact(MousePosition mouse, boolean clicked, boolean released, float x, float y) {
        float uiScale = settings.uiScale();
        float slot = 42.0f * uiScale;
        float gap = 6.0f * uiScale;
        int columns = 9;
        int rows = 4;
        float gridWidth = columns * slot + (columns - 1) * gap;
        float panelWidth = Math.min(gridWidth + 28.0f * uiScale, framebufferWidth - 48.0f * uiScale);
        if (x + panelWidth > framebufferWidth - 24.0f * uiScale) {
            x = Math.max(24.0f * uiScale, framebufferWidth * 0.5f - panelWidth * 0.5f);
        }
        drawAssetPanel("panel_inventory", x - 14.0f * uiScale, y - 42.0f * uiScale, panelWidth, rows * (slot + gap) + 48.0f * uiScale, new UiColor(0.04f, 0.06f, 0.06f, 0.74f));
        uiRenderer.rect(x + 2.0f * uiScale, y - 2.0f * uiScale, gridWidth - 4.0f * uiScale, rows * (slot + gap) - gap + 4.0f * uiScale, new UiColor(0.018f, 0.030f, 0.028f, 0.22f));
        uiRenderer.text("INVENTORY", x, y - 28.0f * uiScale, 2.4f * uiScale, UiColor.WHITE);
        drawButton(new UiButton(x + panelWidth - 188.0f * uiScale, y - 36.0f * uiScale, 76.0f * uiScale, 28.0f * uiScale, "SORT", true), mouse, clicked, () -> {
            if (hotbar.sortBackpack()) {
                setStatus("Backpack sorted");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            }
        });
        boolean trashEnabled = gameMode == GameMode.CREATIVE;
        if (!trashEnabled) {
            inventoryTrashMode = false;
        }
        drawButton(new UiButton(x + panelWidth - 104.0f * uiScale, y - 36.0f * uiScale, 90.0f * uiScale, 28.0f * uiScale, inventoryTrashMode ? "TRASH ON" : "TRASH", trashEnabled), mouse, clicked, () -> {
            inventoryTrashMode = !inventoryTrashMode;
            setStatus(inventoryTrashMode ? "Trash mode enabled" : "Trash mode disabled");
            audio.play(AudioCue.INVENTORY_CLICK);
            updateWindowTitle();
        });
        String hoverHint = "";
        boolean droppedOnSlot = false;
        for (int i = 0; i < Math.min(hotbar.inventorySlotCount(), columns * rows); i++) {
            int column = i % columns;
            int row = i / columns;
            float sx = x + column * (slot + gap);
            float sy = y + row * (slot + gap);
            boolean selected = i == hotbar.selectedIndex();
            Hotbar.SlotView slotView = hotbar.slotView(i);
            boolean hovered = contains(mouse, sx, sy, slot, slot);
            if (hovered) {
                uiRenderer.rect(sx - 3.0f * uiScale, sy - 3.0f * uiScale, slot + 6.0f * uiScale, slot + 6.0f * uiScale, UiColor.BUTTON_HOVER);
            }
            if (draggedInventorySlot == i) {
                uiRenderer.rect(sx - 2.0f * uiScale, sy - 2.0f * uiScale, slot + 4.0f * uiScale, slot + 4.0f * uiScale, new UiColor(0.45f, 0.74f, 0.42f, 0.36f));
            }
            drawAssetSlot(sx, sy, slot, selected, i < Hotbar.HOTBAR_SLOTS);
            if (!slotView.isEmpty()) {
                drawItemIcon(slotView.itemKey(), sx + 6.0f * uiScale, sy + 5.0f * uiScale, 30.0f * uiScale);
                if (slotView.count() > 1) {
                    uiRenderer.text(String.valueOf(slotView.count()), sx + 27.0f * uiScale, sy + 28.0f * uiScale, 1.05f * uiScale, UiColor.WHITE);
                }
                if (slotView.hasDurability()) {
                    drawDurabilityBar(sx + 7.0f * uiScale, sy + 36.0f * uiScale, 28.0f * uiScale, slotView.durabilityLeft(), slotView.maxDurability());
                }
                if (hovered) {
                    hoverHint = slotHoverText(slotView);
                }
            }
            if (hovered && released && draggedInventorySlot >= 0) {
                droppedOnSlot = true;
                if (draggedInventorySlot != i && hotbar.moveInventorySlot(draggedInventorySlot, i)) {
                    setStatus("Moved stack");
                    audio.play(AudioCue.INVENTORY_CLICK);
                    updateWindowTitle();
                }
            } else if (hovered && rightClicked && hotbar.splitInventorySlot(i)) {
                setStatus("Split stack");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && inventoryTrashMode && hotbar.trashInventorySlot(i)) {
                setStatus("Item trashed");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && isShiftDown() && hotbar.quickMoveInventorySlot(i)) {
                setStatus(i < Hotbar.HOTBAR_SLOTS ? "Moved to backpack" : "Moved to hotbar");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && !slotView.isEmpty()) {
                draggedInventorySlot = i;
            }
        }
        renderDraggedInventoryStack(mouse);
        if (released && draggedInventorySlot >= 0) {
            if (!droppedOnSlot) {
                setStatus("Drag cancelled");
            }
            draggedInventorySlot = -1;
        }
        if (!hoverHint.isBlank()) {
            renderSlotHoverHint(mouse, hoverHint);
        }
    }

    private void renderDraggedInventoryStack(MousePosition mouse) {
        if (draggedInventorySlot < 0 || draggedInventorySlot >= hotbar.inventorySlotCount()) {
            return;
        }
        Hotbar.SlotView slotView = hotbar.slotView(draggedInventorySlot);
        if (slotView.isEmpty()) {
            draggedInventorySlot = -1;
            return;
        }
        float size = 38.0f;
        float x = (float) mouse.x() + 14.0f;
        float y = (float) mouse.y() + 14.0f;
        drawAssetSlot(x, y, size, false, draggedInventorySlot < Hotbar.HOTBAR_SLOTS);
        drawItemIcon(slotView.itemKey(), x + 5.0f, y + 4.0f, size - 10.0f);
        if (slotView.count() > 1) {
            uiRenderer.text(String.valueOf(slotView.count()), x + size - 15.0f, y + size - 14.0f, 0.95f, UiColor.WHITE);
        }
    }

    private String slotHoverText(Hotbar.SlotView slotView) {
        StringBuilder text = new StringBuilder(slotView.label());
        if (!slotView.category().isBlank()) {
            text.append(" | ").append(slotView.category());
        }
        if (!slotView.description().isBlank()) {
            text.append(" | ").append(slotView.description());
        }
        if (!slotView.rarity().isBlank()) {
            text.append(" | ").append(slotView.rarity());
        }
        if (slotView.count() > 1) {
            text.append(" x").append(slotView.count());
        }
        if (!slotView.toolTypeLabel().isBlank()) {
            text.append(" | ").append(slotView.toolTypeLabel()).append(" Level ").append(slotView.toolLevel());
        }
        if (slotView.foodValue() > 0) {
            text.append(" | Food +").append(slotView.foodValue());
        }
        if (slotView.healValue() > 0) {
            text.append(" | Heal +").append(slotView.healValue());
        }
        if (slotView.comfortValue() > 0) {
            text.append(" | Comfort +").append(slotView.comfortValue());
        }
        if (slotView.hasDurability()) {
            text.append(" | Durability ").append(slotView.durabilityLeft()).append("/").append(slotView.maxDurability());
        }
        if (slotView.placeable()) {
            text.append(" | Placeable");
        }
        return text.toString();
    }

    private void renderSlotHoverHint(MousePosition mouse, String text) {
        String label = clampText(text, 52);
        float scale = 1.2f;
        float width = BitmapFont.textWidth(label, scale) + 16.0f;
        float height = BitmapFont.textHeight(scale) + 14.0f;
        float maxX = Math.max(12.0f, framebufferWidth - width - 12.0f);
        float maxY = Math.max(12.0f, framebufferHeight - height - 12.0f);
        float x = Math.max(12.0f, Math.min((float) mouse.x() + 16.0f, maxX));
        float y = Math.max(12.0f, Math.min((float) mouse.y() - height - 10.0f, maxY));
        uiRenderer.rect(x, y, width, height, new UiColor(0.035f, 0.045f, 0.042f, 0.96f));
        uiRenderer.rect(x, y, width, 2.0f, UiColor.ACCENT);
        uiRenderer.text(label, x + 8.0f, y + 8.0f, scale, UiColor.WHITE);
    }

    private void renderFeedbackOverlay(float centerX, float bottomY) {
        List<String> messages = feedbackLog.visible(frameTimeSeconds);
        if (messages.isEmpty()) {
            return;
        }
        float uiScale = settings.uiScale();
        float scale = 1.15f * uiScale;
        float lineHeight = 18.0f * uiScale;
        float width = 0.0f;
        for (String message : messages) {
            width = Math.max(width, BitmapFont.textWidth(clampText(message, 48), scale));
        }
        width = Math.min(width + 24.0f * uiScale, Math.max(240.0f * uiScale, framebufferWidth - 48.0f * uiScale));
        float height = messages.size() * lineHeight + 14.0f * uiScale;
        float margin = 18.0f * uiScale;
        float x = Math.max(margin, Math.min(centerX - width * 0.5f, framebufferWidth - width - margin));
        float y = Math.max(24.0f * uiScale, bottomY - height);
        uiRenderer.rect(x, y, width, height, new UiColor(0.025f, 0.035f, 0.032f, 0.68f));
        uiRenderer.rect(x, y, width, 2.0f * uiScale, UiColor.ACCENT);
        float textY = y + 9.0f * uiScale;
        for (String message : messages) {
            uiRenderer.centeredText(clampText(message, 48), x + width * 0.5f, textY, scale, UiColor.WHITE);
            textY += lineHeight;
        }
    }

    private void renderHud() {
        if ((gameState != GameState.PLAYING && gameState != GameState.CHAT) || world == null || !settings.hudEnabled()) {
            return;
        }
        float uiScale = settings.uiScale();
        float crosshairLength = 10.0f * uiScale;
        float crosshairThickness = Math.max(1.0f, 2.0f * uiScale);
        uiRenderer.rect(framebufferWidth * 0.5f - crosshairLength * 0.5f, framebufferHeight * 0.5f - crosshairThickness * 0.5f, crosshairLength, crosshairThickness, UiColor.WHITE);
        uiRenderer.rect(framebufferWidth * 0.5f - crosshairThickness * 0.5f, framebufferHeight * 0.5f - crosshairLength * 0.5f, crosshairThickness, crosshairLength, UiColor.WHITE);
        renderBreakOverlay();

        float hotbarSlotSize = 58.0f * uiScale;
        float hotbarGap = 6.0f * uiScale;
        float hotbarWidth = Hotbar.HOTBAR_SLOTS * hotbarSlotSize + (Hotbar.HOTBAR_SLOTS - 1) * hotbarGap;
        float hotbarX = Math.max(20.0f * uiScale, framebufferWidth * 0.5f - hotbarWidth * 0.5f);
        float hotbarY = framebufferHeight - 86.0f * uiScale;
        float statsY = hotbarY - 44.0f * uiScale;
        uiRenderer.centeredText(clampText(hotbar.selectedTooltip(), 64), framebufferWidth * 0.5f, statsY - 42.0f * uiScale, 1.45f * uiScale, UiColor.WHITE);
        float statWidth = Math.min(188.0f * uiScale, hotbarWidth * 0.31f);
        drawStatStrip("HEALTH", "heart_full", "heart_half", "heart_empty", playerStats.health(), 20, hotbarX + 4.0f * uiScale, statsY, statWidth, 11.0f * uiScale, UiColor.HEART);
        drawStatStrip("ENERGY", "leaf_full", "leaf_half", "leaf_empty", playerStats.stamina(), 20, hotbarX + hotbarWidth * 0.5f - statWidth * 0.5f, statsY, statWidth, 10.5f * uiScale, UiColor.ENERGY);
        drawStatStrip("HUNGER", "hunger_full", "hunger_half", "hunger_empty", playerStats.hunger(), 20, hotbarX + hotbarWidth - statWidth - 4.0f * uiScale, statsY, statWidth, 11.0f * uiScale, UiColor.HUNGER);
        if (playerStats.comfort() > 0) {
            uiRenderer.centeredText("COMFORT " + playerStats.comfort(), framebufferWidth * 0.5f, statsY + 22.0f * uiScale, 0.9f * uiScale, UiColor.MUTED);
        }
        uiRenderer.centeredText("TIME " + dayTimeLabel(), framebufferWidth * 0.5f, statsY - 18.0f * uiScale, 1.0f * uiScale, UiColor.MUTED);
        uiRenderer.text("MODE " + gameMode.name(), Math.max(18.0f * uiScale, framebufferWidth - 150.0f * uiScale), 18.0f * uiScale, 1.0f * uiScale, UiColor.MUTED);
        org.joml.Vector3f position = camera.position();
        String biomeKey = world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z));
        String biome = biomeLabel(biomeKey);
        uiRenderer.text("BIOME " + clampText(biome, 20), 18.0f * uiScale, 18.0f * uiScale, 1.0f * uiScale, UiColor.MUTED);
        uiRenderer.text("TEMP " + temperatureLabel(biomeKey), 18.0f * uiScale, 34.0f * uiScale, 0.95f * uiScale, UiColor.MUTED);
        renderFeedbackOverlay(framebufferWidth * 0.5f, statsY - 66.0f * uiScale);
        if (headUnderwaterNow || playerStats.breath() < 20) {
            drawStatStrip("AIR", "air_full", "air_half", "air_empty", playerStats.breath(), 20, hotbarX + hotbarWidth * 0.5f - statWidth * 0.5f, statsY - 28.0f * uiScale, statWidth, 10.5f * uiScale, UiColor.WATER);
        }
        if (playerStats.armor() > 0) {
            float armorY = (headUnderwaterNow || playerStats.breath() < 20) ? statsY - 54.0f * uiScale : statsY - 28.0f * uiScale;
            drawStatStrip("ARMOR", "armor_full", "armor_half", "armor_empty", playerStats.armor(), 20, hotbarX + hotbarWidth * 0.5f - statWidth * 0.5f, armorY, statWidth, 10.5f * uiScale, UiColor.MUTED);
        }
        for (int i = 0; i < Hotbar.HOTBAR_SLOTS; i++) {
            float x = hotbarX + i * (hotbarSlotSize + hotbarGap);
            float y = hotbarY;
            Hotbar.SlotView slot = hotbar.slotView(i);
            boolean selected = i == hotbar.selectedIndex();
            drawAssetSlot(x, y, hotbarSlotSize, selected, true);
            uiRenderer.text(String.valueOf(i + 1), x + 7.0f * uiScale, y + 8.0f * uiScale, 1.05f * uiScale, UiColor.MUTED);
            if (!slot.isEmpty()) {
                drawItemIcon(slot.itemKey(), x + 14.0f * uiScale, y + 10.0f * uiScale, 34.0f * uiScale);
                if (slot.count() > 1) {
                    uiRenderer.text(String.valueOf(slot.count()), x + 40.0f * uiScale, y + 39.0f * uiScale, 1.15f * uiScale, UiColor.WHITE);
                }
                if (slot.hasDurability()) {
                    drawDurabilityBar(x + 10.0f * uiScale, y + 51.0f * uiScale, 38.0f * uiScale, slot.durabilityLeft(), slot.maxDurability());
                }
            }
        }
        if (gameState == GameState.PLAYING || gameState == GameState.CHAT) {
            renderHeldItem();
        }
        renderPickupAnimation();
        if (settings.debugOverlayEnabled()) {
            renderDebugOverlay();
        }
    }

    private void renderHeldItem() {
        hotbar.selectedItemKey().ifPresent(itemKey -> {
            float uiScale = settings.uiScale();
            float size = Math.max(78.0f * uiScale, Math.min(116.0f * uiScale, framebufferHeight * 0.15f * uiScale));
            float bob = (float) Math.sin(frameTimeSeconds * 5.0) * 1.4f * uiScale;
            float swingPhase = 1.0f - blockBreakAnimation.handSwing(frameTimeSeconds);
            float punch = (float) Math.sin(swingPhase * Math.PI);
            float x = framebufferWidth - size - 50.0f * uiScale - punch * 8.0f * uiScale;
            float y = framebufferHeight - size - 28.0f * uiScale + bob + punch * 6.0f * uiScale;
            uiRenderer.rect(x + size * 0.55f, y + size * 0.62f, size * 0.16f, size * 0.34f, new UiColor(0.70f, 0.50f, 0.34f, 0.90f));
            uiRenderer.rect(x + size * 0.49f, y + size * 0.56f, size * 0.28f, size * 0.14f, new UiColor(0.82f, 0.62f, 0.42f, 0.92f));
            drawItemIcon(itemKey, x - punch * 3.0f * uiScale, y - punch * 3.0f * uiScale, size);
        });
    }

    private void drawMeterBar(String label, int value, int max, float x, float y, UiColor fill) {
        float uiScale = settings.uiScale();
        float width = 128.0f * uiScale;
        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        float barX = label.isBlank() ? x : x + 78.0f * uiScale;
        if (!label.isBlank()) {
            uiRenderer.text(label, x, y + 2.0f * uiScale, 1.2f * uiScale, UiColor.MUTED);
        }
        uiRenderer.rect(barX, y, width, 14.0f * uiScale, UiColor.SLOT);
        uiRenderer.rect(barX + 2.0f * uiScale, y + 2.0f * uiScale, (width - 4.0f * uiScale) * ratio, 10.0f * uiScale, fill);
        if (!label.isBlank()) {
            uiRenderer.text(value + "/" + max, barX + width + 8.0f * uiScale, y + 2.0f * uiScale, 1.05f * uiScale, UiColor.WHITE);
        }
    }

    private String dayTimeLabel() {
        double elapsed = Math.max(0.0, frameTimeSeconds - worldStartTimeSeconds);
        int day = (int) (elapsed / LOCAL_DAY_LENGTH_SECONDS) + 1;
        int totalMinutes = localDayMinutes();
        int hour = totalMinutes / 60;
        int minute = totalMinutes % 60;
        return "DAY " + day + " " + dayPhaseLabel(totalMinutes) + " " + String.format(Locale.ROOT, "%02d:%02d", hour, minute);
    }

    private int localDayMinutes() {
        double elapsed = Math.max(0.0, frameTimeSeconds - worldStartTimeSeconds);
        double dayProgress = elapsed % LOCAL_DAY_LENGTH_SECONDS / LOCAL_DAY_LENGTH_SECONDS;
        return Math.floorMod((int) Math.floor(dayProgress * 24.0 * 60.0) + DAY_START_MINUTES, 24 * 60);
    }

    private boolean isNightTime() {
        int totalMinutes = localDayMinutes();
        return totalMinutes < 5 * 60 || totalMinutes >= 19 * 60;
    }

    private static String dayPhaseLabel(int totalMinutes) {
        if (totalMinutes < 5 * 60) {
            return "NIGHT";
        }
        if (totalMinutes < 7 * 60) {
            return "DAWN";
        }
        if (totalMinutes < 12 * 60) {
            return "MORNING";
        }
        if (totalMinutes < 17 * 60) {
            return "AFTERNOON";
        }
        if (totalMinutes < 19 * 60) {
            return "DUSK";
        }
        return "NIGHT";
    }

    private String temperatureLabel(String biomeKey) {
        float temperature = biomes.findByKey(biomeKey).map(BiomeType::temperature).orElse(0.65f);
        double elapsed = Math.max(0.0, frameTimeSeconds - worldStartTimeSeconds);
        double dayProgress = elapsed % LOCAL_DAY_LENGTH_SECONDS / LOCAL_DAY_LENGTH_SECONDS;
        float dayWarmth = (float) Math.sin(dayProgress * Math.PI * 2.0 - Math.PI * 0.5);
        float apparent = Math.max(0.0f, Math.min(1.0f, temperature + dayWarmth * 0.05f));
        if (apparent < 0.22f) {
            return "FREEZING";
        }
        if (apparent < 0.42f) {
            return "COLD";
        }
        if (apparent > 0.84f) {
            return "HOT";
        }
        if (apparent > 0.68f) {
            return "WARM";
        }
        return "MILD";
    }

    private void drawStatStrip(String label, String fullKey, String halfKey, String emptyKey, int value, int max, float x, float y, float width, float iconSize, UiColor accent) {
        float uiScale = settings.uiScale();
        float gap = Math.max(3.0f, iconSize * 0.22f);
        float meterWidth = iconSize * 10.0f + gap * 9.0f;
        float meterX = x + Math.max(43.0f * uiScale, width - meterWidth);
        int clampedValue = Math.max(0, Math.min(max, value));
        uiRenderer.text(label, x, y + 1.0f * uiScale, 0.72f * uiScale, UiColor.MUTED);
        uiRenderer.text(clampedValue + "/" + max, x, y + 10.5f * uiScale, 0.66f * uiScale, accent);
        drawIconMeter(fullKey, halfKey, emptyKey, clampedValue, max, meterX, y + 4.0f * uiScale, iconSize);
    }

    private void drawIconMeter(String fullKey, String halfKey, String emptyKey, int value, int max, float x, float y, float size) {
        if (sprites == null || spriteRenderer == null) {
            return;
        }
        int slots = 10;
        for (int i = 0; i < slots; i++) {
            float lower = i * (max / (float) slots);
            float upper = (i + 1) * (max / (float) slots);
            String key = value >= upper ? fullKey : value > lower ? halfKey : emptyKey;
            if (key.isBlank()) {
                continue;
            }
            float sx = x + i * (size + Math.max(3.0f, size * 0.22f));
            drawHudSpriteFit(key, sx, y, size, size);
        }
    }

    private boolean drawHudSpriteFit(String key, float x, float y, float boxWidth, float boxHeight) {
        if (sprites == null || spriteRenderer == null) {
            return false;
        }
        Optional<dev.voxelgame.client.ui.UiSprite> sprite = sprites.hud(key);
        sprite.ifPresent(value -> {
            float drawWidth = boxWidth;
            float drawHeight = boxHeight;
            if (value.pixelWidth() > 0 && value.pixelHeight() > 0) {
                if (value.pixelWidth() > value.pixelHeight()) {
                    drawHeight = boxWidth * value.pixelHeight() / (float) value.pixelWidth();
                } else if (value.pixelHeight() > value.pixelWidth()) {
                    drawWidth = boxHeight * value.pixelWidth() / (float) value.pixelHeight();
                }
            }
            spriteRenderer.sprite(value, x + (boxWidth - drawWidth) * 0.5f, y + (boxHeight - drawHeight) * 0.5f, drawWidth, drawHeight);
        });
        return sprite.isPresent();
    }

    private void drawDurabilityBar(float x, float y, float width, int value, int max) {
        if (max <= 0) {
            return;
        }
        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        float filledWidth = Math.max(ratio > 0.0f ? 2.0f : 0.0f, width * ratio);
        uiRenderer.rect(x - 1.0f, y - 1.0f, width + 2.0f, 5.0f, new UiColor(0.01f, 0.012f, 0.012f, 0.92f));
        uiRenderer.rect(x, y, width, 3.0f, new UiColor(0.10f, 0.115f, 0.105f, 0.84f));
        uiRenderer.rect(x, y, filledWidth, 3.0f, durabilityColor(ratio));
        if (ratio <= 0.18f && ratio > 0.0f) {
            uiRenderer.rect(x + filledWidth, y, width - filledWidth, 3.0f, new UiColor(0.18f, 0.04f, 0.035f, 0.42f));
        }
    }

    private static UiColor durabilityColor(float ratio) {
        if (ratio > 0.55f) {
            return new UiColor(0.42f, 0.88f, 0.46f, 0.96f);
        }
        if (ratio > 0.25f) {
            return new UiColor(0.94f, 0.72f, 0.26f, 0.96f);
        }
        return new UiColor(0.95f, 0.28f, 0.22f, 0.98f);
    }

    private void renderChatOverlay() {
        if (!settings.chatEnabled()) {
            return;
        }
        List<String> lines = chatLog.recent(gameState == GameState.CHAT ? 8 : 4);
        float uiScale = settings.uiScale();
        float lineHeight = 18.0f * uiScale;
        float y = framebufferHeight - 176.0f * uiScale - lines.size() * lineHeight;
        if (!lines.isEmpty()) {
            uiRenderer.rect(14.0f * uiScale, y - 8.0f * uiScale, Math.min(760.0f * uiScale, framebufferWidth - 28.0f * uiScale), lines.size() * lineHeight + 14.0f * uiScale, new UiColor(0.02f, 0.03f, 0.035f, 0.45f));
        }
        for (String line : lines) {
            uiRenderer.text(clampText(line, 86), 22.0f * uiScale, y, 1.55f * uiScale, UiColor.WHITE);
            y += lineHeight;
        }
        if (gameState == GameState.CHAT) {
            uiRenderer.rect(14.0f * uiScale, framebufferHeight - 42.0f * uiScale, Math.min(820.0f * uiScale, framebufferWidth - 28.0f * uiScale), 30.0f * uiScale, new UiColor(0.02f, 0.025f, 0.03f, 0.82f));
            uiRenderer.text("> " + chatDraft, 24.0f * uiScale, framebufferHeight - 34.0f * uiScale, 1.8f * uiScale, UiColor.WHITE);
        }
    }

    private void renderDebugOverlay() {
        org.joml.Vector3f position = camera.position();
        String biome = world == null ? "none" : world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z()));
        int chunkX = Math.floorDiv((int) Math.floor(position.x), ChunkPos.SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(position.z), ChunkPos.SIZE);
        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);
        int skyLight = world == null ? 0 : world.skyLightAt(blockX, blockY, blockZ);
        int blockLight = world == null ? 0 : world.blockLightAt(blockX, blockY, blockZ);
        int combinedLight = Math.max(skyLight, blockLight);
        RenderResourceTracker.Snapshot resources = RenderResourceTracker.snapshot();
        ClientNetworkStats.Snapshot network = connection == null ? ClientNetworkStats.Snapshot.offline() : connection.stats();
        String selectedItem = hotbar.selectedLabel();
        String lookingAt = "none";
        if (world != null) {
            Optional<dev.voxelgame.common.math.Raycast.Hit> hit = world.pick(position, camera.forward(), 7.0);
            if (hit.isPresent()) {
                dev.voxelgame.common.math.Raycast.Hit picked = hit.get();
                lookingAt = world.targetBlock(picked)
                        .map(block -> cozyName(block.key()) + " @ " + picked.x() + " " + picked.y() + " " + picked.z())
                        .orElse("none");
            }
        }
        uiRenderer.rect(12.0f, 12.0f, 570.0f, 256.0f, new UiColor(0.02f, 0.03f, 0.035f, 0.58f));
        uiRenderer.text("FPS " + lastFps + " FRAME " + formatMilliseconds(lastFrameMilliseconds) + " RENDER " + formatMilliseconds(lastWorldRenderMilliseconds), 22.0f, 24.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("XYZ " + Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z), 22.0f, 44.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("CHUNK " + chunkX + " " + chunkZ + " BIOME " + biomeLabel(biome), 22.0f, 64.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("RD " + settings.renderDistanceChunks() + " MB " + settings.meshBuildBudgetChunks() + " DIRTY " + (world == null ? 0 : world.dirtyChunkCount()) + " BUILT " + lastMeshBuilds, 22.0f, 84.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("LOADED " + (world == null ? 0 : world.loadedChunkCount()) + " GPU MESH " + lastRenderStats.loadedGpuMeshes() + " GPU CHUNK " + lastRenderStats.loadedChunkPositions(), 22.0f, 104.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("DRAW " + lastRenderStats.drawCalls() + " SOLID " + lastRenderStats.renderedOpaqueChunks() + " CUTOUT " + lastRenderStats.renderedCutoutChunks() + " WATER " + lastRenderStats.renderedTransparentChunks() + " CULLM " + lastRenderStats.culledMeshes() + " CULLC " + lastRenderStats.culledChunkPositions(), 22.0f, 124.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("TRIS " + formatCount(lastRenderStats.triangles()) + " VRAM " + formatMegabytes(lastRenderStats.meshBytes()) + " ENT " + lastRenderedEntities + " EDC " + lastEntityRenderStats.drawCalls() + " EP " + lastEntityRenderStats.modelParts() + " EMDL " + lastEntityRenderStats.cachedModels() + " ECULL " + lastEntityRenderStats.culledEntities() + " HITBOX " + lastRenderedEntityHitboxes, 22.0f, 144.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("GL MESH " + resources.liveChunkMeshes() + " VAO " + resources.liveChunkVertexArrays() + " BUF " + resources.liveChunkBuffers() + " MB " + formatMegabytes(resources.liveChunkMeshBytes()) + "/" + formatMegabytes(resources.peakChunkMeshBytes()) + " BORDERS " + lastChunkBorderDebugChunks, 22.0f, 164.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("PART " + lastParticleRenderStats.liveParticles() + " PDC " + lastParticleRenderStats.drawCalls() + " PTRI " + lastParticleRenderStats.triangles() + " MODE " + gameMode.name() + " GROUND " + onOff(camera.onGround()) + " LIGHT " + combinedLight + " S " + skyLight + " B " + blockLight, 22.0f, 184.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("NET " + onOff(onlineMode) + " TX " + formatCount(network.sentPackets()) + " RX " + formatCount(network.receivedPackets()) + " TX/s " + formatRate(network.sentPacketsPerSecond()) + " RX/s " + formatRate(network.receivedPacketsPerSecond()) + " CH " + formatCount(network.chunkPackets()) + " BLK " + formatCount(network.blockUpdatePackets()) + " ENT " + formatCount(network.entitySnapshotPackets()) + " INV " + formatCount(network.inventoryPackets()), 22.0f, 204.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("SEL " + clampText(selectedItem, 52), 22.0f, 224.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("LOOK " + clampText(lookingAt, 52), 22.0f, 244.0f, 1.65f, UiColor.MUTED);
    }

    private void renderBreakOverlay() {
        if (!blockBreakAnimation.active()) {
            return;
        }
        float progress = blockBreakAnimation.progress(frameTimeSeconds);
        float uiScale = settings.uiScale();
        float eased = progress * progress * (3.0f - 2.0f * progress);
        float size = (24.0f + eased * 22.0f) * uiScale;
        float cx = framebufferWidth * 0.5f;
        float cy = framebufferHeight * 0.5f;
        float x = cx - size * 0.5f;
        float y = cy - size * 0.5f;
        UiColor crack = new UiColor(0.95f, 0.88f, 0.72f, 0.24f + eased * 0.54f);
        float thickness = Math.max(1.0f, (1.0f + eased * 1.6f) * uiScale);
        uiRenderer.rect(x + size * 0.20f, y + size * 0.34f, size * 0.42f, thickness, crack);
        uiRenderer.rect(x + size * 0.48f, y + size * 0.22f, thickness, size * 0.46f, crack);
        uiRenderer.rect(x + size * 0.32f, y + size * 0.58f, size * 0.36f, thickness, crack);
        if (eased > 0.45f) {
            uiRenderer.rect(x + size * 0.22f, y + size * 0.72f, size * 0.26f, thickness, crack);
            uiRenderer.rect(x + size * 0.68f, y + size * 0.44f, thickness, size * 0.24f, crack);
        }
        if (eased > 0.78f) {
            drawHudSprite("sparkle", x + size * 0.68f, y - size * 0.12f, 26.0f * uiScale, 24.0f * uiScale);
        }
        float barWidth = 96.0f * uiScale;
        float barY = cy + 22.0f * uiScale;
        uiRenderer.rect(cx - barWidth * 0.5f, barY, barWidth, Math.max(2.0f, 3.0f * uiScale), new UiColor(0.02f, 0.025f, 0.023f, 0.58f));
        uiRenderer.rect(cx - barWidth * 0.5f, barY, barWidth * eased, Math.max(2.0f, 3.0f * uiScale), UiColor.ACCENT);
    }

    private void renderPickupAnimation() {
        if (pickupAnimationItemKey.isBlank() || frameTimeSeconds >= pickupAnimationUntil) {
            return;
        }
        float remaining = (float) Math.max(0.0, pickupAnimationUntil - frameTimeSeconds);
        float t = Math.max(0.0f, Math.min(1.0f, remaining / 0.48f));
        float uiScale = settings.uiScale();
        float size = (34.0f + (1.0f - t) * 16.0f) * uiScale;
        float x = framebufferWidth * 0.5f - size * 0.5f;
        float y = framebufferHeight * 0.5f + 36.0f * uiScale - (1.0f - t) * 46.0f * uiScale;
        drawItemIcon(pickupAnimationItemKey, x, y, size);
        uiRenderer.centeredText("+", x - 8.0f * uiScale, y + 8.0f * uiScale, 1.5f * uiScale, UiColor.ACCENT);
    }

    private void drawAssetSlot(float x, float y, float size, boolean selected, boolean hotbarSlot) {
        if (selected && hotbarSlot) {
            drawSelectedSlotPulse(x, y, size);
        }
        UiColor outer = selected
                ? UiColor.ACCENT
                : hotbarSlot ? new UiColor(0.11f, 0.17f, 0.15f, 0.96f) : new UiColor(0.075f, 0.095f, 0.092f, 0.95f);
        UiColor fill = selected
                ? UiColor.SLOT_ACTIVE
                : hotbarSlot ? new UiColor(0.055f, 0.075f, 0.070f, 0.95f) : UiColor.SLOT;
        uiRenderer.rect(x - 2.0f, y - 2.0f, size + 4.0f, size + 4.0f, new UiColor(0.012f, 0.016f, 0.015f, 0.88f));
        uiRenderer.rect(x - 1.0f, y - 1.0f, size + 2.0f, size + 2.0f, outer);
        uiRenderer.rect(x, y, size, size, fill);
        uiRenderer.rect(x + 4.0f, y + 4.0f, size - 8.0f, size - 8.0f, new UiColor(0.018f, 0.026f, 0.024f, 0.62f));
        uiRenderer.rect(x + 3.0f, y + 3.0f, size - 6.0f, 2.0f, selected ? UiColor.WHITE : new UiColor(0.20f, 0.28f, 0.25f, 0.55f));
        uiRenderer.rect(x + 3.0f, y + size - 5.0f, size - 6.0f, 2.0f, new UiColor(0.01f, 0.014f, 0.014f, 0.70f));
    }

    private void drawHotbarPanel(float x, float y, float width, float height) {
        uiRenderer.rect(x, y, width, height, new UiColor(0.012f, 0.018f, 0.017f, 0.72f));
        uiRenderer.rect(x + 4.0f, y + 4.0f, width - 8.0f, height - 8.0f, new UiColor(0.028f, 0.040f, 0.036f, 0.76f));
        uiRenderer.rect(x + 4.0f, y + 4.0f, width - 8.0f, 2.0f, new UiColor(0.30f, 0.46f, 0.35f, 0.62f));
        uiRenderer.rect(x + 4.0f, y + height - 6.0f, width - 8.0f, 2.0f, new UiColor(0.004f, 0.006f, 0.006f, 0.55f));
    }

    private void drawSelectedSlotPulse(float x, float y, float size) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(frameTimeSeconds * 5.8f);
        float inset = 4.0f + pulse * 2.0f;
        UiColor glow = new UiColor(0.55f, 0.86f, 0.48f, 0.18f + pulse * 0.18f);
        uiRenderer.rect(x - inset, y - inset, size + inset * 2.0f, 2.0f, glow);
        uiRenderer.rect(x - inset, y + size + inset - 2.0f, size + inset * 2.0f, 2.0f, glow);
        uiRenderer.rect(x - inset, y - inset, 2.0f, size + inset * 2.0f, glow);
        uiRenderer.rect(x + size + inset - 2.0f, y - inset, 2.0f, size + inset * 2.0f, glow);
    }

    private boolean drawHudSprite(String key, float x, float y, float width, float height) {
        if (sprites == null || spriteRenderer == null) {
            return false;
        }
        Optional<dev.voxelgame.client.ui.UiSprite> sprite = sprites.hud(key);
        sprite.ifPresent(value -> spriteRenderer.sprite(value, x, y, width, height));
        return sprite.isPresent();
    }

    private boolean drawHudBackdropSprite(String key, float x, float y, float width, float height) {
        return drawHudBackdropSprite(key, x, y, width, height, UiColor.WHITE);
    }

    private boolean drawHudBackdropSprite(String key, float x, float y, float width, float height, UiColor tint) {
        if (sprites == null || backgroundSpriteRenderer == null) {
            return false;
        }
        Optional<dev.voxelgame.client.ui.UiSprite> sprite = sprites.hud(key);
        sprite.ifPresent(value -> backgroundSpriteRenderer.sprite(value, x, y, width, height, tint));
        return sprite.isPresent();
    }

    private void drawAssetPanel(String spriteKey, float x, float y, float width, float height, UiColor fallbackColor) {
        if (!drawHudBackdropSprite(spriteKey, x, y, width, height)) {
            uiRenderer.rect(x, y, width, height, fallbackColor);
        }
    }

    private void drawItemIcon(String itemKey, float x, float y, float size) {
        if (sprites == null || spriteRenderer == null || itemKey == null || itemKey.isBlank()) {
            return;
        }
        sprites.item(itemKey).ifPresent(sprite -> {
            float drawWidth = size;
            float drawHeight = size;
            if (sprite.pixelWidth() > 0 && sprite.pixelHeight() > 0) {
                if (sprite.pixelWidth() > sprite.pixelHeight()) {
                    drawHeight = size * sprite.pixelHeight() / (float) sprite.pixelWidth();
                } else if (sprite.pixelHeight() > sprite.pixelWidth()) {
                    drawWidth = size * sprite.pixelWidth() / (float) sprite.pixelHeight();
                }
            }
            spriteRenderer.sprite(sprite, x + (size - drawWidth) * 0.5f, y + (size - drawHeight) * 0.5f, drawWidth, drawHeight);
        });
    }

    private void settingStepper(
            MousePosition mouse,
            boolean clicked,
            float x,
            float y,
            float width,
            String label,
            String value,
            Runnable minus,
            Runnable plus
    ) {
        uiRenderer.text(label, x, y + 13.0f, 2.0f, UiColor.WHITE);
        uiRenderer.text(value, x + width - 220.0f, y + 13.0f, 2.0f, UiColor.MUTED);
        drawButton(new UiButton(x + width - 86.0f, y, 36.0f, 36.0f, "-", true), mouse, clicked, minus);
        drawButton(new UiButton(x + width - 42.0f, y, 36.0f, 36.0f, "+", true), mouse, clicked, plus);
    }

    private void settingToggle(MousePosition mouse, boolean clicked, float x, float y, float width, String label, boolean enabled, Runnable toggle) {
        uiRenderer.text(label, x, y + 13.0f, 2.0f, UiColor.WHITE);
        drawButton(new UiButton(x + width - 170.0f, y, 128.0f, 36.0f, enabled ? "ON" : "OFF", true), mouse, clicked, toggle);
    }

    private void drawButton(UiButton button, MousePosition mouse, boolean clicked, Runnable action) {
        boolean hovered = button.contains(mouse.x(), mouse.y());
        String spriteKey = buttonSpriteKey(button.label());
        UiColor tint = button.enabled()
                ? hovered ? new UiColor(1.0f, 1.0f, 1.0f, 1.0f) : new UiColor(0.92f, 0.96f, 0.90f, 0.96f)
                : new UiColor(0.34f, 0.38f, 0.34f, 0.72f);
        if (drawHudBackdropSprite(spriteKey, button.x(), button.y(), button.width(), button.height(), tint)) {
            if (hovered && button.enabled()) {
                uiRenderer.rect(button.x() + 4.0f, button.y() + 4.0f, button.width() - 8.0f, button.height() - 8.0f, new UiColor(0.78f, 0.98f, 0.66f, 0.14f));
            }
            if (!button.enabled()) {
                uiRenderer.rect(button.x(), button.y(), button.width(), button.height(), new UiColor(0.02f, 0.025f, 0.024f, 0.42f));
            }
            float scale = Math.min(2.2f, Math.min(
                    (button.width() - 14.0f) / Math.max(1.0f, BitmapFont.textWidth(button.label(), 1.0f)),
                    (button.height() - 10.0f) / BitmapFont.textHeight(1.0f)
            ));
            uiRenderer.centeredText(
                    button.label(),
                    button.x() + button.width() * 0.5f,
                    button.y() + button.height() * 0.5f - BitmapFont.textHeight(scale) * 0.5f,
                    scale,
                    button.enabled() ? UiColor.WHITE : UiColor.MUTED
            );
        } else {
            uiRenderer.button(button, hovered);
        }
        if (button.enabled() && hovered && clicked) {
            action.run();
        }
    }

    private static String buttonSpriteKey(String label) {
        String upper = label.toUpperCase(Locale.ROOT);
        if (upper.contains("CLOSE") || upper.contains("BACK") || upper.contains("QUIT") || upper.equals("X")) {
            return "button_close";
        }
        return "button_green";
    }

    private static boolean contains(MousePosition mouse, float x, float y, float width, float height) {
        return mouse.x() >= x && mouse.x() <= x + width && mouse.y() >= y && mouse.y() <= y + height;
    }

    private void updateFps(double now) {
        framesThisSecond++;
        if (now >= nextFpsSampleTime) {
            lastFps = framesThisSecond;
            framesThisSecond = 0;
            nextFpsSampleTime = now + 1.0;
        }
    }

    private double currentTimeSeconds() {
        return frameTimeSeconds > 0.0 ? frameTimeSeconds : System.nanoTime() / 1_000_000_000.0;
    }

    private void setStatus(String message) {
        statusMessage = message;
        feedbackLog.add(message, currentTimeSeconds());
    }

    private void syncKnownRecipeUnlocks() {
        announcedRecipeUnlocks.clear();
        for (CraftingRecipe recipe : hotbar.discoveredRecipes()) {
            announcedRecipeUnlocks.add(recipe.key());
        }
    }

    private void announceNewRecipeUnlocks() {
        CraftingRecipe firstNewRecipe = null;
        for (CraftingRecipe recipe : hotbar.discoveredRecipes()) {
            if (announcedRecipeUnlocks.add(recipe.key()) && firstNewRecipe == null) {
                firstNewRecipe = recipe;
            }
        }
        if (firstNewRecipe != null) {
            setStatus("New recipe unlocked: " + firstNewRecipe.label());
        }
    }

    private boolean isShiftDown() {
        return glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
    }

    private void openSettings(GameState returnState) {
        settingsReturnState = returnState;
        gameState = GameState.SETTINGS;
        setCursorForState();
        updateWindowTitle();
    }

    private void toggleCrafting() {
        if (gameState == GameState.CRAFTING) {
            craftingSearchFocused = false;
            draggedInventorySlot = -1;
            resumeGame();
            return;
        }
        gameState = GameState.CRAFTING;
        craftingSearchFocused = false;
        draggedInventorySlot = -1;
        previousEnter = false;
        previousBackspace = false;
        setCursorForState();
        updateWindowTitle();
    }

    private void refreshPreview() {
        if (!onlineMode && world != null) {
            world.ensurePreviewAround(camera.position(), settings.previewRadiusChunks());
            worldRenderer.rebuildDirty(world, settings.ambientOcclusionEnabled(), settings.transparentWaterEnabled(), Integer.MAX_VALUE);
        }
    }

    private RenderSettings currentRenderSettings() {
        float fogEnd = Math.max(72.0f, settings.renderDistanceChunks() * 16.0f);
        return new RenderSettings(
                settings.renderDistanceChunks(),
                settings.fogEnabled(),
                settings.ambientOcclusionEnabled(),
                settings.softShadowsEnabled(),
                settings.bloomEnabled(),
                settings.bloomEnabled() ? 0.22f : 0.0f,
                fogEnd * 0.58f,
                fogEnd,
                0.52f,
                0.72f,
                0.95f
        );
    }

    private static String clampText(String text, int maxChars) {
        return text.length() <= maxChars ? text : text.substring(0, maxChars - 3) + "...";
    }

    private static String biomeLabel(String biomeKey) {
        int colon = biomeKey.indexOf(':');
        String value = colon >= 0 ? biomeKey.substring(colon + 1) : biomeKey;
        return value.replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private void startSingleplayer() {
        closeGameSession();
        onlineMode = false;
        worldStartTimeSeconds = currentTimeSeconds();
        world = new ClientWorld(connectionOptions.seed());
        hotbar.resetForNewGame();
        syncKnownRecipeUnlocks();
        world.generatePreview(settings.previewRadiusChunks());
        setCameraToSpawn();
        worldRenderer.rebuildDirty(world, settings.ambientOcclusionEnabled(), settings.transparentWaterEnabled(), Integer.MAX_VALUE);
        gameState = GameState.PLAYING;
        setStatus("Singleplayer world loaded");
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void startMultiplayer() {
        closeGameSession();
        onlineMode = true;
        worldStartTimeSeconds = currentTimeSeconds();
        world = new ClientWorld(connectionOptions.seed());
        hotbar.resetForNewGame();
        syncKnownRecipeUnlocks();
        setCameraToSpawn();
        String host = connectionOptions.host() == null ? "127.0.0.1" : connectionOptions.host();
        try {
            connection = new GameClientConnection(host, connectionOptions.port(), connectionOptions.username(), world, hotbar, playerStats, chatLog);
            connection.connect();
            gameState = GameState.PLAYING;
            setStatus("Connected to " + host + ":" + connectionOptions.port());
            setCursorForState();
            camera.resetMouseTracking();
            updateWindowTitle();
        } catch (RuntimeException e) {
            closeGameSession();
            setStatus("Connection failed: " + host + ":" + connectionOptions.port());
        }
    }

    private void resumeGame() {
        if (world == null) {
            gameState = GameState.MAIN_MENU;
        } else {
            gameState = GameState.PLAYING;
        }
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void returnToMainMenu() {
        closeGameSession();
        gameState = GameState.MAIN_MENU;
        setStatus("Ready");
        setCursorForState();
        updateWindowTitle();
    }

    private void closeGameSession() {
        if (connection != null) {
            connection.close();
            connection = null;
        }
        if (worldRenderer != null) {
            worldRenderer.clearMeshes();
        }
        hotbar.closeStorage();
        feedbackLog.clear();
        announcedRecipeUnlocks.clear();
        playerStats.respawn();
        world = null;
        onlineMode = false;
        nextMoveSendTime = 0.0;
        nextComfortScanTime = 0.0;
        nextRecipeUnlockScanTime = 0.0;
        nextToolHintTime = 0.0;
        lastComfortFeedbackValue = -1;
        discoveredRecipeStations.clear();
        discoveredRecipeStations.add(CraftingStationType.INVENTORY);
    }

    private void setCursorForState() {
        glfwSetInputMode(window, GLFW_CURSOR, gameState == GameState.PLAYING ? GLFW_CURSOR_DISABLED : GLFW_CURSOR_NORMAL);
    }

    private MousePosition mousePosition() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            DoubleBuffer x = stack.mallocDouble(1);
            DoubleBuffer y = stack.mallocDouble(1);
            glfwGetCursorPos(window, x, y);
            return new MousePosition(x.get(0), y.get(0));
        }
    }

    private enum GameState {
        MAIN_MENU,
        PLAYING,
        PAUSED,
        SETTINGS,
        CHAT,
        CRAFTING,
        STORAGE,
        DEAD
    }

    private record MousePosition(double x, double y) {
    }
}
