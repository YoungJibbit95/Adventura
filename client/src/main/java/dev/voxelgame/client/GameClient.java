package dev.voxelgame.client;

import dev.voxelgame.client.animation.AnimationChannels;
import dev.voxelgame.client.animation.AnimationClip;
import dev.voxelgame.client.animation.AnimationPlayback;
import dev.voxelgame.client.animation.AnimationSample;
import dev.voxelgame.client.animation.DamageFlash;
import dev.voxelgame.client.animation.HeldItemAnimation;
import dev.voxelgame.client.animation.PopAnimation;
import dev.voxelgame.client.animation.SlotHoverAnimation;
import dev.voxelgame.client.animation.UiPulse;
import dev.voxelgame.client.animation.UiAnimationPresets;
import dev.voxelgame.client.audio.AudioCue;
import dev.voxelgame.client.audio.AudioCueRules;
import dev.voxelgame.client.audio.GameAudio;
import dev.voxelgame.client.hud.ClientComfortSources;
import dev.voxelgame.client.hud.ComfortHudInfo;
import dev.voxelgame.client.hud.HudLayout;
import dev.voxelgame.client.hud.HudStat;
import dev.voxelgame.client.hud.InteractionHudCard;
import dev.voxelgame.client.hud.InteractionStatusCard;
import dev.voxelgame.client.hud.WorldHudInfo;
import dev.voxelgame.client.net.ClientNetworkStats;
import dev.voxelgame.client.net.GameClientConnection;
import dev.voxelgame.client.render.BlockRenderProperties;
import dev.voxelgame.client.render.ChunkBorderRenderer;
import dev.voxelgame.client.render.CozyColorPipeline;
import dev.voxelgame.client.render.LightDebugInfo;
import dev.voxelgame.client.render.RenderMaterial;
import dev.voxelgame.client.render.RenderDebugView;
import dev.voxelgame.client.render.RenderSettings;
import dev.voxelgame.client.render.RenderResourceTracker;
import dev.voxelgame.client.render.ShaderRegistry;
import dev.voxelgame.client.render.WeatherLightningController;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.assets.BlockTextureAtlas;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.render.particle.ParticleSystem;
import dev.voxelgame.client.ui.BitmapFont;
import dev.voxelgame.client.ui.GameSprites;
import dev.voxelgame.client.ui.UiButton;
import dev.voxelgame.client.ui.UiColor;
import dev.voxelgame.client.ui.UiRenderer;
import dev.voxelgame.client.ui.UiSpriteRenderer;
import dev.voxelgame.client.viewmodel.LoadingScreenViewModel;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.entity.ItemDropType;
import dev.voxelgame.common.gameplay.CampfireRules;
import dev.voxelgame.common.gameplay.CraftingStationRules;
import dev.voxelgame.common.gameplay.GameplayEvent;
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.gameplay.ProjectileItemRules;
import dev.voxelgame.common.item.CraftingCategory;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.item.CraftingStationType;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.item.Items;
import dev.voxelgame.common.math.Raycast;
import dev.voxelgame.common.net.GamePacket;
import dev.voxelgame.common.physics.BlockSurfacePhysics;
import dev.voxelgame.common.physics.CollisionShapeCache;
import dev.voxelgame.common.physics.PlayerWaterState;
import dev.voxelgame.common.registry.Registry;
import dev.voxelgame.common.world.BiomeType;
import dev.voxelgame.common.world.Biomes;
import dev.voxelgame.common.world.ChunkPos;
import dev.voxelgame.common.world.ChunkStreamingRings;
import dev.voxelgame.common.world.light.LightRules;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_F6;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_J;
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
import static org.lwjgl.glfw.GLFW.GLFW_COCOA_RETINA_FRAMEBUFFER;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetCursorPos;
import static org.lwjgl.glfw.GLFW.glfwGetFramebufferSize;
import static org.lwjgl.glfw.GLFW.glfwGetKey;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwGetMouseButton;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetFramebufferSizeCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowSizeCallback;
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
    private static final int INVENTORY_COLUMNS = 9;
    private static final int INVENTORY_MAIN_ROWS = 3;
    private static final int INVENTORY_ROWS = 4;
    private static final UiPulse SELECTED_SLOT_PULSE = UiPulse.selectedHotbarSlot();
    private static final Vector3f BLOCK_SELECT_COLOR = new Vector3f(0.95f, 0.82f, 0.42f);
    private static final Vector3f BLOCK_SELECT_BLOCKED_COLOR = new Vector3f(0.96f, 0.34f, 0.34f);
    private static final Vector3f BLOCK_MINE_PROGRESS_COLOR = new Vector3f(1.0f, 0.90f, 0.58f);
    private static final Vector3f PLACE_PREVIEW_VALID_COLOR = new Vector3f(0.38f, 0.88f, 0.48f);
    private static final Vector3f PLACE_PREVIEW_INVALID_COLOR = new Vector3f(0.96f, 0.28f, 0.28f);
    private static final Vector3f FAR_TARGET_COLOR = new Vector3f(0.95f, 0.42f, 0.34f);
    private static final int SINGLEPLAYER_SPAWN_LOAD_RADIUS_CAP = 2;
    private static final int SINGLEPLAYER_SPAWN_LOAD_BATCH_CHUNKS = 4;

    private final ConnectionOptions connectionOptions;
    private final GameSettings settings;
    private final Camera camera = new Camera();
    private final Hotbar hotbar = new Hotbar();
    private final ChatLog chatLog = new ChatLog();
    private final FeedbackLog feedbackLog = new FeedbackLog();
    private final EarlyGameMilestones earlyGameMilestones = new EarlyGameMilestones();
    private final NightSafetyPrompts nightSafetyPrompts = new NightSafetyPrompts();
    private final EnumSet<CraftingStationType> discoveredRecipeStations = EnumSet.of(CraftingStationType.INVENTORY);
    private final PlayerStats playerStats = new PlayerStats();
    private final BlockBreakAnimation blockBreakAnimation = new BlockBreakAnimation();
    private final GameAudio audio = new GameAudio();
    private final Registry<ItemType> items = Items.createDefaultRegistry();
    private final Registry<BlockType> blocks = Blocks.createDefaultRegistry();
    private final Registry<BiomeType> biomes = Biomes.createDefaultRegistry();
    private final StringBuilder chatDraft = new StringBuilder();
    private final StringBuilder craftingSearch = new StringBuilder();
    private final Set<String> announcedRecipeUnlocks = new HashSet<>();
    private final Set<String> discoveredBiomeKeys = new HashSet<>();
    private final Set<String> discoveredCreatureKeys = new HashSet<>();
    private final PopAnimation pickupPop = new PopAnimation(0.48);
    private final PopAnimation comfortPop = new PopAnimation(0.78);
    private final PopAnimation recipeUnlockPop = new PopAnimation(0.90);
    private final PopAnimation craftingSuccessPop = new PopAnimation(0.72);
    private final DamageFlash damageFlash = new DamageFlash();
    private final UiPulse survivalWarningPulse = new UiPulse(0.72, 0.0f, 0.0f, 0.04f, 0.24f);
    private final HeldItemAnimation heldItemAnimation = new HeldItemAnimation();
    private final SlotHoverAnimation slotHoverAnimation = new SlotHoverAnimation();
    private final AnimationClip craftingFailureShakeClip = UiAnimationPresets.subtleShake("crafting-failure-shake", 0.36, 6.0f);
    private final AnimationPlayback craftingFailureShake = new AnimationPlayback();
    private final AtomicReference<GamePacket.PlayerPositionSnapshot> pendingAuthoritativePlayerState = new AtomicReference<>();
    private final ConcurrentLinkedQueue<GamePacket.ProjectileImpact> pendingProjectileImpacts = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<GamePacket.GameplayEvents> pendingGameplayEvents = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<Runnable> mainThreadActions = new ConcurrentLinkedQueue<>();
    private final AtomicInteger loadingSession = new AtomicInteger();
    private final WeatherLightningController weatherLightning = new WeatherLightningController();
    private GameMode gameMode = GameMode.SURVIVAL;
    private GameState gameState = GameState.MAIN_MENU;
    private volatile LoadingScreenViewModel loadingScreen = LoadingScreenViewModel.boot();
    private Runnable pendingLoadingAction;
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
    private int windowWidth = 1280;
    private int windowHeight = 720;
    private boolean previousLeftMouse;
    private boolean previousRightMouse;
    private boolean previousEscape;
    private boolean previousChat;
    private boolean previousSlash;
    private boolean previousEnter;
    private boolean previousBackspace;
    private boolean previousF3;
    private boolean previousCrafting;
    private boolean previousJournal;
    private boolean previousHudToggle;
    private boolean previousModeCycle;
    private boolean previousDebugViewCycle;
    private boolean previousSettingsKey;
    private boolean previousSpawnKey;
    private boolean onlineMode;
    private long nextMovementSequence = 1L;
    private long lastAuthoritativeMovementSequence;
    private double nextMoveSendTime;
    private double nextFpsSampleTime;
    private int framesThisSecond;
    private int lastFps;
    private WorldRenderer.RenderStats lastRenderStats = new WorldRenderer.RenderStats(0, 0);
    private String statusMessage = "Ready";
    private GameState settingsReturnState = GameState.MAIN_MENU;
    private GameState journalReturnState = GameState.PLAYING;
    private double nextBlockActionTime;
    private double pendingScrollY;
    private double frameTimeSeconds;
    private double worldStartTimeSeconds;
    private double lastFrameMilliseconds;
    private double lastWorldRenderMilliseconds;
    private double lastUiMilliseconds;
    private double lastInputPhaseMilliseconds;
    private double lastNetworkPhaseMilliseconds;
    private double lastPlayerPhaseMilliseconds;
    private double lastWorldPhaseMilliseconds;
    private double lastChunkJobsPhaseMilliseconds;
    private double lastGpuUploadPhaseMilliseconds;
    private double lastRenderPassPhaseMilliseconds;
    private EngineFrameStats engineFrameStats = EngineFrameStats.empty();
    private int lastMeshBuilds;
    private int lastUnloadedChunks;
    private int lastReleasedGpuMeshLayers;
    private EntityRenderer.RenderStats lastEntityRenderStats = EntityRenderer.RenderStats.empty();
    private ParticleSystem.RenderStats lastParticleRenderStats = ParticleSystem.RenderStats.empty();
    private int lastRenderedEntityHitboxes;
    private int lastChunkBorderDebugChunks;
    private int lastMeshBoundsDebugBoxes;
    private int lastSectionBoundsDebugBoxes;
    private int lastParticleBoundsDebugBoxes;
    private int lastCollisionShapeDebugBoxes;
    private int lastProjectileSweepDebugBoxes;
    private CraftingCategory craftingCategoryFilter;
    private boolean craftableRecipesOnly;
    private boolean craftingSearchFocused;
    private boolean inventoryTrashMode;
    private float settingsLayoutScale = 1.0f;
    private int lastSurvivalHealth = 20;
    private int lastSurvivalHunger = 20;
    private int lastSurvivalStamina = 20;
    private int lastSurvivalBreath = 20;
    private double healthRegenPulseUntil;
    private double sprintBlockedUntil;
    private double nextLowHungerFeedbackTime;
    private double nextLowBreathFeedbackTime;
    private double nextLowHealthFeedbackTime;
    private int draggedInventorySlot = -1;
    private InventoryDragSource draggedInventorySource = InventoryDragSource.PLAYER;
    private int clientTransactionId;
    private JournalTab journalTab = JournalTab.NOTES;
    private boolean previousUnderwater;
    private boolean headUnderwaterNow;
    private double nextAmbientParticleSourceScanTime;
    private double nextStepAudioTime;
    private double nextAmbientAudioTime;
    private double nextLandingFeedbackTime;
    private double nextToolHintTime;
    private double nextComfortScanTime;
    private double nextRecipeUnlockScanTime;
    private double nextCampfireReadyFeedbackScanTime;
    private ComfortHudInfo.Level lastComfortFeedbackLevel = ComfortHudInfo.Level.NONE;
    private String lastCampfireReadySignature = "";
    private String lastHudBiomeKey = "";
    private double nextBiomeFeedbackTime;
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

        // macOS compatibility
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("mac")) {
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            glfwWindowHint(GLFW_COCOA_RETINA_FRAMEBUFFER, GLFW_TRUE);
        }

        window = glfwCreateWindow(1920, 1080, "Adventura", 0, 0);
        if (window == 0) {
            throw new IllegalStateException("Failed to create GLFW window");
        }

        glfwMakeContextCurrent(window);
        glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        glfwShowWindow(window);
        GL.createCapabilities();

        // Query actual sizes once so Retina framebuffers start with correct UI and viewport dimensions.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var fbWidthBuf = stack.mallocInt(1);
            var fbHeightBuf = stack.mallocInt(1);
            var winWidthBuf = stack.mallocInt(1);
            var winHeightBuf = stack.mallocInt(1);

            glfwGetFramebufferSize(window, fbWidthBuf, fbHeightBuf);
            glfwGetWindowSize(window, winWidthBuf, winHeightBuf);

            framebufferWidth = Math.max(1, fbWidthBuf.get(0));
            framebufferHeight = Math.max(1, fbHeightBuf.get(0));
            windowWidth = Math.max(1, winWidthBuf.get(0));
            windowHeight = Math.max(1, winHeightBuf.get(0));
        }

        glEnable(GL_DEPTH_TEST);
        glViewport(0, 0, framebufferWidth, framebufferHeight);
        glfwSetFramebufferSizeCallback(window, (handle, width, height) -> {
            framebufferWidth = Math.max(1, width);
            framebufferHeight = Math.max(1, height);
            glViewport(0, 0, framebufferWidth, framebufferHeight);
        });
        glfwSetWindowSizeCallback(window, (handle, width, height) -> {
            windowWidth = Math.max(1, width);
            windowHeight = Math.max(1, height);
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
        } else {
            beginLoading(LoadingScreenViewModel.boot(), () -> {
                gameState = GameState.MAIN_MENU;
                setCursorForState();
                updateWindowTitle();
            });
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
            runMainThreadActions();
            long updateStartNanos = System.nanoTime();
            long phaseStartNanos = updateStartNanos;

            boolean leftMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
            boolean rightMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;
            boolean leftClicked = leftMouse && !previousLeftMouse;
            boolean leftReleased = !leftMouse && previousLeftMouse;
            boolean rightClicked = rightMouse && !previousRightMouse;
            MousePosition mouse = mousePosition();

            handleEscape();
            handleGlobalKeys();
            lastInputPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;
            lastNetworkPhaseMilliseconds = 0.0;
            lastPlayerPhaseMilliseconds = 0.0;
            lastWorldPhaseMilliseconds = 0.0;

            if (gameState == GameState.PLAYING) {
                phaseStartNanos = System.nanoTime();
                consumeAuthoritativePlayerState();
                consumeProjectileImpacts();
                consumeGameplayEvents();
                lastNetworkPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;

                phaseStartNanos = System.nanoTime();
                boolean moving = camera.hasMovementInput(window);
                boolean wantsSprint = camera.wantsSprint(window);
                boolean sprinting = wantsSprint && moving && playerStats.canSprint();
                camera.update(window, deltaSeconds, settings.mouseSensitivity(), world, gameMode, playerStats.canSprint());
                float fallImpact = camera.consumeFallImpactSpeed();
                PlayerWaterState water = world == null ? new PlayerWaterState(false, false, false) : world.playerWaterState(camera.position());
                if (gameMode == GameMode.SURVIVAL && fallImpact > 13.0f) {
                    playerStats.hurt(Math.round((fallImpact - 12.0f) * 0.55f));
                }
                emitLandingFeedback(fallImpact, water.movementAffected(), now);
                headUnderwaterNow = water.headUnderwater();
                audio.setUnderwater(headUnderwaterNow);
                refreshLocalComfort(now);
                playerStats.tick(deltaSeconds, gameMode, headUnderwaterNow, sprinting, moving);
                refreshSurvivalHudSignals(now, wantsSprint && moving);
                emitComfortFeedback();
                emitRecipeUnlockFeedback(now);
                refreshJournalDiscoveries(now);
                emitNightSafetyFeedback(now);
                handleDeathIfNeeded();
                emitWaterSplashIfNeeded(water.movementAffected(), now);
                emitMovementAudio(moving, sprinting, water.movementAffected(), now);
                lastPlayerPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;

                phaseStartNanos = System.nanoTime();
                if (!onlineMode) {
                    ChunkStreamingRings rings = chunkStreamingRings();
                    world.tickCampfires(now);
                    emitCampfireReadyFeedback(now);
                    world.ensurePreviewAround(
                            camera.position(),
                            rings.previewRadiusChunks(),
                            settings.effectiveChunkGenerationBudgetChunks(lastFrameMilliseconds),
                            settings.effectiveChunkGenerationBudgetMilliseconds(lastFrameMilliseconds)
                    );
                    int releaseSafeUnloadBudget = Math.min(settings.chunkUnloadBudgetChunks(), settings.gpuReleaseBudgetChunks());
                    List<ChunkPos> unloadedChunks = world.unloadOutside(camera.position(), rings.retainRadiusChunks(), releaseSafeUnloadBudget);
                    WorldRenderer.MeshReleaseStats releaseStats = worldRenderer.releaseChunks(unloadedChunks);
                    lastUnloadedChunks = unloadedChunks.size();
                    lastReleasedGpuMeshLayers = releaseStats.releasedLayers();
                } else {
                    lastUnloadedChunks = 0;
                    lastReleasedGpuMeshLayers = 0;
                    emitCampfireReadyFeedback(now);
                    sendMovementIfDue(now);
                }
                if (hotbar.updateSelection(window) || consumeHotbarScroll()) {
                    audio.play(AudioCue.INVENTORY_CLICK);
                    updateWindowTitle();
                }
                handleBlockInteraction(leftMouse, rightClicked, now);
                lastWorldPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;
            } else if (gameState == GameState.CHAT) {
                phaseStartNanos = System.nanoTime();
                handleChatInput();
                lastPlayerPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;
            } else if (gameState == GameState.CRAFTING) {
                phaseStartNanos = System.nanoTime();
                handleCraftingSearchInput();
                lastPlayerPhaseMilliseconds = (System.nanoTime() - phaseStartNanos) / 1_000_000.0;
            }
            if (gameState == GameState.PLAYING || gameState == GameState.CRAFTING) {
                phaseStartNanos = System.nanoTime();
                emitAmbientParticles(now);
                emitAmbientAudio(now);
                updateWeatherLightning(now);
                lastWorldPhaseMilliseconds += (System.nanoTime() - phaseStartNanos) / 1_000_000.0;
            }
            double updateMilliseconds = (System.nanoTime() - updateStartNanos) / 1_000_000.0;

            RenderSettings renderSettings = currentRenderSettings();
            glClearColor(renderSettings.skyR(), renderSettings.skyG(), renderSettings.skyB(), 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            int visibleEntitySnapshotCount = 0;
            boolean renderWorldThisFrame = world != null && gameState != GameState.LOADING;
            if (renderWorldThisFrame) {
                long renderStartNanos = System.nanoTime();
                long chunkJobsStartNanos = renderStartNanos;
                lastMeshBuilds = worldRenderer.rebuildDirty(
                        world,
                        settings.ambientOcclusionEnabled(),
                        settings.transparentWaterEnabled(),
                        settings.meshBuildBudgetChunks(),
                        camera.position(),
                        settings.effectiveMeshBuildBudgetMilliseconds(lastFrameMilliseconds),
                        settings.effectiveGpuUploadBudgetMilliseconds(lastFrameMilliseconds),
                        chunkStreamingRings().renderRadiusChunks(),
                        chunkStreamingRings().previewRadiusChunks(),
                        settings.greedyMeshingEnabled()
                );
                lastChunkJobsPhaseMilliseconds = (System.nanoTime() - chunkJobsStartNanos) / 1_000_000.0;
                lastGpuUploadPhaseMilliseconds = world.buildQueueStats().lastGpuUploadMilliseconds();
                long renderPassStartNanos = System.nanoTime();
                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(settings.fieldOfViewDegrees()),
                        (float) framebufferWidth / framebufferHeight,
                        0.05f,
                        1200.0f
                );
                Matrix4f view = camera.viewMatrix();
                List<EntitySnapshot> visibleEntities = world.visibleEntities(now);
                visibleEntitySnapshotCount = visibleEntities.size();
                lastRenderStats = worldRenderer.render(projection, view, world, camera.position(), renderSettings, now);
                lastEntityRenderStats = entityRenderer.renderDetailed(projection, view, visibleEntities, now, renderSettings, world, camera.position());
                emitEntityParticles(visibleEntities, now);
                particleSystem.setQuality(settings.particleQuality());
                lastParticleRenderStats = particleSystem.render(projection, view, now);
                renderSelectionVisuals(projection, view, now);
                lastChunkBorderDebugChunks = settings.debugChunkBordersEnabled()
                        ? chunkBorderRenderer.render(projection, view, camera.position(), world.dimension(), settings.renderDistanceChunks())
                        : 0;
                lastMeshBoundsDebugBoxes = settings.debugMeshBoundsEnabled()
                        ? chunkBorderRenderer.renderMeshBounds(projection, view, worldRenderer.meshBounds())
                        : 0;
                lastSectionBoundsDebugBoxes = settings.debugSectionBoundsEnabled()
                        ? chunkBorderRenderer.renderSectionBounds(projection, view, world.sectionBoundsAround(camera.position(), settings.renderDistanceChunks()))
                        : 0;
                lastParticleBoundsDebugBoxes = settings.debugParticleBoundsEnabled()
                        ? chunkBorderRenderer.renderParticleBounds(projection, view, particleSystem.particleBounds())
                        : 0;
                lastCollisionShapeDebugBoxes = settings.debugOverlayEnabled()
                        ? chunkBorderRenderer.renderCollisionShapeBounds(projection, view, world.collisionShapeBoundsAround(camera.position(), 7))
                        : 0;
                lastProjectileSweepDebugBoxes = settings.debugOverlayEnabled()
                        ? chunkBorderRenderer.renderProjectileSweepBounds(projection, view, world.projectileSweepBoundsAround(camera.position(), 32, now))
                        : 0;
                lastRenderedEntityHitboxes = settings.debugOverlayEnabled()
                        ? chunkBorderRenderer.renderEntityHitboxes(projection, view, visibleEntities)
                        : 0;
                lastRenderPassPhaseMilliseconds = (System.nanoTime() - renderPassStartNanos) / 1_000_000.0;
                lastWorldRenderMilliseconds = (System.nanoTime() - renderStartNanos) / 1_000_000.0;
            } else {
                lastMeshBuilds = 0;
                lastUnloadedChunks = 0;
                lastReleasedGpuMeshLayers = 0;
                lastEntityRenderStats = EntityRenderer.RenderStats.empty();
                lastParticleRenderStats = ParticleSystem.RenderStats.empty();
                lastRenderedEntityHitboxes = 0;
                lastChunkBorderDebugChunks = 0;
                lastMeshBoundsDebugBoxes = 0;
                lastSectionBoundsDebugBoxes = 0;
                lastParticleBoundsDebugBoxes = 0;
                lastCollisionShapeDebugBoxes = 0;
                lastProjectileSweepDebugBoxes = 0;
                lastWorldRenderMilliseconds = 0.0;
                lastChunkJobsPhaseMilliseconds = 0.0;
                lastGpuUploadPhaseMilliseconds = 0.0;
                lastRenderPassPhaseMilliseconds = 0.0;
                lastRenderStats = new WorldRenderer.RenderStats(0, 0);
                previousUnderwater = false;
            }

            engineFrameStats = EngineFrameStats.capture(
                    lastFps,
                    lastFrameMilliseconds,
                    updateMilliseconds,
                    lastWorldRenderMilliseconds,
                    lastUiMilliseconds,
                    new EngineFrameStats.FramePhases(
                            lastInputPhaseMilliseconds,
                            lastNetworkPhaseMilliseconds,
                            lastPlayerPhaseMilliseconds,
                            lastWorldPhaseMilliseconds,
                            lastChunkJobsPhaseMilliseconds,
                            lastGpuUploadPhaseMilliseconds,
                            lastRenderPassPhaseMilliseconds,
                            lastUiMilliseconds
                    ),
                    settings,
                    chunkRetentionRadiusChunks(),
                    world,
                    lastRenderStats,
                    lastMeshBuilds,
                    lastUnloadedChunks,
                    lastReleasedGpuMeshLayers,
                    visibleEntitySnapshotCount,
                    lastEntityRenderStats,
                    lastRenderedEntityHitboxes,
                    lastChunkBorderDebugChunks,
                    lastMeshBoundsDebugBoxes,
                    lastSectionBoundsDebugBoxes,
                    lastParticleBoundsDebugBoxes,
                    lastParticleRenderStats,
                    onlineMode,
                    connection == null ? ClientNetworkStats.Snapshot.offline() : connection.stats(world == null ? 0 : world.dirtyChunkCount()),
                    RenderResourceTracker.snapshot()
            );

            long uiStartNanos = System.nanoTime();
            uiRenderer.begin();
            backgroundSpriteRenderer.begin();
            spriteRenderer.begin();
            if (gameState == GameState.MAIN_MENU) {
                renderMainMenu(mouse, leftClicked);
            } else if (gameState == GameState.LOADING) {
                renderLoadingScreen(loadingScreen, now);
            } else if (gameState == GameState.PAUSED) {
                renderPauseMenu(mouse, leftClicked);
            } else if (gameState == GameState.SETTINGS) {
                renderSettingsMenu(mouse, leftClicked);
            } else if (gameState == GameState.CRAFTING) {
                renderCraftingScreen(mouse, leftClicked, leftReleased, rightClicked);
            } else if (gameState == GameState.JOURNAL) {
                renderJournalScreen(mouse, leftClicked);
            } else if (gameState == GameState.STORAGE) {
                renderStorageScreen(mouse, leftClicked, leftReleased, rightClicked);
            } else if (gameState == GameState.DEAD) {
                renderDeathScreen(mouse, leftClicked);
            }
            if (gameState != GameState.LOADING) {
                renderHud(mouse);
                renderChatOverlay();
            }
            backgroundSpriteRenderer.flush(framebufferWidth, framebufferHeight, windowWidth, windowHeight);
            uiRenderer.flush(framebufferWidth, framebufferHeight, windowWidth, windowHeight);
            spriteRenderer.flush(framebufferWidth, framebufferHeight, windowWidth, windowHeight);
            lastUiMilliseconds = (System.nanoTime() - uiStartNanos) / 1_000_000.0;

            glfwSwapBuffers(window);
            runPendingLoadingAction();
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
            if (tryShootProjectile(now)) {
                return;
            }
            String foodLabel = hotbar.selectedLabel();
            if (hotbar.useSelectedFood(playerStats)) {
                setStatus("Ate " + foodLabel);
                nextBlockActionTime = now + 0.22;
                heldItemAnimation.eat(now);
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
                        heldItemAnimation.use(now);
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
                        heldItemAnimation.use(now);
                        updateWindowTitle();
                    } else {
                        handleLocalBlockInteract(hit, now);
                    }
                    return;
                }
                short blockId = placeBlockId.get();
                org.joml.Vector3f eyePosition = camera.position();
                if (InteractionRules.placementIntersectsPlayer(eyePosition.x, eyePosition.y, eyePosition.z, hit.placeX(), hit.placeY(), hit.placeZ(), blockId)) {
                    setStatus("Too close to place");
                    nextBlockActionTime = now + 0.10;
                    return;
                }
                if (world.placementIntersectsVisibleEntity(hit.placeX(), hit.placeY(), hit.placeZ(), blockId, now)) {
                    setStatus("Blocked by entity");
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
                    heldItemAnimation.place(now);
                } else {
                    if (world.placeBlock(hit, blockId, eyePosition)) {
                        hotbar.consumeSelectedOne();
                        String placedKey = blockKey(blockId);
                        setStatus(earlyGameMilestones.placeBlock(placedKey)
                                .orElse("Placed " + cozyName(placedKey)));
                        nextBlockActionTime = now + 0.12;
                        heldItemAnimation.place(now);
                        audio.play(AudioCue.BLOCK_PLACE);
                    } else {
                        setStatus("Can't place there");
                        nextBlockActionTime = now + 0.10;
                    }
                }
            });
        }
    }

    private void renderSelectionVisuals(Matrix4f projection, Matrix4f view, double now) {
        if (gameState != GameState.PLAYING || world == null || chunkBorderRenderer == null) {
            return;
        }
        Optional<Raycast.Hit> reachable = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (reachable.isPresent()) {
            Raycast.Hit hit = reachable.get();
            Optional<BlockType> target = world.targetBlock(hit);
            if (target.isPresent()) {
                boolean canHarvest = gameMode == GameMode.CREATIVE || hotbar.canHarvestSelected(target.get());
                Vector3f outline = canHarvest ? BLOCK_SELECT_COLOR : BLOCK_SELECT_BLOCKED_COLOR;
                chunkBorderRenderer.renderBlockOutline(projection, view, hit.x(), hit.y(), hit.z(), outline, 0.70f, true);
                if (blockBreakAnimation.active()) {
                    chunkBorderRenderer.renderMiningFaceProgress(
                            projection,
                            view,
                            hit,
                            blockBreakAnimation.progress(now),
                            BLOCK_MINE_PROGRESS_COLOR,
                            0.78f
                    );
                }
            }
            hotbar.selectedPlaceBlockId().ifPresent(blockId -> {
                PlacementPreview preview = placementPreview(hit, blockId, now);
                Vector3f color = preview.valid() ? PLACE_PREVIEW_VALID_COLOR : PLACE_PREVIEW_INVALID_COLOR;
                chunkBorderRenderer.renderBlockOutline(
                        projection,
                        view,
                        hit.placeX(),
                        hit.placeY(),
                        hit.placeZ(),
                        color,
                        preview.valid() ? 0.48f : 0.62f,
                        true
                );
            });
            return;
        }
        Optional<Raycast.Hit> far = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH + 3.0);
        far.ifPresent(hit -> chunkBorderRenderer.renderBlockOutline(
                projection,
                view,
                hit.x(),
                hit.y(),
                hit.z(),
                FAR_TARGET_COLOR,
                0.34f,
                true
        ));
    }

    private PlacementPreview placementPreview(Raycast.Hit hit, short blockId, double now) {
        if (!world.dimension().containsY(hit.placeY())) {
            return PlacementPreview.invalid("height");
        }
        Optional<BlockType> placed = blocks.findById(blockId);
        if (placed.isEmpty() || blockId == Blocks.AIR || blockId == Blocks.WATER) {
            return PlacementPreview.invalid("block");
        }
        short currentId = world.blockIdAt(hit.placeX(), hit.placeY(), hit.placeZ());
        if (currentId != Blocks.AIR && currentId != Blocks.WATER) {
            return PlacementPreview.invalid("occupied");
        }
        Vector3f eye = camera.position();
        if (InteractionRules.placementIntersectsPlayer(eye.x, eye.y, eye.z, hit.placeX(), hit.placeY(), hit.placeZ(), blockId)) {
            return PlacementPreview.invalid("player");
        }
        if (world.placementIntersectsVisibleEntity(hit.placeX(), hit.placeY(), hit.placeZ(), blockId, now)) {
            return PlacementPreview.invalid("entity");
        }
        return PlacementPreview.allowed();
    }

    private record PlacementPreview(boolean valid, String reason) {
        static PlacementPreview allowed() {
            return new PlacementPreview(true, "");
        }

        static PlacementPreview invalid(String reason) {
            return new PlacementPreview(false, reason);
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
        heldItemAnimation.breakSwing(now, multiplier);
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
            return;
        }
        if (world.breakBlock(hit)) {
            particleSystem.spawnBlockBreak(target, hit, now);
            collectDrops(target, multiplier, now);
            hotbar.damageSelectedTool(target);
            nextBlockActionTime = now + 0.10;
            announceNewRecipeUnlocks();
            audio.play(breakCueFor(target));
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
                setStatus(earlyGameMilestones.lightCampfire().orElse("Campfire fueled"));
                nextBlockActionTime = now + 0.25;
                heldItemAnimation.use(now);
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
                setStatus(earlyGameMilestones.collectItem(interaction.itemKey(), cozyName(interaction.itemKey()))
                        .orElse(interaction.message()));
                announceNewRecipeUnlocks();
                nextBlockActionTime = now + interaction.cooldownSeconds();
                heldItemAnimation.use(now);
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
            connection.send(new GamePacket.StorageOpenRequest(hit.x(), hit.y(), hit.z(), nextClientTransactionId()));
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
                : GamePacket.EntityInteract.Action.ATTACK;
        if (onlineMode) {
            connection.send(new GamePacket.EntityInteract(target.entityId(), hotbar.selectedIndex(), action));
            setStatus(action == GamePacket.EntityInteract.Action.FEED ? "Feed requested" : "Entity attacked");
        } else {
            setStatus(action == GamePacket.EntityInteract.Action.FEED
                    ? cozyName(target.typeKey()) + " seems interested"
                    : cozyName(target.typeKey()) + " attacked");
        }
        nextBlockActionTime = now + 0.22;
        heldItemAnimation.use(now);
        updateWindowTitle();
    }

    private boolean tryShootProjectile(double now) {
        if (hotbar.selectedItemKey().filter(ProjectileItemRules::canLaunchKey).isEmpty()) {
            return false;
        }
        if (onlineMode) {
            connection.send(new GamePacket.ProjectileShoot(hotbar.selectedIndex(), nextMovementSequence));
            setStatus("Projectile launched");
        } else {
            setStatus("Projectile tools need server authority");
        }
        nextBlockActionTime = now + 0.24;
        heldItemAnimation.use(now);
        audio.play(AudioCue.PROJECTILE_SHOOT);
        updateWindowTitle();
        return true;
    }

    private void handleProjectileImpact(GamePacket.ProjectileImpact impact) {
        double now = currentTimeSeconds();
        if (particleSystem != null) {
            particleSystem.spawnProjectileImpact(impact, now);
        }
        audio.play(impact.hitType() == dev.voxelgame.common.physics.ProjectileHit.Type.ENTITY
                ? AudioCue.PROJECTILE_HIT_ENTITY
                : AudioCue.PROJECTILE_HIT);
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

    private void refreshSurvivalHudSignals(double now, boolean wantsSprint) {
        int health = playerStats.health();
        int hunger = playerStats.hunger();
        int stamina = playerStats.stamina();
        int breath = playerStats.breath();
        if (health < lastSurvivalHealth) {
            damageFlash.trigger(lastSurvivalHealth - health, now);
        } else if (health > lastSurvivalHealth) {
            healthRegenPulseUntil = now + 1.2;
        }
        if (health <= 6 && lastSurvivalHealth > 6 && now >= nextLowHealthFeedbackTime) {
            feedbackLog.add("Health critical", now, FeedbackLog.Kind.WARNING);
            nextLowHealthFeedbackTime = now + 8.0;
        }
        if (hunger <= 5 && (lastSurvivalHunger > 5 || now >= nextLowHungerFeedbackTime)) {
            feedbackLog.add(hunger <= 0 ? "Starving" : "Low hunger", now, FeedbackLog.Kind.WARNING);
            nextLowHungerFeedbackTime = now + 9.0;
        }
        if (breath <= 6 && (headUnderwaterNow || lastSurvivalBreath > 6) && now >= nextLowBreathFeedbackTime) {
            feedbackLog.add("Air running out", now, FeedbackLog.Kind.WARNING);
            nextLowBreathFeedbackTime = now + 4.5;
        }
        if (wantsSprint && !playerStats.canSprint()) {
            sprintBlockedUntil = now + 0.85;
            if (stamina <= 2 && lastSurvivalStamina > 2) {
                feedbackLog.add("Too tired to sprint", now, FeedbackLog.Kind.WARNING);
            }
            if (hunger <= 0 && lastSurvivalHunger > 0) {
                feedbackLog.add("Too hungry to sprint", now, FeedbackLog.Kind.WARNING);
            }
        }
        lastSurvivalHealth = health;
        lastSurvivalHunger = hunger;
        lastSurvivalStamina = stamina;
        lastSurvivalBreath = breath;
    }

    private void resetSurvivalHudSignals() {
        lastSurvivalHealth = playerStats.health();
        lastSurvivalHunger = playerStats.hunger();
        lastSurvivalStamina = playerStats.stamina();
        lastSurvivalBreath = playerStats.breath();
        healthRegenPulseUntil = 0.0;
        sprintBlockedUntil = 0.0;
        nextLowHungerFeedbackTime = 0.0;
        nextLowBreathFeedbackTime = 0.0;
        nextLowHealthFeedbackTime = 0.0;
        damageFlash.clear();
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
        resetSurvivalHudSignals();
        setCameraToSpawn();
        gameState = GameState.PLAYING;
        setStatus("Respawned");
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void emitComfortFeedback() {
        ComfortHudInfo info = currentComfortHudInfo();
        ComfortHudInfo.Level level = info.level();
        if (lastComfortFeedbackLevel == ComfortHudInfo.Level.NONE && level == ComfortHudInfo.Level.NONE) {
            return;
        }
        if (level.ordinal() > lastComfortFeedbackLevel.ordinal()) {
            feedbackLog.add(info.feedbackLine(), currentTimeSeconds(), FeedbackLog.Kind.COMFORT);
            comfortPop.trigger("comfort", currentTimeSeconds());
        }
        lastComfortFeedbackLevel = level;
    }

    private void emitRecipeUnlockFeedback(double now) {
        if (world == null || now < nextRecipeUnlockScanTime) {
            return;
        }
        nextRecipeUnlockScanTime = now + 1.0;
        announceNewRecipeUnlocks();
        for (CraftingStationType stationType : currentCraftingStations()) {
            if (stationType != CraftingStationType.INVENTORY && discoveredRecipeStations.add(stationType)) {
                setStatus(CraftingFeedback.stationUnlockMessage(stationType));
                recipeUnlockPop.trigger(stationType.name(), now);
            }
        }
    }

    private void emitCampfireReadyFeedback(double now) {
        if (world == null || now < nextCampfireReadyFeedbackScanTime) {
            return;
        }
        nextCampfireReadyFeedbackScanTime = now + 0.75;
        Optional<ClientWorld.BlockPos> campfire = world.nearestActiveCampfireWithin(camera.position(), CampfireRules.STATION_RADIUS_BLOCKS, now);
        if (campfire.isEmpty()) {
            lastCampfireReadySignature = "";
            return;
        }
        ClientWorld.BlockPos pos = campfire.get();
        Optional<ClientWorld.CampfireStatusView> status = world.campfireStatusAt(pos, now);
        if (status.isEmpty() || !status.get().cooking() || status.get().cookProgress() < 0.99f) {
            lastCampfireReadySignature = "";
            return;
        }
        String signature = pos.x() + ":" + pos.y() + ":" + pos.z() + ":" + status.get().cookingRecipeKey();
        if (signature.equals(lastCampfireReadySignature)) {
            return;
        }
        lastCampfireReadySignature = signature;
        feedbackLog.add("Campfire output ready: " + recipeLabel(status.get().cookingRecipeKey()), now, FeedbackLog.Kind.SUCCESS);
    }

    private void emitNightSafetyFeedback(double now) {
        if (world == null || gameMode != GameMode.SURVIVAL) {
            return;
        }
        boolean activeCampfireNearby = world.hasActiveCampfireWithin(camera.position(), 10, now);
        nightSafetyPrompts.update(localDayNumber(), localDayMinutes(), activeCampfireNearby).ifPresent(message -> {
            setStatus(message);
            if (activeCampfireNearby) {
                comfortPop.trigger("night-campfire", now);
            }
        });
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
            pickupPop.trigger(drop, now);
            setStatus(earlyGameMilestones.collectItem(drop, cozyName(drop))
                    .orElse("Gathered " + cozyName(drop)));
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
            nextAmbientParticleSourceScanTime = now + settings.ambientParticleSourceScanIntervalSeconds();
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
            particleSystem.spawnProjectileTrail(entity, now);
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

    private void emitLandingFeedback(float impactSpeed, boolean waterAffected, double now) {
        if (impactSpeed < 13.0f || waterAffected || now < nextLandingFeedbackTime) {
            return;
        }
        if (particleSystem != null) {
            particleSystem.spawnLandingDust(camera.position(), impactSpeed, now);
        }
        audio.play(AudioCue.HARD_LANDING);
        nextLandingFeedbackTime = now + 0.35;
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

    private void updateWeatherLightning(double now) {
        if (world == null) {
            return;
        }
        Vector3f position = camera.position();
        int x = (int) Math.floor(position.x);
        int z = (int) Math.floor(position.z);
        String biomeKey = world.biomeKeyAt(x, z);
        BiomeType biome = biomes.findByKey(biomeKey).orElse(null);
        WeatherLightningController.WeatherLightningSample sample = weatherLightning.update(
                now,
                connectionOptions.seed(),
                localDayMinutes(),
                biomeKey,
                biome
        );
        if (sample.thunderCue()) {
            audio.play(AudioCue.THUNDER);
        }
    }

    private static double breakDelay(BlockType target, float multiplier) {
        return InteractionRules.breakDelaySeconds(target, multiplier);
    }

    private static AudioCue breakCueFor(BlockType target) {
        return AudioCueRules.breakCueFor(target);
    }

    private EnumSet<CraftingStationType> currentCraftingStations() {
        EnumSet<CraftingStationType> stations = EnumSet.of(CraftingStationType.INVENTORY);
        if (world == null) {
            return stations;
        }
        if (world.hasActiveCampfireWithin(camera.position(), CraftingStationRules.STATION_RADIUS_BLOCKS, frameTimeSeconds)) {
            stations.add(CraftingStationType.CAMPFIRE);
        }
        if (world.nearestBlockWithin(camera.position(), Blocks.COOKING_POT, CraftingStationRules.STATION_RADIUS_BLOCKS).isPresent()) {
            stations.add(CraftingStationType.COOKING_POT);
        }
        if (world.nearestBlockWithin(camera.position(), Blocks.WORKBENCH, CraftingStationRules.STATION_RADIUS_BLOCKS).isPresent()) {
            stations.add(CraftingStationType.WORKBENCH);
        }
        if (world.nearestBlockWithin(camera.position(), Blocks.FORGE, CraftingStationRules.STATION_RADIUS_BLOCKS).isPresent()) {
            stations.add(CraftingStationType.FORGE);
        }
        return stations;
    }

    private static CraftingStationType primaryCraftingStation(EnumSet<CraftingStationType> stations) {
        if (stations.contains(CraftingStationType.FORGE)) {
            return CraftingStationType.FORGE;
        }
        if (stations.contains(CraftingStationType.WORKBENCH)) {
            return CraftingStationType.WORKBENCH;
        }
        if (stations.contains(CraftingStationType.COOKING_POT)) {
            return CraftingStationType.COOKING_POT;
        }
        if (stations.contains(CraftingStationType.CAMPFIRE)) {
            return CraftingStationType.CAMPFIRE;
        }
        return CraftingStationType.INVENTORY;
    }

    private void handleGlobalKeys() {
        boolean chat = glfwGetKey(window, GLFW_KEY_T) == GLFW_PRESS;
        boolean slash = glfwGetKey(window, GLFW_KEY_SLASH) == GLFW_PRESS;
        boolean f3 = glfwGetKey(window, GLFW_KEY_F3) == GLFW_PRESS;
        boolean crafting = glfwGetKey(window, GLFW_KEY_E) == GLFW_PRESS;
        boolean journal = glfwGetKey(window, GLFW_KEY_J) == GLFW_PRESS;
        boolean hudToggle = glfwGetKey(window, GLFW_KEY_F1) == GLFW_PRESS;
        boolean modeCycle = glfwGetKey(window, GLFW_KEY_F4) == GLFW_PRESS;
        boolean debugViewCycle = glfwGetKey(window, GLFW_KEY_F6) == GLFW_PRESS;
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
        if (gameState == GameState.JOURNAL && journal && !previousJournal) {
            closeJournal();
        } else if (gameplayHotkeys && journal && !previousJournal) {
            openJournal();
        }
        if (!craftingTextInput && f3 && !previousF3) {
            settings.toggleDebugOverlay();
        }
        if (gameplayHotkeys && hudToggle && !previousHudToggle) {
            cycleHudMode();
        }
        if (gameplayHotkeys && modeCycle && !previousModeCycle) {
            cycleGameMode();
        }
        if (!craftingTextInput && debugViewCycle && !previousDebugViewCycle) {
            settings.cycleRenderDebugView();
            chatLog.add("Render debug view: " + settings.renderDebugView().commandName());
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
        previousJournal = journal;
        previousHudToggle = hudToggle;
        previousModeCycle = modeCycle;
        previousDebugViewCycle = debugViewCycle;
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
                case "help" -> chatLog.add("Commands: /help /keys /seed /pos /tp x y z /spawn /gamemode survival|creative|spectator /preset low|medium|high /renderdistance n /preview n /meshbudget n /meshms n /uploadms n /greedymesh /fov n /fog /ao /shadows /bloom /lightning /hud [normal|minimal|hidden] /debug /debugchunks /debugbounds /debugsections /debugparticles /debugview off|material|light|sky|block|emissive|ao|biome|layer|uv|transparent /debuglight /debugbiome /debugmaterial /debugatlas [dump|uv block] /shaderreload /water /simplewater /particles 0.25-1.0 /settings /clear /say text or !phys projectile|entity|water|unloaded|stats");
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
                case "meshms" -> {
                    settings.setMeshBuildBudgetMilliseconds(parseDouble(parts, 1));
                    chatLog.add("Mesh ms budget: " + formatMilliseconds(settings.meshBuildBudgetMilliseconds()));
                }
                case "uploadms" -> {
                    settings.setGpuUploadBudgetMilliseconds(parseDouble(parts, 1));
                    chatLog.add("GPU upload ms budget: " + formatMilliseconds(settings.gpuUploadBudgetMilliseconds()));
                }
                case "greedymesh" -> {
                    settings.toggleGreedyMeshing();
                    if (world != null) {
                        world.markAllLoadedDirty();
                    }
                    chatLog.add("Greedy mesh: " + onOff(settings.greedyMeshingEnabled()));
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
                case "lightning" -> triggerWeatherLightningCommand();
                case "hud" -> hudCommand(parts);
                case "debug" -> toggleCommand("Debug overlay", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
                case "debugchunks" -> toggleCommand("Chunk borders", settings.debugChunkBordersEnabled(), settings::toggleDebugChunkBorders);
                case "debugbounds" -> toggleCommand("Mesh bounds", settings.debugMeshBoundsEnabled(), settings::toggleDebugMeshBounds);
                case "debugsections" -> toggleCommand("Section bounds", settings.debugSectionBoundsEnabled(), settings::toggleDebugSectionBounds);
                case "debugparticles" -> toggleCommand("Particle bounds", settings.debugParticleBoundsEnabled(), settings::toggleDebugParticleBounds);
                case "debugview" -> setRenderDebugView(parts);
                case "debuglight" -> chatLog.add(lightDebugLine());
                case "debugbiome" -> chatLog.add(biomeDebugLine());
                case "debugmaterial" -> chatLog.add(materialDebugLine());
                case "debugatlas" -> debugAtlasCommand(parts);
                case "shaderreload" -> reloadShadersCommand();
                case "water" -> {
                    settings.toggleTransparentWater();
                    if (world != null) {
                        world.markAllLoadedDirty();
                    }
                    chatLog.add("Transparent water: " + onOff(settings.transparentWaterEnabled()));
                }
                case "simplewater" -> toggleCommand("Simple water", settings.simpleWaterEnabled(), settings::toggleSimpleWater);
                case "particles" -> {
                    settings.setParticleQuality(parseDouble(parts, 1));
                    chatLog.add("Particle quality: " + formatPercent(settings.particleQuality()));
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

    private void triggerWeatherLightningCommand() {
        weatherLightning.trigger(currentTimeSeconds());
        chatLog.add("Weather lightning triggered");
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

    private void setRenderDebugView(String[] parts) {
        if (parts.length < 2) {
            throw new IllegalArgumentException("Usage: /debugview off|material|light|sky|block|emissive|ao|biome|layer|uv|transparent");
        }
        RenderDebugView view = RenderDebugView.parse(parts[1]);
        settings.setRenderDebugView(view);
        chatLog.add("Render debug view: " + view.commandName());
    }

    private void reloadShadersCommand() {
        ShaderRegistry.ReloadReport report = ShaderRegistry.reloadAll();
        String line = "Shader reload: "
                + report.reloadedPrograms()
                + "/"
                + report.attemptedPrograms()
                + " ok, "
                + report.failedPrograms()
                + " failed in "
                + formatMilliseconds(report.elapsedMilliseconds());
        if (!report.successful() && !report.lastError().isBlank()) {
            line += " " + clampText(report.lastError(), 72);
        }
        chatLog.add(line);
    }

    private void debugAtlasCommand(String[] parts) {
        if (parts.length >= 2 && "dump".equalsIgnoreCase(parts[1])) {
            try {
                Path output = Path.of("build", "debug", "block-atlas.png");
                Files.createDirectories(output.getParent());
                BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.writeDebugAtlas(Blocks.createDefaultRegistry(), output);
                chatLog.add("Atlas debug PNG: " + output + " (" + report.atlasWidth() + "x" + report.atlasHeight() + ")");
            } catch (Exception e) {
                throw new IllegalArgumentException("Atlas dump failed: " + e.getMessage());
            }
            return;
        }
        if (parts.length >= 3 && "uv".equalsIgnoreCase(parts[1])) {
            String query = parts[2].toLowerCase(Locale.ROOT);
            BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.validationReport(Blocks.createDefaultRegistry());
            List<String> lines = report.uvRectDebugLines().stream()
                    .filter(line -> line.toLowerCase(Locale.ROOT).contains(query))
                    .limit(3)
                    .toList();
            if (lines.isEmpty()) {
                chatLog.add("Atlas UV: no match for " + parts[2]);
                return;
            }
            lines.forEach(chatLog::add);
            return;
        }
        chatLog.add(atlasDebugLine());
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
        int blockLight = world.blockLightAt(x, y, z);
        short blockId = world.blockIdAt(x, y, z);
        BlockType block = world.blockTypeAt(x, y, z);
        BlockRenderProperties properties = BlockRenderProperties.forBlock(blockId);
        return new LightDebugInfo(
                x,
                y,
                z,
                sky,
                blockLight,
                properties.emissive(),
                block.key(),
                block.lightEmission(),
                LightRules.occlusionType(block)
        ).format();
    }

    private String biomeDebugLine() {
        if (world == null) {
            return "Biome: no world";
        }
        int x = (int) Math.floor(camera.position().x);
        int z = (int) Math.floor(camera.position().z);
        var transition = world.biomeTransitionAt(x, z);
        return "Biome @ " + x + " " + z + ": " + biomeLabel(world.biomeKeyAt(x, z))
                + " height " + world.terrainHeightAt(x, z)
                + " edge " + String.format(Locale.ROOT, "%.2f", transition.edgeFactor());
    }

    private String materialDebugLine() {
        if (world == null) {
            return "Material: no world";
        }
        Optional<dev.voxelgame.common.math.Raycast.Hit> target = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (target.isEmpty()) {
            return "Material: no block in reach";
        }
        Optional<BlockType> block = world.targetBlock(target.get());
        if (block.isEmpty()) {
            return "Material: no renderable block";
        }
        RenderMaterial material = RenderMaterial.forBlock(block.get());
        return String.format(
                Locale.ROOT,
                "Material %s id %d layer %s alpha %.2f glow %.2f fluid %s flags %d",
                block.get().key(),
                material.materialIndex(),
                material.renderLayer(),
                material.alpha(),
                material.emissive(),
                onOff(material.animatedFluid()),
                material.flags()
        );
    }

    private String atlasDebugLine() {
        BlockTextureAtlas.AtlasValidationReport report = BlockTextureAtlas.validationReport(Blocks.createDefaultRegistry());
        return String.format(
                Locale.ROOT,
                "Atlas %dx%d tex %d mat %d/%d pad %d inset %.1f %s missing %d dup %d uv %d",
                report.atlasWidth(),
                report.atlasHeight(),
                report.textureCount(),
                report.materialCount(),
                report.materialCapacity(),
                report.tilePaddingPixels(),
                report.uvInsetPixels(),
                report.filterMode(),
                report.missingTextures().size(),
                report.duplicateMappings().size(),
                report.uvRectDebugLines().size()
        );
    }

    private void showKeybinds() {
        chatLog.add("Keys: WASD move, Space jump/up, Ctrl down, Shift sprint");
        chatLog.add("Keys: E crafting, J journal, O settings, R spawn, F1 HUD mode, F3 debug, F4 mode, F6 debug view");
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
        resetSurvivalHudSignals();
        chatLog.add("Game mode: " + mode.name());
        updateWindowTitle();
    }

    private void cycleHudMode() {
        settings.cycleHudMode();
        chatLog.add("HUD: " + settings.hudMode().label());
    }

    private void hudCommand(String[] parts) {
        if (parts.length > 1) {
            settings.setHudMode(GameSettings.HudMode.parse(parts[1]));
        } else {
            settings.cycleHudMode();
        }
        chatLog.add("HUD: " + settings.hudMode().label());
    }

    private int parseInt(String[] parts, int index) {
        if (parts.length <= index) {
            throw new IllegalArgumentException("Missing number");
        }
        return Integer.parseInt(parts[index]);
    }

    private double parseDouble(String[] parts, int index) {
        if (parts.length <= index) {
            throw new IllegalArgumentException("Missing number");
        }
        return Double.parseDouble(parts[index]);
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

    private static String formatPercent(double value) {
        if (!Double.isFinite(value) || value < 0.0) {
            return "0%";
        }
        return String.format(Locale.ROOT, "%.0f%%", value * 100.0);
    }

    private static String formatJobCounter(EngineFrameStats.JobCounter counter) {
        return counter.type().debugLabel()
                + " " + counter.pendingJobs()
                + "/" + counter.runningJobs()
                + "/" + formatCount(counter.completedJobs())
                + "/" + formatCount(counter.canceledJobs());
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
        PlayerWaterState water = world == null ? new PlayerWaterState(false, false, false) : world.playerWaterState(position);
        connection.send(new GamePacket.PlayerMove(
                nextMovementSequence++,
                position.x,
                position.y,
                position.z,
                camera.yaw(),
                camera.pitch(),
                camera.onGround(),
                water
        ));
    }

    private void consumeAuthoritativePlayerState() {
        GamePacket.PlayerPositionSnapshot snapshot = pendingAuthoritativePlayerState.getAndSet(null);
        if (snapshot == null || snapshot.sequence() < lastAuthoritativeMovementSequence) {
            return;
        }
        lastAuthoritativeMovementSequence = snapshot.sequence();
        camera.reconcileAuthoritativePosition(
                snapshot.x(),
                snapshot.y(),
                snapshot.z(),
                snapshot.onGround(),
                snapshot.correction()
        );
    }

    private void consumeProjectileImpacts() {
        GamePacket.ProjectileImpact impact;
        while ((impact = pendingProjectileImpacts.poll()) != null) {
            handleProjectileImpact(impact);
        }
    }

    private void consumeGameplayEvents() {
        GamePacket.GameplayEvents packet;
        while ((packet = pendingGameplayEvents.poll()) != null) {
            for (GameplayEvent event : packet.events()) {
                handleGameplayEvent(event);
            }
        }
    }

    private void handleGameplayEvent(GameplayEvent event) {
        double now = currentTimeSeconds();
        GameplayEventFeedback.describe(event, GameClient::cozyName).ifPresent(entry -> {
            statusMessage = entry.message();
            feedbackLog.add(entry.message(), now, entry.kind());
            entry.audioCue().ifPresent(audio::play);
        });
        switch (event) {
            case GameplayEvent.Damage damage -> damageFlash.trigger(damage.amount(), now);
            case GameplayEvent.Pickup pickup -> pickupPop.trigger(pickup.itemKey(), now);
            case GameplayEvent.Craft craft when craft.success() -> craftingSuccessPop.trigger(craft.recipeKey(), now);
            case GameplayEvent.RecipeUnlocked recipe -> recipeUnlockPop.trigger(recipe.recipeKey(), now);
            default -> {
            }
        }
    }

    private void updateWindowTitle() {
        if (window != 0) {
            String suffix = switch (gameState) {
                case MAIN_MENU -> "Main Menu";
                case LOADING -> loadingScreen.title();
                case PAUSED -> "Paused";
                case SETTINGS -> "Settings";
                case CHAT -> "Chat";
                case CRAFTING -> "Crafting";
                case JOURNAL -> "Journal";
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
                clearInventoryDrag();
                resumeGame();
            } else if (gameState == GameState.JOURNAL) {
                closeJournal();
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

    private void renderLoadingScreen(LoadingScreenViewModel viewModel, double nowSeconds) {
        LoadingScreenViewModel screen = viewModel == null ? LoadingScreenViewModel.boot() : viewModel;
        LoadingScreenLayout layout = loadingScreenLayout(framebufferWidth, framebufferHeight, settings.uiScale());
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.025f, 0.04f, 0.045f, 1.0f));
        uiRenderer.rect(0, layout.horizonY(), framebufferWidth, framebufferHeight - layout.horizonY(), new UiColor(0.05f, 0.10f, 0.08f, 0.92f));
        uiRenderer.centeredText(screen.title().toUpperCase(Locale.ROOT), framebufferWidth * 0.5f, layout.titleY(), layout.titleScale(), UiColor.WHITE);
        uiRenderer.centeredText(screen.detail(), framebufferWidth * 0.5f, layout.detailY(), layout.detailScale(), screen.error() ? UiColor.WARNING : UiColor.MUTED);
        uiRenderer.rect(layout.barX(), layout.barY(), layout.barWidth(), layout.barHeight(), new UiColor(0.10f, 0.13f, 0.12f, 0.96f));
        uiRenderer.rect(layout.barX() + layout.border(), layout.barY() + layout.border(), layout.barWidth() - layout.border() * 2.0f, layout.barHeight() - layout.border() * 2.0f, new UiColor(0.24f, 0.28f, 0.24f, 0.70f));
        float fill = (float) loadingBarFill(screen, nowSeconds);
        float fillWidth = Math.max(0.0f, (layout.barWidth() - layout.border() * 2.0f) * fill);
        UiColor fillColor = screen.error() ? UiColor.WARNING : new UiColor(0.78f, 0.86f, 0.56f, 0.94f);
        uiRenderer.rect(layout.barX() + layout.border(), layout.barY() + layout.border(), fillWidth, layout.barHeight() - layout.border() * 2.0f, fillColor);
        uiRenderer.centeredText(Math.round(screen.progress() * 100.0) + "%", framebufferWidth * 0.5f, layout.percentY(), layout.percentScale(), UiColor.WHITE);
    }

    private void renderPauseMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, UiColor.PANEL);
        uiRenderer.centeredText("PAUSED", framebufferWidth * 0.5f, 110.0f, 6.0f, UiColor.WHITE);

        float buttonWidth = Math.min(320.0f, framebufferWidth - 80.0f);
        float buttonHeight = 46.0f;
        float x = framebufferWidth * 0.5f - buttonWidth * 0.5f;
        float y = framebufferHeight * 0.5f - 118.0f;
        drawButton(new UiButton(x, y, buttonWidth, buttonHeight, "RESUME", true), mouse, clicked, this::resumeGame);
        drawButton(new UiButton(x, y + 54.0f, buttonWidth, buttonHeight, "JOURNAL", true), mouse, clicked, () -> openJournal(GameState.PAUSED));
        drawButton(new UiButton(x, y + 108.0f, buttonWidth, buttonHeight, "SETTINGS", true), mouse, clicked, () -> openSettings(GameState.PAUSED));
        drawButton(new UiButton(x, y + 162.0f, buttonWidth, buttonHeight, "MAIN MENU", true), mouse, clicked, this::returnToMainMenu);
        drawButton(new UiButton(x, y + 216.0f, buttonWidth, buttonHeight, "QUIT", true), mouse, clicked, () -> glfwSetWindowShouldClose(window, true));
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
        uiRenderer.centeredText(fitTextToWidth("PRESS R OR CLICK RESPAWN", 1.1f, panelWidth - 64.0f), framebufferWidth * 0.5f, y + 124.0f, 1.1f, UiColor.WHITE);
        float buttonWidth = Math.min(260.0f, panelWidth - 80.0f);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, y + 150.0f, buttonWidth, 42.0f, "RESPAWN", true), mouse, clicked, this::respawnPlayer);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, y + 202.0f, buttonWidth, 34.0f, "MAIN MENU", true), mouse, clicked, this::returnToMainMenu);
    }

    private void renderJournalScreen(MousePosition mouse, boolean clicked) {
        refreshJournalDiscoveries(frameTimeSeconds);
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.015f, 0.020f, 0.024f, 0.78f));
        float uiScale = settings.uiScale();
        float margin = Math.max(18.0f * uiScale, 28.0f);
        float panelWidth = Math.min(920.0f * uiScale, framebufferWidth - margin * 2.0f);
        float panelHeight = Math.min(560.0f * uiScale, framebufferHeight - margin * 2.0f);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float y = framebufferHeight * 0.5f - panelHeight * 0.5f;
        drawAssetPanel("panel_journal", x, y, panelWidth, panelHeight, new UiColor(0.035f, 0.045f, 0.040f, 0.92f));
        uiRenderer.rect(x + 10.0f * uiScale, y + 10.0f * uiScale, panelWidth - 20.0f * uiScale, panelHeight - 20.0f * uiScale, new UiColor(0.020f, 0.030f, 0.028f, 0.46f));
        uiRenderer.rect(x + 18.0f * uiScale, y + 58.0f * uiScale, panelWidth - 36.0f * uiScale, Math.max(2.0f, 3.0f * uiScale), UiColor.ACCENT);
        uiRenderer.text("JOURNAL", x + 24.0f * uiScale, y + 23.0f * uiScale, 2.7f * uiScale, UiColor.WHITE);
        float headerX = x + 220.0f * uiScale;
        float headerWidth = panelWidth - 348.0f * uiScale;
        if (headerWidth > 70.0f * uiScale) {
            uiRenderer.text(fitTextToWidth(journalHeaderLine(), 1.05f * uiScale, headerWidth), headerX, y + 31.0f * uiScale, 1.05f * uiScale, UiColor.MUTED);
        }
        drawButton(new UiButton(x + panelWidth - 104.0f * uiScale, y + 20.0f * uiScale, 76.0f * uiScale, 30.0f * uiScale, "CLOSE", true), mouse, clicked, this::closeJournal);

        float tabY = y + 74.0f * uiScale;
        float tabGap = 7.0f * uiScale;
        float tabWidth = Math.min(126.0f * uiScale, (panelWidth - 48.0f * uiScale - tabGap * (JournalTab.values().length - 1)) / JournalTab.values().length);
        float tabX = x + 24.0f * uiScale;
        for (JournalTab tab : JournalTab.values()) {
            drawJournalTab(mouse, clicked, tabX, tabY, tabWidth, 30.0f * uiScale, tab);
            tabX += tabWidth + tabGap;
        }

        float contentX = x + 24.0f * uiScale;
        float contentY = tabY + 44.0f * uiScale;
        float contentWidth = panelWidth - 48.0f * uiScale;
        float contentHeight = panelHeight - (contentY - y) - 24.0f * uiScale;
        uiRenderer.rect(contentX, contentY, contentWidth, contentHeight, new UiColor(0.010f, 0.016f, 0.015f, 0.42f));
        uiRenderer.rect(contentX, contentY, Math.max(2.0f, 3.0f * uiScale), contentHeight, withAlpha(interactionHintAccent(journalTone()), 0.72f));
        switch (journalTab) {
            case NOTES -> renderJournalNotes(contentX, contentY, contentWidth, uiScale);
            case BIOMES -> renderJournalBiomes(contentX, contentY, contentWidth, uiScale);
            case STRUCTURES -> renderJournalStructures(contentX, contentY, contentWidth, uiScale);
            case CREATURES -> renderJournalCreatures(contentX, contentY, contentWidth, uiScale);
            case RECIPES -> renderJournalRecipes(contentX, contentY, contentWidth, uiScale);
            case COLLECTIBLES -> renderJournalCollectibles(contentX, contentY, contentWidth, uiScale);
        }
    }

    private void drawJournalTab(MousePosition mouse, boolean clicked, float x, float y, float width, float height, JournalTab tab) {
        boolean selected = journalTab == tab;
        boolean hovered = contains(mouse, x, y, width, height);
        UiColor fill = selected ? UiColor.SLOT_ACTIVE : hovered ? UiColor.BUTTON_HOVER : UiColor.SLOT;
        uiRenderer.rect(x, y, width, height, fill);
        uiRenderer.rect(x, y, width, Math.max(2.0f, height * 0.08f), selected ? UiColor.ACCENT : UiColor.MUTED);
        float scale = Math.min(1.0f * settings.uiScale(), (width - 10.0f * settings.uiScale()) / Math.max(1.0f, BitmapFont.textWidth(tab.label(), 1.0f)));
        uiRenderer.centeredText(tab.label(), x + width * 0.5f, y + height * 0.5f - BitmapFont.textHeight(scale) * 0.5f, scale, selected ? UiColor.WHITE : UiColor.MUTED);
        if (hovered && clicked) {
            journalTab = tab;
            audio.play(AudioCue.INVENTORY_CLICK);
        }
    }

    private void renderJournalNotes(float x, float y, float width, float uiScale) {
        float lineY = journalTitle(x, y, "FIELD NOTES", "Session state and nearby stations", uiScale);
        lineY = journalLine(x, lineY, "World", onlineMode ? "Online" : "Singleplayer", UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Mode", gameMode.name(), UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Day", dayTimeLabel(), UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Comfort", comfortLabel(playerStats.comfort()), UiColor.ENERGY, uiScale);
        lineY = journalLine(x, lineY, "Station", stationSetLabel(currentCraftingStations()), UiColor.ACCENT, uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("LORE NOTES", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 20.0f * uiScale;
        uiRenderer.text("No lore notes found", x + 32.0f * uiScale, lineY, 1.1f * uiScale, UiColor.BUTTON_DISABLED);
        drawJournalProgressIfRoom(x, y, width, "ALPHA FLOW", journalProgressRatio(), uiScale);
    }

    private void renderJournalBiomes(float x, float y, float width, float uiScale) {
        Vector3f position = camera.position();
        String currentBiome = world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z));
        float lineY = journalTitle(x, y, "BIOMES", "Discovered " + discoveredBiomeKeys.size(), uiScale);
        lineY = journalLine(x, lineY, "Current", biomeLabel(currentBiome), UiColor.ACCENT, uiScale);
        lineY = journalLine(x, lineY, "Temperature", temperatureLabel(currentBiome), UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Position", Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z), UiColor.WHITE, uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("KNOWN BIOMES", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 22.0f * uiScale;
        List<String> biomesList = discoveredBiomeKeys.stream().sorted().limit(8).toList();
        if (biomesList.isEmpty()) {
            uiRenderer.text("No biome entries yet", x + 32.0f * uiScale, lineY, 1.1f * uiScale, UiColor.BUTTON_DISABLED);
            return;
        }
        for (String biomeKey : biomesList) {
            UiColor color = biomeKey.equals(currentBiome) ? UiColor.ACCENT : UiColor.WHITE;
            uiRenderer.text("- " + biomeLabel(biomeKey), x + 32.0f * uiScale, lineY, 1.08f * uiScale, color);
            lineY += 18.0f * uiScale;
        }
        drawJournalProgressIfRoom(x, y, width, "WORLD", Math.min(1.0f, discoveredBiomeKeys.size() / 8.0f), uiScale);
    }

    private void renderJournalStructures(float x, float y, float width, float uiScale) {
        int radius = CraftingStationRules.STATION_RADIUS_BLOCKS;
        Optional<ClientWorld.BlockPos> storage = nearestStructure(Blocks.STORAGE_CRATE, radius);
        Optional<ClientWorld.BlockPos> activeCampfire = nearestStructure(Blocks.CAMPFIRE_ACTIVE, radius);
        Optional<ClientWorld.BlockPos> coldCampfire = nearestStructure(Blocks.CAMPFIRE, radius);
        Optional<ClientWorld.BlockPos> burnedCampfire = nearestStructure(Blocks.CAMPFIRE_BURNED_OUT, radius);
        Optional<ClientWorld.BlockPos> campfire = nearestPresent(activeCampfire, coldCampfire, burnedCampfire);
        Optional<ClientWorld.BlockPos> cookingPot = nearestStructure(Blocks.COOKING_POT, radius);
        Optional<ClientWorld.BlockPos> workbench = nearestStructure(Blocks.WORKBENCH, radius);
        Optional<ClientWorld.BlockPos> forge = nearestStructure(Blocks.FORGE, radius);
        Optional<ClientWorld.BlockPos> sleepingMat = nearestStructure(Blocks.SLEEPING_MAT, radius);
        int nearby = nearbyCount(storage, campfire, cookingPot, workbench, forge, sleepingMat);

        float lineY = journalTitle(x, y, "STRUCTURES", nearby + "/6 useful nearby", uiScale);
        lineY = journalLine(x, lineY, "Storage", structureStatus(storage, "E opens crate", "No crate in reach"), structureColor(storage), uiScale);
        lineY = journalLine(x, lineY, "Campfire", campfireStatus(activeCampfire, coldCampfire, burnedCampfire), campfireColor(activeCampfire, coldCampfire, burnedCampfire), uiScale);
        lineY = journalLine(x, lineY, "Cooking pot", structureStatus(cookingPot, "Recipes unlocked", "No pot in reach"), structureColor(cookingPot), uiScale);
        lineY = journalLine(x, lineY, "Workbench", structureStatus(workbench, "Progression recipes", "No bench in reach"), structureColor(workbench), uiScale);
        lineY = journalLine(x, lineY, "Forge", structureStatus(forge, "Iron and seals", "No forge in reach"), structureColor(forge), uiScale);
        lineY = journalLine(x, lineY, "Sleep", structureStatus(sleepingMat, "Safe rest spot", "No mat in reach"), structureColor(sleepingMat), uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("NEARBY FLOW", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 22.0f * uiScale;
        uiRenderer.text("- E uses storage, crafting and rest targets", x + 32.0f * uiScale, lineY, 1.05f * uiScale, UiColor.WHITE);
        lineY += 18.0f * uiScale;
        uiRenderer.text("- Campfire, pot, bench and forge unlock station recipes", x + 32.0f * uiScale, lineY, 1.05f * uiScale, UiColor.WHITE);
        lineY += 18.0f * uiScale;
        uiRenderer.text("- Missing entries mean move closer or build/place one", x + 32.0f * uiScale, lineY, 1.05f * uiScale, UiColor.MUTED);
        drawJournalProgressIfRoom(x, y, width, "STRUCTURES", journalStructureRatio(), uiScale);
    }

    private void renderJournalCreatures(float x, float y, float width, float uiScale) {
        List<EntitySnapshot> visibleCreatures = world.visibleEntities(frameTimeSeconds).stream()
                .filter(entity -> !ItemDropType.isTypeKey(entity.typeKey()))
                .filter(entity -> !"voxel:player".equals(entity.typeKey()))
                .toList();
        float lineY = journalTitle(x, y, "CREATURES", "Discovered " + discoveredCreatureKeys.size(), uiScale);
        lineY = journalLine(x, lineY, "Nearby", String.valueOf(visibleCreatures.size()), UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Feeding", hotbar.selectedItemIsFood() ? "Food ready" : "Select food first", hotbar.selectedItemIsFood() ? UiColor.ACCENT : UiColor.MUTED, uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("NEARBY", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 22.0f * uiScale;
        if (visibleCreatures.isEmpty()) {
            uiRenderer.text("No creatures in sight", x + 32.0f * uiScale, lineY, 1.1f * uiScale, UiColor.BUTTON_DISABLED);
        } else {
            for (EntitySnapshot entity : visibleCreatures.stream().limit(6).toList()) {
                String state = entity.stateKey().toLowerCase(Locale.ROOT).replace('_', ' ');
                uiRenderer.text("- " + labelKey(entity.typeKey()) + "  " + state + "  HP " + entity.health(), x + 32.0f * uiScale, lineY, 1.05f * uiScale, UiColor.WHITE);
                lineY += 18.0f * uiScale;
            }
        }
        drawJournalProgressIfRoom(x, y, width, "CREATURES", Math.min(1.0f, discoveredCreatureKeys.size() / 8.0f), uiScale);
    }

    private void renderJournalRecipes(float x, float y, float width, float uiScale) {
        EnumSet<CraftingStationType> stationTypes = currentCraftingStations();
        List<CraftingRecipe> recipes = prioritizedRecipes(stationTypes);
        long unlocked = hotbar.recipes().stream().filter(recipe -> recipeUnlockedForUi(recipe, stationTypes)).count();
        long craftable = hotbar.recipes().stream().filter(recipe -> canCraftRecipe(recipe, stationTypes)).count();
        float lineY = journalTitle(x, y, "RECIPES", "Known " + unlocked + "/" + hotbar.recipes().size(), uiScale);
        lineY = journalLine(x, lineY, "Station", stationSetLabel(stationTypes), UiColor.ACCENT, uiScale);
        lineY = journalLine(x, lineY, "Ready", craftable + " craftable", craftable > 0 ? UiColor.ACCENT : UiColor.MUTED, uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("RECIPE BOOK", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 22.0f * uiScale;
        for (CraftingRecipe recipe : recipes.stream().limit(8).toList()) {
            UiColor color = canCraftRecipe(recipe, stationTypes) ? UiColor.ACCENT : recipeUnlockedForUi(recipe, stationTypes) ? UiColor.WHITE : UiColor.BUTTON_DISABLED;
            uiRenderer.text("- " + clampText(recipe.label() + "  " + recipeStatusLine(recipe, stationTypes), 58), x + 32.0f * uiScale, lineY, 1.0f * uiScale, color);
            lineY += 18.0f * uiScale;
        }
        drawJournalProgressIfRoom(x, y, width, "RECIPES", hotbar.recipes().isEmpty() ? 0.0f : unlocked / (float) hotbar.recipes().size(), uiScale);
    }

    private void renderJournalCollectibles(float x, float y, float width, float uiScale) {
        List<EntitySnapshot> itemDrops = world.visibleEntities(frameTimeSeconds).stream()
                .filter(entity -> ItemDropType.isTypeKey(entity.typeKey()))
                .toList();
        float lineY = journalTitle(x, y, "COLLECTIBLES", "Nearby drops " + itemDrops.size(), uiScale);
        lineY = journalLine(x, lineY, "Selected", hotbar.selectedLabel(), UiColor.WHITE, uiScale);
        lineY = journalLine(x, lineY, "Inventory", inventoryFilledSlots() + "/" + hotbar.inventorySlotCount() + " slots", UiColor.WHITE, uiScale);
        lineY += 10.0f * uiScale;
        uiRenderer.text("NEARBY DROPS", x + 20.0f * uiScale, lineY, 1.15f * uiScale, UiColor.MUTED);
        lineY += 22.0f * uiScale;
        if (itemDrops.isEmpty()) {
            uiRenderer.text("No loose items in sight", x + 32.0f * uiScale, lineY, 1.1f * uiScale, UiColor.BUTTON_DISABLED);
        } else {
            for (EntitySnapshot drop : itemDrops.stream().limit(7).toList()) {
                String item = ItemDropType.itemKey(drop.typeKey()).map(GameClient::labelKey).orElse("Item");
                uiRenderer.text("- " + item + "  nearby", x + 32.0f * uiScale, lineY, 1.05f * uiScale, UiColor.WHITE);
                lineY += 18.0f * uiScale;
            }
        }
        drawJournalProgressIfRoom(x, y, width, "PACK", inventoryFilledSlots() / (float) Math.max(1, hotbar.inventorySlotCount()), uiScale);
    }

    private String journalHeaderLine() {
        if (world == null) {
            return "NO WORLD";
        }
        return currentWorldHudInfo().journalHeader();
    }

    private InteractionHint.Tone journalTone() {
        return switch (journalTab) {
            case NOTES, BIOMES -> InteractionHint.Tone.NEUTRAL;
            case STRUCTURES, CREATURES, RECIPES, COLLECTIBLES -> InteractionHint.Tone.READY;
        };
    }

    private float journalTitle(float x, float y, String title, String meta, float uiScale) {
        uiRenderer.text(title, x + 20.0f * uiScale, y + 18.0f * uiScale, 1.65f * uiScale, UiColor.WHITE);
        uiRenderer.text(meta.toUpperCase(Locale.ROOT), x + 20.0f * uiScale, y + 42.0f * uiScale, 0.95f * uiScale, UiColor.MUTED);
        return y + 72.0f * uiScale;
    }

    private float journalLine(float x, float y, String label, String value, UiColor valueColor, float uiScale) {
        uiRenderer.text(label.toUpperCase(Locale.ROOT), x + 20.0f * uiScale, y, 1.02f * uiScale, UiColor.MUTED);
        float valueX = x + 148.0f * uiScale;
        float valueWidth = Math.max(48.0f * uiScale, Math.min(300.0f * uiScale, framebufferWidth - valueX - 42.0f * uiScale));
        uiRenderer.text(fitTextToWidth(value, 1.08f * uiScale, valueWidth), valueX, y, 1.08f * uiScale, valueColor);
        return y + 21.0f * uiScale;
    }

    private void drawJournalProgressIfRoom(float contentX, float contentY, float contentWidth, String label, float ratio, float uiScale) {
        if (contentWidth < 520.0f * uiScale) {
            return;
        }
        drawJournalProgress(contentX + contentWidth - 226.0f * uiScale, contentY + 24.0f * uiScale, 186.0f * uiScale, label, ratio, uiScale);
    }

    private void drawJournalProgress(float x, float y, float width, String label, float ratio, float uiScale) {
        float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
        uiRenderer.text(label, x, y, 0.92f * uiScale, UiColor.MUTED);
        drawProgressBar(x, y + 18.0f * uiScale, width, 10.0f * uiScale, clamped, interactionHintAccent(journalTone()));
        uiRenderer.text(Math.round(clamped * 100.0f) + "%", x + width - 32.0f * uiScale, y + 34.0f * uiScale, 0.86f * uiScale, UiColor.WHITE);
    }

    private float journalProgressRatio() {
        float biome = Math.min(1.0f, discoveredBiomeKeys.size() / 8.0f);
        float creature = Math.min(1.0f, discoveredCreatureKeys.size() / 8.0f);
        float recipe = hotbar.recipes().isEmpty() ? 0.0f : hotbar.discoveredRecipes().size() / (float) hotbar.recipes().size();
        float structures = journalStructureRatio();
        return (biome + creature + recipe + structures) / 4.0f;
    }

    private String comfortLabel(int comfort) {
        ComfortHudInfo info = ComfortHudInfo.of(comfort, List.of());
        return info.statLabel().isBlank()
                ? "None"
                : info.statLabel();
    }

    private int inventoryFilledSlots() {
        int filled = 0;
        for (int slot = 0; slot < hotbar.inventorySlotCount(); slot++) {
            if (!hotbar.slotView(slot).isEmpty()) {
                filled++;
            }
        }
        return filled;
    }

    @SafeVarargs
    private static int nearbyCount(Optional<ClientWorld.BlockPos>... positions) {
        int count = 0;
        for (Optional<ClientWorld.BlockPos> position : positions) {
            if (position.isPresent()) {
                count++;
            }
        }
        return count;
    }

    @SafeVarargs
    private static Optional<ClientWorld.BlockPos> nearestPresent(Optional<ClientWorld.BlockPos>... positions) {
        for (Optional<ClientWorld.BlockPos> position : positions) {
            if (position.isPresent()) {
                return position;
            }
        }
        return Optional.empty();
    }

    private Optional<ClientWorld.BlockPos> nearestStructure(short blockId, int radius) {
        if (world == null) {
            return Optional.empty();
        }
        return world.nearestBlockWithin(camera.position(), blockId, radius);
    }

    private float journalStructureRatio() {
        int radius = CraftingStationRules.STATION_RADIUS_BLOCKS;
        Optional<ClientWorld.BlockPos> storage = nearestStructure(Blocks.STORAGE_CRATE, radius);
        Optional<ClientWorld.BlockPos> campfire = nearestPresent(
                nearestStructure(Blocks.CAMPFIRE_ACTIVE, radius),
                nearestStructure(Blocks.CAMPFIRE, radius),
                nearestStructure(Blocks.CAMPFIRE_BURNED_OUT, radius)
        );
        Optional<ClientWorld.BlockPos> cookingPot = nearestStructure(Blocks.COOKING_POT, radius);
        Optional<ClientWorld.BlockPos> workbench = nearestStructure(Blocks.WORKBENCH, radius);
        Optional<ClientWorld.BlockPos> forge = nearestStructure(Blocks.FORGE, radius);
        Optional<ClientWorld.BlockPos> sleepingMat = nearestStructure(Blocks.SLEEPING_MAT, radius);
        return nearbyCount(storage, campfire, cookingPot, workbench, forge, sleepingMat) / 6.0f;
    }

    private String structureStatus(Optional<ClientWorld.BlockPos> position, String ready, String missing) {
        return position.map(pos -> ready + "  " + structureDistanceLabel(pos)).orElse(missing);
    }

    private String campfireStatus(Optional<ClientWorld.BlockPos> active, Optional<ClientWorld.BlockPos> cold, Optional<ClientWorld.BlockPos> burned) {
        if (active.isPresent()) {
            return "Active  " + structureDistanceLabel(active.get());
        }
        if (cold.isPresent()) {
            return "Needs fuel  " + structureDistanceLabel(cold.get());
        }
        if (burned.isPresent()) {
            return "Burned out  " + structureDistanceLabel(burned.get());
        }
        return "No campfire in reach";
    }

    private UiColor campfireColor(Optional<ClientWorld.BlockPos> active, Optional<ClientWorld.BlockPos> cold, Optional<ClientWorld.BlockPos> burned) {
        if (active.isPresent()) {
            return UiColor.ACCENT;
        }
        if (cold.isPresent()) {
            return UiColor.ENERGY;
        }
        return structureColor(burned);
    }

    private UiColor structureColor(Optional<ClientWorld.BlockPos> position) {
        return position.isPresent() ? UiColor.ACCENT : UiColor.BUTTON_DISABLED;
    }

    private String structureDistanceLabel(ClientWorld.BlockPos position) {
        return Math.round(structureDistance(position)) + "m";
    }

    private float structureDistance(ClientWorld.BlockPos position) {
        Vector3f cameraPosition = camera.position();
        float dx = cameraPosition.x - (position.x() + 0.5f);
        float dy = cameraPosition.y - (position.y() + 0.5f);
        float dz = cameraPosition.z - (position.z() + 0.5f);
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private void refreshJournalDiscoveries(double now) {
        if (world == null) {
            return;
        }
        Vector3f position = camera.position();
        String biomeKey = world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z));
        if (!lastHudBiomeKey.isBlank() && !lastHudBiomeKey.equals(biomeKey) && now >= nextBiomeFeedbackTime) {
            feedbackLog.add(WorldHudInfo.of(biomeKey, biomeLabel(biomeKey), temperatureLabel(biomeKey), localDayNumber(), localDayMinutes()).enteredMessage(), now, FeedbackLog.Kind.DISCOVERY);
            nextBiomeFeedbackTime = now + 4.0;
        }
        lastHudBiomeKey = biomeKey;
        discoveredBiomeKeys.add(biomeKey);
        for (EntitySnapshot entity : world.visibleEntities(now)) {
            if (!ItemDropType.isTypeKey(entity.typeKey()) && !"voxel:player".equals(entity.typeKey())) {
                discoveredCreatureKeys.add(entity.typeKey());
            }
        }
    }

    private static String labelKey(String key) {
        if (key == null || key.isBlank()) {
            return "Unknown";
        }
        int colon = key.indexOf(':');
        String value = colon >= 0 ? key.substring(colon + 1) : key;
        String label = value.replace('_', ' ');
        return label.substring(0, 1).toUpperCase(Locale.ROOT) + label.substring(1);
    }

    private void renderCraftingScreen(MousePosition mouse, boolean clicked, boolean released, boolean rightClicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.72f));
        CraftingScreenLayout layout = craftingScreenLayout(framebufferWidth, framebufferHeight, settings.uiScale());
        uiRenderer.centeredText("CRAFTING", framebufferWidth * 0.5f, layout.titleY(), layout.titleScale(), UiColor.WHITE);
        uiRenderer.centeredText(fitTextToWidth("E CLOSE  CLICK RECIPE TO CRAFT  RIGHT-CLICK SPLIT  O SETTINGS", layout.hintScale(), framebufferWidth - layout.margin() * 2.0f), framebufferWidth * 0.5f, layout.hintY(), layout.hintScale(), UiColor.MUTED);

        float contentWidth = layout.contentWidth();
        float x = layout.x();
        float y = layout.contentY();
        AnimationSample shake = craftingFailureShake.sample(frameTimeSeconds);
        float shakeX = shake.value(AnimationChannels.SHAKE_X, 0.0f) * settings.uiScale();
        renderInventoryTabs(x, layout.tabsY(), contentWidth);
        EnumSet<CraftingStationType> stationTypes = currentCraftingStations();
        renderWorkbenchPreview(previewCraftingRecipe(stationTypes), x + shakeX, y, stationTypes);
        renderCraftingMenu(mouse, clicked, x + 360.0f + shakeX, y, stationTypes);
        renderInventoryGridCompact(mouse, clicked, released, rightClicked, x, layout.inventoryY());
        renderCraftingPopAnimations();
    }

    private void renderStorageScreen(MousePosition mouse, boolean clicked, boolean released, boolean rightClicked) {
        if (!hotbar.storageOpen()) {
            resumeGame();
            return;
        }
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.74f));

        float uiScale = settings.uiScale();
        StorageScreenLayout layout = storageScreenLayout(framebufferWidth, framebufferHeight, uiScale);
        float slot = layout.slot();
        float gap = layout.gap();
        int columns = 9;
        float panelWidth = layout.panelWidth();
        float x = layout.gridX();
        float y = layout.crateGridY();
        UiColor storageLabel = new UiColor(0.13f, 0.13f, 0.12f, 1.0f);
        UiColor storageMuted = new UiColor(0.28f, 0.28f, 0.25f, 1.0f);

        uiRenderer.centeredText("STORAGE CRATE", framebufferWidth * 0.5f, layout.titleY(), layout.titleScale(), UiColor.WHITE);
        uiRenderer.centeredText(fitTextToWidth("DRAG MOVE  SHIFT-CLICK TRANSFER  RIGHT-CLICK SPLIT  E OR ESC CLOSE", layout.hintScale(), framebufferWidth - 36.0f), framebufferWidth * 0.5f, layout.hintY(), layout.hintScale(), UiColor.MUTED);

        drawInventoryBackdrop(layout.panelX(), layout.cratePanelY(), panelWidth, layout.cratePanelHeight(), layout.layoutScale());
        uiRenderer.text("CRATE", x, layout.cratePanelY() + 14.0f * layout.layoutScale(), 1.75f * layout.layoutScale(), storageLabel);
        uiRenderer.text(fitTextToWidth(storageMetaLine(), 1.0f * layout.layoutScale(), panelWidth - 230.0f * layout.layoutScale()), x + 74.0f * layout.layoutScale(), layout.cratePanelY() + 18.0f * layout.layoutScale(), 1.0f * layout.layoutScale(), storageMuted);
        drawInventoryActionButton(new UiButton(layout.panelX() + panelWidth - 128.0f * layout.layoutScale(), layout.cratePanelY() + 10.0f * layout.layoutScale(), 112.0f * layout.layoutScale(), 26.0f * layout.layoutScale(), "SORT CRATE", !onlineMode), mouse, clicked, () -> {
            if (hotbar.sortStorage()) {
                setStatus("Crate sorted");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            }
        });
        Hotbar.SlotView hoveredSlot = null;
        boolean droppedOnSlot = false;
        for (int i = 0; i < Math.min(hotbar.storageSlotCount(), 18); i++) {
            int slotIndex = i;
            int column = i % columns;
            int row = i / columns;
            float sx = x + column * (slot + gap);
            float sy = y + row * (slot + gap);
            Hotbar.SlotView slotView = hotbar.storageSlotView(i);
            StorageSlotInteraction interaction = renderStorageSlot(mouse, clicked, released, rightClicked, "storage:" + slotIndex, sx, sy, slot, slotView, false, false, InventoryDragSource.STORAGE, slotIndex);
            if (interaction.hoveredSlot() != null) {
                hoveredSlot = interaction.hoveredSlot();
            }
            droppedOnSlot = droppedOnSlot || interaction.droppedOnSlot();
        }

        float inventoryY = layout.backpackGridY();
        drawInventoryBackdrop(layout.panelX(), layout.backpackPanelY(), panelWidth, layout.backpackPanelHeight(), layout.layoutScale());
        uiRenderer.text("BACKPACK", x, layout.backpackPanelY() + 14.0f * layout.layoutScale(), 1.75f * layout.layoutScale(), storageLabel);
        float hotbarGap = layout.hotbarGap();
        for (int displayIndex = 0; displayIndex < Math.min(hotbar.inventorySlotCount(), 36); displayIndex++) {
            int slotIndex = playerInventorySlotIndex(displayIndex);
            int column = displayIndex % columns;
            int row = displayIndex / columns;
            float sx = x + column * (slot + gap);
            float sy = row < INVENTORY_MAIN_ROWS
                    ? inventoryY + row * (slot + gap)
                    : inventoryY + INVENTORY_MAIN_ROWS * (slot + gap) + hotbarGap;
            boolean selected = slotIndex == hotbar.selectedIndex();
            Hotbar.SlotView slotView = hotbar.slotView(slotIndex);
            StorageSlotInteraction interaction = renderStorageSlot(mouse, clicked, released, rightClicked, "storage-inventory:" + slotIndex, sx, sy, slot, slotView, selected, slotIndex < Hotbar.HOTBAR_SLOTS, InventoryDragSource.PLAYER, slotIndex);
            if (interaction.hoveredSlot() != null) {
                hoveredSlot = interaction.hoveredSlot();
            }
            droppedOnSlot = droppedOnSlot || interaction.droppedOnSlot();
        }

        renderDraggedInventoryStack(mouse);
        float buttonWidth = Math.min(220.0f, panelWidth);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, layout.closeY(), buttonWidth, layout.closeHeight(), "CLOSE", true), mouse, clicked, this::closeStorageScreen);
        if (released && draggedInventorySlot >= 0) {
            if (!droppedOnSlot) {
                setStatus("Drag cancelled");
            }
            clearInventoryDrag();
        }
        if (hoveredSlot != null) {
            renderSlotHoverHint(mouse, hoveredSlot);
        }
    }

    private StorageSlotInteraction renderStorageSlot(MousePosition mouse, boolean clicked, boolean released, boolean rightClicked, String hoverKey, float x, float y, float size, Hotbar.SlotView slotView, boolean selected, boolean hotbarSlot, InventoryDragSource source, int slotIndex) {
        boolean hovered = contains(mouse, x, y, size, size);
        drawAssetSlot(x, y, size, selected, hotbarSlot);
        if (hovered) {
            drawInventorySlotHoverFrame(hoverKey, x, y, size, settings.uiScale());
        }
        if (draggedInventorySlot == slotIndex && draggedInventorySource == source) {
            drawInventorySlotDragFrame(x, y, size, settings.uiScale());
        }
        if (!slotView.isEmpty()) {
            drawSlotStack(slotView, x, y, size, settings.uiScale());
        }
        boolean droppedOnSlot = false;
        if (hovered && released && draggedInventorySlot >= 0) {
            droppedOnSlot = true;
            moveDraggedStorageSlot(source, slotIndex);
        } else if (hovered && clicked && isShiftDown() && !slotView.isEmpty()) {
            transferStorageSlot(source == InventoryDragSource.STORAGE, slotIndex, Integer.MAX_VALUE);
        } else if (hovered && clicked && !slotView.isEmpty()) {
            draggedInventorySource = source;
            draggedInventorySlot = slotIndex;
        } else if (hovered && rightClicked && !slotView.isEmpty()) {
            transferStorageSlot(source == InventoryDragSource.STORAGE, slotIndex, splitCount(slotView));
        }
        return new StorageSlotInteraction(hovered && !slotView.isEmpty() ? slotView : null, droppedOnSlot);
    }

    private int splitCount(Hotbar.SlotView slotView) {
        return Math.max(1, (slotView.count() + 1) / 2);
    }

    private void transferStorageSlot(boolean fromStorage, int slot, int count) {
        if (!hotbar.storageOpen()) {
            return;
        }
        if (onlineMode && connection != null) {
            sendStorageTransferPacket(fromStorage, slot, GamePacket.StorageTransfer.AUTO_TARGET_SLOT, count);
        } else if (hotbar.transferStorage(fromStorage, slot, count)) {
            setStatus(fromStorage ? "Moved from crate" : "Stored in crate");
            if (fromStorage) {
                announceNewRecipeUnlocks();
            }
            audio.play(AudioCue.INVENTORY_CLICK);
        } else {
            setStatus(fromStorage ? "Backpack full" : "Crate full");
            audio.play(AudioCue.CRAFT_FAIL);
        }
        updateWindowTitle();
    }

    private void moveDraggedStorageSlot(InventoryDragSource targetSource, int targetSlot) {
        if (!hotbar.storageOpen() || draggedInventorySlot < 0) {
            return;
        }
        InventoryDragSource source = draggedInventorySource;
        int sourceSlot = draggedInventorySlot;
        if (source == targetSource && sourceSlot == targetSlot) {
            transferStorageSlot(source == InventoryDragSource.STORAGE, sourceSlot, Integer.MAX_VALUE);
            return;
        }
        if (onlineMode && connection != null) {
            if (source != targetSource) {
                sendStorageTransferPacket(source == InventoryDragSource.STORAGE, sourceSlot, targetSlot, Integer.MAX_VALUE);
            } else {
                setStatus("Server reorder unavailable");
                audio.play(AudioCue.CRAFT_FAIL);
            }
            updateWindowTitle();
            return;
        }
        boolean moved;
        if (source == InventoryDragSource.STORAGE && targetSource == InventoryDragSource.STORAGE) {
            moved = hotbar.moveStorageSlot(sourceSlot, targetSlot);
        } else if (source == InventoryDragSource.PLAYER && targetSource == InventoryDragSource.PLAYER) {
            moved = hotbar.moveInventorySlot(sourceSlot, targetSlot);
        } else if (source == InventoryDragSource.STORAGE) {
            moved = hotbar.moveStorageToInventorySlot(sourceSlot, targetSlot);
        } else {
            moved = hotbar.moveInventoryToStorageSlot(sourceSlot, targetSlot);
        }
        if (moved) {
            setStatus(storageMoveMessage(source, targetSource));
            if (source == InventoryDragSource.STORAGE) {
                announceNewRecipeUnlocks();
            }
            audio.play(AudioCue.INVENTORY_CLICK);
        } else {
            setStatus("Target slot full");
            audio.play(AudioCue.CRAFT_FAIL);
        }
        updateWindowTitle();
    }

    private void sendStorageTransferPacket(boolean fromStorage, int sourceSlot, int targetSlot, int count) {
        connection.send(new GamePacket.StorageTransfer(
                hotbar.storageX(),
                hotbar.storageY(),
                hotbar.storageZ(),
                fromStorage,
                sourceSlot,
                targetSlot,
                count,
                nextClientTransactionId()
        ));
        setStatus("Storage transfer requested");
    }

    private static String storageMoveMessage(InventoryDragSource source, InventoryDragSource target) {
        if (source == target) {
            return source == InventoryDragSource.STORAGE ? "Moved inside crate" : "Moved stack";
        }
        return source == InventoryDragSource.STORAGE ? "Moved from crate" : "Stored in crate";
    }

    private String storageMetaLine() {
        Vector3f position = camera.position();
        float dx = position.x - (hotbar.storageX() + 0.5f);
        float dy = position.y - (hotbar.storageY() + 0.5f);
        float dz = position.z - (hotbar.storageZ() + 0.5f);
        float distance = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        return "POS " + hotbar.storageX() + " " + hotbar.storageY() + " " + hotbar.storageZ()
                + "  DIST " + String.format(Locale.ROOT, "%.1fm", distance);
    }

    private int nextClientTransactionId() {
        clientTransactionId = clientTransactionId == Integer.MAX_VALUE ? 1 : clientTransactionId + 1;
        return clientTransactionId;
    }

    private void closeStorageScreen() {
        hotbar.closeStorage();
        clearInventoryDrag();
        resumeGame();
    }

    static LoadingScreenLayout loadingScreenLayout(int framebufferWidth, int framebufferHeight, float uiScale) {
        float safeUiScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        float layoutScale = Math.max(0.78f, Math.min(safeUiScale, Math.min(1.5f, framebufferHeight / 520.0f)));
        float margin = Math.max(28.0f, 32.0f * layoutScale);
        float contentWidth = Math.min(520.0f * layoutScale, Math.max(220.0f, framebufferWidth - margin * 2.0f));
        float barWidth = Math.max(180.0f, Math.min(contentWidth, framebufferWidth - margin * 2.0f));
        float barHeight = Math.max(16.0f, 18.0f * layoutScale);
        float titleScale = fitTextScale("STREAMING SPAWN", 4.8f * layoutScale, 1.8f, framebufferWidth - margin * 2.0f);
        float detailScale = Math.max(0.95f, Math.min(1.55f * layoutScale, 1.75f));
        float percentScale = Math.max(0.85f, Math.min(1.25f * layoutScale, 1.45f));
        float titleY = Math.max(72.0f, framebufferHeight * 0.28f);
        float detailY = titleY + BitmapFont.textHeight(titleScale) + 20.0f * layoutScale;
        float barY = detailY + BitmapFont.textHeight(detailScale) + 28.0f * layoutScale;
        float bottomLimit = framebufferHeight - margin - barHeight - BitmapFont.textHeight(percentScale);
        if (barY > bottomLimit) {
            barY = Math.max(margin, bottomLimit);
            detailY = Math.max(margin, barY - BitmapFont.textHeight(detailScale) - 24.0f * layoutScale);
            titleY = Math.max(margin, detailY - BitmapFont.textHeight(titleScale) - 18.0f * layoutScale);
        }
        return new LoadingScreenLayout(
                Math.max(framebufferHeight * 0.58f, barY + barHeight + 34.0f * layoutScale),
                framebufferWidth * 0.5f - barWidth * 0.5f,
                barY,
                barWidth,
                barHeight,
                Math.max(2.0f, 2.0f * layoutScale),
                titleY,
                titleScale,
                detailY,
                detailScale,
                barY + barHeight + 12.0f * layoutScale,
                percentScale
        );
    }

    static double loadingBarFill(LoadingScreenViewModel viewModel, double nowSeconds) {
        LoadingScreenViewModel screen = viewModel == null ? LoadingScreenViewModel.boot() : viewModel;
        if (!screen.indeterminate()) {
            return screen.progress();
        }
        double pulse = (Math.sin(nowSeconds * 3.0) + 1.0) * 0.5;
        return 0.18 + pulse * 0.64;
    }

    static StorageScreenLayout storageScreenLayout(int framebufferWidth, int framebufferHeight, float uiScale) {
        float safeUiScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        float layoutScale = Math.max(0.72f, Math.min(safeUiScale, Math.min(1.45f, framebufferHeight / 650.0f)));
        String hint = "DRAG MOVE  SHIFT-CLICK TRANSFER  RIGHT-CLICK SPLIT  E OR ESC CLOSE";
        float horizontalMargin = Math.max(18.0f, 20.0f * layoutScale);
        float titleScale = fitTextScale("STORAGE CRATE", 4.8f * layoutScale, 2.25f, framebufferWidth - horizontalMargin * 2.0f);
        float titleY = Math.max(22.0f, 34.0f * layoutScale);
        float hintScale = fitTextScale(hint, 1.55f * layoutScale, 0.75f, framebufferWidth - horizontalMargin * 2.0f);
        float hintY = titleY + BitmapFont.textHeight(titleScale) + 12.0f * layoutScale;
        float topReserve = hintY + BitmapFont.textHeight(hintScale) + 20.0f * layoutScale;
        float bottomMargin = Math.max(12.0f, 16.0f * layoutScale);
        float sidePad = Math.max(16.0f, 24.0f * layoutScale);
        float gap = Math.max(4.0f, 6.0f * layoutScale);
        float hotbarGap = Math.max(8.0f, 13.0f * layoutScale);
        float header = Math.max(30.0f, 40.0f * layoutScale);
        float panelExtraY = Math.max(12.0f, 20.0f * layoutScale);
        float screenGap = Math.max(12.0f, 18.0f * layoutScale);
        float closeGap = Math.max(10.0f, 12.0f * layoutScale);
        float closeHeight = Math.max(32.0f, Math.min(42.0f * layoutScale, 46.0f));
        float availableWidth = Math.max(220.0f, framebufferWidth - horizontalMargin * 2.0f);
        float desiredSlot = 42.0f * layoutScale;
        float slotByWidth = (availableWidth - sidePad * 2.0f - 8.0f * gap) / 9.0f;
        float availableHeight = Math.max(220.0f, framebufferHeight - topReserve - bottomMargin);
        float fixedHeight = header * 2.0f + gap * 3.0f + hotbarGap + panelExtraY * 2.0f + screenGap + closeGap + closeHeight;
        float slotByHeight = (availableHeight - fixedHeight) / 6.0f;
        float slot = Math.max(18.0f, Math.min(desiredSlot, Math.min(slotByWidth, slotByHeight)));
        float gridWidth = 9.0f * slot + 8.0f * gap;
        float panelWidth = gridWidth + sidePad * 2.0f;
        float panelX = clampFloat(framebufferWidth * 0.5f - panelWidth * 0.5f, horizontalMargin, framebufferWidth - panelWidth - horizontalMargin);
        float gridX = panelX + sidePad;
        float cratePanelHeight = header + slot * 2.0f + gap + panelExtraY;
        float backpackPanelHeight = header + slot * 4.0f + gap * 2.0f + hotbarGap + panelExtraY;
        float cratePanelY = topReserve;
        float crateGridY = cratePanelY + header;
        float backpackPanelY = cratePanelY + cratePanelHeight + screenGap;
        float backpackGridY = backpackPanelY + header;
        float closeY = backpackPanelY + backpackPanelHeight + closeGap;
        float maxCloseY = framebufferHeight - closeHeight - bottomMargin;
        closeY = Math.min(closeY, maxCloseY);
        return new StorageScreenLayout(
                layoutScale,
                panelX,
                gridX,
                panelWidth,
                cratePanelY,
                cratePanelHeight,
                crateGridY,
                backpackPanelY,
                backpackPanelHeight,
                backpackGridY,
                closeY,
                closeHeight,
                slot,
                gap,
                hotbarGap,
                titleY,
                titleScale,
                hintY,
                hintScale
        );
    }

    static CraftingScreenLayout craftingScreenLayout(int framebufferWidth, int framebufferHeight, float uiScale) {
        float safeUiScale = Float.isFinite(uiScale) && uiScale > 0.0f ? uiScale : 1.0f;
        float margin = Math.max(22.0f, Math.min(32.0f * safeUiScale, framebufferWidth * 0.06f));
        float contentWidth = Math.max(420.0f, Math.min(980.0f, framebufferWidth - margin * 2.0f));
        float widthScale = contentWidth / 980.0f;
        float heightScale = Math.max(0.72f, Math.min(1.0f, framebufferHeight / 700.0f));
        float titleScale = fitTextScale("CRAFTING", 5.0f * Math.min(1.0f, Math.max(0.78f, widthScale)), 2.4f, framebufferWidth - margin * 2.0f);
        float titleY = Math.max(28.0f, 52.0f * heightScale);
        float hintScale = fitTextScale("E CLOSE  CLICK RECIPE TO CRAFT  RIGHT-CLICK SPLIT  O SETTINGS", 1.7f * Math.min(1.0f, Math.max(0.76f, widthScale)), 0.85f, framebufferWidth - margin * 2.0f);
        float hintY = titleY + BitmapFont.textHeight(titleScale) + 16.0f * heightScale;
        float contentY = Math.max(hintY + BitmapFont.textHeight(hintScale) + 34.0f * heightScale, framebufferHeight * 0.18f);
        contentY = Math.min(contentY, Math.max(112.0f, framebufferHeight - 360.0f));
        float x = framebufferWidth * 0.5f - contentWidth * 0.5f;
        float tabsY = Math.max(76.0f, contentY - 52.0f);
        float inventoryY = contentY + Math.max(218.0f, 266.0f * Math.min(1.0f, heightScale));
        return new CraftingScreenLayout(
                margin,
                x,
                contentWidth,
                contentY,
                tabsY,
                inventoryY,
                titleY,
                titleScale,
                hintY,
                hintScale
        );
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
        float selectedX = x + Math.min(width - 360.0f, 540.0f);
        float selectedWidth = x + width - selectedX;
        if (selectedWidth >= 210.0f && selectedX > x + tabWidth * 3.0f + 12.0f) {
            uiRenderer.text(fitTextToWidth("Selected: " + hotbar.selectedTooltip(), 1.35f, selectedWidth), selectedX, y + 9.0f, 1.35f, UiColor.MUTED);
        }
    }

    private void renderSettingsMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.035f, 0.047f, 0.045f, 0.96f));
        SettingsScreenLayout layout = settingsScreenLayout(framebufferWidth, framebufferHeight);
        settingsLayoutScale = layout.scale();
        uiRenderer.centeredText("SETTINGS", framebufferWidth * 0.5f, layout.titleY(), layout.titleScale(), UiColor.WHITE);
        uiRenderer.centeredText(fitTextToWidth("BALANCE PERFORMANCE, LOOKS AND COMFORT", layout.subtitleScale(), framebufferWidth - layout.margin() * 2.0f), framebufferWidth * 0.5f, layout.subtitleY(), layout.subtitleScale(), UiColor.MUTED);

        float x = layout.x();
        float y = layout.contentY();
        float panelWidth = layout.panelWidth();
        float scale = layout.scale();
        float gap = layout.columnGap();
        float columnWidth = layout.columnWidth();
        float leftX = x;
        float rightX = x + columnWidth + gap;
        float sectionHeight = layout.sectionHeight();
        float rowTop = 48.0f * scale;
        float rowStep = 42.0f * scale;
        float toggleTwoColumnWidth = columnWidth * 0.5f - 26.0f * scale;
        float toggleTwoColumnRightX = columnWidth * 0.5f + 6.0f * scale;

        settingsSection(leftX, y, columnWidth, sectionHeight, "WORLD & RENDERING");
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop, columnWidth - 36.0f * scale, "Render distance", settings.renderDistanceChunks() + " chunks",
                () -> settings.adjustRenderDistance(-1),
                () -> settings.adjustRenderDistance(1));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep, columnWidth - 36.0f * scale, "World preview", settings.previewRadiusChunks() + " chunks",
                () -> {
                    settings.adjustPreviewRadius(-1);
                    refreshPreview();
                },
                () -> {
                    settings.adjustPreviewRadius(1);
                    refreshPreview();
                });
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 2.0f, columnWidth - 36.0f * scale, "Chunk jobs", settings.chunkGenerationBudgetChunks() + " chunks",
                () -> settings.adjustChunkGenerationBudget(-1),
                () -> settings.adjustChunkGenerationBudget(1));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 3.0f, columnWidth - 36.0f * scale, "Chunk time", formatMilliseconds(settings.chunkGenerationBudgetMilliseconds()),
                () -> settings.adjustChunkGenerationBudgetMilliseconds(-0.5),
                () -> settings.adjustChunkGenerationBudgetMilliseconds(0.5));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 4.0f, columnWidth - 36.0f * scale, "Mesh jobs", settings.meshBuildBudgetChunks() + " chunks",
                () -> settings.adjustMeshBuildBudget(-1),
                () -> settings.adjustMeshBuildBudget(1));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 5.0f, columnWidth - 36.0f * scale, "Mesh time", formatMilliseconds(settings.meshBuildBudgetMilliseconds()),
                () -> settings.adjustMeshBuildBudgetMilliseconds(-0.5),
                () -> settings.adjustMeshBuildBudgetMilliseconds(0.5));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 6.0f, columnWidth - 36.0f * scale, "GPU upload", formatMilliseconds(settings.gpuUploadBudgetMilliseconds()),
                () -> settings.adjustGpuUploadBudgetMilliseconds(-0.5),
                () -> settings.adjustGpuUploadBudgetMilliseconds(0.5));
        settingStepperCompact(mouse, clicked, leftX + 18.0f * scale, y + rowTop + rowStep * 7.0f, columnWidth - 36.0f * scale, "Field of view", settings.fieldOfViewDegrees() + " deg",
                () -> settings.adjustFieldOfView(-5),
                () -> settings.adjustFieldOfView(5));

        settingsSection(rightX, y, columnWidth, sectionHeight, "QUALITY & INTERFACE");
        settingStepperCompact(mouse, clicked, rightX + 18.0f * scale, y + rowTop, columnWidth - 36.0f * scale, "Mouse speed", settings.mouseSensitivityPercent() + "%",
                () -> settings.adjustMouseSensitivity(-10),
                () -> settings.adjustMouseSensitivity(10));
        settingStepperCompact(mouse, clicked, rightX + 18.0f * scale, y + rowTop + rowStep, columnWidth - 36.0f * scale, "UI scale", settings.uiScalePercent() + "%",
                () -> settings.adjustUiScale(-10),
                () -> settings.adjustUiScale(10));
        settingStepperCompact(mouse, clicked, rightX + 18.0f * scale, y + rowTop + rowStep * 2.0f, columnWidth - 36.0f * scale, "Particles", formatPercent(settings.particleQuality()),
                () -> settings.adjustParticleQuality(-0.25),
                () -> settings.adjustParticleQuality(0.25));
        settingStepperCompact(mouse, clicked, rightX + 18.0f * scale, y + rowTop + rowStep * 3.0f, columnWidth - 36.0f * scale, "Render debug", settings.renderDebugView().commandName(),
                settings::cycleRenderDebugView,
                settings::cycleRenderDebugView);
        settingToggleCompact(mouse, clicked, rightX + 18.0f * scale, y + 224.0f * scale, toggleTwoColumnWidth, "Ambient AO", settings.ambientOcclusionEnabled(), () -> {
            settings.toggleAmbientOcclusion();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });
        settingToggleCompact(mouse, clicked, rightX + toggleTwoColumnRightX, y + 224.0f * scale, toggleTwoColumnWidth, "Soft shadows", settings.softShadowsEnabled(), settings::toggleSoftShadows);
        settingToggleCompact(mouse, clicked, rightX + 18.0f * scale, y + 272.0f * scale, toggleTwoColumnWidth, "Fog", settings.fogEnabled(), settings::toggleFog);
        settingToggleCompact(mouse, clicked, rightX + toggleTwoColumnRightX, y + 272.0f * scale, toggleTwoColumnWidth, "Bloom", settings.bloomEnabled(), settings::toggleBloom);
        float toggleGap = 8.0f * scale;
        float thirdToggleWidth = (columnWidth - 36.0f * scale - toggleGap * 2.0f) / 3.0f;
        float thirdToggleX = rightX + 18.0f * scale;
        settingToggleCompact(mouse, clicked, thirdToggleX, y + 320.0f * scale, thirdToggleWidth, "VSync", settings.vsyncEnabled(), () -> {
            settings.toggleVsync();
            glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        });
        settingToggleCompact(mouse, clicked, thirdToggleX + thirdToggleWidth + toggleGap, y + 320.0f * scale, thirdToggleWidth, "Water", settings.transparentWaterEnabled(), () -> {
            settings.toggleTransparentWater();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });
        settingToggleCompact(mouse, clicked, thirdToggleX + (thirdToggleWidth + toggleGap) * 2.0f, y + 320.0f * scale, thirdToggleWidth, "Simple", settings.simpleWaterEnabled(), settings::toggleSimpleWater);
        settingToggleCompact(mouse, clicked, thirdToggleX, y + 368.0f * scale, thirdToggleWidth, "HUD", settings.hudEnabled(), settings::toggleHud);
        settingToggleCompact(mouse, clicked, thirdToggleX + thirdToggleWidth + toggleGap, y + 368.0f * scale, thirdToggleWidth, "Debug", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
        settingToggleCompact(mouse, clicked, thirdToggleX + (thirdToggleWidth + toggleGap) * 2.0f, y + 368.0f * scale, thirdToggleWidth, "Greedy", settings.greedyMeshingEnabled(), () -> {
            settings.toggleGreedyMeshing();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });

        drawModernPanel(
                x,
                layout.presetY(),
                panelWidth,
                layout.presetHeight(),
                10.0f * scale,
                new UiColor(0.040f, 0.058f, 0.050f, 0.88f),
                new UiColor(0.15f, 0.21f, 0.17f, 0.92f),
                UiColor.ACCENT,
                scale
        );
        drawModernInsetPanel(
                x + 8.0f * scale,
                layout.presetY() + 8.0f * scale,
                panelWidth - 16.0f * scale,
                layout.presetHeight() - 16.0f * scale,
                7.0f * scale,
                new UiColor(0.02f, 0.032f, 0.030f, 0.32f),
                new UiColor(0.09f, 0.13f, 0.10f, 0.50f),
                UiColor.ACCENT,
                scale
        );
        uiRenderer.text("PRESET " + settings.activePresetLabel().toUpperCase(Locale.ROOT), x + 18.0f * scale, layout.presetY() + 20.0f * scale, 1.45f * scale, UiColor.MUTED);
        float presetButtonWidth = Math.min(106.0f * scale, (panelWidth - 148.0f * scale) / 3.0f);
        float presetGap = 10.0f * scale;
        float presetX = x + Math.max(112.0f * scale, panelWidth - presetButtonWidth * 3.0f - presetGap * 2.0f - 18.0f * scale);
        float presetButtonY = layout.presetY() + 10.0f * scale;
        float presetButtonHeight = Math.max(26.0f, 34.0f * scale);
        drawModernButton(new UiButton(presetX, presetButtonY, presetButtonWidth, presetButtonHeight, "LOW", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.LOW));
        drawModernButton(new UiButton(presetX + presetButtonWidth + presetGap, presetButtonY, presetButtonWidth, presetButtonHeight, "MEDIUM", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.MEDIUM));
        drawModernButton(new UiButton(presetX + (presetButtonWidth + presetGap) * 2.0f, presetButtonY, presetButtonWidth, presetButtonHeight, "HIGH", true), mouse, clicked, () -> applyRenderPreset(RenderPreset.HIGH));
        if (layout.showControls()) {
            renderSettingsControlsSummary(x, layout.controlsY(), panelWidth);
        }

        float buttonWidth = Math.min(280.0f * scale, framebufferWidth - 80.0f);
        drawModernButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, layout.backY(), buttonWidth, layout.backHeight(), "BACK", true), mouse, clicked, () -> {
            gameState = settingsReturnState;
            setCursorForState();
            updateWindowTitle();
        });
    }

    static SettingsScreenLayout settingsScreenLayout(int framebufferWidth, int framebufferHeight) {
        float margin = Math.max(20.0f, Math.min(36.0f, framebufferWidth * 0.055f));
        float panelWidth = Math.max(360.0f, Math.min(920.0f, framebufferWidth - margin * 2.0f));
        float widthScale = panelWidth / 920.0f;
        float heightScale = Math.max(0.62f, Math.min(1.0f, framebufferHeight / 820.0f));
        float scale = Math.max(0.72f, Math.min(1.0f, Math.min(widthScale, heightScale)));
        float titleScale = fitTextScale("SETTINGS", 5.2f * scale, 2.35f, framebufferWidth - margin * 2.0f);
        float titleY = Math.max(20.0f, 34.0f * scale);
        float subtitleScale = fitTextScale("BALANCE PERFORMANCE, LOOKS AND COMFORT", 1.45f * scale, 0.8f, framebufferWidth - margin * 2.0f);
        float subtitleY = titleY + BitmapFont.textHeight(titleScale) + 8.0f * scale;
        float contentY = subtitleY + BitmapFont.textHeight(subtitleScale) + 14.0f * scale;
        float columnGap = 22.0f * scale;
        float columnWidth = (panelWidth - columnGap) * 0.5f;
        float sectionHeight = 416.0f * scale;
        float presetY = contentY + sectionHeight + 14.0f * scale;
        float presetHeight = 54.0f * scale;
        float controlsY = presetY + presetHeight + 18.0f * scale;
        float controlsHeight = 66.0f * scale;
        float backHeight = Math.max(34.0f, 46.0f * scale);
        float bottomMargin = Math.max(18.0f, 28.0f * scale);
        float backY = framebufferHeight - backHeight - bottomMargin;
        boolean showControls = controlsY + controlsHeight + 18.0f * scale <= backY;
        if (!showControls) {
            backY = Math.max(presetY + presetHeight + 14.0f * scale, backY);
            backY = Math.min(backY, framebufferHeight - backHeight - bottomMargin);
        }
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        return new SettingsScreenLayout(
                scale,
                margin,
                x,
                panelWidth,
                contentY,
                columnGap,
                columnWidth,
                sectionHeight,
                presetY,
                presetHeight,
                controlsY,
                controlsHeight,
                showControls,
                backY,
                backHeight,
                titleY,
                titleScale,
                subtitleY,
                subtitleScale
        );
    }

    private void renderSettingsControlsSummary(float x, float y, float width) {
        float scale = settingsLayoutScale;
        float height = 66.0f * scale;
        drawModernPanel(
                x,
                y,
                width,
                height,
                10.0f * scale,
                new UiColor(0.035f, 0.052f, 0.048f, 0.88f),
                new UiColor(0.17f, 0.22f, 0.19f, 0.92f),
                UiColor.ACCENT,
                scale
        );
        drawModernInsetPanel(
                x + 8.0f * scale,
                y + 8.0f * scale,
                width - 16.0f * scale,
                height - 16.0f * scale,
                7.0f * scale,
                new UiColor(0.02f, 0.032f, 0.030f, 0.36f),
                new UiColor(0.09f, 0.13f, 0.12f, 0.50f),
                UiColor.ACCENT,
                scale
        );
        uiRenderer.text("CONTROLS", x + 18.0f * scale, y + 16.0f * scale, 1.45f * scale, UiColor.WHITE);
        float left = x + 132.0f * scale;
        float right = x + width * 0.55f;
        settingsControlLine(left, y + 14.0f * scale, "MOVE", "WASD SPACE CTRL SHIFT");
        settingsControlLine(left, y + 36.0f * scale, "ACT", "MOUSE E J O ESC");
        settingsControlLine(right, y + 14.0f * scale, "PACK", "DRAG SHIFT-CLICK RIGHT-CLICK");
        settingsControlLine(right, y + 36.0f * scale, "HUD", "F1 CYCLE /HUD MINIMAL|HIDDEN");
    }

    private void settingsControlLine(float x, float y, String label, String value) {
        float scale = settingsLayoutScale;
        uiRenderer.text(label, x, y, 0.95f * scale, UiColor.MUTED);
        uiRenderer.text(fitTextToWidth(value, 1.0f * scale, Math.max(60.0f, 210.0f * scale)), x + 54.0f * scale, y, 1.0f * scale, UiColor.WHITE);
    }

    private void settingsSection(float x, float y, float width, float height, String title) {
        float scale = settingsLayoutScale;
        drawModernPanel(
                x,
                y,
                width,
                height,
                12.0f * scale,
                new UiColor(0.035f, 0.050f, 0.052f, 0.90f),
                new UiColor(0.19f, 0.23f, 0.22f, 0.96f),
                UiColor.ACCENT,
                scale
        );
        uiRenderer.roundedRect(x + 9.0f * scale, y + 8.0f * scale, width - 18.0f * scale, Math.max(2.0f, 3.0f * scale), 2.0f * scale, UiColor.ACCENT);
        uiRenderer.text(fitTextToWidth(title, 2.0f * scale, width - 36.0f * scale), x + 18.0f * scale, y + 18.0f * scale, 2.0f * scale, UiColor.WHITE);
    }

    private void settingStepperCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, String value, Runnable minus, Runnable plus) {
        float scale = settingsLayoutScale;
        float height = 40.0f * scale;
        float buttonSize = Math.max(24.0f, 30.0f * scale);
        drawModernInsetPanel(
                x,
                y,
                width,
                height,
                8.0f * scale,
                new UiColor(0.022f, 0.034f, 0.032f, 0.72f),
                new UiColor(0.12f, 0.16f, 0.15f, 0.82f),
                UiColor.ACCENT,
                scale
        );
        float textWidth = Math.max(40.0f, width - buttonSize * 2.0f - 32.0f * scale);
        uiRenderer.text(fitTextToWidth(label.toUpperCase(Locale.ROOT), 1.15f * scale, textWidth), x + 10.0f * scale, y + 7.0f * scale, 1.15f * scale, UiColor.MUTED);
        uiRenderer.text(fitTextToWidth(value.toUpperCase(Locale.ROOT), 1.2f * scale, textWidth), x + 10.0f * scale, y + 23.0f * scale, 1.2f * scale, UiColor.WHITE);
        drawModernButton(new UiButton(x + width - buttonSize * 2.0f - 10.0f * scale, y + (height - buttonSize) * 0.5f, buttonSize, buttonSize, "-", true), mouse, clicked, minus);
        drawModernButton(new UiButton(x + width - buttonSize - 5.0f * scale, y + (height - buttonSize) * 0.5f, buttonSize, buttonSize, "+", true), mouse, clicked, plus);
    }

    private void settingToggleCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, boolean enabled, Runnable toggle) {
        float scale = settingsLayoutScale;
        float height = 44.0f * scale;
        float buttonWidth = Math.max(48.0f, 68.0f * scale);
        float buttonHeight = Math.max(24.0f, 30.0f * scale);
        drawModernInsetPanel(
                x,
                y,
                width,
                height,
                8.0f * scale,
                new UiColor(0.024f, 0.035f, 0.032f, 0.70f),
                new UiColor(0.13f, 0.17f, 0.14f, 0.84f),
                enabled ? UiColor.ACCENT : UiColor.WARNING,
                scale
        );
        uiRenderer.text(fitTextToWidth(label.toUpperCase(Locale.ROOT), 1.2f * scale, width - buttonWidth - 22.0f * scale), x + 10.0f * scale, y + 8.0f * scale, 1.2f * scale, UiColor.WHITE);
        drawModernToggleButton(mouse, clicked, x + width - buttonWidth - 8.0f * scale, y + (height - buttonHeight) * 0.5f, buttonWidth, buttonHeight, enabled, toggle);
    }

    private void renderCraftingMenu(MousePosition mouse, boolean clicked, float x, float y, EnumSet<CraftingStationType> stationTypes) {
        float panelWidth = Math.min(560.0f, framebufferWidth - x - 32.0f);
        if (panelWidth < 260.0f) {
            return;
        }
        List<CraftingRecipe> recipes = filteredCraftingRecipes(stationTypes);
        int visibleRecipes = Math.min(6, recipes.size());
        float rowHeight = 50.0f;
        float panelHeight = 142.0f + Math.max(1, visibleRecipes) * rowHeight + (recipes.size() > visibleRecipes ? 24.0f : 0.0f);
        drawModernPanel(
                x - 14.0f,
                y - 44.0f,
                panelWidth + 28.0f,
                panelHeight,
                12.0f,
                new UiColor(0.036f, 0.055f, 0.054f, 0.88f),
                new UiColor(0.17f, 0.22f, 0.20f, 0.94f),
                UiColor.ACCENT,
                1.0f
        );
        uiRenderer.roundedRect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, panelHeight - 56.0f, 8.0f, new UiColor(0.018f, 0.030f, 0.028f, 0.28f));
        uiRenderer.text("RECIPES", x, y - 30.0f, 2.6f, UiColor.WHITE);
        float stationX = x + Math.max(126.0f, panelWidth - 210.0f);
        float stationWidth = x + panelWidth - stationX;
        if (stationWidth >= 82.0f) {
            uiRenderer.text(fitTextToWidth("STATION " + stationSetLabel(stationTypes).toUpperCase(Locale.ROOT), 1.2f, stationWidth), stationX, y - 25.0f, 1.2f, UiColor.MUTED);
        }
        renderCraftingFilters(mouse, clicked, x, y + 2.0f, panelWidth);
        renderCraftingSearchField(mouse, clicked, x, y + 34.0f, panelWidth);
        float buttonHeight = 34.0f;
        for (int index = 0; index < visibleRecipes; index++) {
            CraftingRecipe recipe = recipes.get(index);
            boolean unlocked = recipeUnlockedForUi(recipe, stationTypes);
            boolean canCraft = canCraftRecipe(recipe, stationTypes);
            float rowY = y + 76.0f + index * rowHeight;
            String buttonLabel = recipe.label().toUpperCase(Locale.ROOT) + " [" + recipe.category().name() + "]";
            UiButton button = new UiButton(x, rowY, panelWidth, buttonHeight, buttonLabel, canCraft);
            drawModernButtonSurface(
                    button,
                    mouse,
                    clicked,
                    () -> {
                        if (onlineMode) {
                            connection.send(craftRequestFor(recipe));
                            setStatus(craftingRequestMessage(recipe));
                            updateWindowTitle();
                        } else if (activeStationFor(recipe, stationTypes).map(station -> hotbar.craft(recipe, station)).orElse(false)) {
                            String craftedKey = itemKey(recipe.result().itemId());
                            setStatus(earlyGameMilestones.craftItem(
                                            craftedKey,
                                            cozyName(craftedKey),
                                            itemToolType(recipe.result().itemId()))
                                    .orElse("Crafted " + recipe.label()));
                            craftingSuccessPop.trigger(recipe.key(), currentTimeSeconds());
                            announceNewRecipeUnlocks();
                            audio.play(AudioCue.CRAFT_SUCCESS);
                            updateWindowTitle();
                        }
                    },
                    canCraft ? new UiColor(0.085f, 0.135f, 0.105f, 0.92f) : new UiColor(0.055f, 0.065f, 0.062f, 0.74f),
                    canCraft ? new UiColor(0.145f, 0.210f, 0.150f, 0.96f) : new UiColor(0.078f, 0.088f, 0.082f, 0.78f),
                    canCraft ? UiColor.ACCENT : UiColor.WARNING
            );
            if (!canCraft && clicked && button.contains(mouse.x(), mouse.y())) {
                emitCraftingFailure(craftFailMessage(recipe, stationTypes));
            }
            boolean available = recipeAvailableAtCurrent(recipe, stationTypes);
            UiColor stationColor = available ? UiColor.MUTED : UiColor.BUTTON_DISABLED;
            UiColor statusColor = canCraft ? UiColor.ACCENT : unlocked && available ? UiColor.HEART : UiColor.BUTTON_DISABLED;
            float detailX = x + 42.0f;
            float statusX = x + panelWidth - 168.0f;
            float detailWidth = Math.max(80.0f, statusX - detailX - 10.0f);
            float statusWidth = Math.max(72.0f, x + panelWidth - statusX - 8.0f);
            uiRenderer.text(fitTextToWidth(buttonLabel, 1.05f, detailWidth), detailX, rowY + 7.0f, 1.05f, canCraft ? UiColor.WHITE : unlocked ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            uiRenderer.text(fitTextToWidth(stationRequirementLine(recipe), 0.95f, detailWidth), detailX, rowY + 23.0f, 0.95f, stationColor);
            uiRenderer.text(fitTextToWidth(hotbar.recipeSummary(recipe), 0.95f, detailWidth), detailX, rowY + 36.0f, 0.95f, unlocked ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            uiRenderer.text(fitTextToWidth(recipeStatusLine(recipe, stationTypes).toUpperCase(Locale.ROOT), 0.9f, statusWidth), statusX, rowY + 36.0f, 0.9f, statusColor);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), x + 8.0f, rowY + 4.0f, 26.0f);
        }
        if (recipes.isEmpty()) {
            uiRenderer.text("NO MATCHING RECIPES", x + 12.0f, y + 92.0f, 1.55f, UiColor.MUTED);
            if (clicked && contains(mouse, x, y + 76.0f, panelWidth, 42.0f)) {
                emitCraftingFailure("No matching recipe");
            }
        }
        if (recipes.size() > visibleRecipes) {
            uiRenderer.text("MORE RECIPES IN THIS FILTER", x, y + 76.0f + visibleRecipes * rowHeight + 10.0f, 1.15f, UiColor.MUTED);
        }
    }

    private GamePacket craftRequestFor(CraftingRecipe recipe) {
        if (recipe.stationType() != CraftingStationType.INVENTORY && world != null) {
            Optional<ClientWorld.BlockPos> station = nearestStationFor(recipe.stationType());
            if (station.isPresent() && recipe.craftingTimeTicks() > 0) {
                Optional<List<Integer>> inputSlots = hotbar.inputSlotsFor(recipe);
                if (inputSlots.isPresent()) {
                    ClientWorld.BlockPos pos = station.get();
                    return new GamePacket.CookRequest(pos.x(), pos.y(), pos.z(), recipe.key(), inputSlots.get(), nextClientTransactionId());
                }
            } else if (station.isPresent()) {
                ClientWorld.BlockPos pos = station.get();
                return GamePacket.CraftRequest.atStation(recipe.key(), 1, pos.x(), pos.y(), pos.z(), nextClientTransactionId());
            }
        }
        return new GamePacket.CraftRequest(recipe.key(), 1, false, 0, 0, 0, nextClientTransactionId());
    }

    private String craftingRequestMessage(CraftingRecipe recipe) {
        if (recipe.craftingTimeTicks() > 0 && recipe.stationType() != CraftingStationType.INVENTORY) {
            String verb = recipe.stationType() == CraftingStationType.CAMPFIRE
                    || recipe.stationType() == CraftingStationType.COOKING_POT ? "cooking" : "work";
            return "Started " + Hotbar.stationLabel(recipe.stationType()).toLowerCase(Locale.ROOT) + " " + verb;
        }
        return "Crafting requested";
    }

    private Optional<ClientWorld.BlockPos> nearestStationFor(CraftingStationType stationType) {
        if (world == null) {
            return Optional.empty();
        }
        return switch (stationType) {
            case INVENTORY -> Optional.empty();
            case CAMPFIRE -> world.nearestActiveCampfireWithin(camera.position(), CraftingStationRules.STATION_RADIUS_BLOCKS, frameTimeSeconds);
            case COOKING_POT -> world.nearestBlockWithin(camera.position(), Blocks.COOKING_POT, CraftingStationRules.STATION_RADIUS_BLOCKS);
            case WORKBENCH -> world.nearestBlockWithin(camera.position(), Blocks.WORKBENCH, CraftingStationRules.STATION_RADIUS_BLOCKS);
            case FORGE -> world.nearestBlockWithin(camera.position(), Blocks.FORGE, CraftingStationRules.STATION_RADIUS_BLOCKS);
            case CRAFTING_TABLE -> Optional.empty();
        };
    }

    private void emitCraftingFailure(String message) {
        setStatus(message);
        craftingFailureShake.play(craftingFailureShakeClip, currentTimeSeconds());
        audio.play(AudioCue.CRAFT_FAIL);
    }

    private boolean recipeUnlockedForUi(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        return switch (recipe.unlockCondition()) {
            case ALWAYS -> true;
            case NEAR_STATION -> recipeAvailableAtCurrent(recipe, stationTypes);
            case DISCOVERED_ITEM, FOUND_LORE_NOTE, BIOME_DISCOVERED -> false;
        };
    }

    private boolean recipeAvailableAtCurrent(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        return activeStationFor(recipe, stationTypes).isPresent();
    }

    private boolean canCraftRecipe(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        return recipeUnlockedForUi(recipe, stationTypes)
                && activeStationFor(recipe, stationTypes)
                .map(station -> hotbar.canCraft(recipe, station))
                .orElse(false);
    }

    private Optional<CraftingStationType> activeStationFor(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        if (recipe.stationType() == CraftingStationType.INVENTORY) {
            return Optional.of(CraftingStationType.INVENTORY);
        }
        return stationTypes.contains(recipe.stationType()) ? Optional.of(recipe.stationType()) : Optional.empty();
    }

    private String recipeStatusLine(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        if (!recipeUnlockedForUi(recipe, stationTypes)) {
            return "Locked: " + unlockRequirementLine(recipe);
        }
        return activeStationFor(recipe, stationTypes)
                .map(station -> hotbar.canCraft(recipe, station) ? "Ready" : ingredientOrInventoryMessage(recipe))
                .orElse("Need " + Hotbar.stationLabel(recipe.stationType()));
    }

    private String craftFailMessage(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        if (!recipeAvailableAtCurrent(recipe, stationTypes)) {
            return CraftingFeedback.missingStationMessage(recipe.stationType());
        }
        if (!recipeUnlockedForUi(recipe, stationTypes)) {
            return "Locked: " + unlockRequirementLine(recipe);
        }
        return ingredientOrInventoryMessage(recipe);
    }

    private String ingredientOrInventoryMessage(CraftingRecipe recipe) {
        return CraftingFeedback.missingIngredientMessage(
                recipe,
                itemId -> hotbar.itemCount((short) itemId),
                itemId -> cozyName(hotbar.itemKey((short) itemId))
        );
    }

    private String stationRequirementLine(CraftingRecipe recipe) {
        return "Station: " + Hotbar.stationLabel(recipe.stationType());
    }

    private String stationSetLabel(EnumSet<CraftingStationType> stationTypes) {
        int stationCount = stationCount(stationTypes);
        if (stationCount > 2) {
            return Hotbar.stationLabel(primaryCraftingStation(stationTypes)) + " +" + (stationCount - 1);
        }
        StringBuilder builder = new StringBuilder();
        appendStationLabel(builder, stationTypes, CraftingStationType.FORGE);
        appendStationLabel(builder, stationTypes, CraftingStationType.WORKBENCH);
        appendStationLabel(builder, stationTypes, CraftingStationType.COOKING_POT);
        appendStationLabel(builder, stationTypes, CraftingStationType.CAMPFIRE);
        return builder.length() == 0 ? Hotbar.stationLabel(CraftingStationType.INVENTORY) : builder.toString();
    }

    private static void appendStationLabel(StringBuilder builder, EnumSet<CraftingStationType> stationTypes, CraftingStationType stationType) {
        if (!stationTypes.contains(stationType)) {
            return;
        }
        if (builder.length() > 0) {
            builder.append(" + ");
        }
        builder.append(Hotbar.stationLabel(stationType));
    }

    private static int stationCount(EnumSet<CraftingStationType> stationTypes) {
        int count = 0;
        for (CraftingStationType stationType : stationTypes) {
            if (stationType != CraftingStationType.INVENTORY && stationType != CraftingStationType.CRAFTING_TABLE) {
                count++;
            }
        }
        return count;
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
        UiColor color = selected ? new UiColor(0.18f, 0.35f, 0.24f, 0.94f) : new UiColor(0.055f, 0.080f, 0.076f, 0.88f);
        UiColor hoverColor = selected ? new UiColor(0.24f, 0.42f, 0.28f, 0.96f) : new UiColor(0.100f, 0.145f, 0.125f, 0.92f);
        drawModernButtonSurface(
                new UiButton(x, y, width, height, label, true),
                mouse,
                false,
                action,
                color,
                hoverColor,
                selected ? UiColor.ACCENT : UiColor.MUTED
        );
        if (selected) {
            uiRenderer.roundedRect(x + 4.0f, y + height - 4.0f, width - 8.0f, 2.0f, 1.0f, UiColor.ACCENT);
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
        UiColor color = craftingSearchFocused ? new UiColor(0.105f, 0.175f, 0.135f, 0.92f) : hovered ? new UiColor(0.080f, 0.120f, 0.110f, 0.90f) : new UiColor(0.045f, 0.065f, 0.062f, 0.86f);
        drawModernInsetPanel(x, y, width, 28.0f, 7.0f, color, new UiColor(0.13f, 0.17f, 0.15f, 0.84f), craftingSearchFocused ? UiColor.ACCENT : UiColor.MUTED, 1.0f);
        uiRenderer.roundedRect(x + 5.0f, y + 24.0f, width - 10.0f, 2.0f, 1.0f, craftingSearchFocused ? UiColor.ACCENT : withAlpha(UiColor.BUTTON, 0.74f));
        String value = craftingSearch.isEmpty() ? "SEARCH" : craftingSearch.toString().toUpperCase(Locale.ROOT);
        UiColor textColor = craftingSearch.isEmpty() ? UiColor.MUTED : UiColor.WHITE;
        uiRenderer.text(clampText(value, 50), x + 10.0f, y + 9.0f, 1.15f, textColor);
        if (!craftingSearch.isEmpty()) {
            float clearWidth = 24.0f;
            float clearX = x + width - clearWidth - 5.0f;
            boolean clearHovered = contains(mouse, clearX, y + 2.0f, clearWidth, 24.0f);
            drawModernButtonSurface(
                    new UiButton(clearX, y + 2.0f, clearWidth, 24.0f, "X", true),
                    mouse,
                    false,
                    () -> {
                    },
                    clearHovered ? new UiColor(0.16f, 0.22f, 0.18f, 0.92f) : UiColor.BUTTON,
                    new UiColor(0.22f, 0.30f, 0.22f, 0.96f),
                    UiColor.ACCENT
            );
            uiRenderer.centeredText("X", clearX + clearWidth * 0.5f, y + 8.0f, 1.15f, UiColor.WHITE);
            if (clearHovered && clicked) {
                craftingSearch.setLength(0);
                craftingSearchFocused = true;
                audio.play(AudioCue.INVENTORY_CLICK);
            }
        }
    }

    private void renderWorkbenchPreview(CraftingRecipe recipe, float x, float y, EnumSet<CraftingStationType> stationTypes) {
        CraftingStationType stationType = previewStationFor(recipe, stationTypes);
        if (stationType == CraftingStationType.CAMPFIRE) {
            renderCampfirePreview(recipe, x, y);
            return;
        }
        if (stationType == CraftingStationType.COOKING_POT) {
            renderCookingPotPreview(recipe, x, y, stationTypes);
            return;
        }
        float panelWidth = 316.0f;
        float panelHeight = 232.0f;
        String panelSprite = stationType == CraftingStationType.CAMPFIRE ? "panel_campfire" : "panel_crafting";
        drawAssetPanel(panelSprite, x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.76f));
        uiRenderer.rect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, 136.0f, new UiColor(0.018f, 0.030f, 0.028f, 0.22f));
        uiRenderer.text(stationSetLabel(stationTypes).toUpperCase(Locale.ROOT), x, y - 30.0f, 2.6f, UiColor.WHITE);
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
            boolean canCraft = canCraftRecipe(recipe, stationTypes);
            uiRenderer.rect(outX - 4.0f, outY - 4.0f, 58.0f, 58.0f, canCraft ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), outX + 4.0f, outY + 4.0f, 42.0f);
            if (recipe.result().count() > 1) {
                uiRenderer.text(String.valueOf(recipe.result().count()), outX + 38.0f, outY + 39.0f, 1.15f, UiColor.WHITE);
            }
            uiRenderer.text(clampText(recipe.label(), 28), x, y + 158.0f, 1.55f, UiColor.WHITE);
            uiRenderer.text(stationRequirementLine(recipe).toUpperCase(Locale.ROOT), x, y + 178.0f, 1.05f, recipeAvailableAtCurrent(recipe, stationTypes) ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            uiRenderer.text(recipeStatusLine(recipe, stationTypes).toUpperCase(Locale.ROOT), x, y + 196.0f, 1.1f, canCraft ? UiColor.ACCENT : recipeUnlockedForUi(recipe, stationTypes) ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
        }
    }

    static CraftingStationType previewStationFor(CraftingRecipe recipe, EnumSet<CraftingStationType> stationTypes) {
        return recipe == null ? primaryCraftingStation(stationTypes) : recipe.stationType();
    }

    private void renderCookingPotPreview(CraftingRecipe recipe, float x, float y, EnumSet<CraftingStationType> stationTypes) {
        float panelWidth = 316.0f;
        float panelHeight = 270.0f;
        boolean stationReady = stationTypes.contains(CraftingStationType.COOKING_POT);

        drawAssetPanel("panel_crafting", x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.055f, 0.052f, 0.78f));
        uiRenderer.rect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, 136.0f, new UiColor(0.018f, 0.030f, 0.028f, 0.22f));
        uiRenderer.text("COOKING POT", x, y - 30.0f, 2.6f, UiColor.WHITE);
        uiRenderer.text(stationReady ? "READY" : "MOVE CLOSER", x + panelWidth - 112.0f, y - 25.0f, 1.2f, stationReady ? UiColor.ACCENT : UiColor.HEART);

        float slot = 38.0f;
        uiRenderer.text("INGREDIENTS", x, y - 4.0f, 0.95f, UiColor.MUTED);
        if (recipe != null) {
            for (int i = 0; i < Math.min(6, recipe.ingredients().size()); i++) {
                CraftingRecipe.Ingredient ingredient = recipe.ingredients().get(i);
                float sx = x + (i % 3) * 45.0f;
                float sy = y + 10.0f + (i / 3) * 46.0f;
                int owned = hotbar.itemCount(ingredient.itemId());
                boolean hasIngredient = owned >= ingredient.count();
                drawAssetSlot(sx, sy, slot, hasIngredient, false);
                if (!hasIngredient) {
                    uiRenderer.rect(sx + 3.0f, sy + 3.0f, slot - 6.0f, slot - 6.0f, new UiColor(0.55f, 0.10f, 0.12f, 0.34f));
                }
                drawItemIcon(hotbar.itemKey(ingredient.itemId()), sx + 5.0f, sy + 4.0f, 28.0f);
                uiRenderer.text(owned + "/" + ingredient.count(), sx + 6.0f, sy + 26.0f, 0.82f, hasIngredient ? UiColor.WHITE : UiColor.HEART);
            }
        }

        float outX = x + 222.0f;
        float outY = y + 32.0f;
        uiRenderer.text("OUTPUT", outX, y - 4.0f, 0.95f, UiColor.MUTED);
        boolean canCraft = recipe != null && canCraftRecipe(recipe, stationTypes);
        drawAssetSlot(outX, outY, 58.0f, canCraft, false);
        if (recipe != null) {
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), outX + 8.0f, outY + 8.0f, 42.0f);
            if (recipe.result().count() > 1) {
                uiRenderer.text(String.valueOf(recipe.result().count()), outX + 42.0f, outY + 43.0f, 1.05f, UiColor.WHITE);
            }
        }

        float cookY = y + 124.0f;
        String needLine = CraftingFeedback.cookingPotNeedLine(recipe, itemId -> hotbar.itemKey((short) itemId)).toUpperCase(Locale.ROOT);
        uiRenderer.text(needLine, x, cookY, 1.05f, recipe == null ? UiColor.MUTED : UiColor.WHITE);
        if (recipe != null) {
            float cookSeconds = Math.max(1, recipe.craftingTimeTicks()) / 20.0f;
            drawProgressBar(x, cookY + 24.0f, panelWidth, 12.0f, canCraft ? 1.0f : 0.0f, canCraft ? UiColor.ACCENT : UiColor.BUTTON_DISABLED);
            uiRenderer.text("COOK TIME " + formatSeconds(cookSeconds), x, cookY + 44.0f, 1.05f, UiColor.MUTED);
            uiRenderer.text(clampText(recipe.label(), 28), x, cookY + 66.0f, 1.45f, UiColor.WHITE);
            uiRenderer.text(recipeStatusLine(recipe, stationTypes).toUpperCase(Locale.ROOT), x, cookY + 88.0f, 1.05f, canCraft ? UiColor.ACCENT : UiColor.HEART);
        } else {
            drawProgressBar(x, cookY + 24.0f, panelWidth, 12.0f, 0.0f, UiColor.BUTTON_DISABLED);
            uiRenderer.text("SELECT A FOOD RECIPE", x, cookY + 44.0f, 1.05f, UiColor.MUTED);
        }
    }

    private void renderCampfirePreview(CraftingRecipe recipe, float x, float y) {
        float panelWidth = 316.0f;
        float panelHeight = 232.0f;
        Optional<ClientWorld.CampfireStatusView> status = nearestCampfireStatus();
        boolean active = status.map(ClientWorld.CampfireStatusView::active)
                .orElseGet(() -> world != null && world.hasActiveCampfireWithin(camera.position(), CampfireRules.STATION_RADIUS_BLOCKS, frameTimeSeconds));

        drawAssetPanel("panel_campfire", x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.05f, 0.052f, 0.045f, 0.78f));
        uiRenderer.rect(x + 2.0f, y - 2.0f, panelWidth - 4.0f, 136.0f, new UiColor(0.025f, 0.028f, 0.024f, 0.24f));
        uiRenderer.text("CAMPFIRE", x, y - 30.0f, 2.6f, UiColor.WHITE);
        uiRenderer.text(active ? "ACTIVE" : "NEEDS FUEL", x + panelWidth - 106.0f, y - 25.0f, 1.2f, active ? UiColor.ACCENT : UiColor.HEART);

        float slot = 42.0f;
        float inputY = y + 4.0f;
        uiRenderer.text("INPUT", x, y - 4.0f, 0.95f, UiColor.MUTED);
        if (recipe != null) {
            for (int i = 0; i < Math.min(4, recipe.ingredients().size()); i++) {
                CraftingRecipe.Ingredient ingredient = recipe.ingredients().get(i);
                float sx = x + (i % 2) * 48.0f;
                float sy = inputY + (i / 2) * 48.0f;
                int owned = hotbar.itemCount(ingredient.itemId());
                boolean hasIngredient = owned >= ingredient.count();
                drawAssetSlot(sx, sy, slot, false, false);
                if (!hasIngredient) {
                    uiRenderer.rect(sx + 3.0f, sy + 3.0f, slot - 6.0f, slot - 6.0f, new UiColor(0.55f, 0.10f, 0.12f, 0.34f));
                }
                drawItemIcon(hotbar.itemKey(ingredient.itemId()), sx + 6.0f, sy + 5.0f, 30.0f);
                uiRenderer.text(owned + "/" + ingredient.count(), sx + 7.0f, sy + 29.0f, 0.88f, hasIngredient ? UiColor.WHITE : UiColor.HEART);
            }
        }

        float fuelX = x + 118.0f;
        float fuelY = y + 28.0f;
        uiRenderer.text("FUEL", fuelX, y - 4.0f, 0.95f, UiColor.MUTED);
        drawAssetSlot(fuelX, fuelY, slot, active, false);
        selectedFuelItemKey().ifPresent(itemKey -> drawItemIcon(itemKey, fuelX + 6.0f, fuelY + 5.0f, 30.0f));

        float outX = x + 222.0f;
        float outY = y + 28.0f;
        uiRenderer.text("OUTPUT", outX, y - 4.0f, 0.95f, UiColor.MUTED);
        drawAssetSlot(outX, outY, 58.0f, recipe != null && hotbar.canCraft(recipe, CraftingStationType.CAMPFIRE), false);
        if (recipe != null) {
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), outX + 8.0f, outY + 8.0f, 42.0f);
            if (recipe.result().count() > 1) {
                uiRenderer.text(String.valueOf(recipe.result().count()), outX + 42.0f, outY + 43.0f, 1.05f, UiColor.WHITE);
            }
        }
        uiRenderer.rect(x + 174.0f, y + 55.0f, 34.0f, 5.0f, active ? UiColor.ACCENT : UiColor.MUTED);
        uiRenderer.rect(x + 202.0f, y + 50.0f, 10.0f, 15.0f, active ? UiColor.ACCENT : UiColor.MUTED);

        float burnY = y + 142.0f;
        double fuelSeconds = status.map(ClientWorld.CampfireStatusView::fuelSecondsRemaining).orElse(active ? 1.0 : 0.0);
        drawProgressBar(x, burnY, panelWidth, 12.0f, (float) Math.min(1.0, fuelSeconds / 180.0), active ? UiColor.HUNGER : UiColor.BUTTON_DISABLED);
        uiRenderer.text("BURN " + (active ? formatSeconds(fuelSeconds) : "NO FUEL"), x, burnY + 20.0f, 1.05f, active ? UiColor.MUTED : UiColor.HEART);

        float cookY = y + 180.0f;
        float cookProgress = status.filter(ClientWorld.CampfireStatusView::cooking)
                .map(ClientWorld.CampfireStatusView::cookProgress)
                .orElse(0.0f);
        drawProgressBar(x, cookY, panelWidth, 12.0f, cookProgress, UiColor.ACCENT);
        String cookLine = campfireCookLine(recipe, status);
        uiRenderer.text(clampText(cookLine, 42), x, cookY + 20.0f, 1.05f, recipe == null ? UiColor.MUTED : UiColor.WHITE);
    }

    private Optional<ClientWorld.CampfireStatusView> nearestCampfireStatus() {
        if (world == null) {
            return Optional.empty();
        }
        return world.nearestActiveCampfireWithin(camera.position(), CampfireRules.STATION_RADIUS_BLOCKS, frameTimeSeconds)
                .flatMap(pos -> world.campfireStatusAt(pos, frameTimeSeconds));
    }

    private Optional<String> selectedFuelItemKey() {
        return hotbar.selectedItemKey()
                .filter(key -> CampfireRules.fuelSeconds(key).isPresent());
    }

    private String campfireCookLine(CraftingRecipe recipe, Optional<ClientWorld.CampfireStatusView> status) {
        if (status.isPresent() && status.get().cooking()) {
            ClientWorld.CampfireStatusView view = status.get();
            return "COOKING " + recipeLabel(view.cookingRecipeKey()) + "  " + formatSeconds(view.cookSecondsRemaining()) + " LEFT";
        }
        if (recipe == null) {
            return "NO MATCHING RECIPE";
        }
        return "COOK TIME " + formatSeconds(Math.max(1, recipe.craftingTimeTicks()) / 20.0);
    }

    private String recipeLabel(String recipeKey) {
        return hotbar.recipes().stream()
                .filter(recipe -> recipe.key().equals(recipeKey))
                .map(CraftingRecipe::label)
                .findFirst()
                .orElseGet(() -> cozyName(recipeKey));
    }

    private static String formatSeconds(double seconds) {
        if (!Double.isFinite(seconds)) {
            return "0S";
        }
        int rounded = (int) Math.ceil(Math.max(0.0, seconds));
        return rounded + "S";
    }

    private void drawProgressBar(float x, float y, float width, float height, float ratio, UiColor fill) {
        float clamped = Math.max(0.0f, Math.min(1.0f, ratio));
        uiRenderer.rect(x, y, width, height, new UiColor(0.02f, 0.024f, 0.022f, 0.86f));
        uiRenderer.rect(x + 2.0f, y + 2.0f, Math.max(0.0f, (width - 4.0f) * clamped), height - 4.0f, fill);
        uiRenderer.rect(x, y, width, 2.0f, new UiColor(0.78f, 0.76f, 0.64f, 0.28f));
    }

    private CraftingRecipe previewCraftingRecipe(EnumSet<CraftingStationType> stationTypes) {
        List<CraftingRecipe> recipes = filteredCraftingRecipes(stationTypes);
        for (CraftingRecipe recipe : recipes) {
            if (canCraftRecipe(recipe, stationTypes)) {
                return recipe;
            }
        }
        if (!recipes.isEmpty()) {
            return recipes.get(0);
        }
        return null;
    }

    private List<CraftingRecipe> filteredCraftingRecipes(EnumSet<CraftingStationType> stationTypes) {
        return prioritizedRecipes(stationTypes).stream()
                .filter(recipe -> craftingCategoryFilter == null || recipe.category() == craftingCategoryFilter)
                .filter(recipe -> !craftableRecipesOnly || canCraftRecipe(recipe, stationTypes))
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

    private List<CraftingRecipe> prioritizedRecipes(EnumSet<CraftingStationType> stationTypes) {
        CraftingStationType primaryStation = primaryCraftingStation(stationTypes);
        return hotbar.recipes().stream()
                .sorted(Comparator
                        .comparingInt((CraftingRecipe recipe) -> recipeUnlockedForUi(recipe, stationTypes) ? 0 : 1)
                        .thenComparingInt(recipe -> stationPriority(recipe, primaryStation))
                        .thenComparingInt(recipe -> canCraftRecipe(recipe, stationTypes) ? 0 : 1)
                        .thenComparingInt(recipe -> recipeAvailableAtCurrent(recipe, stationTypes) ? 0 : 1)
                        .thenComparing(CraftingRecipe::category)
                        .thenComparing(CraftingRecipe::label))
                .toList();
    }

    private static int stationPriority(CraftingRecipe recipe, CraftingStationType primaryStation) {
        if (recipe.stationType() == primaryStation) {
            return 0;
        }
        if (recipe.stationType() == CraftingStationType.INVENTORY) {
            return 1;
        }
        return 2;
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

    private void renderInventoryGridCompact(MousePosition mouse, boolean clicked, boolean released, boolean rightClicked, float x, float y) {
        float uiScale = settings.uiScale();
        InventoryPanelLayout layout = inventoryPanelLayout(x, y);
        drawInventoryBackdrop(layout.panelX(), layout.panelY(), layout.panelWidth(), layout.panelHeight(), uiScale);
        drawInventoryShelf(layout.x() - 5.0f * uiScale, layout.y() - 5.0f * uiScale, layout.gridWidth() + 10.0f * uiScale, layout.mainRowsHeight() + 10.0f * uiScale, uiScale);
        drawInventoryShelf(layout.x() - 5.0f * uiScale, layout.hotbarY() - 5.0f * uiScale, layout.gridWidth() + 10.0f * uiScale, layout.slot() + 10.0f * uiScale, uiScale);

        UiColor labelColor = new UiColor(0.13f, 0.13f, 0.12f, 1.0f);
        uiRenderer.text("INVENTORY", layout.x(), layout.panelY() + 14.0f * uiScale, 1.35f * uiScale, labelColor);
        uiRenderer.text("HOTBAR", layout.x(), layout.hotbarY() - 14.0f * uiScale, 0.9f * uiScale, labelColor);

        boolean trashEnabled = gameMode == GameMode.CREATIVE;
        if (!trashEnabled) {
            inventoryTrashMode = false;
        }
        float buttonHeight = 26.0f * uiScale;
        float sortWidth = 96.0f * uiScale;
        float trashWidth = 104.0f * uiScale;
        float buttonY = layout.panelY() + 9.0f * uiScale;
        float sortX = layout.panelX() + layout.panelWidth() - sortWidth - 14.0f * uiScale;
        if (trashEnabled) {
            sortX -= trashWidth + 8.0f * uiScale;
        }
        drawInventoryActionButton(new UiButton(sortX, buttonY, sortWidth, buttonHeight, "SORT BAG", true), mouse, clicked, () -> {
            if (hotbar.sortBackpack()) {
                setStatus("Backpack sorted");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            }
        });
        if (trashEnabled) {
            drawInventoryActionButton(new UiButton(layout.panelX() + layout.panelWidth() - trashWidth - 14.0f * uiScale, buttonY, trashWidth, buttonHeight, inventoryTrashMode ? "TRASH ON" : "TRASH", true), mouse, clicked, () -> {
                inventoryTrashMode = !inventoryTrashMode;
                setStatus(inventoryTrashMode ? "Trash mode enabled" : "Trash mode disabled");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            });
        }
        Hotbar.SlotView hoveredSlot = null;
        boolean droppedOnSlot = false;
        for (int displayIndex = 0; displayIndex < INVENTORY_COLUMNS * INVENTORY_ROWS; displayIndex++) {
            int slotIndex = playerInventorySlotIndex(displayIndex);
            if (slotIndex >= hotbar.inventorySlotCount()) {
                continue;
            }
            float sx = inventorySlotX(layout, displayIndex);
            float sy = inventorySlotY(layout, displayIndex);
            boolean selected = slotIndex == hotbar.selectedIndex();
            Hotbar.SlotView slotView = hotbar.slotView(slotIndex);
            boolean hovered = contains(mouse, sx, sy, layout.slot(), layout.slot());
            drawAssetSlot(sx, sy, layout.slot(), selected, slotIndex < Hotbar.HOTBAR_SLOTS);
            if (hovered) {
                drawInventorySlotHoverFrame("inventory:" + slotIndex, sx, sy, layout.slot(), uiScale);
            }
            if (draggedInventorySlot == slotIndex) {
                drawInventorySlotDragFrame(sx, sy, layout.slot(), uiScale);
            }
            if (!slotView.isEmpty()) {
                drawSlotStack(slotView, sx, sy, layout.slot(), uiScale);
                if (hovered) {
                    hoveredSlot = slotView;
                }
            }
            if (hovered && released && draggedInventorySlot >= 0) {
                droppedOnSlot = true;
                if (draggedInventorySlot != slotIndex && hotbar.moveInventorySlot(draggedInventorySlot, slotIndex)) {
                    setStatus("Moved stack");
                    audio.play(AudioCue.INVENTORY_CLICK);
                    updateWindowTitle();
                }
            } else if (hovered && rightClicked && hotbar.splitInventorySlot(slotIndex)) {
                setStatus("Split stack");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && inventoryTrashMode && hotbar.trashInventorySlot(slotIndex)) {
                setStatus("Item trashed");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && isShiftDown() && hotbar.quickMoveInventorySlot(slotIndex)) {
                setStatus(slotIndex < Hotbar.HOTBAR_SLOTS ? "Moved to backpack" : "Moved to hotbar");
                audio.play(AudioCue.INVENTORY_CLICK);
                updateWindowTitle();
            } else if (hovered && clicked && !slotView.isEmpty()) {
                draggedInventorySource = InventoryDragSource.PLAYER;
                draggedInventorySlot = slotIndex;
            }
        }
        renderDraggedInventoryStack(mouse);
        if (released && draggedInventorySlot >= 0) {
            if (!droppedOnSlot) {
                setStatus("Drag cancelled");
            }
            clearInventoryDrag();
        }
        if (hoveredSlot != null) {
            renderSlotHoverHint(mouse, hoveredSlot);
        }
    }

    private InventoryPanelLayout inventoryPanelLayout(float requestedX, float requestedY) {
        float uiScale = settings.uiScale();
        float sidePad = 14.0f * uiScale;
        float topPad = 42.0f * uiScale;
        float bottomPad = 18.0f * uiScale;
        float gap = 6.0f * uiScale;
        float hotbarGap = 14.0f * uiScale;
        float slot = 42.0f * uiScale;
        float margin = Math.min(24.0f * uiScale, Math.max(8.0f * uiScale, framebufferWidth * 0.04f));
        float maxPanelWidth = Math.max(220.0f * uiScale, framebufferWidth - margin * 2.0f);
        float gridWidth = INVENTORY_COLUMNS * slot + (INVENTORY_COLUMNS - 1) * gap;
        if (gridWidth + sidePad * 2.0f > maxPanelWidth) {
            slot = Math.min(slot, (maxPanelWidth - sidePad * 2.0f - (INVENTORY_COLUMNS - 1) * gap) / INVENTORY_COLUMNS);
        }
        float maxPanelHeight = Math.max(180.0f * uiScale, framebufferHeight - margin * 2.0f);
        float maxSlotByHeight = (maxPanelHeight - topPad - bottomPad - hotbarGap - (INVENTORY_MAIN_ROWS - 1) * gap) / (INVENTORY_MAIN_ROWS + 1);
        slot = Math.max(24.0f * uiScale, Math.min(slot, maxSlotByHeight));
        gridWidth = INVENTORY_COLUMNS * slot + (INVENTORY_COLUMNS - 1) * gap;
        float mainRowsHeight = INVENTORY_MAIN_ROWS * slot + (INVENTORY_MAIN_ROWS - 1) * gap;
        float panelWidth = gridWidth + sidePad * 2.0f;
        float panelHeight = topPad + mainRowsHeight + hotbarGap + slot + bottomPad;
        float panelX = requestedX - sidePad;
        panelX = Math.min(panelX, framebufferWidth - panelWidth - margin);
        panelX = Math.max(margin, panelX);
        float panelY = requestedY - topPad;
        panelY = Math.min(panelY, framebufferHeight - panelHeight - margin);
        panelY = Math.max(margin, panelY);
        return new InventoryPanelLayout(
                panelX + sidePad,
                panelY + topPad,
                slot,
                gap,
                hotbarGap,
                gridWidth,
                mainRowsHeight,
                panelX,
                panelY,
                panelWidth,
                panelHeight
        );
    }

    private static int playerInventorySlotIndex(int displayIndex) {
        int mainInventorySlots = INVENTORY_COLUMNS * INVENTORY_MAIN_ROWS;
        if (displayIndex < mainInventorySlots) {
            return Hotbar.HOTBAR_SLOTS + displayIndex;
        }
        return displayIndex - mainInventorySlots;
    }

    private static float inventorySlotX(InventoryPanelLayout layout, int displayIndex) {
        int column = displayIndex % INVENTORY_COLUMNS;
        return layout.x() + column * (layout.slot() + layout.gap());
    }

    private static float inventorySlotY(InventoryPanelLayout layout, int displayIndex) {
        int row = displayIndex / INVENTORY_COLUMNS;
        if (row < INVENTORY_MAIN_ROWS) {
            return layout.y() + row * (layout.slot() + layout.gap());
        }
        return layout.hotbarY();
    }

    private void drawInventoryBackdrop(float x, float y, float width, float height, float uiScale) {
        drawModernPanel(
                x,
                y,
                width,
                height,
                10.0f * uiScale,
                new UiColor(0.60f, 0.59f, 0.53f, 0.96f),
                new UiColor(0.24f, 0.24f, 0.22f, 0.98f),
                new UiColor(0.86f, 0.85f, 0.74f, 1.0f),
                uiScale
        );
        drawModernInsetPanel(
                x + 7.0f * uiScale,
                y + 7.0f * uiScale,
                width - 14.0f * uiScale,
                height - 14.0f * uiScale,
                7.0f * uiScale,
                new UiColor(0.43f, 0.43f, 0.39f, 0.54f),
                new UiColor(0.75f, 0.74f, 0.66f, 0.52f),
                new UiColor(0.92f, 0.91f, 0.82f, 0.86f),
                uiScale
        );
    }

    private void drawInventoryShelf(float x, float y, float width, float height, float uiScale) {
        drawModernInsetPanel(
                x,
                y,
                width,
                height,
                7.0f * uiScale,
                new UiColor(0.32f, 0.32f, 0.29f, 0.44f),
                new UiColor(0.76f, 0.75f, 0.66f, 0.36f),
                new UiColor(0.92f, 0.91f, 0.82f, 0.72f),
                uiScale
        );
    }

    private void drawInventorySlotHoverFrame(String hoverKey, float x, float y, float size, float uiScale) {
        SlotHoverAnimation.Sample hover = slotHoverAnimation.sample(hoverKey, frameTimeSeconds);
        float inset = hover.inset() * uiScale;
        float grow = Math.max(0.0f, (hover.scale() - 1.0f) * size * 0.5f);
        float left = x - inset - grow;
        float top = y - inset - grow;
        float hoveredSize = size + (inset + grow) * 2.0f;
        float thickness = Math.max(2.0f * uiScale, 2.0f);
        UiColor border = new UiColor(0.96f, 0.95f, 0.82f, 0.52f + hover.alpha() * 0.45f);
        UiColor glow = new UiColor(1.0f, 0.98f, 0.72f, hover.alpha() * 0.20f);
        uiRenderer.roundedRect(left, top, hoveredSize, hoveredSize, Math.max(4.0f, hoveredSize * 0.14f), glow);
        uiRenderer.rect(left, top, hoveredSize, thickness, border);
        uiRenderer.rect(left, top + hoveredSize - thickness, hoveredSize, thickness, border);
        uiRenderer.rect(left, top, thickness, hoveredSize, border);
        uiRenderer.rect(left + hoveredSize - thickness, top, thickness, hoveredSize, border);
    }

    private void drawInventorySlotDragFrame(float x, float y, float size, float uiScale) {
        UiColor border = new UiColor(0.45f, 0.74f, 0.42f, 0.86f);
        uiRenderer.rect(x - 3.0f * uiScale, y - 3.0f * uiScale, size + 6.0f * uiScale, 3.0f * uiScale, border);
        uiRenderer.rect(x - 3.0f * uiScale, y + size, size + 6.0f * uiScale, 3.0f * uiScale, border);
        uiRenderer.rect(x - 3.0f * uiScale, y - 3.0f * uiScale, 3.0f * uiScale, size + 6.0f * uiScale, border);
        uiRenderer.rect(x + size, y - 3.0f * uiScale, 3.0f * uiScale, size + 6.0f * uiScale, border);
    }

    private void drawInventoryActionButton(UiButton button, MousePosition mouse, boolean clicked, Runnable action) {
        drawModernButton(
                button,
                mouse,
                clicked,
                action,
                new UiColor(0.19f, 0.26f, 0.19f, 0.94f),
                new UiColor(0.27f, 0.37f, 0.24f, 0.96f),
                UiColor.ACCENT
        );
    }

    private record InventoryPanelLayout(
            float x,
            float y,
            float slot,
            float gap,
            float hotbarGap,
            float gridWidth,
            float mainRowsHeight,
            float panelX,
            float panelY,
            float panelWidth,
            float panelHeight
    ) {
        private float hotbarY() {
            return y + mainRowsHeight + hotbarGap;
        }
    }

    record LoadingScreenLayout(
            float horizonY,
            float barX,
            float barY,
            float barWidth,
            float barHeight,
            float border,
            float titleY,
            float titleScale,
            float detailY,
            float detailScale,
            float percentY,
            float percentScale
    ) {
        float bottom() {
            return percentY + BitmapFont.textHeight(percentScale);
        }
    }

    record StorageScreenLayout(
            float layoutScale,
            float panelX,
            float gridX,
            float panelWidth,
            float cratePanelY,
            float cratePanelHeight,
            float crateGridY,
            float backpackPanelY,
            float backpackPanelHeight,
            float backpackGridY,
            float closeY,
            float closeHeight,
            float slot,
            float gap,
            float hotbarGap,
            float titleY,
            float titleScale,
            float hintY,
            float hintScale
    ) {
        float bottom() {
            return closeY + closeHeight;
        }
    }

    record SettingsScreenLayout(
            float scale,
            float margin,
            float x,
            float panelWidth,
            float contentY,
            float columnGap,
            float columnWidth,
            float sectionHeight,
            float presetY,
            float presetHeight,
            float controlsY,
            float controlsHeight,
            boolean showControls,
            float backY,
            float backHeight,
            float titleY,
            float titleScale,
            float subtitleY,
            float subtitleScale
    ) {
        float bottom() {
            return backY + backHeight;
        }
    }

    record CraftingScreenLayout(
            float margin,
            float x,
            float contentWidth,
            float contentY,
            float tabsY,
            float inventoryY,
            float titleY,
            float titleScale,
            float hintY,
            float hintScale
    ) {
    }

    private record StorageSlotInteraction(Hotbar.SlotView hoveredSlot, boolean droppedOnSlot) {
    }

    private enum InventoryDragSource {
        PLAYER,
        STORAGE
    }

    private void renderDraggedInventoryStack(MousePosition mouse) {
        if (draggedInventorySlot < 0) {
            return;
        }
        Hotbar.SlotView slotView = draggedInventorySource == InventoryDragSource.STORAGE && gameState == GameState.STORAGE
                ? hotbar.storageSlotView(draggedInventorySlot)
                : hotbar.slotView(draggedInventorySlot);
        if (slotView.isEmpty()) {
            clearInventoryDrag();
            return;
        }
        float uiScale = settings.uiScale();
        float size = Math.max(28.0f, 38.0f * Math.min(uiScale, 1.4f));
        float x = Math.min((float) mouse.x() + 14.0f * uiScale, framebufferWidth - size - 8.0f);
        float y = Math.min((float) mouse.y() + 14.0f * uiScale, framebufferHeight - size - 8.0f);
        x = Math.max(8.0f, x);
        y = Math.max(8.0f, y);
        drawAssetSlot(x, y, size, false, draggedInventorySource == InventoryDragSource.PLAYER && draggedInventorySlot < Hotbar.HOTBAR_SLOTS);
        drawSlotStack(slotView, x, y, size, uiScale);
    }

    private void renderSlotHoverHint(MousePosition mouse, Hotbar.SlotView slotView) {
        List<TooltipLine> lines = tooltipLines(slotView);
        float scale = 1.08f * settings.uiScale();
        float lineHeight = 15.0f * settings.uiScale();
        float width = 0.0f;
        for (TooltipLine line : lines) {
            width = Math.max(width, BitmapFont.textWidth(line.text(), scale));
        }
        float screenMargin = Math.max(10.0f, 12.0f * settings.uiScale());
        float maxWidth = Math.max(80.0f, framebufferWidth - screenMargin * 2.0f);
        float minWidth = Math.min(220.0f * settings.uiScale(), maxWidth);
        width = Math.min(Math.max(minWidth, width + 18.0f * settings.uiScale()), maxWidth);
        float height = lines.size() * lineHeight + 16.0f * settings.uiScale();
        float maxX = Math.max(screenMargin, framebufferWidth - width - screenMargin);
        float maxY = Math.max(screenMargin, framebufferHeight - height - screenMargin);
        float x = Math.max(screenMargin, Math.min((float) mouse.x() + 16.0f * settings.uiScale(), maxX));
        float y = Math.max(screenMargin, Math.min((float) mouse.y() - height - 10.0f * settings.uiScale(), maxY));
        uiRenderer.rect(x, y, width, height, new UiColor(0.035f, 0.035f, 0.032f, 0.97f));
        uiRenderer.rect(x, y, width, 2.0f * settings.uiScale(), rarityColor(slotView.rarity()));
        float textY = y + 8.0f * settings.uiScale();
        float textWidth = Math.max(20.0f, width - 18.0f * settings.uiScale());
        for (TooltipLine line : lines) {
            uiRenderer.text(fitTextToWidth(line.text(), scale, textWidth), x + 9.0f * settings.uiScale(), textY, scale, line.color());
            textY += lineHeight;
        }
    }

    private void drawSlotStack(Hotbar.SlotView slotView, float x, float y, float size, float uiScale) {
        if (slotView.isEmpty()) {
            return;
        }
        float localScale = Math.max(0.62f, Math.min(Math.max(0.75f, uiScale), size / 38.0f));
        float inset = Math.max(3.0f, Math.min(size * 0.18f, 5.5f * localScale));
        float iconSize = Math.max(12.0f, size - inset * 2.0f);
        drawItemIcon(slotView.itemKey(), x + inset, y + Math.max(2.0f, inset - 1.0f), iconSize);
        if (slotView.count() > 1) {
            String count = String.valueOf(slotView.count());
            float countScale = Math.max(0.62f, Math.min(1.05f * localScale, size / 36.0f));
            float countWidth = BitmapFont.textWidth(count, countScale);
            uiRenderer.text(count, x + size - countWidth - 3.0f * localScale, y + size - BitmapFont.textHeight(countScale) - 3.0f * localScale, countScale, UiColor.WHITE);
        }
        if (slotView.hasDurability()) {
            drawDurabilityBar(x + inset, y + size - Math.max(5.0f, 5.0f * localScale), size - inset * 2.0f, slotView.durabilityLeft(), slotView.maxDurability());
        }
    }

    private List<TooltipLine> tooltipLines(Hotbar.SlotView slotView) {
        java.util.ArrayList<TooltipLine> lines = new java.util.ArrayList<>();
        String title = slotView.count() > 1 ? slotView.label() + " x" + slotView.count() : slotView.label();
        lines.add(new TooltipLine(title, UiColor.WHITE));
        if (!slotView.rarity().isBlank() || !slotView.category().isBlank()) {
            String meta = (slotView.rarity().isBlank() ? "" : slotView.rarity())
                    + (slotView.category().isBlank() ? "" : " " + slotView.category());
            lines.add(new TooltipLine(meta.trim(), rarityColor(slotView.rarity())));
        }
        if (!slotView.description().isBlank()) {
            lines.add(new TooltipLine(slotView.description(), UiColor.MUTED));
        }
        if (!slotView.toolTypeLabel().isBlank()) {
            lines.add(new TooltipLine(slotView.toolTypeLabel() + " Level " + slotView.toolLevel()
                    + "  Speed x" + formatToolSpeed(slotView.toolSpeed()), UiColor.WHITE));
        }
        if (slotView.hasDurability()) {
            lines.add(new TooltipLine("Durability " + slotView.durabilityLeft() + "/" + slotView.maxDurability(), durabilityTextColor(slotView)));
            lines.add(new TooltipLine("Repair: " + slotView.repairMaterialLabel(), UiColor.MUTED));
            toolComparisonLine(slotView).ifPresent(lines::add);
        }
        if (slotView.foodValue() > 0 || slotView.healValue() > 0) {
            String food = "Food +" + slotView.foodValue() + (slotView.healValue() > 0 ? "  Heal +" + slotView.healValue() : "");
            lines.add(new TooltipLine(food, UiColor.HUNGER));
        }
        if (slotView.comfortValue() > 0) {
            lines.add(new TooltipLine("Comfort +" + slotView.comfortValue(), UiColor.ENERGY));
        }
        if (slotView.placeable()) {
            lines.add(new TooltipLine("Placeable", UiColor.ACCENT));
        }
        return lines;
    }

    private Optional<TooltipLine> toolComparisonLine(Hotbar.SlotView slotView) {
        Hotbar.SlotView selected = hotbar.slotView(hotbar.selectedIndex());
        if (selected.isEmpty() || selected.itemKey().equals(slotView.itemKey()) || selected.toolTypeLabel().isBlank()) {
            return Optional.empty();
        }
        if (!selected.toolTypeLabel().equals(slotView.toolTypeLabel())) {
            return Optional.of(new TooltipLine("Selected: " + selected.toolTypeLabel() + " tool", UiColor.MUTED));
        }
        int levelDelta = slotView.toolLevel() - selected.toolLevel();
        float hoveredDurability = slotView.maxDurability() <= 0 ? 0.0f : slotView.durabilityLeft() / (float) slotView.maxDurability();
        float selectedDurability = selected.maxDurability() <= 0 ? 0.0f : selected.durabilityLeft() / (float) selected.maxDurability();
        int durabilityDelta = Math.round((hoveredDurability - selectedDurability) * 100.0f);
        int speedDelta = Math.round((slotView.toolSpeed() - selected.toolSpeed()) * 100.0f);
        String level = signed(levelDelta) + " level";
        String durability = signed(durabilityDelta) + "% durability";
        String speed = signed(speedDelta) + "% speed";
        UiColor color = levelDelta > 0 || durabilityDelta > 10 || speedDelta > 5
                ? UiColor.ACCENT
                : levelDelta < 0 || durabilityDelta < -10 || speedDelta < -5 ? UiColor.HEART : UiColor.MUTED;
        return Optional.of(new TooltipLine("Vs selected: " + level + ", " + durability + ", " + speed, color));
    }

    private static String signed(int value) {
        return value > 0 ? "+" + value : String.valueOf(value);
    }

    private static String formatToolSpeed(float speed) {
        if (speed == Math.round(speed)) {
            return Integer.toString(Math.round(speed));
        }
        String text = String.format(Locale.ROOT, "%.2f", speed);
        while (text.endsWith("0")) {
            text = text.substring(0, text.length() - 1);
        }
        return text.endsWith(".") ? text.substring(0, text.length() - 1) : text;
    }

    private static UiColor rarityColor(String rarity) {
        return switch (rarity) {
            case "Legendary" -> UiColor.ENERGY;
            case "Rare" -> UiColor.WATER;
            case "Uncommon" -> UiColor.ACCENT;
            default -> UiColor.MUTED;
        };
    }

    private static UiColor durabilityTextColor(Hotbar.SlotView slotView) {
        if (!slotView.hasDurability()) {
            return UiColor.MUTED;
        }
        float ratio = slotView.durabilityLeft() / (float) slotView.maxDurability();
        return ratio < 0.22f ? UiColor.HEART : ratio < 0.50f ? UiColor.HUNGER : UiColor.ACCENT;
    }

    private record TooltipLine(String text, UiColor color) {
    }

    private void renderFeedbackOverlay(float centerX, float bottomY) {
        List<FeedbackLog.VisibleEntry> entries = feedbackLog.visibleEntries(frameTimeSeconds);
        if (entries.isEmpty()) {
            return;
        }
        float uiScale = settings.uiScale();
        float scale = 1.15f * uiScale;
        float lineHeight = 18.0f * uiScale;
        float width = 0.0f;
        float maxAlpha = 0.0f;
        FeedbackLog.Kind strongestKind = FeedbackLog.Kind.INFO;
        for (FeedbackLog.VisibleEntry entry : entries) {
            width = Math.max(width, BitmapFont.textWidth(clampText(entry.message(), 48), scale));
            maxAlpha = Math.max(maxAlpha, entry.alpha());
            if (entry.kind().priority() > strongestKind.priority()) {
                strongestKind = entry.kind();
            }
        }
        width = Math.min(width + 24.0f * uiScale, Math.max(240.0f * uiScale, framebufferWidth - 48.0f * uiScale));
        float height = entries.size() * lineHeight + 14.0f * uiScale;
        float margin = 18.0f * uiScale;
        float x = Math.max(margin, Math.min(centerX - width * 0.5f, framebufferWidth - width - margin));
        float y = Math.max(24.0f * uiScale, bottomY - height);
        uiRenderer.rect(x, y, width, height, new UiColor(0.025f, 0.035f, 0.032f, 0.46f + maxAlpha * 0.22f));
        uiRenderer.rect(x, y, width, 2.0f * uiScale, feedbackToneColor(strongestKind, maxAlpha));
        float textY = y + 9.0f * uiScale;
        for (FeedbackLog.VisibleEntry entry : entries) {
            UiColor toneColor = feedbackToneColor(entry.kind(), entry.alpha());
            UiColor textColor = feedbackTextColor(entry.kind(), entry.alpha());
            uiRenderer.rect(x + 9.0f * uiScale, textY + 4.0f * uiScale, Math.max(2.0f, 3.0f * uiScale), Math.max(7.0f * uiScale, lineHeight - 8.0f * uiScale), toneColor);
            uiRenderer.centeredText(clampText(entry.message(), 48), x + width * 0.5f, textY, scale, textColor);
            textY += lineHeight;
        }
    }

    private static UiColor feedbackToneColor(FeedbackLog.Kind kind, float alpha) {
        UiColor base = feedbackToneBase(kind);
        return new UiColor(base.r(), base.g(), base.b(), alpha);
    }

    private static UiColor feedbackTextColor(FeedbackLog.Kind kind, float alpha) {
        UiColor base = switch (kind) {
            case INFO -> UiColor.WHITE;
            case SUCCESS -> UiColor.ACCENT;
            case COMFORT -> UiColor.ENERGY;
            case WARNING -> UiColor.WARNING;
            case DISCOVERY -> UiColor.WATER;
            case UNLOCK -> UiColor.ACCENT;
        };
        return new UiColor(base.r(), base.g(), base.b(), alpha);
    }

    private static UiColor feedbackToneBase(FeedbackLog.Kind kind) {
        return switch (kind) {
            case INFO -> UiColor.MUTED;
            case SUCCESS -> UiColor.ACCENT;
            case COMFORT -> UiColor.ENERGY;
            case WARNING -> UiColor.WARNING;
            case DISCOVERY -> UiColor.WATER;
            case UNLOCK -> UiColor.ACCENT;
        };
    }

    private void renderInteractionHud(float uiScale) {
        Optional<dev.voxelgame.common.math.Raycast.Hit> picked = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (picked.isEmpty()) {
            return;
        }
        dev.voxelgame.common.math.Raycast.Hit hit = picked.get();
        Optional<BlockType> target = world.targetBlock(hit);
        if (target.isEmpty()) {
            return;
        }
        if (!CampfireRules.isCampfire(target.get().id())) {
            if (target.get().id() == Blocks.COOKING_POT) {
                renderCookingPotInteractionHud(uiScale);
            } else if (target.get().id() == Blocks.WORKBENCH) {
                renderCraftingStationInteractionHud(CraftingStationType.WORKBENCH, "WORKBENCH READY", "PRESS E TO CRAFT", uiScale);
            } else if (target.get().id() == Blocks.FORGE) {
                renderCraftingStationInteractionHud(CraftingStationType.FORGE, "FORGE READY", "PRESS E TO FORGE", uiScale);
            }
            return;
        }

        renderCampfireInteractionHud(hit, target.get(), uiScale);
    }

    private void renderCampfireInteractionHud(Raycast.Hit hit, BlockType block, float uiScale) {
        Optional<ClientWorld.CampfireStatusView> status = world.campfireStatusAt(hit.x(), hit.y(), hit.z(), frameTimeSeconds);
        boolean active = status.map(ClientWorld.CampfireStatusView::active)
                .orElseGet(() -> CampfireRules.isActiveCampfire(block.id()));
        double fuelSeconds = status.map(ClientWorld.CampfireStatusView::fuelSecondsRemaining).orElse(0.0);
        double heldFuelSeconds = selectedFuelItemKey()
                .map(key -> CampfireRules.fuelSeconds(key).orElse(0.0))
                .orElse(0.0);
        String title = active ? "CAMPFIRE ACTIVE" : "CAMPFIRE INACTIVE";
        String detail = campfireFuelLine(active, fuelSeconds, heldFuelSeconds);
        float cookProgress = status.filter(ClientWorld.CampfireStatusView::cooking)
                .map(ClientWorld.CampfireStatusView::cookProgress)
                .orElse(0.0f);
        CraftingRecipe recipe = stationPreviewRecipe(CraftingStationType.CAMPFIRE, EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.CAMPFIRE));
        String extra = campfireHudExtraLine(recipe, status, active);
        float progress = cookProgress > 0.0f
                ? cookProgress
                : active ? (float) Math.min(1.0, fuelSeconds / 180.0) : 0.0f;

        renderInteractionStatusCard(title, detail, extra, active ? UiColor.ACCENT : UiColor.HEART, active ? UiColor.WHITE : UiColor.HEART, uiScale, progress);
    }

    private void renderCookingPotInteractionHud(float uiScale) {
        EnumSet<CraftingStationType> stationTypes = EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.COOKING_POT);
        CraftingRecipe previewRecipe = stationPreviewRecipe(CraftingStationType.COOKING_POT, stationTypes);
        long readyRecipes = hotbar.recipes().stream()
                .filter(recipe -> recipe.stationType() == CraftingStationType.COOKING_POT)
                .filter(recipe -> canCraftRecipe(recipe, stationTypes))
                .count();
        String detail = readyRecipes > 0
                ? readyRecipes + (readyRecipes == 1 ? " RECIPE READY" : " RECIPES READY")
                : stationHudMissingLine(previewRecipe, stationTypes, CraftingStationType.COOKING_POT);
        String extra = previewRecipe == null
                ? "PRESS E TO VIEW RECIPES"
                : readyRecipes > 0 ? "PRESS E: " + previewRecipe.label().toUpperCase(Locale.ROOT) : "OUTPUT " + cozyName(hotbar.itemKey(previewRecipe.result().itemId())).toUpperCase(Locale.ROOT);
        renderInteractionStatusCard("COOKING POT READY", detail, extra, readyRecipes > 0 ? UiColor.ACCENT : UiColor.HEART, UiColor.WHITE, uiScale, readyRecipes > 0 ? 1.0f : 0.0f);
    }

    private void renderCraftingStationInteractionHud(CraftingStationType stationType, String title, String action, float uiScale) {
        EnumSet<CraftingStationType> stationTypes = EnumSet.of(CraftingStationType.INVENTORY, stationType);
        CraftingRecipe previewRecipe = stationPreviewRecipe(stationType, stationTypes);
        long readyRecipes = hotbar.recipes().stream()
                .filter(recipe -> recipe.stationType() == stationType)
                .filter(recipe -> canCraftRecipe(recipe, stationTypes))
                .count();
        String detail = readyRecipes > 0
                ? readyRecipes + (readyRecipes == 1 ? " RECIPE READY" : " RECIPES READY")
                : stationHudMissingLine(previewRecipe, stationTypes, stationType);
        String extra = previewRecipe == null || readyRecipes == 0
                ? action
                : action + ": " + previewRecipe.label().toUpperCase(Locale.ROOT);
        renderInteractionStatusCard(title, detail, extra, readyRecipes > 0 ? UiColor.ACCENT : UiColor.HEART, UiColor.WHITE, uiScale, readyRecipes > 0 ? 1.0f : 0.0f);
    }

    private String campfireFuelLine(boolean active, double fuelSeconds, double heldFuelSeconds) {
        if (active && fuelSeconds > 0.0 && heldFuelSeconds > 0.0) {
            return "BURN " + formatSeconds(fuelSeconds) + "  HELD FUEL +" + formatSeconds(heldFuelSeconds);
        }
        if (active && fuelSeconds > 0.0) {
            return "BURN " + formatSeconds(fuelSeconds);
        }
        if (heldFuelSeconds > 0.0) {
            return "HELD FUEL +" + formatSeconds(heldFuelSeconds);
        }
        return active ? "ADD FUEL TO KEEP ACTIVE" : "NEEDS FUEL IN HAND";
    }

    private String campfireHudExtraLine(
            CraftingRecipe recipe,
            Optional<ClientWorld.CampfireStatusView> status,
            boolean active
    ) {
        if (status.isPresent() && status.get().cooking()) {
            ClientWorld.CampfireStatusView view = status.get();
            return view.cookProgress() >= 0.99f
                    ? "OUTPUT READY " + recipeLabel(view.cookingRecipeKey()).toUpperCase(Locale.ROOT)
                    : "COOKING " + recipeLabel(view.cookingRecipeKey()).toUpperCase(Locale.ROOT) + " " + Math.round(view.cookProgress() * 100.0f) + "%";
        }
        if (!active) {
            return "RIGHT CLICK WITH FUEL";
        }
        if (recipe == null) {
            return "NO MATCHING RECIPE";
        }
        return recipeStatusLine(recipe, EnumSet.of(CraftingStationType.INVENTORY, CraftingStationType.CAMPFIRE)).toUpperCase(Locale.ROOT);
    }

    private String stationHudMissingLine(
            CraftingRecipe recipe,
            EnumSet<CraftingStationType> stationTypes,
            CraftingStationType stationType
    ) {
        if (recipe != null) {
            return recipeStatusLine(recipe, stationTypes).toUpperCase(Locale.ROOT);
        }
        return switch (stationType) {
            case COOKING_POT -> "NEEDS WATER, BOWL OR FOOD";
            case FORGE -> "ORE, FUEL AND HEAT REQUIRED";
            case WORKBENCH -> "NEEDS WORKBENCH MATERIALS";
            case CAMPFIRE -> "NO MATCHING RECIPE";
            case CRAFTING_TABLE -> "NEEDS CRAFTING TABLE";
            case INVENTORY -> "OPEN INVENTORY";
        };
    }

    private CraftingRecipe stationPreviewRecipe(CraftingStationType stationType, EnumSet<CraftingStationType> stationTypes) {
        return hotbar.recipes().stream()
                .filter(recipe -> recipe.stationType() == stationType)
                .filter(recipe -> recipeUnlockedForUi(recipe, stationTypes))
                .sorted(Comparator
                        .comparingInt((CraftingRecipe recipe) -> canCraftRecipe(recipe, stationTypes) ? 0 : 1)
                        .thenComparing(CraftingRecipe::label))
                .findFirst()
                .orElse(null);
    }

    private void renderInteractionStatusCard(String title, String detail, String extra, UiColor accent, UiColor titleColor, float uiScale) {
        renderInteractionStatusCard(title, detail, extra, accent, titleColor, uiScale, 0.0f);
    }

    private void renderInteractionStatusCard(String title, String detail, String extra, UiColor accent, UiColor titleColor, float uiScale, float progress) {
        InteractionStatusCard card = InteractionStatusCard.from(title, detail, extra, uiScale, framebufferWidth, progress);
        float width = card.width();
        float height = card.height();
        float x = framebufferWidth * 0.5f - width * 0.5f;
        float y = framebufferHeight * 0.5f + 28.0f * uiScale;
        uiRenderer.rect(x, y, width, height, new UiColor(0.02f, 0.028f, 0.025f, 0.62f));
        uiRenderer.rect(x, y, width, 2.0f * uiScale, accent);
        uiRenderer.centeredText(card.title(), framebufferWidth * 0.5f, y + 8.0f * uiScale, card.titleScale(), titleColor);
        uiRenderer.centeredText(card.detail(), framebufferWidth * 0.5f, y + 27.0f * uiScale, card.detailScale(), UiColor.MUTED);
        if (!card.extra().isBlank()) {
            uiRenderer.centeredText(card.extra(), framebufferWidth * 0.5f, y + 45.0f * uiScale, card.extraScale(), accent);
        }
        if (card.progressVisible()) {
            drawProgressBar(x + 12.0f * uiScale, y + height - 15.0f * uiScale, width - 24.0f * uiScale, Math.max(5.0f, 7.0f * uiScale), card.progress(), accent);
        }
    }

    private void renderInteractionReticle(float uiScale) {
        if (gameState != GameState.PLAYING) {
            return;
        }
        Optional<InteractionHint> hint = currentInteractionHint();
        if (hint.isEmpty()) {
            return;
        }
        UiColor accent = withAlpha(interactionHintAccent(hint.get().tone()), 0.74f);
        float centerX = framebufferWidth * 0.5f;
        float centerY = framebufferHeight * 0.5f;
        float gap = 15.0f * uiScale;
        float length = 7.0f * uiScale;
        float thickness = Math.max(1.0f, 1.5f * uiScale);
        uiRenderer.rect(centerX - gap - length, centerY - gap, length, thickness, accent);
        uiRenderer.rect(centerX + gap, centerY - gap, length, thickness, accent);
        uiRenderer.rect(centerX - gap - length, centerY + gap, length, thickness, accent);
        uiRenderer.rect(centerX + gap, centerY + gap, length, thickness, accent);
    }

    private void renderHud(MousePosition mouse) {
        if ((gameState != GameState.PLAYING && gameState != GameState.CHAT) || world == null) {
            return;
        }
        float uiScale = settings.uiScale();
        GameSettings.HudMode hudMode = settings.debugOverlayEnabled() ? GameSettings.HudMode.NORMAL : settings.hudMode();
        if (hudMode == GameSettings.HudMode.HIDDEN) {
            return;
        }
        boolean minimalHud = hudMode == GameSettings.HudMode.MINIMAL;
        HudLayout hud = HudLayout.forViewport(framebufferWidth, framebufferHeight, uiScale, hudMode);
        renderSurvivalScreenFeedback(uiScale);
        float crosshairLength = 10.0f * uiScale;
        float crosshairThickness = Math.max(1.0f, 2.0f * uiScale);
        uiRenderer.rect(framebufferWidth * 0.5f - crosshairLength * 0.5f, framebufferHeight * 0.5f - crosshairThickness * 0.5f, crosshairLength, crosshairThickness, UiColor.WHITE);
        uiRenderer.rect(framebufferWidth * 0.5f - crosshairThickness * 0.5f, framebufferHeight * 0.5f - crosshairLength * 0.5f, crosshairThickness, crosshairLength, UiColor.WHITE);
        renderBreakOverlay();
        if (!minimalHud) {
            renderInteractionHud(uiScale);
        }
        renderInteractionReticle(uiScale);

        ComfortHudInfo comfortInfo = currentComfortHudInfo();
        WorldHudInfo worldInfo = currentWorldHudInfo();
        List<HudStat> primaryStats = primaryHudStats(minimalHud);
        List<HudStat> secondaryStats = secondaryHudStats(minimalHud);
        Optional<HudStat> comfortStat = comfortHudStat(hud, minimalHud, comfortInfo);
        renderInteractionHint(hud.interactionBottomLimitY());
        if (hud.showSelectedTooltip() && secondaryStats.isEmpty()) {
            uiRenderer.centeredText(fitTextToWidth(hotbar.selectedTooltip(), hud.selectedTooltipScale(), hud.hotbarWidth()), framebufferWidth * 0.5f, hud.selectedTooltipY(), hud.selectedTooltipScale(), UiColor.WHITE);
        }
        drawHudStatRow(primaryStats, hud, hud.statsY());
        comfortStat.ifPresent(stat -> drawHudStatRow(List.of(stat), hud, hud.comfortY()));
        if (comfortStat.isPresent()) {
            renderComfortDetail(hud, comfortInfo);
        }
        drawHudSecondaryStats(secondaryStats, hud);
        renderWorldHudInfo(hud, worldInfo);
        if (hud.showMode()) {
            uiRenderer.text("MODE " + gameMode.name(), Math.max(18.0f * hud.scale(), framebufferWidth - 150.0f * hud.scale()), 18.0f * hud.scale(), 1.0f * hud.scale(), UiColor.MUTED);
        }
        renderFeedbackOverlay(framebufferWidth * 0.5f, feedbackBottomY(hud, secondaryStats));
        for (int i = 0; i < Hotbar.HOTBAR_SLOTS; i++) {
            float x = hud.hotbarX() + i * (hud.hotbarSlotSize() + hud.hotbarGap());
            float y = hud.hotbarY();
            Hotbar.SlotView slot = hotbar.slotView(i);
            boolean selected = i == hotbar.selectedIndex();
            drawAssetSlot(x, y, hud.hotbarSlotSize(), selected, true);
            if (contains(mouse, x, y, hud.hotbarSlotSize(), hud.hotbarSlotSize())) {
                drawInventorySlotHoverFrame("hotbar:" + i, x, y, hud.hotbarSlotSize(), hud.scale());
            }
            uiRenderer.text(String.valueOf(i + 1), x + 7.0f * hud.scale(), y + 8.0f * hud.scale(), 1.05f * hud.scale(), UiColor.MUTED);
            if (!slot.isEmpty()) {
                drawSlotStack(slot, x + 6.0f * hud.scale(), y + 6.0f * hud.scale(), hud.hotbarSlotSize() - 12.0f * hud.scale(), hud.scale());
            }
        }
        if (gameState == GameState.PLAYING || gameState == GameState.CHAT) {
            renderHeldItem();
        }
        renderPopAnimations();
        if (settings.debugOverlayEnabled()) {
            renderDebugOverlay();
        }
    }

    private void renderSurvivalScreenFeedback(float uiScale) {
        if (damageFlash.active(frameTimeSeconds)) {
            DamageFlash.Sample flash = damageFlash.sample(frameTimeSeconds);
            float edge = Math.max(18.0f * uiScale, 42.0f * flash.amount() * uiScale);
            uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.56f, 0.04f, 0.025f, 0.18f * flash.alpha()));
            uiRenderer.rect(0, 0, framebufferWidth, edge, new UiColor(0.95f, 0.09f, 0.045f, 0.18f * flash.alpha()));
            uiRenderer.rect(0, framebufferHeight - edge, framebufferWidth, edge, new UiColor(0.95f, 0.09f, 0.045f, 0.14f * flash.alpha()));
            uiRenderer.rect(0, 0, edge, framebufferHeight, new UiColor(0.95f, 0.09f, 0.045f, 0.11f * flash.alpha()));
            uiRenderer.rect(framebufferWidth - edge, 0, edge, framebufferHeight, new UiColor(0.95f, 0.09f, 0.045f, 0.11f * flash.alpha()));
        }
        if (headUnderwaterNow && playerStats.breath() <= 6) {
            float pulse = survivalWarningPulse.sample(frameTimeSeconds).alpha();
            uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.04f, 0.20f, 0.32f, 0.08f + pulse * 0.18f));
        }
    }

    private void renderWorldHudInfo(HudLayout hud, WorldHudInfo info) {
        float scale = hud.scale();
        if (hud.showTime()) {
            float width = Math.min(hud.hotbarWidth(), 246.0f * scale);
            float height = 24.0f * scale;
            float x = framebufferWidth * 0.5f - width * 0.5f;
            float y = hud.timeY() - 4.0f * scale;
            uiRenderer.rect(x, y, width, height, new UiColor(0.015f, 0.022f, 0.022f, 0.58f));
            uiRenderer.rect(x, y, width, Math.max(1.0f, 2.0f * scale), info.night() ? UiColor.WATER : UiColor.ACCENT);
            drawTimeIcon(x + 8.0f * scale, y + 5.0f * scale, 14.0f * scale, info.night());
            uiRenderer.text(
                    fitTextToWidth(info.timeLine().toUpperCase(Locale.ROOT), 0.86f * scale, width - 32.0f * scale),
                    x + 29.0f * scale,
                    y + 7.0f * scale,
                    0.86f * scale,
                    UiColor.WHITE
            );
        }
        if (hud.showWorldInfo()) {
            float width = Math.min(238.0f * scale, framebufferWidth * 0.42f);
            float height = info.showTemperature() ? 44.0f * scale : 30.0f * scale;
            float x = 18.0f * scale;
            float y = 16.0f * scale;
            uiRenderer.rect(x, y, width, height, new UiColor(0.014f, 0.022f, 0.020f, 0.58f));
            uiRenderer.rect(x, y, Math.max(2.0f, 3.0f * scale), height, UiColor.ACCENT);
            uiRenderer.text(fitTextToWidth(info.biomeLine().toUpperCase(Locale.ROOT), 0.88f * scale, width - 22.0f * scale), x + 12.0f * scale, y + 7.0f * scale, 0.88f * scale, UiColor.WHITE);
            uiRenderer.text(fitTextToWidth(info.detailLine().toUpperCase(Locale.ROOT), 0.74f * scale, width - 22.0f * scale), x + 12.0f * scale, y + 22.0f * scale, 0.74f * scale, UiColor.MUTED);
        }
    }

    private void drawTimeIcon(float x, float y, float size, boolean night) {
        if (night) {
            uiRenderer.rect(x + size * 0.30f, y, size * 0.45f, size, new UiColor(0.52f, 0.76f, 0.94f, 0.88f));
            uiRenderer.rect(x + size * 0.52f, y + size * 0.12f, size * 0.35f, size * 0.76f, new UiColor(0.015f, 0.022f, 0.022f, 0.86f));
            return;
        }
        uiRenderer.rect(x + size * 0.28f, y + size * 0.28f, size * 0.44f, size * 0.44f, new UiColor(0.96f, 0.83f, 0.34f, 0.92f));
        uiRenderer.rect(x + size * 0.46f, y, size * 0.08f, size, new UiColor(0.96f, 0.83f, 0.34f, 0.58f));
        uiRenderer.rect(x, y + size * 0.46f, size, size * 0.08f, new UiColor(0.96f, 0.83f, 0.34f, 0.58f));
    }

    private void renderComfortDetail(HudLayout hud, ComfortHudInfo info) {
        float scale = hud.scale();
        if (framebufferWidth < 680) {
            return;
        }
        float width = Math.min(318.0f * scale, framebufferWidth * 0.42f);
        float height = 28.0f * scale;
        float x = framebufferWidth - width - 18.0f * scale;
        float y = hud.showMode() ? 38.0f * scale : 16.0f * scale;
        uiRenderer.rect(x, y, width, height, new UiColor(0.025f, 0.035f, 0.026f, 0.48f));
        uiRenderer.rect(x, y, Math.max(2.0f, 3.0f * scale), height, UiColor.ENERGY);
        uiRenderer.text(
                fitTextToWidth(info.detailLine().toUpperCase(Locale.ROOT), 0.62f * scale, width - 18.0f * scale),
                x + 10.0f * scale,
                y + 9.0f * scale,
                0.62f * scale,
                UiColor.MUTED
        );
    }

    private void renderInteractionHint(float bottomLimitY) {
        if (gameState != GameState.PLAYING) {
            return;
        }
        if (lookingAtReachableStatusBlock()) {
            return;
        }
        Optional<InteractionHint> hint = currentInteractionHint();
        if (hint.isEmpty()) {
            return;
        }
        float uiScale = settings.uiScale();
        InteractionHudCard card = InteractionHudCard.from(hint.get(), uiScale, framebufferWidth);
        float width = card.width();
        float height = card.height();
        float x = framebufferWidth * 0.5f - width * 0.5f;
        float preferredY = framebufferHeight * 0.5f + 42.0f * uiScale;
        float y = Math.min(preferredY, bottomLimitY - height);
        y = Math.max(framebufferHeight * 0.5f + 20.0f * uiScale, y);
        UiColor accent = interactionHintAccent(card.tone());
        uiRenderer.rect(x - 2.0f * uiScale, y - 2.0f * uiScale, width + 4.0f * uiScale, height + 4.0f * uiScale, new UiColor(0.004f, 0.006f, 0.006f, 0.54f));
        uiRenderer.rect(x, y, width, height, new UiColor(0.022f, 0.030f, 0.028f, 0.74f));
        uiRenderer.rect(x, y, width, Math.max(2.0f, 2.0f * uiScale), withAlpha(accent, 0.88f));
        uiRenderer.rect(x + 7.0f * uiScale, y + 8.0f * uiScale, Math.max(2.0f, 3.0f * uiScale), height - 16.0f * uiScale, withAlpha(accent, 0.62f));
        float textX = x + 17.0f * uiScale;
        uiRenderer.text(card.title(), textX, y + 8.0f * uiScale, card.titleScale(), UiColor.MUTED);
        uiRenderer.text(card.action(), textX, y + 21.0f * uiScale, card.actionScale(), card.tone() == InteractionHint.Tone.WARNING ? UiColor.HEART : UiColor.WHITE);
        float lineY = y + 38.0f * uiScale;
        if (!card.detail().isBlank()) {
            uiRenderer.text(card.detail(), textX, lineY, card.detailScale(), UiColor.MUTED);
            lineY += 16.0f * uiScale;
        }
        if (card.progressVisible()) {
            drawProgressBar(textX, lineY - 1.0f * uiScale, width - 34.0f * uiScale, Math.max(5.0f, 7.0f * uiScale), card.progress(), accent);
            lineY += 12.0f * uiScale;
        }
        if (!card.chips().isEmpty()) {
            float chipX = textX;
            for (String chip : card.chips()) {
                float chipScale = 0.62f * uiScale;
                float chipWidth = BitmapFont.textWidth(chip, chipScale) + 14.0f * uiScale;
                uiRenderer.rect(chipX, lineY - 3.0f * uiScale, chipWidth, 12.0f * uiScale, new UiColor(0.07f, 0.095f, 0.084f, 0.68f));
                uiRenderer.rect(chipX, lineY - 3.0f * uiScale, Math.max(1.0f, 2.0f * uiScale), 12.0f * uiScale, withAlpha(accent, 0.72f));
                uiRenderer.text(chip, chipX + 7.0f * uiScale, lineY, chipScale, UiColor.MUTED);
                chipX += chipWidth + 5.0f * uiScale;
            }
        }
    }

    private boolean lookingAtReachableStatusBlock() {
        Optional<dev.voxelgame.common.math.Raycast.Hit> picked = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (picked.isEmpty()) {
            return false;
        }
        return world.targetBlock(picked.get())
                .map(block -> CampfireRules.isCampfire(block.id())
                        || block.id() == Blocks.COOKING_POT
                        || block.id() == Blocks.WORKBENCH
                        || block.id() == Blocks.FORGE)
                .orElse(false);
    }

    private Optional<InteractionHint> currentInteractionHint() {
        Optional<dev.voxelgame.common.math.Raycast.Hit> reachableBlock = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH);
        if (reachableBlock.isPresent() && targetIsPriorityInteractionBlock(reachableBlock.get())) {
            return blockInteractionHint(reachableBlock.get(), true);
        }
        Optional<EntitySnapshot> entity = nearestEntityTarget(
                world.visibleEntities(frameTimeSeconds),
                camera.position(),
                camera.forward(),
                reachableBlock.map(dev.voxelgame.common.math.Raycast.Hit::distance).orElse(InteractionRules.BLOCK_REACH)
        );
        if (entity.isPresent()) {
            return Optional.of(InteractionHint.forEntity(entity.get().typeKey(), hotbar.selectedItemIsFood()));
        }
        if (reachableBlock.isPresent()) {
            return blockInteractionHint(reachableBlock.get(), true);
        }
        Optional<dev.voxelgame.common.math.Raycast.Hit> farBlock = world.pick(camera.position(), camera.forward(), InteractionRules.BLOCK_REACH + 3.0);
        if (farBlock.isEmpty()) {
            return Optional.empty();
        }
        return blockInteractionHint(farBlock.get(), false);
    }

    private boolean targetIsPriorityInteractionBlock(dev.voxelgame.common.math.Raycast.Hit hit) {
        return world.targetBlock(hit)
                .map(block -> block.id() == Blocks.STORAGE_CRATE || block.id() == Blocks.SLEEPING_MAT)
                .orElse(false);
    }

    private Optional<InteractionHint> blockInteractionHint(dev.voxelgame.common.math.Raycast.Hit hit, boolean inReach) {
        Optional<BlockType> block = world.targetBlock(hit);
        if (block.isEmpty()) {
            return Optional.empty();
        }
        BlockType target = block.get();
        boolean canHarvest = gameMode == GameMode.CREATIVE || hotbar.canHarvestSelected(target);
        float breakMultiplier = hotbar.selectedBreakMultiplier(target);
        float miningProgress = blockBreakAnimation.active() ? blockBreakAnimation.progress(frameTimeSeconds) : 0.0f;
        return Optional.of(InteractionHint.forBlock(target, new InteractionHint.Context(
                inReach,
                selectedItemIsCampfireFuel(),
                hotbar.selectedItemIsFood(),
                hotbar.selectedPlaceBlockId().isPresent(),
                canHarvest,
                breakMultiplier,
                gameMode,
                miningProgress
        )));
    }

    private static UiColor interactionHintAccent(InteractionHint.Tone tone) {
        return switch (tone) {
            case READY -> UiColor.ACCENT;
            case WARNING -> UiColor.HEART;
            case NEUTRAL -> UiColor.MUTED;
        };
    }

    private static UiColor withAlpha(UiColor color, float alpha) {
        return new UiColor(color.r(), color.g(), color.b(), alpha);
    }

    private void renderHeldItem() {
        hotbar.selectedItemKey().ifPresent(itemKey -> {
            float uiScale = settings.uiScale();
            HeldItemAnimation.Sample motion = heldItemAnimation.sample(frameTimeSeconds);
            float size = Math.max(78.0f * uiScale, Math.min(116.0f * uiScale, framebufferHeight * 0.15f * uiScale)) * motion.scale();
            float x = framebufferWidth - size - 50.0f * uiScale + motion.handX() * uiScale;
            float y = framebufferHeight - size - 28.0f * uiScale + motion.handY() * uiScale;
            uiRenderer.rect(x + size * 0.55f, y + size * 0.62f, size * 0.16f, size * 0.34f, new UiColor(0.70f, 0.50f, 0.34f, 0.90f));
            uiRenderer.rect(x + size * 0.49f, y + size * 0.56f, size * 0.28f, size * 0.14f, new UiColor(0.82f, 0.62f, 0.42f, 0.92f));
            drawItemIcon(itemKey, x + motion.itemX() * uiScale, y + motion.itemY() * uiScale, size);
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
        return currentWorldHudInfo().timeLine().toUpperCase(Locale.ROOT);
    }

    private int localDayNumber() {
        double elapsed = Math.max(0.0, frameTimeSeconds - worldStartTimeSeconds);
        return (int) (elapsed / LOCAL_DAY_LENGTH_SECONDS) + 1;
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

    private WorldHudInfo currentWorldHudInfo() {
        if (world == null) {
            return WorldHudInfo.of("none", "No World", "MILD", 1, 0);
        }
        Vector3f position = camera.position();
        String biomeKey = world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z));
        return WorldHudInfo.of(biomeKey, biomeLabel(biomeKey), temperatureLabel(biomeKey), localDayNumber(), localDayMinutes());
    }

    private ComfortHudInfo currentComfortHudInfo() {
        if (world == null) {
            return ComfortHudInfo.none();
        }
        return ClientComfortSources.scan(world, camera.position(), frameTimeSeconds, world.visibleEntities(frameTimeSeconds));
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

    private List<HudStat> primaryHudStats(boolean minimalHud) {
        List<HudStat> stats = new ArrayList<>(3);
        int health = playerStats.health();
        int stamina = playerStats.stamina();
        int hunger = playerStats.hunger();
        boolean healthWarning = health <= 6;
        boolean healthBoosted = frameTimeSeconds < healthRegenPulseUntil;
        if (!minimalHud || health <= 8 || healthBoosted) {
            stats.add(new HudStat(
                    "HEALTH",
                    "heart_full",
                    "heart_half",
                    "heart_empty",
                    health,
                    20,
                    UiColor.HEART,
                    1.0f,
                    healthWarning,
                    healthBoosted,
                    healthBoosted ? "REGEN" : healthWarning ? "LOW" : ""
            ));
        }
        boolean sprintBlocked = frameTimeSeconds < sprintBlockedUntil;
        boolean energyWarning = stamina <= 4 || sprintBlocked;
        boolean comfortBoost = playerStats.comfort() >= 5;
        if (!minimalHud || stamina <= 6 || sprintBlocked) {
            stats.add(new HudStat(
                    "ENERGY",
                    "leaf_full",
                    "leaf_half",
                    "leaf_empty",
                    stamina,
                    20,
                    UiColor.ENERGY,
                    0.96f,
                    energyWarning,
                    comfortBoost,
                    sprintBlocked ? "NO SPRINT" : comfortBoost ? "BOOST" : energyWarning ? "LOW" : ""
            ));
        }
        boolean hungerWarning = hunger <= 5;
        if (!minimalHud || hunger <= 6) {
            stats.add(new HudStat(
                    "HUNGER",
                    "hunger_full",
                    "hunger_half",
                    "hunger_empty",
                    hunger,
                    20,
                    UiColor.HUNGER,
                    1.0f,
                    hungerWarning,
                    false,
                    hunger <= 0 ? "STARVING" : hungerWarning ? "LOW" : ""
            ));
        }
        return stats;
    }

    private List<HudStat> secondaryHudStats(boolean minimalHud) {
        List<HudStat> stats = new ArrayList<>(2);
        int breath = playerStats.breath();
        if (headUnderwaterNow || breath < 20) {
            stats.add(new HudStat(
                    "AIR",
                    "air_full",
                    "air_half",
                    "air_empty",
                    breath,
                    20,
                    UiColor.WATER,
                    0.96f,
                    breath <= 6,
                    false,
                    breath <= 6 ? "LOW AIR" : ""
            ));
        }
        if (!minimalHud && playerStats.armor() > 0) {
            stats.add(new HudStat("ARMOR", "armor_full", "armor_half", "armor_empty", playerStats.armor(), 20, UiColor.MUTED, 0.96f));
        }
        return stats;
    }

    private Optional<HudStat> comfortHudStat(HudLayout hud, boolean minimalHud, ComfortHudInfo info) {
        if (minimalHud || !hud.showComfort() || !info.active()) {
            return Optional.empty();
        }
        return Optional.of(new HudStat("COMFORT", "sparkle", "", "", info.value(), 40, UiColor.ENERGY, 0.82f, false, true, info.statLabel().toUpperCase(Locale.ROOT)));
    }

    private void drawHudStatRow(List<HudStat> stats, HudLayout hud, float y) {
        if (stats.isEmpty()) {
            return;
        }
        float gap = stats.size() <= 1
                ? 0.0f
                : Math.max(8.0f * hud.scale(), (hud.hotbarWidth() - hud.statWidth() * stats.size()) / (stats.size() - 1));
        float totalWidth = stats.size() * hud.statWidth() + (stats.size() - 1) * gap;
        float centeredX = hud.hotbarX() + hud.hotbarWidth() * 0.5f - totalWidth * 0.5f;
        float minX = hud.hotbarX() + 4.0f * hud.scale();
        float maxX = hud.hotbarX() + hud.hotbarWidth() - totalWidth - 4.0f * hud.scale();
        float startX = maxX < minX ? centeredX : clampFloat(centeredX, minX, maxX);
        for (int i = 0; i < stats.size(); i++) {
            drawStatStrip(stats.get(i), startX + i * (hud.statWidth() + gap), y, hud.statWidth(), hud.statIconSize() * stats.get(i).iconScale(), hud.scale());
        }
    }

    private void drawHudSecondaryStats(List<HudStat> stats, HudLayout hud) {
        for (int i = 0; i < stats.size(); i++) {
            float y = hud.statsY() - (28.0f + i * 26.0f) * hud.scale();
            drawHudStatRow(List.of(stats.get(i)), hud, y);
        }
    }

    private float feedbackBottomY(HudLayout hud, List<HudStat> secondaryStats) {
        if (secondaryStats.isEmpty()) {
            return hud.feedbackBottomY();
        }
        float topOfHighestStat = hud.statsY() - (34.0f + (secondaryStats.size() - 1) * 26.0f) * hud.scale();
        return Math.min(hud.feedbackBottomY(), Math.max(70.0f * hud.scale(), topOfHighestStat));
    }

    private void drawStatStrip(HudStat stat, float x, float y, float width, float iconSize, float uiScale) {
        float height = 24.0f * uiScale;
        if (stat.warning() || stat.boosted()) {
            float pulseAlpha = stat.warning() ? survivalWarningPulse.sample(frameTimeSeconds).alpha() : 0.0f;
            UiColor glow = stat.warning()
                    ? new UiColor(0.72f, 0.08f, 0.04f, 0.16f + pulseAlpha * 0.50f)
                    : new UiColor(0.38f, 0.72f, 0.36f, 0.18f);
            uiRenderer.rect(x - 4.0f * uiScale, y - 2.0f * uiScale, width + 8.0f * uiScale, height, glow);
            uiRenderer.rect(x - 4.0f * uiScale, y - 2.0f * uiScale, width + 8.0f * uiScale, Math.max(1.0f, 2.0f * uiScale), withAlpha(stat.warning() ? UiColor.HEART : UiColor.ENERGY, 0.72f));
        }
        float gap = Math.max(3.0f, iconSize * 0.22f);
        float meterWidth = iconSize * 10.0f + gap * 9.0f;
        float meterX = x + Math.max(43.0f * uiScale, width - meterWidth);
        int clampedValue = Math.max(0, Math.min(stat.max(), stat.value()));
        UiColor labelColor = stat.warning() ? UiColor.WHITE : UiColor.MUTED;
        uiRenderer.text(stat.label(), x, y + 1.0f * uiScale, 0.72f * uiScale, labelColor);
        String valueText = stat.stateLabel().isBlank() ? clampedValue + "/" + stat.max() : stat.stateLabel();
        uiRenderer.text(fitTextToWidth(valueText, 0.66f * uiScale, 52.0f * uiScale), x, y + 10.5f * uiScale, 0.66f * uiScale, stat.warning() ? UiColor.HEART : stat.accent());
        drawIconMeter(stat.fullKey(), stat.halfKey(), stat.emptyKey(), clampedValue, stat.max(), meterX, y + 4.0f * uiScale, iconSize);
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
        int terrainHeight = world == null ? 0 : world.terrainHeightAt((int) Math.floor(position.x), (int) Math.floor(position.z()));
        int chunkX = Math.floorDiv((int) Math.floor(position.x), ChunkPos.SIZE);
        int chunkZ = Math.floorDiv((int) Math.floor(position.z), ChunkPos.SIZE);
        int blockX = (int) Math.floor(position.x);
        int blockY = (int) Math.floor(position.y);
        int blockZ = (int) Math.floor(position.z);
        int skyLight = world == null ? 0 : world.skyLightAt(blockX, blockY, blockZ);
        int blockLight = world == null ? 0 : world.blockLightAt(blockX, blockY, blockZ);
        int combinedLight = Math.max(skyLight, blockLight);
        EngineFrameStats.Frame frame = engineFrameStats.frame();
        EngineFrameStats.FramePhases phases = engineFrameStats.phases();
        EngineFrameStats.Jobs jobs = engineFrameStats.jobs();
        EngineFrameStats.Chunks chunks = engineFrameStats.chunks();
        EngineFrameStats.Rendering rendering = engineFrameStats.rendering();
        EngineFrameStats.Entities entities = engineFrameStats.entities();
        EngineFrameStats.Particles particles = engineFrameStats.particles();
        EngineFrameStats.Network network = engineFrameStats.network();
        GamePacket.ServerStatsSnapshot serverStats = network.serverStats();
        EngineFrameStats.GpuResources resources = engineFrameStats.gpuResources();
        EngineFrameStats.Budgets budgets = engineFrameStats.budgets();
        ClientWorld.PhysicsLoadingStatus physicsLoading = world == null ? null : world.physicsLoadingStatus(position);
        CollisionShapeCache.CacheStats collisionCache = world == null ? null : world.collisionCacheStats();
        BlockSurfacePhysics.SurfaceMaterial surface = world == null ? BlockSurfacePhysics.DEFAULT : world.playerSurface(position);
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
        //uiRenderer.rect(12.0f, 12.0f, 970.0f, 402.0f, new UiColor(0.02f, 0.03f, 0.035f, 0.58f));
        uiRenderer.text("FPS " + frame.fps() + " FRAME " + formatMilliseconds(frame.frameMilliseconds()) + " UPD " + formatMilliseconds(frame.updateMilliseconds()) + " RENDER " + formatMilliseconds(frame.renderMilliseconds()) + " UI " + formatMilliseconds(frame.uiMilliseconds()), 22.0f, 24.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("PHASE IN " + formatMilliseconds(phases.inputMilliseconds()) + " NET " + formatMilliseconds(phases.networkMilliseconds()) + " PLY " + formatMilliseconds(phases.playerMilliseconds()) + " WORLD " + formatMilliseconds(phases.worldMilliseconds()) + " JOB " + formatMilliseconds(phases.chunkJobsMilliseconds()) + " GPU " + formatMilliseconds(phases.gpuUploadMilliseconds()) + " RPASS " + formatMilliseconds(phases.renderPassMilliseconds()), 22.0f, 44.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("XYZ " + Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z) + " CHUNK " + chunkX + " " + chunkZ + " BIOME " + biomeLabel(biome) + " H " + terrainHeight, 22.0f, 64.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("PRESET " + settings.activePresetLabel().toUpperCase(Locale.ROOT) + " RD " + chunks.renderDistanceChunks() + " PRE " + chunks.previewRadiusChunks() + " RET " + chunks.retentionRadiusChunks() + " GEN " + settings.effectiveChunkGenerationBudgetChunks(lastFrameMilliseconds) + "/" + formatMilliseconds(settings.effectiveChunkGenerationBudgetMilliseconds(lastFrameMilliseconds)) + " MB " + chunks.meshBuildBudgetChunks() + "/" + formatMilliseconds(chunks.meshBuildBudgetMilliseconds()) + " UP " + formatMilliseconds(settings.gpuUploadBudgetMilliseconds()) + " GREEDY " + onOff(settings.greedyMeshingEnabled()) + " DIRTY " + chunks.dirtyChunks() + " BUILT " + chunks.builtChunks(), 22.0f, 84.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("LOADED " + chunks.loadedChunks() + " VIS " + chunks.visibleChunks() + " UNLD " + chunks.unloadedChunks() + "/" + formatCount(chunks.totalUnloadedChunks()) + " FREED " + chunks.releasedGpuMeshLayers() + " GPU MESH " + rendering.loadedGpuMeshes() + " GPU CHUNK " + rendering.loadedGpuChunkPositions(), 22.0f, 104.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("QUEUE " + chunks.queuedChunks() + " REP " + formatCount(chunks.replacedChunkBuilds()) + " CAN " + formatCount(chunks.canceledChunkBuilds()) + " WAIT " + formatMilliseconds(chunks.averageChunkBuildWaitMilliseconds()) + " GEN " + formatMilliseconds(chunks.chunkGenerationMilliseconds()) + " MESH " + formatMilliseconds(chunks.meshingMilliseconds()) + " LIGHT " + formatMilliseconds(chunks.lightingMilliseconds()) + " UP " + formatMilliseconds(chunks.gpuUploadMilliseconds()) + " B/s " + formatRate(chunks.chunksBuiltPerSecond()), 22.0f, 124.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("JOBS P/R/C/X " + formatJobCounter(jobs.chunkGenerate()) + " " + formatJobCounter(jobs.chunkLight()) + " " + formatJobCounter(jobs.chunkMesh()) + " " + formatJobCounter(jobs.saveWrite()) + " " + formatJobCounter(jobs.netEncode()), 22.0f, 144.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("BUD " + budgets.profile().toUpperCase(Locale.ROOT) + " F " + formatMilliseconds(frame.frameMilliseconds()) + "/" + formatMilliseconds(budgets.frameTargetMilliseconds()) + " GEN " + formatPercent(budgets.chunkGenerationUsage()) + " LIGHT " + formatPercent(budgets.lightingUsage()) + " MESH " + formatPercent(budgets.meshingUsage()) + " GPU " + formatPercent(budgets.gpuUploadMillisecondsUsage()) + " UPB " + formatPercent(budgets.gpuUploadBytesUsage()) + " DRAW " + formatPercent(budgets.drawCallUsage()) + " TRI " + formatPercent(budgets.triangleUsage()) + " PART " + formatPercent(budgets.particleUsage()) + " ENT " + formatPercent(budgets.entityUsage()) + " SAVE " + formatPercent(budgets.saveWriteUsage()) + " NET " + formatPercent(budgets.networkBytesPerSecondUsage()), 22.0f, 164.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("SECTIONS " + chunks.nonEmptySections() + "/" + chunks.totalSections() + " EMPTY " + chunks.emptySections() + " BOUNDS " + chunks.chunksWithSectionBounds() + " DIRTY G/L/F/B " + chunks.dirtyGeometrySections() + "/" + chunks.dirtyLightSections() + "/" + chunks.dirtyFluidSections() + "/" + chunks.dirtyBlockEntitySections() + " TCACHE " + chunks.terrainCacheChunks() + "/" + formatMegabytes(chunks.terrainCacheBytes()), 22.0f, 184.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("DRAW " + rendering.drawCalls() + " PDC S/C/W " + rendering.solidDrawCalls() + "/" + rendering.cutoutDrawCalls() + "/" + rendering.transparentDrawCalls() + " MESH S/C/W " + rendering.solidMeshCount() + "/" + rendering.cutoutMeshCount() + "/" + rendering.transparentMeshCount() + " SPART R/C/L " + rendering.renderedSectionParts() + "/" + rendering.culledSectionParts() + "/" + rendering.loadedSectionParts() + " SORT " + rendering.sortedTransparentMeshes() + " RST " + rendering.renderStateChanges() + " CULLM " + rendering.culledMeshes() + " CULLC " + rendering.culledChunks() + " CD " + rendering.culledByDistance() + " CB " + rendering.culledByBounds(), 22.0f, 204.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("TRIS S " + formatCount(rendering.solidTriangles()) + " C " + formatCount(rendering.cutoutTriangles()) + " W " + formatCount(rendering.transparentTriangles()) + " TOTAL " + formatCount(rendering.triangles()) + " VRAM " + formatMegabytes(rendering.estimatedVramBytes()) + " UPB " + formatMegabytes(rendering.gpuUploadBytes()) + " ENT " + entities.visibleEntityCount() + "/" + entities.entityCount() + " EDC " + entities.drawCalls() + " EP " + entities.modelParts() + " EMDL " + entities.cachedModels() + " ECULL " + entities.culledEntityCount() + " HITBOX " + entities.debugHitboxes(), 22.0f, 224.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("GL MESH " + resources.liveChunkMeshes() + " VAO " + resources.liveChunkVertexArrays() + " BUF " + resources.liveChunkBuffers() + " EVAO " + resources.liveEntityVertexArrays() + " EBUF " + resources.liveEntityBuffers() + " TEX " + resources.liveTextures() + " SHD " + resources.liveShaderPrograms() + " RLD " + resources.shaderReloadCount() + " F " + resources.failedShaderReloadCount() + " " + formatMilliseconds(resources.lastShaderReloadMilliseconds()) + " PVAO " + resources.liveParticleVertexArrays() + " PBUF " + resources.liveParticleBuffers() + " FB " + resources.liveFramebuffers() + " MB " + formatMegabytes(resources.liveChunkMeshBytes()) + "/" + formatMegabytes(resources.peakChunkMeshBytes()), 22.0f, 244.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("MAT " + rendering.materialCount() + " LUT " + formatMegabytes(rendering.materialLutBytes()) + " MISS " + rendering.missingMaterialCount() + " VTX " + rendering.chunkVertexBytes() + "B MESH-GROW " + formatMegabytes(chunks.meshBufferGrowthBytes()) + " BUF " + formatMegabytes(chunks.retainedMeshBufferBytes()) + " ATLAS " + rendering.atlasTextureCount() + " " + rendering.atlasWidth() + "x" + rendering.atlasHeight() + " " + formatMegabytes(rendering.atlasBytes()) + " DEBUGVIEW " + settings.renderDebugView().commandName(), 22.0f, 264.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("PART " + particles.particleCount() + " SPAWN/s " + formatRate(particles.spawnRate()) + " BUD " + formatPercent(particles.budgetUsage()) + " Q " + formatPercent(settings.particleQuality()) + " EVICT " + particles.evictedParticles() + " PDC " + particles.drawCalls() + " PTRI " + particles.triangles() + " BORDERS " + rendering.debugChunkBorders() + " MBOUNDS " + rendering.debugMeshBounds() + " SBOUNDS " + rendering.debugSectionBounds() + " PBOUNDS " + particles.debugBounds() + " SHAPES " + lastCollisionShapeDebugBoxes + " PSWEEP " + lastProjectileSweepDebugBoxes + " MODE " + gameMode.name() + " GROUND " + onOff(camera.onGround()) + " LIGHT " + combinedLight + " S " + skyLight + " B " + blockLight, 22.0f, 284.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("NET " + onOff(network.online()) + " TX " + formatCount(network.sentPackets()) + " RX " + formatCount(network.receivedPackets()) + " TX/s " + formatRate(network.sentPacketsPerSecond()) + " RX/s " + formatRate(network.receivedPacketsPerSecond()) + " AVG " + formatCount(Math.round(network.averagePacketBytes())) + "B BAD " + formatCount(network.invalidPacketsDropped()) + " Q " + network.chunkStreamQueueLength() + " CH " + formatCount(network.chunkPackets()) + " BLK " + formatCount(network.blockUpdatePackets()) + " ENT " + formatCount(network.entitySnapshotPackets()) + " INV " + formatCount(network.inventoryPackets()), 22.0f, 304.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("SRVSTAT PKT " + formatCount(network.serverStatsPackets()) + " SUB " + serverStats.chunkSubscriptions() + " CH " + formatCount(serverStats.sentChunkPackets()) + " ES " + formatCount(serverStats.sentEntitySnapshots()) + "/" + formatCount(serverStats.sentEntitySnapshotPackets()) + " BLK " + formatCount(serverStats.sentBlockUpdates()) + " DROP " + formatCount(serverStats.discardedUpdatesOutsideInterest()) + " REJ " + formatCount(serverStats.rejectedChunkRequests()) + " FAIL " + formatCount(serverStats.failedChunkRequests()) + " SAVE " + serverStats.savePendingWrites() + "/" + serverStats.saveRunningWrites() + "/" + formatCount(serverStats.saveCompletedWrites()) + " " + formatMilliseconds(serverStats.saveAverageWriteMilliseconds()) + " AVG " + formatCount(serverStats.averagePacketBytes()) + "B PPS " + formatRate(serverStats.packetRatePerSecond()), 22.0f, 324.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("SEL " + clampText(selectedItem, 72), 22.0f, 344.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("LOOK " + clampText(lookingAt, 72), 22.0f, 364.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("PHYS " + physicsLoadingLabel(physicsLoading)
                + " SURF " + surfaceDebugLabel(surface)
                + " SPD " + formatPercent(surface.speedMultiplier())
                + " FRI " + formatPercent(surface.frictionMultiplier())
                + " " + collisionCacheLabel(collisionCache), 22.0f, 384.0f, 1.65f, physicsLoading != null && physicsLoading.blocked() ? UiColor.WARNING : UiColor.MUTED);
    }

    private static String physicsLoadingLabel(ClientWorld.PhysicsLoadingStatus status) {
        if (status == null) {
            return "no world";
        }
        return (status.blocked() ? "blocked by loading" : "ready")
                + " C " + status.center().x() + " " + status.center().z()
                + " " + status.loadedChunks() + "/" + status.requiredChunks();
    }

    private static String surfaceDebugLabel(BlockSurfacePhysics.SurfaceMaterial surface) {
        BlockSurfacePhysics.SurfaceMaterial safe = surface == null ? BlockSurfacePhysics.DEFAULT : surface;
        String key = safe.key();
        if (key.startsWith("voxel:")) {
            return key.substring("voxel:".length());
        }
        return key.isBlank() ? "default" : key;
    }

    private static String collisionCacheLabel(CollisionShapeCache.CacheStats stats) {
        if (stats == null) {
            return "CSEC 0 SHP 0 EVICT 0";
        }
        return "CSEC " + stats.sections() + "/" + stats.maxSectionsPerShapeSet()
                + " SHP " + stats.shapes()
                + " EVICT " + formatCount(stats.evictedSections());
    }

    private void renderBreakOverlay() {
        if (!blockBreakAnimation.active()) {
            return;
        }
        float uiScale = settings.uiScale();
        float eased = blockBreakAnimation.easedProgress(frameTimeSeconds);
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

    private void renderPopAnimations() {
        renderPickupPop();
        float uiScale = settings.uiScale();
        renderTextPop(craftingSuccessPop, "CRAFTED", framebufferWidth * 0.5f, framebufferHeight * 0.5f - 154.0f * uiScale, 1.10f * uiScale, UiColor.ACCENT);
        renderTextPop(comfortPop, "COZY", framebufferWidth * 0.5f, framebufferHeight * 0.5f - 86.0f * uiScale, 1.22f * uiScale, UiColor.ENERGY);
        renderTextPop(recipeUnlockPop, "NEW RECIPE", framebufferWidth * 0.5f, framebufferHeight * 0.5f - 122.0f * uiScale, 1.12f * uiScale, UiColor.ACCENT);
    }

    private void renderCraftingPopAnimations() {
        float uiScale = settings.uiScale();
        float y = Math.max(142.0f, framebufferHeight * 0.5f - 158.0f * uiScale);
        renderTextPop(craftingSuccessPop, "CRAFTED", framebufferWidth * 0.5f, y, 1.18f * uiScale, UiColor.ACCENT);
        renderTextPop(recipeUnlockPop, "NEW RECIPE", framebufferWidth * 0.5f, y + 34.0f * uiScale, 1.05f * uiScale, UiColor.ACCENT);
    }

    private void renderPickupPop() {
        if (!pickupPop.active(frameTimeSeconds)) {
            return;
        }
        PopAnimation.Sample pop = pickupPop.sample(frameTimeSeconds);
        float uiScale = settings.uiScale();
        float size = 36.0f * pop.scale() * uiScale;
        float x = framebufferWidth * 0.5f - size * 0.5f;
        float y = framebufferHeight * 0.5f + 40.0f * uiScale - pop.lift() * 50.0f * uiScale;
        drawItemIcon(pickupPop.key(), x, y, size);
        uiRenderer.centeredText("+", x - 8.0f * uiScale, y + 8.0f * uiScale, 1.5f * pop.scale() * uiScale, new UiColor(UiColor.ACCENT.r(), UiColor.ACCENT.g(), UiColor.ACCENT.b(), pop.alpha()));
    }

    private void renderTextPop(PopAnimation animation, String label, float centerX, float y, float baseScale, UiColor color) {
        if (!animation.active(frameTimeSeconds)) {
            return;
        }
        PopAnimation.Sample pop = animation.sample(frameTimeSeconds);
        float uiScale = settings.uiScale();
        float scale = baseScale * pop.scale();
        float textY = y - pop.lift() * 22.0f * uiScale;
        float textWidth = BitmapFont.textWidth(label, scale);
        float panelHeight = BitmapFont.textHeight(scale) + 8.0f * uiScale;
        float padX = 10.0f * uiScale;
        float alpha = pop.alpha();
        uiRenderer.rect(centerX - textWidth * 0.5f - padX, textY - 4.0f * uiScale, textWidth + padX * 2.0f, panelHeight, new UiColor(0.025f, 0.035f, 0.032f, alpha * 0.34f));
        uiRenderer.centeredText(label, centerX, textY, scale, new UiColor(color.r(), color.g(), color.b(), alpha));
    }

    private void drawModernPanel(float x, float y, float width, float height, float radius, UiColor fill, UiColor border, UiColor accent, float uiScale) {
        float scale = Math.max(0.5f, uiScale);
        float shadow = Math.max(2.0f, 5.0f * scale);
        float pad = Math.max(2.0f, 2.0f * scale);
        float innerPad = Math.max(5.0f, 6.0f * scale);
        uiRenderer.roundedRect(x + shadow * 0.55f, y + shadow * 0.70f, width, height, radius, new UiColor(0.0f, 0.0f, 0.0f, 0.26f));
        uiRenderer.roundedRect(x, y, width, height, radius, border);
        uiRenderer.roundedRect(x + pad, y + pad, width - pad * 2.0f, height - pad * 2.0f, Math.max(1.0f, radius - pad), fill);
        uiRenderer.roundedRect(x + innerPad, y + innerPad, width - innerPad * 2.0f, Math.max(1.0f, 3.0f * scale), Math.max(1.0f, radius * 0.35f), withAlpha(accent, 0.40f));
        uiRenderer.roundedRect(x + innerPad, y + height - innerPad - Math.max(1.0f, 2.0f * scale), width - innerPad * 2.0f, Math.max(1.0f, 2.0f * scale), Math.max(1.0f, radius * 0.30f), new UiColor(0.0f, 0.0f, 0.0f, 0.20f));
    }

    private void drawModernInsetPanel(float x, float y, float width, float height, float radius, UiColor fill, UiColor edge, UiColor accent, float uiScale) {
        float scale = Math.max(0.5f, uiScale);
        float pad = Math.max(1.0f, 2.0f * scale);
        uiRenderer.roundedRect(x, y, width, height, radius, edge);
        uiRenderer.roundedRect(x + pad, y + pad, width - pad * 2.0f, height - pad * 2.0f, Math.max(1.0f, radius - pad), fill);
        uiRenderer.roundedRect(x + pad * 2.0f, y + pad * 2.0f, width - pad * 4.0f, Math.max(1.0f, 2.0f * scale), Math.max(1.0f, radius * 0.30f), withAlpha(accent, 0.26f));
    }

    private void drawModernButton(UiButton button, MousePosition mouse, boolean clicked, Runnable action) {
        drawModernButton(button, mouse, clicked, action, UiColor.BUTTON, UiColor.BUTTON_HOVER, UiColor.ACCENT);
    }

    private void drawModernButton(UiButton button, MousePosition mouse, boolean clicked, Runnable action, UiColor base, UiColor hover, UiColor accent) {
        drawModernButtonSurface(button, mouse, clicked, action, base, hover, accent);
        float scale = Math.max(0.28f, Math.min(2.2f, Math.min(
                (button.width() - 14.0f) / Math.max(1.0f, BitmapFont.textWidth(button.label(), 1.0f)),
                (button.height() - 10.0f) / BitmapFont.textHeight(1.0f)
        )));
        uiRenderer.centeredText(
                button.label(),
                button.x() + button.width() * 0.5f,
                button.y() + button.height() * 0.5f - BitmapFont.textHeight(scale) * 0.5f,
                scale,
                button.enabled() ? UiColor.WHITE : UiColor.MUTED
        );
    }

    private void drawModernButtonSurface(UiButton button, MousePosition mouse, boolean clicked, Runnable action, UiColor base, UiColor hover, UiColor accent) {
        boolean hovered = button.contains(mouse.x(), mouse.y());
        float uiScale = settings == null ? 1.0f : settings.uiScale();
        float radius = Math.max(3.0f, Math.min(11.0f * uiScale, button.height() * 0.28f));
        float pad = Math.max(1.0f, 2.0f * uiScale);
        float shadow = Math.max(1.0f, 3.0f * uiScale);
        UiColor border = button.enabled()
                ? hovered ? withAlpha(accent, 0.86f) : new UiColor(0.12f, 0.16f, 0.14f, 0.94f)
                : new UiColor(0.10f, 0.11f, 0.10f, 0.54f);
        UiColor fill = button.enabled()
                ? hovered ? hover : base
                : new UiColor(0.075f, 0.085f, 0.080f, 0.62f);
        uiRenderer.roundedRect(button.x() + shadow * 0.45f, button.y() + shadow * 0.55f, button.width(), button.height(), radius, new UiColor(0.0f, 0.0f, 0.0f, button.enabled() ? 0.24f : 0.14f));
        uiRenderer.roundedRect(button.x(), button.y(), button.width(), button.height(), radius, border);
        uiRenderer.roundedRect(button.x() + pad, button.y() + pad, button.width() - pad * 2.0f, button.height() - pad * 2.0f, Math.max(1.0f, radius - pad), fill);
        uiRenderer.roundedRect(button.x() + pad * 2.0f, button.y() + pad * 2.0f, button.width() - pad * 4.0f, Math.max(1.0f, 2.0f * uiScale), Math.max(1.0f, radius * 0.30f), button.enabled() ? withAlpha(accent, hovered ? 0.46f : 0.28f) : new UiColor(0.65f, 0.65f, 0.58f, 0.10f));
        if (button.enabled() && hovered) {
            uiRenderer.roundedRect(button.x() + pad * 2.0f, button.y() + button.height() - pad * 3.0f, button.width() - pad * 4.0f, Math.max(1.0f, 2.0f * uiScale), Math.max(1.0f, radius * 0.30f), withAlpha(accent, 0.22f));
        }
        if (button.enabled() && hovered && clicked) {
            action.run();
        }
    }

    private void drawModernToggleButton(MousePosition mouse, boolean clicked, float x, float y, float width, float height, boolean enabled, Runnable action) {
        boolean hovered = contains(mouse, x, y, width, height);
        float uiScale = settings == null ? 1.0f : settings.uiScale();
        float radius = height * 0.5f;
        UiColor border = enabled ? withAlpha(UiColor.ACCENT, hovered ? 0.95f : 0.74f) : new UiColor(0.30f, 0.24f, 0.19f, hovered ? 0.90f : 0.72f);
        UiColor fill = enabled
                ? hovered ? new UiColor(0.15f, 0.34f, 0.19f, 0.94f) : new UiColor(0.10f, 0.25f, 0.15f, 0.90f)
                : hovered ? new UiColor(0.18f, 0.13f, 0.11f, 0.90f) : new UiColor(0.11f, 0.10f, 0.09f, 0.82f);
        float pad = Math.max(2.0f, 3.0f * uiScale);
        float knobSize = Math.max(8.0f, height - pad * 2.0f);
        float knobX = enabled ? x + width - knobSize - pad : x + pad;
        uiRenderer.roundedRect(x + 1.0f * uiScale, y + 2.0f * uiScale, width, height, radius, new UiColor(0.0f, 0.0f, 0.0f, 0.22f));
        uiRenderer.roundedRect(x, y, width, height, radius, border);
        uiRenderer.roundedRect(x + pad * 0.75f, y + pad * 0.75f, width - pad * 1.5f, height - pad * 1.5f, Math.max(1.0f, radius - pad), fill);
        uiRenderer.roundedRect(knobX, y + pad, knobSize, knobSize, knobSize * 0.5f, enabled ? withAlpha(UiColor.ACCENT, 0.86f) : new UiColor(0.62f, 0.58f, 0.50f, 0.68f));
        float textScale = Math.min(1.05f * uiScale, (width - 8.0f * uiScale) / Math.max(1.0f, BitmapFont.textWidth(enabled ? "ON" : "OFF", 1.0f)));
        uiRenderer.centeredText(enabled ? "ON" : "OFF", x + width * 0.5f, y + height * 0.5f - BitmapFont.textHeight(textScale) * 0.5f, textScale, UiColor.WHITE);
        if (hovered && clicked) {
            action.run();
        }
    }

    private void drawAssetSlot(float x, float y, float size, boolean selected, boolean hotbarSlot) {
        if (selected && hotbarSlot) {
            drawSelectedSlotPulse(x, y, size);
        }
        UiColor outer = selected
                ? UiColor.ACCENT
                : hotbarSlot ? new UiColor(0.40f, 0.40f, 0.36f, 0.98f) : new UiColor(0.36f, 0.36f, 0.33f, 0.98f);
        UiColor fill = selected
                ? new UiColor(0.44f, 0.54f, 0.38f, 0.98f)
                : hotbarSlot ? new UiColor(0.30f, 0.30f, 0.27f, 0.98f) : new UiColor(0.28f, 0.28f, 0.26f, 0.98f);
        float radius = Math.max(3.0f, Math.min(8.0f, size * 0.14f));
        uiRenderer.roundedRect(x + 1.5f, y + 2.5f, size, size, radius, new UiColor(0.0f, 0.0f, 0.0f, 0.26f));
        uiRenderer.roundedRect(x - 1.0f, y - 1.0f, size + 2.0f, size + 2.0f, radius + 1.0f, outer);
        uiRenderer.roundedRect(x, y, size, size, radius, fill);
        uiRenderer.roundedRect(x + 3.0f, y + 3.0f, size - 6.0f, size - 6.0f, Math.max(1.0f, radius - 2.0f), new UiColor(0.17f, 0.17f, 0.16f, 0.72f));
        uiRenderer.roundedRect(x + 2.0f, y + 2.0f, size - 4.0f, 2.0f, 1.0f, selected ? UiColor.WHITE : new UiColor(0.76f, 0.75f, 0.68f, 0.42f));
        uiRenderer.rect(x + 2.0f, y + 3.0f, 2.0f, size - 6.0f, selected ? UiColor.WHITE : new UiColor(0.76f, 0.75f, 0.68f, 0.32f));
        uiRenderer.rect(x + 2.0f, y + size - 4.0f, size - 4.0f, 2.0f, new UiColor(0.08f, 0.08f, 0.075f, 0.72f));
        uiRenderer.rect(x + size - 4.0f, y + 2.0f, 2.0f, size - 4.0f, new UiColor(0.08f, 0.08f, 0.075f, 0.70f));
    }

    private void drawHotbarPanel(float x, float y, float width, float height) {
        float uiScale = settings == null ? 1.0f : settings.uiScale();
        drawModernPanel(
                x,
                y,
                width,
                height,
                10.0f * uiScale,
                new UiColor(0.028f, 0.040f, 0.036f, 0.78f),
                new UiColor(0.012f, 0.018f, 0.017f, 0.76f),
                UiColor.ACCENT,
                uiScale
        );
    }

    private void drawSelectedSlotPulse(float x, float y, float size) {
        UiPulse.Sample pulse = SELECTED_SLOT_PULSE.sample(frameTimeSeconds);
        float inset = pulse.inset();
        UiColor glow = new UiColor(0.55f, 0.86f, 0.48f, pulse.alpha());
        uiRenderer.roundedRect(x - inset, y - inset, size + inset * 2.0f, size + inset * 2.0f, Math.max(4.0f, size * 0.16f), glow);
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
            drawModernButton(button, mouse, clicked, action);
            return;
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
            setStatus(earlyGameMilestones.unlockRecipe(firstNewRecipe.label()));
            recipeUnlockPop.trigger(firstNewRecipe.key(), currentTimeSeconds());
        }
    }

    private boolean isShiftDown() {
        return glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
    }

    private void clearInventoryDrag() {
        draggedInventorySlot = -1;
        draggedInventorySource = InventoryDragSource.PLAYER;
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
            clearInventoryDrag();
            resumeGame();
            return;
        }
        gameState = GameState.CRAFTING;
        craftingSearchFocused = false;
        clearInventoryDrag();
        previousEnter = false;
        previousBackspace = false;
        setCursorForState();
        updateWindowTitle();
    }

    private void openJournal() {
        openJournal(GameState.PLAYING);
    }

    private void openJournal(GameState returnState) {
        if (world == null) {
            return;
        }
        refreshJournalDiscoveries(currentTimeSeconds());
        craftingSearchFocused = false;
        clearInventoryDrag();
        journalReturnState = returnState == GameState.PAUSED ? GameState.PAUSED : GameState.PLAYING;
        gameState = GameState.JOURNAL;
        setCursorForState();
        updateWindowTitle();
    }

    private void closeJournal() {
        if (journalReturnState == GameState.PAUSED && world != null) {
            gameState = GameState.PAUSED;
            setCursorForState();
            updateWindowTitle();
            return;
        }
        resumeGame();
    }

    private void refreshPreview() {
        if (!onlineMode && world != null) {
            ChunkStreamingRings rings = chunkStreamingRings();
            world.ensurePreviewAround(camera.position(), rings.previewRadiusChunks());
            int releaseSafeUnloadBudget = Math.min(settings.chunkUnloadBudgetChunks(), settings.gpuReleaseBudgetChunks());
            List<ChunkPos> unloadedChunks = world.unloadOutside(camera.position(), rings.retainRadiusChunks(), releaseSafeUnloadBudget);
            WorldRenderer.MeshReleaseStats releaseStats = worldRenderer.releaseChunks(unloadedChunks);
            lastUnloadedChunks = unloadedChunks.size();
            lastReleasedGpuMeshLayers = releaseStats.releasedLayers();
            worldRenderer.rebuildDirty(
                    world,
                    settings.ambientOcclusionEnabled(),
                    settings.transparentWaterEnabled(),
                    Integer.MAX_VALUE,
                    camera.position(),
                    Double.POSITIVE_INFINITY,
                    Double.POSITIVE_INFINITY,
                    rings.renderRadiusChunks(),
                    rings.previewRadiusChunks(),
                    settings.greedyMeshingEnabled()
            );
        }
    }

    private int chunkRetentionRadiusChunks() {
        return chunkStreamingRings().retainRadiusChunks();
    }

    private ChunkStreamingRings chunkStreamingRings() {
        return ChunkStreamingRings.client(settings.renderDistanceChunks(), settings.previewRadiusChunks());
    }

    private RenderSettings currentRenderSettings() {
        float fogEnd = Math.max(72.0f, settings.renderDistanceChunks() * 16.0f);
        int dayMinute = localDayMinutes();
        fogEnd *= CozyColorPipeline.fogDistanceScaleForMinute(dayMinute);
        float fogStart = fogEnd * 0.58f;
        Vector3f sky = CozyColorPipeline.skyColorForMinute(dayMinute);
        Vector3f fog = CozyColorPipeline.fogColorForMinute(dayMinute);
        Vector3f biomeTint = currentBiomeTint();
        float globalBrightness = CozyColorPipeline.globalBrightnessForMinute(dayMinute);
        float nightLightBoost = CozyColorPipeline.nightLightBoostForMinute(dayMinute);
        float caveDarkness = 0.62f;
        float weatherFlash = weatherLightning.currentFlash(currentTimeSeconds());
        sky = CozyColorPipeline.mix(sky, biomeTint, 0.045f);
        fog = CozyColorPipeline.mix(fog, biomeTint, 0.10f);
        if (headUnderwaterNow) {
            sky = new Vector3f(0.10f, 0.34f, 0.48f);
            fog = new Vector3f(0.08f, 0.28f, 0.38f);
            fogStart = 2.0f;
            fogEnd = Math.min(28.0f, Math.max(10.0f, fogEnd * 0.28f));
            caveDarkness = 0.18f;
            weatherFlash *= 0.28f;
        }
        sky = WeatherLightningController.applySkyFlash(sky, weatherFlash);
        fog = WeatherLightningController.applyFogFlash(fog, weatherFlash);
        globalBrightness = Math.min(1.15f, globalBrightness + weatherFlash * 0.22f);
        return new RenderSettings(
                settings.renderDistanceChunks(),
                settings.fogEnabled(),
                settings.ambientOcclusionEnabled(),
                settings.softShadowsEnabled(),
                settings.bloomEnabled(),
                headUnderwaterNow,
                settings.simpleWaterEnabled(),
                settings.bloomEnabled() ? 0.22f : 0.0f,
                0.18f,
                fogStart,
                fogEnd,
                sky.x,
                sky.y,
                sky.z,
                fog.x,
                fog.y,
                fog.z,
                biomeTint.x,
                biomeTint.y,
                biomeTint.z,
                globalBrightness,
                nightLightBoost,
                caveDarkness,
                weatherFlash,
                settings.renderDebugView()
        );
    }

    private Vector3f currentBiomeTint() {
        if (world == null) {
            return CozyColorPipeline.biomeTint("", null);
        }
        Vector3f position = camera.position();
        String biomeKey = world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z));
        BiomeType biome = biomes.findByKey(biomeKey).orElse(null);
        return CozyColorPipeline.biomeTint(biomeKey, biome);
    }

    private static String clampText(String text, int maxChars) {
        if (text == null || text.isEmpty() || maxChars <= 0) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        if (maxChars <= 3) {
            return ".".repeat(maxChars);
        }
        return text.substring(0, maxChars - 3) + "...";
    }

    static String fitTextToWidth(String text, float scale, float maxWidth) {
        if (text == null || text.isEmpty() || scale <= 0.0f || maxWidth <= 0.0f) {
            return "";
        }
        if (BitmapFont.textWidth(text, scale) <= maxWidth) {
            return text;
        }
        String suffix = "...";
        if (BitmapFont.textWidth(suffix, scale) > maxWidth) {
            return "";
        }
        int low = 0;
        int high = text.length();
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            String candidate = text.substring(0, mid) + suffix;
            if (BitmapFont.textWidth(candidate, scale) <= maxWidth) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return text.substring(0, low).stripTrailing() + suffix;
    }

    static float fitTextScale(String text, float preferredScale, float minimumScale, float maxWidth) {
        if (maxWidth <= 0.0f || text == null || text.isEmpty()) {
            return Math.max(0.1f, minimumScale);
        }
        if (preferredScale <= minimumScale || BitmapFont.textWidth(text, preferredScale) <= maxWidth) {
            return Math.max(0.1f, preferredScale);
        }
        float fitted = preferredScale * (maxWidth / BitmapFont.textWidth(text, preferredScale));
        return Math.max(Math.max(0.1f, minimumScale), Math.min(preferredScale, fitted));
    }

    private static float clampFloat(float value, float min, float max) {
        if (max < min) {
            return min;
        }
        return Math.max(min, Math.min(max, value));
    }

    private static String biomeLabel(String biomeKey) {
        int colon = biomeKey.indexOf(':');
        String value = colon >= 0 ? biomeKey.substring(colon + 1) : biomeKey;
        return value.replace('_', ' ').toUpperCase(Locale.ROOT);
    }

    private String itemKey(short itemId) {
        return items.findById(itemId)
                .map(ItemType::key)
                .orElse("unknown:" + itemId);
    }

    private ToolType itemToolType(short itemId) {
        return items.findById(itemId)
                .map(ItemType::toolType)
                .orElse(ToolType.NONE);
    }

    private String blockKey(short blockId) {
        return blocks.findById(blockId)
                .map(BlockType::key)
                .orElse("unknown:" + blockId);
    }

    private void startSingleplayer() {
        beginLoading(LoadingScreenViewModel.loadingWorld(0.05), this::loadSingleplayerAsync);
    }

    private void loadSingleplayerAsync() {
        int ticket = loadingSession.get();
        closeGameSession();
        publishLoadingScreen(LoadingScreenViewModel.loadingWorld(0.10));
        onlineMode = false;
        double startedAtSeconds = currentTimeSeconds();
        pendingAuthoritativePlayerState.set(null);
        pendingProjectileImpacts.clear();
        pendingGameplayEvents.clear();
        weatherLightning.reset();
        hotbar.resetForNewGame();
        resetSurvivalHudSignals();
        syncKnownRecipeUnlocks();

        ChunkStreamingRings rings = chunkStreamingRings();
        int initialRadius = initialSingleplayerLoadRadius(rings);
        int requiredChunks = loadingChunkTarget(initialRadius);

        startDaemonThread("Singleplayer-World-Loader", () -> {
            try {
                ClientWorld loadedWorld = new ClientWorld(connectionOptions.seed());
                int loadedChunks = Math.min(loadedWorld.loadedChunkCount(), requiredChunks);
                publishLoadingScreen(LoadingScreenViewModel.streamingSpawn(loadedChunks, requiredChunks));
                while (loadingSessionActive(ticket) && loadedChunks < requiredChunks) {
                    loadedWorld.generatePreview(initialRadius, SINGLEPLAYER_SPAWN_LOAD_BATCH_CHUNKS);
                    int nextLoadedChunks = Math.min(loadedWorld.loadedChunkCount(), requiredChunks);
                    publishLoadingScreen(LoadingScreenViewModel.streamingSpawn(nextLoadedChunks, requiredChunks));
                    if (nextLoadedChunks <= loadedChunks) {
                        break;
                    }
                    loadedChunks = nextLoadedChunks;
                }
                if (!loadingSessionActive(ticket)) {
                    return;
                }
                publishLoadingScreen(LoadingScreenViewModel.loadingWorld(0.92));
                enqueueMainThread(() -> completeSingleplayerLoad(ticket, loadedWorld, startedAtSeconds));
            } catch (RuntimeException e) {
                failLoadingAsync(ticket, "Failed to load world: " + e.getMessage());
            }
        });
    }

    private void startMultiplayer() {
        String host = connectionOptions.host() == null ? "127.0.0.1" : connectionOptions.host();
        beginLoading(LoadingScreenViewModel.joiningServer(host + ":" + connectionOptions.port()), () -> loadMultiplayer(host));
    }

    private void loadMultiplayer(String host) {
        int ticket = loadingSession.get();
        String endpoint = host + ":" + connectionOptions.port();
        closeGameSession();
        publishLoadingScreen(LoadingScreenViewModel.joiningServer(endpoint));
        onlineMode = false;
        double startedAtSeconds = currentTimeSeconds();
        hotbar.resetForNewGame();
        resetSurvivalHudSignals();
        syncKnownRecipeUnlocks();
        nextMovementSequence = 1L;
        lastAuthoritativeMovementSequence = 0L;
        pendingAuthoritativePlayerState.set(null);
        pendingProjectileImpacts.clear();
        pendingGameplayEvents.clear();
        weatherLightning.reset();
        startDaemonThread("Multiplayer-Connector", () -> {
            GameClientConnection connecting = null;
            try {
                ClientWorld connectedWorld = new ClientWorld(connectionOptions.seed());
                connecting = new GameClientConnection(
                        host,
                        connectionOptions.port(),
                        connectionOptions.username(),
                        connectedWorld,
                        hotbar,
                        playerStats,
                        chatLog,
                        pendingAuthoritativePlayerState::set,
                        pendingProjectileImpacts::add,
                        pendingGameplayEvents::add
                );
                connecting.connect();
                GameClientConnection connected = connecting;
                enqueueMainThread(() -> completeMultiplayerLoad(ticket, host, connectedWorld, connected, startedAtSeconds));
            } catch (RuntimeException e) {
                if (connecting != null) {
                    connecting.close();
                }
                failLoadingAsync(ticket, "Connection failed: " + endpoint);
            }
        });
    }

    private void beginLoading(LoadingScreenViewModel viewModel, Runnable action) {
        loadingSession.incrementAndGet();
        mainThreadActions.clear();
        loadingScreen = viewModel == null ? LoadingScreenViewModel.boot() : viewModel;
        pendingLoadingAction = action;
        gameState = GameState.LOADING;
        setCursorForState();
        updateWindowTitle();
    }

    private void runPendingLoadingAction() {
        if (gameState != GameState.LOADING || pendingLoadingAction == null) {
            return;
        }
        Runnable action = pendingLoadingAction;
        pendingLoadingAction = null;
        action.run();
    }

    private void publishLoadingScreen(LoadingScreenViewModel viewModel) {
        loadingScreen = viewModel == null ? LoadingScreenViewModel.boot() : viewModel;
    }

    private void completeSingleplayerLoad(int ticket, ClientWorld loadedWorld, double startedAtSeconds) {
        if (!loadingSessionActive(ticket)) {
            return;
        }
        worldStartTimeSeconds = startedAtSeconds;
        world = loadedWorld;
        setCameraToSpawn();
        publishLoadingScreen(LoadingScreenViewModel.loadingWorld(1.0));
        gameState = GameState.PLAYING;
        setStatus("Singleplayer world loaded");
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void completeMultiplayerLoad(int ticket, String host, ClientWorld connectedWorld, GameClientConnection connected, double startedAtSeconds) {
        if (!loadingSessionActive(ticket)) {
            connected.close();
            return;
        }
        worldStartTimeSeconds = startedAtSeconds;
        world = connectedWorld;
        connection = connected;
        onlineMode = true;
        setCameraToSpawn();
        gameState = GameState.PLAYING;
        setStatus("Connected to " + host + ":" + connectionOptions.port());
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void failLoadingAsync(int ticket, String message) {
        publishLoadingScreen(LoadingScreenViewModel.error(message));
        enqueueMainThread(() -> {
            if (!loadingSessionActive(ticket)) {
                return;
            }
            closeGameSession();
            gameState = GameState.MAIN_MENU;
            setStatus(message);
            setCursorForState();
            updateWindowTitle();
        });
    }

    private void enqueueMainThread(Runnable action) {
        if (action != null) {
            mainThreadActions.add(action);
        }
    }

    private void runMainThreadActions() {
        Runnable action;
        while ((action = mainThreadActions.poll()) != null) {
            action.run();
        }
    }

    private boolean loadingSessionActive(int ticket) {
        return loadingSession.get() == ticket;
    }

    private static void startDaemonThread(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        thread.start();
    }

    static int initialSingleplayerLoadRadius(ChunkStreamingRings rings) {
        if (rings == null) {
            return 0;
        }
        return Math.max(0, Math.min(rings.simulationRadiusChunks(), SINGLEPLAYER_SPAWN_LOAD_RADIUS_CAP));
    }

    static int loadingChunkTarget(int radiusChunks) {
        int radius = Math.max(0, radiusChunks);
        int diameter = radius * 2 + 1;
        return diameter * diameter;
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
        pendingAuthoritativePlayerState.set(null);
        pendingProjectileImpacts.clear();
        pendingGameplayEvents.clear();
        weatherLightning.reset();
        announcedRecipeUnlocks.clear();
        discoveredBiomeKeys.clear();
        discoveredCreatureKeys.clear();
        earlyGameMilestones.reset();
        nightSafetyPrompts.reset();
        playerStats.respawn();
        resetSurvivalHudSignals();
        world = null;
        onlineMode = false;
        nextMoveSendTime = 0.0;
        nextComfortScanTime = 0.0;
        nextRecipeUnlockScanTime = 0.0;
        nextCampfireReadyFeedbackScanTime = 0.0;
        nextToolHintTime = 0.0;
        clientTransactionId = 0;
        lastUnloadedChunks = 0;
        lastReleasedGpuMeshLayers = 0;
        lastComfortFeedbackLevel = ComfortHudInfo.Level.NONE;
        lastCampfireReadySignature = "";
        lastHudBiomeKey = "";
        nextBiomeFeedbackTime = 0.0;
        journalReturnState = GameState.PLAYING;
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

            // Mouse callbacks report window coordinates; UI interaction uses framebuffer coordinates.
            double scaleX = (double) framebufferWidth / Math.max(1, windowWidth);
            double scaleY = (double) framebufferHeight / Math.max(1, windowHeight);

            return new MousePosition(x.get(0) * scaleX, y.get(0) * scaleY);
        }
    }

    private enum JournalTab {
        NOTES("NOTES"),
        BIOMES("BIOMES"),
        STRUCTURES("STRUCT"),
        CREATURES("CREATURES"),
        RECIPES("RECIPES"),
        COLLECTIBLES("ITEMS");

        private final String label;

        JournalTab(String label) {
            this.label = label;
        }

        private String label() {
            return label;
        }
    }

    private enum GameState {
        MAIN_MENU,
        LOADING,
        PLAYING,
        PAUSED,
        SETTINGS,
        CHAT,
        CRAFTING,
        JOURNAL,
        STORAGE,
        DEAD
    }

    private record MousePosition(double x, double y) {
    }
}
