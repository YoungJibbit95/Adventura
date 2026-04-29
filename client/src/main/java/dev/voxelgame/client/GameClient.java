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
import dev.voxelgame.common.gameplay.InteractionRules;
import dev.voxelgame.common.item.CraftingRecipe;
import dev.voxelgame.common.net.GamePacket;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.DoubleBuffer;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

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
    private double pendingScrollY;
    private double frameTimeSeconds;

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
            frameTimeSeconds = now;
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
                if (gameMode == GameMode.SURVIVAL && fallImpact > 13.0f) {
                    playerStats.hurt(Math.round((fallImpact - 12.0f) * 0.55f));
                }
                playerStats.tick(deltaSeconds, gameMode, world != null && world.isUnderwater(camera.position()), sprinting, moving);
                if (!onlineMode) {
                    world.ensurePreviewAround(camera.position(), settings.previewRadiusChunks());
                } else {
                    sendMovementIfDue(now);
                }
                if (hotbar.updateSelection(window) || consumeHotbarScroll()) {
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
                            hotbar.selectedIndex(),
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
            Optional<Short> placeBlockId = hotbar.selectedPlaceBlockId();
            world.pick(camera.position(), camera.forward(), 7.0).ifPresent(hit -> {
                if (placeBlockId.isEmpty()) {
                    if (onlineMode) {
                        connection.send(new GamePacket.BlockInteract(hotbar.selectedIndex(), hit.x(), hit.y(), hit.z()));
                        statusMessage = "Interaction requested";
                        nextBlockActionTime = now + 0.15;
                        updateWindowTitle();
                    } else {
                        handleLocalBlockInteract(hit, now);
                    }
                    return;
                }
                short blockId = placeBlockId.get();
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
                    if (world.placeBlock(hit, blockId)) {
                        hotbar.consumeSelectedOne();
                        nextBlockActionTime = now + 0.12;
                        audio.play(AudioCue.BLOCK_PLACE);
                    }
                }
            });
        }
    }

    private void handleLocalBlockInteract(dev.voxelgame.common.math.Raycast.Hit hit, double now) {
        world.targetBlock(hit)
                .flatMap(InteractionRules::blockInteraction)
                .ifPresent(interaction -> {
                    if (hotbar.addItem(interaction.itemKey(), interaction.count())) {
                        statusMessage = interaction.message();
                        nextBlockActionTime = now + interaction.cooldownSeconds();
                        audio.play(AudioCue.INVENTORY_CLICK);
                        updateWindowTitle();
                    } else {
                        statusMessage = "Inventory full";
                        updateWindowTitle();
                    }
                });
    }

    private boolean consumeHotbarScroll() {
        double scroll = pendingScrollY;
        pendingScrollY = 0.0;
        if (Math.abs(scroll) < 0.01) {
            return false;
        }
        return hotbar.scroll(scroll > 0.0 ? -1 : 1);
    }

    private void collectDrops(BlockType target, dev.voxelgame.common.math.Raycast.Hit hit, float multiplier) {
        world.dropFor(hit).ifPresent(drop -> {
            int count = 1;
            count = InteractionRules.dropCount(target, multiplier);
            hotbar.addItem(drop, count);
        });
    }

    private static double breakDelay(BlockType target, float multiplier) {
        return InteractionRules.breakDelaySeconds(target, multiplier);
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
                resumeGame();
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

    private void renderCraftingScreen(MousePosition mouse, boolean clicked) {
        uiRenderer.rect(0, 0, framebufferWidth, framebufferHeight, new UiColor(0.02f, 0.025f, 0.03f, 0.72f));
        uiRenderer.centeredText("CRAFTING", framebufferWidth * 0.5f, 70.0f, 5.0f, UiColor.WHITE);
        uiRenderer.centeredText("E CLOSE  CLICK RECIPE TO CRAFT  O SETTINGS", framebufferWidth * 0.5f, 116.0f, 1.7f, UiColor.MUTED);

        float contentWidth = Math.min(980.0f, framebufferWidth - 64.0f);
        float x = framebufferWidth * 0.5f - contentWidth * 0.5f;
        float y = Math.max(142.0f, framebufferHeight * 0.20f);
        renderInventoryTabs(x, y - 52.0f, contentWidth);
        renderWorkbenchPreview(previewCraftingRecipe(), x, y);
        renderCraftingMenu(mouse, clicked, x + 360.0f, y);
        renderInventoryGridCompact(x, y + 266.0f);
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
        settingToggleCompact(mouse, clicked, rightX + columnWidth * 0.5f + 6.0f, y + 168.0f, columnWidth * 0.5f - 24.0f, "Chat", settings.chatEnabled(), settings::toggleChat);
        settingToggleCompact(mouse, clicked, rightX + 18.0f, y + 228.0f, columnWidth * 0.5f - 26.0f, "Debug", settings.debugOverlayEnabled(), settings::toggleDebugOverlay);
        settingToggleCompact(mouse, clicked, rightX + columnWidth * 0.5f + 6.0f, y + 228.0f, columnWidth * 0.5f - 24.0f, "VSync", settings.vsyncEnabled(), () -> {
            settings.toggleVsync();
            glfwSwapInterval(settings.vsyncEnabled() ? 1 : 0);
        });

        uiRenderer.rect(x, y + 352.0f, panelWidth, 54.0f, new UiColor(0.045f, 0.058f, 0.052f, 0.72f));
        uiRenderer.text("Tip: lower World Preview and Mesh Budget first if chunk loading stutters.", x + 18.0f, y + 372.0f, 1.45f, UiColor.MUTED);

        float buttonWidth = Math.min(280.0f, framebufferWidth - 80.0f);
        drawButton(new UiButton(framebufferWidth * 0.5f - buttonWidth * 0.5f, framebufferHeight - 86.0f, buttonWidth, 46.0f, "BACK", true), mouse, clicked, () -> {
            gameState = settingsReturnState;
            setCursorForState();
            updateWindowTitle();
        });
    }

    private void settingsSection(float x, float y, float width, float height, String title) {
        uiRenderer.rect(x, y, width, height, UiColor.PANEL);
        uiRenderer.rect(x, y, width, 3.0f, UiColor.ACCENT);
        uiRenderer.text(title, x + 18.0f, y + 18.0f, 2.0f, UiColor.WHITE);
    }

    private void settingStepperCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, String value, Runnable minus, Runnable plus) {
        uiRenderer.rect(x, y, width, 40.0f, UiColor.SLOT);
        uiRenderer.text(label.toUpperCase(Locale.ROOT), x + 10.0f, y + 7.0f, 1.15f, UiColor.MUTED);
        uiRenderer.text(value.toUpperCase(Locale.ROOT), x + 10.0f, y + 23.0f, 1.2f, UiColor.WHITE);
        drawButton(new UiButton(x + width - 78.0f, y + 5.0f, 30.0f, 30.0f, "-", true), mouse, clicked, minus);
        drawButton(new UiButton(x + width - 38.0f, y + 5.0f, 30.0f, 30.0f, "+", true), mouse, clicked, plus);
    }

    private void settingToggleCompact(MousePosition mouse, boolean clicked, float x, float y, float width, String label, boolean enabled, Runnable toggle) {
        uiRenderer.rect(x, y, width, 44.0f, UiColor.SLOT);
        uiRenderer.text(label.toUpperCase(Locale.ROOT), x + 10.0f, y + 8.0f, 1.2f, UiColor.WHITE);
        drawButton(new UiButton(x + width - 80.0f, y + 7.0f, 68.0f, 30.0f, enabled ? "ON" : "OFF", true), mouse, clicked, toggle);
    }

    private void renderCraftingMenu(MousePosition mouse, boolean clicked, float x, float y) {
        float panelWidth = Math.min(560.0f, framebufferWidth - x - 32.0f);
        if (panelWidth < 260.0f) {
            return;
        }
        int visibleRecipes = Math.min(9, hotbar.recipes().size());
        float panelHeight = 70.0f + visibleRecipes * 42.0f + (hotbar.recipes().size() > visibleRecipes ? 24.0f : 0.0f);
        uiRenderer.rect(x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.72f));
        uiRenderer.text("RECIPES", x, y - 30.0f, 2.6f, UiColor.WHITE);
        uiRenderer.text("CLICK TO CRAFT", x + panelWidth - 150.0f, y - 25.0f, 1.2f, UiColor.MUTED);
        float buttonHeight = 34.0f;
        for (int index = 0; index < visibleRecipes; index++) {
            CraftingRecipe recipe = hotbar.recipes().get(index);
            boolean canCraft = hotbar.canCraft(recipe);
            float rowY = y + index * 42.0f;
            UiButton button = new UiButton(x, rowY, panelWidth, buttonHeight, recipe.label().toUpperCase(Locale.ROOT), canCraft);
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
            uiRenderer.text(clampText(hotbar.recipeSummary(recipe), 48), x + 42.0f, rowY + 24.0f, 1.15f, canCraft ? UiColor.MUTED : UiColor.BUTTON_DISABLED);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), x + 8.0f, rowY + 4.0f, 26.0f);
        }
        if (hotbar.recipes().size() > visibleRecipes) {
            uiRenderer.text("More recipes unlock in the list as the system grows.", x, y + visibleRecipes * 42.0f + 10.0f, 1.15f, UiColor.MUTED);
        }
    }

    private void renderWorkbenchPreview(CraftingRecipe recipe, float x, float y) {
        float panelWidth = 316.0f;
        float panelHeight = 232.0f;
        uiRenderer.rect(x - 14.0f, y - 44.0f, panelWidth + 28.0f, panelHeight, new UiColor(0.04f, 0.06f, 0.06f, 0.76f));
        uiRenderer.text("WORKBENCH", x, y - 30.0f, 2.6f, UiColor.WHITE);
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
                drawItemIcon(hotbar.itemKey(ingredient.itemId()), sx + 6.0f, sy + 5.0f, 30.0f);
                uiRenderer.text(String.valueOf(ingredient.count()), sx + 29.0f, sy + 27.0f, 1.05f, UiColor.WHITE);
            }
            float arrowX = x + 164.0f;
            float arrowY = y + 58.0f;
            uiRenderer.rect(arrowX, arrowY + 9.0f, 34.0f, 5.0f, UiColor.MUTED);
            uiRenderer.rect(arrowX + 28.0f, arrowY + 4.0f, 10.0f, 15.0f, UiColor.MUTED);
            float outX = x + 222.0f;
            float outY = y + 48.0f;
            boolean canCraft = hotbar.canCraft(recipe);
            uiRenderer.rect(outX - 4.0f, outY - 4.0f, 58.0f, 58.0f, canCraft ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            drawItemIcon(hotbar.itemKey(recipe.result().itemId()), outX + 4.0f, outY + 4.0f, 42.0f);
            if (recipe.result().count() > 1) {
                uiRenderer.text(String.valueOf(recipe.result().count()), outX + 38.0f, outY + 39.0f, 1.15f, UiColor.WHITE);
            }
            uiRenderer.text(clampText(recipe.label(), 28), x, y + 158.0f, 1.55f, UiColor.WHITE);
            uiRenderer.text(canCraft ? "READY TO CRAFT" : "MISSING INGREDIENTS", x, y + 180.0f, 1.25f, canCraft ? UiColor.ACCENT : UiColor.MUTED);
        }
    }

    private CraftingRecipe previewCraftingRecipe() {
        for (CraftingRecipe recipe : hotbar.recipes()) {
            if (hotbar.canCraft(recipe)) {
                return recipe;
            }
        }
        return hotbar.recipes().isEmpty() ? null : hotbar.recipes().get(0);
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

    private void renderInventoryGridCompact(float x, float y) {
        float slot = 42.0f;
        float gap = 6.0f;
        int columns = 9;
        int rows = 4;
        float gridWidth = columns * slot + (columns - 1) * gap;
        float panelWidth = Math.min(gridWidth + 28.0f, framebufferWidth - 48.0f);
        if (x + panelWidth > framebufferWidth - 24.0f) {
            x = Math.max(24.0f, framebufferWidth * 0.5f - panelWidth * 0.5f);
        }
        uiRenderer.rect(x - 14.0f, y - 42.0f, panelWidth, rows * (slot + gap) + 48.0f, new UiColor(0.04f, 0.06f, 0.06f, 0.74f));
        uiRenderer.text("INVENTORY", x, y - 28.0f, 2.4f, UiColor.WHITE);
        for (int i = 0; i < Math.min(hotbar.inventorySlotCount(), columns * rows); i++) {
            int column = i % columns;
            int row = i / columns;
            float sx = x + column * (slot + gap);
            float sy = y + row * (slot + gap);
            boolean selected = i == hotbar.selectedIndex();
            Hotbar.SlotView slotView = hotbar.slotView(i);
            uiRenderer.rect(sx - 2.0f, sy - 2.0f, slot + 4.0f, slot + 4.0f, selected ? UiColor.ACCENT : new UiColor(0.02f, 0.025f, 0.025f, 0.45f));
            uiRenderer.rect(sx, sy, slot, slot, i < Hotbar.HOTBAR_SLOTS ? UiColor.SLOT_ACTIVE : UiColor.SLOT);
            if (!slotView.isEmpty()) {
                drawItemIcon(slotView.itemKey(), sx + 6.0f, sy + 5.0f, 30.0f);
                if (slotView.count() > 1) {
                    uiRenderer.text(String.valueOf(slotView.count()), sx + 27.0f, sy + 28.0f, 1.05f, UiColor.WHITE);
                }
                if (slotView.hasDurability()) {
                    drawDurabilityBar(sx + 7.0f, sy + 36.0f, 28.0f, slotView.durabilityLeft(), slotView.maxDurability());
                }
            }
        }
    }

    private void renderHud() {
        if ((gameState != GameState.PLAYING && gameState != GameState.CHAT) || world == null || !settings.hudEnabled()) {
            return;
        }
        uiRenderer.rect(framebufferWidth * 0.5f - 5.0f, framebufferHeight * 0.5f - 1.0f, 10.0f, 2.0f, UiColor.WHITE);
        uiRenderer.rect(framebufferWidth * 0.5f - 1.0f, framebufferHeight * 0.5f - 5.0f, 2.0f, 10.0f, UiColor.WHITE);

        float hotbarWidth = Hotbar.HOTBAR_SLOTS * 72.0f + (Hotbar.HOTBAR_SLOTS - 1) * 6.0f;
        float hotbarX = Math.max(20.0f, framebufferWidth * 0.5f - hotbarWidth * 0.5f);
        float hotbarY = framebufferHeight - 78.0f;
        float statsY = hotbarY - 54.0f;
        uiRenderer.rect(hotbarX - 12.0f, statsY - 12.0f, hotbarWidth + 24.0f, 116.0f, new UiColor(0.025f, 0.032f, 0.03f, 0.54f));
        uiRenderer.centeredText(clampText(hotbar.selectedTooltip(), 64), framebufferWidth * 0.5f, statsY - 32.0f, 1.55f, UiColor.WHITE);
        drawIconMeter("heart_full", "heart_empty", playerStats.health(), 20, hotbarX + 10.0f, statsY, 18.0f);
        drawIconMeter("hunger_full", "hunger_empty", playerStats.hunger(), 20, hotbarX + hotbarWidth - 226.0f, statsY, 18.0f);
        drawMeterBar("ENERGY", playerStats.stamina(), 20, hotbarX + hotbarWidth * 0.5f - 132.0f, statsY + 27.0f, UiColor.ENERGY);
        uiRenderer.text("MODE " + gameMode.name(), hotbarX + hotbarWidth - 116.0f, statsY + 29.0f, 1.15f, UiColor.MUTED);
        if (playerStats.breath() < 20) {
            drawIconMeter("air_full", "", playerStats.breath(), 20, hotbarX + hotbarWidth * 0.5f - 110.0f, statsY - 26.0f, 16.0f);
        }
        if (playerStats.armor() > 0) {
            drawIconMeter("armor_full", "armor_full", playerStats.armor(), 20, hotbarX + hotbarWidth * 0.5f - 110.0f, statsY + 51.0f, 16.0f);
        }
        for (int i = 0; i < Hotbar.HOTBAR_SLOTS; i++) {
            float x = hotbarX + i * 78.0f;
            float y = hotbarY;
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
        if (gameState == GameState.PLAYING || gameState == GameState.CHAT) {
            renderHeldItem();
        }
        if (settings.debugOverlayEnabled()) {
            renderDebugOverlay();
        }
    }

    private void renderHeldItem() {
        hotbar.selectedItemKey().ifPresent(itemKey -> {
            float size = Math.max(82.0f, Math.min(128.0f, framebufferHeight * 0.16f));
            float bob = (float) Math.sin(frameTimeSeconds * 5.0) * 3.0f;
            float x = framebufferWidth - size - 54.0f;
            float y = framebufferHeight - size - 26.0f + bob;
            uiRenderer.rect(x + 10.0f, y + 12.0f, size - 14.0f, size - 10.0f, new UiColor(0.02f, 0.02f, 0.018f, 0.30f));
            drawItemIcon(itemKey, x, y, size);
        });
    }

    private void drawMeterBar(String label, int value, int max, float x, float y, UiColor fill) {
        float width = 128.0f;
        float ratio = Math.max(0.0f, Math.min(1.0f, value / (float) max));
        uiRenderer.text(label, x, y + 2.0f, 1.2f, UiColor.MUTED);
        uiRenderer.rect(x + 78.0f, y, width, 14.0f, UiColor.SLOT);
        uiRenderer.rect(x + 80.0f, y + 2.0f, (width - 4.0f) * ratio, 10.0f, fill);
        uiRenderer.text(value + "/" + max, x + 214.0f, y + 2.0f, 1.05f, UiColor.WHITE);
    }

    private void drawIconMeter(String fullKey, String emptyKey, int value, int max, float x, float y, float size) {
        if (sprites == null || spriteRenderer == null) {
            return;
        }
        int slots = 10;
        for (int i = 0; i < slots; i++) {
            boolean filled = value > Math.round(i * (max / (float) slots));
            String key = filled ? fullKey : emptyKey;
            if (key.isBlank()) {
                continue;
            }
            float sx = x + i * (size + 4.0f);
            sprites.hud(key).ifPresent(sprite -> spriteRenderer.sprite(sprite, sx, y, size, size));
        }
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
