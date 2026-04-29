package dev.voxelgame.client;

import dev.voxelgame.client.audio.AudioCue;
import dev.voxelgame.client.audio.GameAudio;
import dev.voxelgame.client.net.GameClientConnection;
import dev.voxelgame.client.render.RenderSettings;
import dev.voxelgame.client.render.WorldRenderer;
import dev.voxelgame.client.render.entity.EntityRenderer;
import dev.voxelgame.client.ui.GameSprites;
import dev.voxelgame.client.ui.UiButton;
import dev.voxelgame.client.ui.UiColor;
import dev.voxelgame.client.ui.UiRenderer;
import dev.voxelgame.client.ui.UiSpriteRenderer;
import dev.voxelgame.client.world.ClientWorld;
import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Locale;

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
import static org.lwjgl.glfw.GLFW.GLFW_KEY_O;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
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
    private final ConnectionOptions connectionOptions;
    private final GameSettings settings;
    private final Camera camera = new Camera();
    private final Hotbar hotbar = new Hotbar();
    private final ChatLog chatLog = new ChatLog();
    private final PlayerStats playerStats = new PlayerStats();
    private final GameAudio audio = new GameAudio();
    private final StringBuilder chatDraft = new StringBuilder();
    private GameMode gameMode = GameMode.SURVIVAL;
    private GameState gameState = GameState.MAIN_MENU;
    private ClientWorld world;
    private WorldRenderer worldRenderer;
    private EntityRenderer entityRenderer;
    private UiRenderer uiRenderer;
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

        window = glfwCreateWindow(1280, 720, "Voxel Survival", 0, 0);
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
        worldRenderer = new WorldRenderer();
        entityRenderer = new EntityRenderer();
        uiRenderer = new UiRenderer();
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
            float deltaSeconds = (float) Math.min(0.05, now - lastTime);
            lastTime = now;
            updateFps(now);

            boolean leftMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
            boolean rightMouse = glfwGetMouseButton(window, GLFW_MOUSE_BUTTON_RIGHT) == GLFW_PRESS;
            boolean leftClicked = leftMouse && !previousLeftMouse;
            boolean rightClicked = rightMouse && !previousRightMouse;
            MousePosition mouse = mousePosition();

            handleEscape();
            handleGlobalKeys();

            if (gameState == GameState.PLAYING) {
                boolean moving = camera.hasMovementInput(window);
                boolean sprinting = camera.wantsSprint(window) && moving && playerStats.canSprint();
                camera.update(window, deltaSeconds, settings.mouseSensitivity(), world, gameMode, playerStats.canSprint());
                float fallImpact = camera.consumeFallImpactSpeed();
                if (gameMode == GameMode.SURVIVAL && fallImpact > 18.0f) {
                    playerStats.hurt(Math.round((fallImpact - 16.0f) * 0.45f));
                }
                playerStats.tick(deltaSeconds, gameMode, world != null && world.isUnderwater(camera.position()), sprinting, moving);
                if (!onlineMode) {
                    world.ensurePreviewAround(camera.position(), settings.previewRadiusChunks());
                } else {
                    sendMovementIfDue(now);
                }
                if (hotbar.updateSelection(window)) {
                    audio.play(AudioCue.INVENTORY_CLICK);
                    updateWindowTitle();
                }
                handleBlockInteraction(leftClicked, rightClicked, now);
            } else if (gameState == GameState.CHAT) {
                handleChatInput();
            }

            glClearColor(0.52f, 0.72f, 0.95f, 1.0f);
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            if (world != null) {
                worldRenderer.rebuildDirty(world, settings.ambientOcclusionEnabled(), settings.transparentWaterEnabled(), settings.meshBuildBudgetChunks());
                Matrix4f projection = new Matrix4f().perspective(
                        (float) Math.toRadians(settings.fieldOfViewDegrees()),
                        (float) framebufferWidth / framebufferHeight,
                        0.05f,
                        1200.0f
                );
                lastRenderStats = worldRenderer.render(projection, camera.viewMatrix(), world, camera.position(), currentRenderSettings(), now);
                entityRenderer.render(projection, camera.viewMatrix(), world.visibleEntities(), now);
            }

            uiRenderer.begin();
            spriteRenderer.begin();
            if (gameState == GameState.MAIN_MENU) {
                renderMainMenu(mouse, leftClicked);
            } else if (gameState == GameState.PAUSED) {
                renderPauseMenu(mouse, leftClicked);
            } else if (gameState == GameState.SETTINGS) {
                renderSettingsMenu(mouse, leftClicked);
            } else if (gameState == GameState.CRAFTING) {
                renderCraftingScreen(mouse, leftClicked);
            }
            renderHud();
            renderChatOverlay();
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
        if (spriteRenderer != null) {
            spriteRenderer.close();
        }
        if (sprites != null) {
            sprites.close();
        }
        if (worldRenderer != null) {
            worldRenderer.close();
        }
        if (entityRenderer != null) {
            entityRenderer.close();
        }
        if (connection != null) {
            connection.close();
        }
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private void handleBlockInteraction(boolean leftClicked, boolean rightClicked, double now) {
        if (now < nextBlockActionTime) {
            return;
        }
        if (leftClicked) {
            world.pick(camera.position(), camera.forward(), 7.0).ifPresent(hit -> {
                if (onlineMode) {
                    connection.send(new GamePacket.BlockAction(
                            GamePacket.BlockAction.Action.BREAK,
                            hit.x(),
                            hit.y(),
                            hit.z(),
                            hit.placeX(),
                            hit.placeY(),
                            hit.placeZ(),
                            Blocks.AIR
                    ));
                    nextBlockActionTime = now + 0.18;
                } else {
                    world.targetBlock(hit).ifPresent(target -> {
                        float multiplier = hotbar.selectedBreakMultiplier(target);
                        if (world.breakBlock(hit)) {
                            collectDrops(target, hit, multiplier);
                            hotbar.damageSelectedTool(target);
                            nextBlockActionTime = now + breakDelay(target, multiplier);
                            statusMessage = "Gathered " + cozyName(target.dropItemKey());
                            audio.play(AudioCue.BLOCK_BREAK);
                        }
                    });
                }
            });
        }
        if (rightClicked) {
            String foodLabel = hotbar.selectedLabel();
            if (hotbar.useSelectedFood(playerStats)) {
                statusMessage = "Ate " + foodLabel;
                nextBlockActionTime = now + 0.22;
                audio.play(AudioCue.EAT);
                return;
            }
            hotbar.selectedPlaceBlockId().ifPresent(blockId -> world.pick(camera.position(), camera.forward(), 7.0).ifPresent(hit -> {
                if (onlineMode) {
                    connection.send(new GamePacket.BlockAction(
                            GamePacket.BlockAction.Action.PLACE,
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
                    if (world.placeBlock(hit, blockId)) {
                        hotbar.consumeSelectedOne();
                        nextBlockActionTime = now + 0.12;
                        audio.play(AudioCue.BLOCK_PLACE);
                    }
                }
            }));
        }
    }

    private void collectDrops(BlockType target, dev.voxelgame.common.math.Raycast.Hit hit, float multiplier) {
        world.dropFor(hit).ifPresent(drop -> {
            int count = 1;
            if (target.preferredTool().name().equals("KNIFE") && multiplier > 2.0f) {
                count++;
            }
            hotbar.addItem(drop, count);
        });
    }

    private static double breakDelay(BlockType target, float multiplier) {
        if (target.hardness() <= 0.0f) {
            return 0.08;
        }
        double delay = target.hardness() * 0.34 / Math.max(0.35f, multiplier);
        return Math.max(0.08, Math.min(0.85, delay));
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

        if (gameState == GameState.PLAYING && settings.chatEnabled()) {
            if (chat && !previousChat) {
                openChat("");
            } else if (slash && !previousSlash) {
                openChat("/");
            }
        }
        if ((gameState == GameState.PLAYING || gameState == GameState.CRAFTING) && crafting && !previousCrafting) {
            toggleCrafting();
        }
        if (f3 && !previousF3) {
            settings.toggleDebugOverlay();
        }
        if ((gameState == GameState.PLAYING || gameState == GameState.CRAFTING) && hudToggle && !previousHudToggle) {
            settings.toggleHud();
        }
        if ((gameState == GameState.PLAYING || gameState == GameState.CRAFTING) && modeCycle && !previousModeCycle) {
            cycleGameMode();
        }
        if ((gameState == GameState.PLAYING || gameState == GameState.CRAFTING) && settingsKey && !previousSettingsKey) {
            openSettings(gameState);
        }
        if ((gameState == GameState.PLAYING || gameState == GameState.CRAFTING) && spawnKey && !previousSpawnKey) {
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
        if (gameState != GameState.CHAT || codepoint < 32 || codepoint > 126 || chatDraft.length() >= 96) {
            return;
        }
        chatDraft.append((char) codepoint);
    }

    private void executeCommand(String commandLine) {
        String[] parts = commandLine.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isBlank()) {
            return;
        }
        String command = parts[0].toLowerCase(Locale.ROOT);
        try {
            switch (command) {
                case "help" -> chatLog.add("Commands: /help /keys /seed /pos /tp x y z /spawn /gamemode survival|creative|spectator /renderdistance n /preview n /meshbudget n /fov n /fog /ao /shadows /hud /debug /water /settings /clear /say text");
                case "keys", "keybinds" -> showKeybinds();
                case "seed" -> chatLog.add("Seed: " + connectionOptions.seed());
                case "pos" -> chatLog.add(positionLine());
                case "tp" -> teleport(parts);
                case "spawn" -> {
                    setCameraToSpawn();
                    chatLog.add("Teleported to spawn");
                }
                case "gamemode", "gm" -> setGameMode(parts);
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
                case "hud" -> toggleCommand("HUD", settings.hudEnabled(), settings::toggleHud);
                case "debug" -> toggleCommand("Debug overlay", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
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

    private void showKeybinds() {
        chatLog.add("Keys: WASD move, Space jump/up, Ctrl down, Shift sprint");
        chatLog.add("Keys: E crafting, O settings, R spawn, F1 HUD, F3 debug, F4 mode");
        chatLog.add("Keys: T chat, / command, 1-9 hotbar, mouse break/place");
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
                case PLAYING -> hotbar.selectedLabel();
            };
            glfwSetWindowTitle(window, "Voxel Survival - " + suffix);
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
                resumeGame();
            }
        }
        previousEscape = escape;
    }

    private void renderMainMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.03f, 0.05f, 0.05f, 1.0f));
        uiRenderer.rect(0, framebufferHeight * 0.60f, framebufferWidth, framebufferHeight * 0.40f, new UiColor(0.07f, 0.12f, 0.10f, 0.9f));
        uiRenderer.centeredText("VOXEL SURVIVAL", framebufferWidth * 0.5f, 88.0f, 7.0f, UiColor.WHITE);
        uiRenderer.centeredText("JAVA LWJGL ENGINE BUILD", framebufferWidth * 0.5f, 154.0f, 2.0f, UiColor.MUTED);

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
        renderCraftingMenu(mouse, clicked, x + buttonWidth + 34.0f, y);
    }

    private void renderCraftingScreen(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.72f));
        uiRenderer.centeredText("CRAFTING", framebufferWidth * 0.5f, 70.0f, 5.0f, UiColor.WHITE);
        uiRenderer.centeredText("E CLOSE  O SETTINGS  F4 MODE  R SPAWN", framebufferWidth * 0.5f, 116.0f, 1.7f, UiColor.MUTED);

        float contentWidth = Math.min(920.0f, framebufferWidth - 80.0f);
        float x = framebufferWidth * 0.5f - contentWidth * 0.5f;
        float y = 160.0f;
        renderInventoryTabs(x, y - 52.0f, contentWidth);
        renderCraftingMenu(mouse, clicked, x, y);
        renderInventoryGrid(x + Math.min(440.0f, contentWidth * 0.48f) + 42.0f, y);
    }

    private void renderInventoryTabs(float x, float y, float width) {
        float tabWidth = Math.min(170.0f, width / 3.0f - 8.0f);
        String[] labels = {"CRAFT", "PACK", "COZY LOG"};
        for (int i = 0; i < labels.length; i++) {
            float tx = x + i * (tabWidth + 8.0f);
            uiRenderer.rect(tx, y, tabWidth, 32.0f, i == 0 ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            uiRenderer.rect(tx, y + 30.0f, tabWidth, 2.0f, i == 0 ? UiColor.ACCENT : UiColor.BUTTON);
            uiRenderer.centeredText(labels[i], tx + tabWidth * 0.5f, y + 9.0f, 1.45f, i == 0 ? UiColor.WHITE : UiColor.MUTED);
        }
        uiRenderer.text("Selected: " + clampText(hotbar.selectedTooltip(), 42), x + Math.min(width - 360.0f, 540.0f), y + 9.0f, 1.35f, UiColor.MUTED);
    }

    private void renderSettingsMenu(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.04f, 0.055f, 0.06f, 0.95f));
        uiRenderer.centeredText("SETTINGS", framebufferWidth * 0.5f, 72.0f, 6.0f, UiColor.WHITE);

        float panelWidth = Math.min(620.0f, framebufferWidth - 80.0f);
        float x = framebufferWidth * 0.5f - panelWidth * 0.5f;
        float y = 124.0f;
        uiRenderer.rect(x - 18.0f, y - 24.0f, panelWidth + 36.0f, 510.0f, UiColor.PANEL);

        settingStepper(mouse, clicked, x, y, panelWidth, "RENDER DIST", settings.renderDistanceChunks() + " CHUNKS",
                () -> settings.adjustRenderDistance(-1),
                () -> settings.adjustRenderDistance(1));
        settingStepper(mouse, clicked, x, y + 54.0f, panelWidth, "WORLD PREVIEW", settings.previewRadiusChunks() + " CHUNKS",
                () -> {
                    settings.adjustPreviewRadius(-1);
                    refreshPreview();
                },
                () -> {
                    settings.adjustPreviewRadius(1);
                    refreshPreview();
                });
        settingStepper(mouse, clicked, x, y + 108.0f, panelWidth, "FIELD OF VIEW", settings.fieldOfViewDegrees() + " DEG",
                () -> settings.adjustFieldOfView(-5),
                () -> settings.adjustFieldOfView(5));
        settingStepper(mouse, clicked, x, y + 162.0f, panelWidth, "MOUSE SPEED", settings.mouseSensitivityPercent() + "%",
                () -> settings.adjustMouseSensitivity(-10),
                () -> settings.adjustMouseSensitivity(10));
        settingStepper(mouse, clicked, x, y + 216.0f, panelWidth, "MESH BUDGET", settings.meshBuildBudgetChunks() + " CHUNKS",
                () -> settings.adjustMeshBuildBudget(-1),
                () -> settings.adjustMeshBuildBudget(1));

        float leftWidth = panelWidth * 0.46f;
        float rightX = x + panelWidth * 0.52f;
        float rightWidth = panelWidth * 0.48f;
        settingToggle(mouse, clicked, x, y + 270.0f, leftWidth, "FOG", settings.fogEnabled(), settings::toggleFog);
        settingToggle(mouse, clicked, x, y + 324.0f, leftWidth, "AMBIENT AO", settings.ambientOcclusionEnabled(), () -> {
            settings.toggleAmbientOcclusion();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });
        settingToggle(mouse, clicked, x, y + 378.0f, leftWidth, "SOFT SHADOWS", settings.softShadowsEnabled(), settings::toggleSoftShadows);
        settingToggle(mouse, clicked, x, y + 432.0f, leftWidth, "VSYNC", settings.vsyncEnabled(), () -> {
            settings.toggleVsync();
            glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        });
        settingToggle(mouse, clicked, rightX, y + 270.0f, rightWidth, "HUD", settings.hudEnabled(), settings::toggleHud);
        settingToggle(mouse, clicked, rightX, y + 324.0f, rightWidth, "DEBUG", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
        settingToggle(mouse, clicked, rightX, y + 378.0f, rightWidth, "CHAT", settings.chatEnabled(), settings::toggleChat);
        settingToggle(mouse, clicked, rightX, y + 432.0f, rightWidth, "WATER", settings.transparentWaterEnabled(), () -> {
            settings.toggleTransparentWater();
            if (world != null) {
                world.markAllLoadedDirty();
            }
        });

        float buttonWidth = Math.min(280.0f, framebufferWidth - 80.0f);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, framebufferHeight - 86.0f, buttonWidth, 46.0f, "BACK", true), mouse, clicked, () -> {
            gameState = settingsReturnState;
            setCursorForState();
            updateWindowTitle();
        });
    }

    private void renderCraftingMenu(MousePosition mouse, boolean clicked, float x, float y) {
        float panelWidth = Math.min(440.0f, framebufferWidth - x - 32.0f);
        if (panelWidth < 260.0f) {
            return;
        }
        float panelHeight = 72.0f + hotbar.recipes().size() * 48.0f;
        uiRenderer.rect(x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.72f));
        uiRenderer.text("CRAFTING", x, y - 30.0f, 3.0f, UiColor.WHITE);
        float buttonHeight = 38.0f;
        int index = 0;
        for (CraftingRecipe recipe : hotbar.recipes()) {
            boolean canCraft = hotbar.canCraft(recipe);
            UiButton button = new UiButton(x, y + index * 48.0f, panelWidth, buttonHeight, recipe.label().toUpperCase(), canCraft);
            drawButton(button, mouse, clicked, () -> {
                if (onlineMode) {
                    connection.send(new GamePacket.CraftRequest(recipe.key()));
                    statusMessage = "Crafting requested";
                    updateWindowTitle();
                } else if (hotbar.craft(recipe)) {
                    statusMessage = "Crafted " + recipe.label();
                    audio.play(AudioCue.CRAFT);
                    updateWindowTitle();
                }
            });
            uiRenderer.text(hotbar.recipeSummary(recipe), x + 42.0f, y + index * 48.0f + 28.0f, 1.4f, canCraft ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), x + 8.0f, y + index * 48.0f + 5.0f, 28.0f);
            index++;
        }
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

    private void renderHud() {
        if ((gameState != GameState.PLAYING && gameState != GameState.CHAT && gameState != GameState.CRAFTING) || world == null || !settings.hudEnabled()) {
            return;
        }
        uiRenderer.rect(framebufferWidth * 0.5f - 5.0f, framebufferHeight * 0.5f - 1.0f, 10.0f, 2.0f, UiColor.WHITE);
        uiRenderer.rect(framebufferWidth * 0.5f - 1.0f, framebufferHeight * 0.5f - 5.0f, 2.0f, 10.0f, UiColor.WHITE);
        float statsY = framebufferHeight - 145.0f;
        uiRenderer.rect(14.0f, statsY - 10.0f, 342.0f, 88.0f, new UiColor(0.035f, 0.045f, 0.04f, 0.58f));
        drawMeterBar("HEARTS", playerStats.health(), 20, 28.0f, statsY, UiColor.HEART);
        drawMeterBar("HUNGER", playerStats.hunger(), 20, 28.0f, statsY + 24.0f, UiColor.HUNGER);
        drawMeterBar("ENERGY", playerStats.stamina(), 20, 28.0f, statsY + 48.0f, UiColor.ENERGY);
        uiRenderer.text("MODE " + gameMode.name(), 228.0f, statsY + 50.0f, 1.25f, UiColor.MUTED);
        if (playerStats.breath() < 20) {
            drawMeterBar("AIR", playerStats.breath(), 20, 28.0f, statsY - 24.0f, UiColor.WATER);
        }
        uiRenderer.text(clampText(hotbar.selectedTooltip(), 64), 20.0f, framebufferHeight - 32.0f, 1.65f, UiColor.WHITE);
        float hotbarWidth = Hotbar.HOTBAR_SLOTS * 72.0f + (Hotbar.HOTBAR_SLOTS - 1) * 6.0f;
        float hotbarX = Math.max(20.0f, framebufferWidth * 0.5f - hotbarWidth * 0.5f);
        for (int i = 0; i < Hotbar.HOTBAR_SLOTS; i++) {
            float x = hotbarX + i * 78.0f;
            float y = framebufferHeight - 82.0f;
            Hotbar.SlotView slot = hotbar.slotView(i);
            boolean selected = i == hotbar.selectedIndex();
            uiRenderer.rect(x - 2.0f, y - 2.0f, 74.0f, 44.0f, selected ? UiColor.ACCENT : new UiColor(0.02f, 0.025f, 0.025f, 0.55f));
            uiRenderer.rect(x, y, 70.0f, 40.0f, selected ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            uiRenderer.text(String.valueOf(i + 1), x + 5.0f, y + 6.0f, 1.05f, UiColor.MUTED);
            if (!slot.isEmpty()) {
                drawItemIcon(slot.itemKey(), x + 22.0f, y + 5.0f, 28.0f);
                if (slot.count() > 1) {
                    uiRenderer.text(String.valueOf(slot.count()), x + 53.0f, y + 23.0f, 1.15f, UiColor.WHITE);
                }
                if (slot.hasDurability()) {
                    drawDurabilityBar(x + 10.0f, y + 35.0f, 50.0f, slot.durabilityLeft(), slot.maxDurability());
                }
            }
        }
        if (settings.debugOverlayEnabled()) {
            renderDebugOverlay();
        }
    }

    private void drawMeterBar(String label, int value, int max, float x, float y, UiColor fill) {
        float width = 128.0f;
        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        uiRenderer.text(label, x, y + 2.0f, 1.2f, UiColor.MUTED);
        uiRenderer.rect(x + 78.0f, y, width, 14.0f, UiColor.SLOT);
        uiRenderer.rect(x + 80.0f, y + 2.0f, (width - 4.0f) * ratio, 10.0f, fill);
        uiRenderer.text(value + "/" + max, x + 214.0f, y + 2.0f, 1.05f, UiColor.WHITE);
    }

    private void drawDurabilityBar(float x, float y, float width, int value, int max) {
        if (max <= 0) {
            return;
        }
        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        UiColor color = ratio > 0.45f ? UiColor.ACCENT : ratio > 0.18f ? UiColor.ENERGY : UiColor.HEART;
        uiRenderer.rect(x, y, width, 3.0f, new UiColor(0.02f, 0.025f, 0.025f, 0.85f));
        uiRenderer.rect(x, y, width * ratio, 3.0f, color);
    }

    private void renderChatOverlay() {
        if (!settings.chatEnabled()) {
            return;
        }
        List<String> lines = chatLog.recent(gameState == GameState.CHAT ? 8 : 4);
        float y = framebufferHeight - 176.0f - lines.size() * 18.0f;
        if (!lines.isEmpty()) {
            uiRenderer.rect(14.0f, y - 8.0f, Math.min(760.0f, framebufferWidth - 28.0f), lines.size() * 18.0f + 14.0f, new UiColor(0.02f, 0.03f, 0.035f, 0.45f));
        }
        for (String line : lines) {
            uiRenderer.text(clampText(line, 86), 22.0f, y, 1.55f, UiColor.WHITE);
            y += 18.0f;
        }
        if (gameState == GameState.CHAT) {
            uiRenderer.rect(14.0f, framebufferHeight - 42.0f, Math.min(820.0f, framebufferWidth - 28.0f), 30.0f, new UiColor(0.02f, 0.025f, 0.03f, 0.82f));
            uiRenderer.text("> " + chatDraft, 24.0f, framebufferHeight - 34.0f, 1.8f, UiColor.WHITE);
        }
    }

    private void renderDebugOverlay() {
        org.joml.Vector3f position = camera.position();
        String biome = world == null ? "none" : world.biomeKeyAt((int) Math.floor(position.x), (int) Math.floor(position.z()));
        uiRenderer.rect(12.0f, 12.0f, 360.0f, 124.0f, new UiColor(0.02f, 0.03f, 0.035f, 0.58f));
        uiRenderer.text("FPS " + lastFps, 22.0f, 24.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("XYZ " + Math.round(position.x) + " " + Math.round(position.y) + " " + Math.round(position.z), 22.0f, 44.0f, 1.65f, UiColor.WHITE);
        uiRenderer.text("BIOME " + biomeLabel(biome), 22.0f, 64.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("RD " + settings.renderDistanceChunks() + " MB " + settings.meshBuildBudgetChunks() + " DIRTY " + (world == null ? 0 : world.dirtyChunkCount()), 22.0f, 84.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("DRAW " + lastRenderStats.renderedChunks() + " CULL " + lastRenderStats.culledChunks() + " LOADED " + (world == null ? 0 : world.loadedChunkCount()), 22.0f, 104.0f, 1.65f, UiColor.MUTED);
        uiRenderer.text("MODE " + gameMode.name() + " GROUND " + onOff(camera.onGround()), 22.0f, 124.0f, 1.65f, UiColor.MUTED);
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
        uiRenderer.button(button, hovered);
        if (button.enabled() && hovered && clicked) {
            action.run();
        }
    }

    private void updateFps(double now) {
        framesThisSecond++;
        if (now >= nextFpsSampleTime) {
            lastFps = framesThisSecond;
            framesThisSecond = 0;
            nextFpsSampleTime = now + 1.0;
        }
    }

    private void openSettings(GameState returnState) {
        settingsReturnState = returnState;
        gameState = GameState.SETTINGS;
        setCursorForState();
        updateWindowTitle();
    }

    private void toggleCrafting() {
        if (gameState == GameState.CRAFTING) {
            resumeGame();
            return;
        }
        gameState = GameState.CRAFTING;
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
        world = new ClientWorld(connectionOptions.seed());
        hotbar.resetForNewGame();
        world.generatePreview(settings.previewRadiusChunks());
        setCameraToSpawn();
        worldRenderer.rebuildDirty(world, settings.ambientOcclusionEnabled(), settings.transparentWaterEnabled(), Integer.MAX_VALUE);
        gameState = GameState.PLAYING;
        statusMessage = "Singleplayer world loaded";
        setCursorForState();
        camera.resetMouseTracking();
        updateWindowTitle();
    }

    private void startMultiplayer() {
        closeGameSession();
        onlineMode = true;
        world = new ClientWorld(connectionOptions.seed());
        hotbar.resetForNewGame();
        setCameraToSpawn();
        String host = connectionOptions.host() == null ? "127.0.0.1" : connectionOptions.host();
        try {
            connection = new GameClientConnection(host, connectionOptions.port(), connectionOptions.username(), world, hotbar, chatLog);
            connection.connect();
            gameState = GameState.PLAYING;
            statusMessage = "Connected to " + host + ":" + connectionOptions.port();
            setCursorForState();
            camera.resetMouseTracking();
            updateWindowTitle();
        } catch (RuntimeException e) {
            closeGameSession();
            statusMessage = "Connection failed: " + host + ":" + connectionOptions.port();
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
        statusMessage = "Ready";
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
        world = null;
        onlineMode = false;
        nextMoveSendTime = 0.0;
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
        CRAFTING
    }

    private record MousePosition(double x, double y) {
    }
}
