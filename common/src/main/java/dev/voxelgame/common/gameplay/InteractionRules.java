package dev.voxelgame.common.gameplay;

import dev.voxelgame.common.block.BlockType;
import dev.voxelgame.common.block.Blocks;
import dev.voxelgame.common.block.ToolType;
import dev.voxelgame.common.entity.EntityBounds;
import dev.voxelgame.common.entity.EntitySnapshot;
import dev.voxelgame.common.item.ItemStack;
import dev.voxelgame.common.item.ItemType;
import dev.voxelgame.common.physics.PlayerBounds;
import dev.voxelgame.common.physics.BlockCollisionShapes;
import dev.voxelgame.common.registry.Registry;

import java.util.Optional;

public final class InteractionRules {
    public static final int HOTBAR_SLOTS = 9;
    public static final double BLOCK_REACH = 7.0;

    private InteractionRules() {
    }

    public static boolean isHotbarSlot(int slot, int inventorySize) {
        return slot >= 0 && slot < Math.min(HOTBAR_SLOTS, inventorySize);
    }

    public static boolean canReachBlock(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        double dx = blockX + 0.5 - eyeX;
        double dy = blockY + 0.5 - eyeY;
        double dz = blockZ + 0.5 - eyeZ;
        return dx * dx + dy * dy + dz * dz <= BLOCK_REACH * BLOCK_REACH;
    }

    public static boolean hasBlockLineOfSight(
            double eyeX,
            double eyeY,
            double eyeZ,
            int blockX,
            int blockY,
            int blockZ,
            BlockOcclusionQuery occlusionQuery
    ) {
        if (occlusionQuery == null
                || !Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)) {
            return false;
        }
        double targetX = blockX + 0.5;
        double targetY = blockY + 0.5;
        double targetZ = blockZ + 0.5;
        int x = floor(eyeX);
        int y = floor(eyeY);
        int z = floor(eyeZ);
        if (x == blockX && y == blockY && z == blockZ) {
            return true;
        }

        double dx = targetX - eyeX;
        double dy = targetY - eyeY;
        double dz = targetZ - eyeZ;
        int stepX = sign(dx);
        int stepY = sign(dy);
        int stepZ = sign(dz);
        double tMaxX = firstBoundaryT(eyeX, dx, stepX);
        double tMaxY = firstBoundaryT(eyeY, dy, stepY);
        double tMaxZ = firstBoundaryT(eyeZ, dz, stepZ);
        double tDeltaX = deltaT(dx);
        double tDeltaY = deltaT(dy);
        double tDeltaZ = deltaT(dz);

        for (int i = 0; i < 512; i++) {
            if (tMaxX <= tMaxY && tMaxX <= tMaxZ) {
                x += stepX;
                tMaxX += tDeltaX;
            } else if (tMaxY <= tMaxZ) {
                y += stepY;
                tMaxY += tDeltaY;
            } else {
                z += stepZ;
                tMaxZ += tDeltaZ;
            }
            if (x == blockX && y == blockY && z == blockZ) {
                return true;
            }
            if (occlusionQuery.occludes(x, y, z)) {
                return false;
            }
        }
        return false;
    }

    public static boolean canReachEntity(double eyeX, double eyeY, double eyeZ, EntitySnapshot snapshot, double range) {
        if (snapshot == null || !Double.isFinite(eyeX) || !Double.isFinite(eyeY) || !Double.isFinite(eyeZ)
                || !Double.isFinite(range) || range < 0.0) {
            return false;
        }
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        double baseY = EntityBounds.baseY(snapshot);
        double closestX = clamp(eyeX, bounds.minX(snapshot.x()), bounds.maxX(snapshot.x()));
        double closestY = clamp(eyeY, bounds.minY(baseY), bounds.maxY(baseY));
        double closestZ = clamp(eyeZ, bounds.minZ(snapshot.z()), bounds.maxZ(snapshot.z()));
        double dx = closestX - eyeX;
        double dy = closestY - eyeY;
        double dz = closestZ - eyeZ;
        return dx * dx + dy * dy + dz * dz <= range * range;
    }

    public static boolean placementIntersectsPlayer(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ) {
        return PlayerBounds.DEFAULT.intersectsBlock(eyeX, eyeY, eyeZ, blockX, blockY, blockZ);
    }

    public static boolean placementIntersectsPlayer(double eyeX, double eyeY, double eyeZ, int blockX, int blockY, int blockZ, short blockId) {
        return BlockCollisionShapes.placementShape(blockId)
                .intersectsPlayer(PlayerBounds.DEFAULT, eyeX, eyeY, eyeZ, blockX, blockY, blockZ);
    }

    public static boolean placementIntersectsEntity(EntitySnapshot snapshot, int blockX, int blockY, int blockZ) {
        return snapshot != null && EntityBounds.intersectsBlock(snapshot, blockX, blockY, blockZ);
    }

    public static boolean placementIntersectsEntity(EntitySnapshot snapshot, int blockX, int blockY, int blockZ, short blockId) {
        if (snapshot == null) {
            return false;
        }
        EntityBounds bounds = EntityBounds.forType(snapshot.typeKey());
        return BlockCollisionShapes.placementShape(blockId)
                .intersectsEntity(bounds, snapshot.x(), EntityBounds.baseY(snapshot), snapshot.z(), blockX, blockY, blockZ);
    }

    public static float breakMultiplier(ItemStack selectedStack, Registry<ItemType> items, BlockType target) {
        if (selectedStack.isEmpty()) {
            return target.preferredTool() == ToolType.NONE ? 1.0f : 0.65f;
        }
        ItemType item = items.requireById(selectedStack.itemId());
        if (item.toolType() == target.preferredTool() && item.isTool()) {
            float base = switch (item.toolType()) {
                case PICKAXE -> 3.0f;
                case SHOVEL -> 2.6f;
                case AXE -> 2.8f;
                case KNIFE -> 3.4f;
                case NONE -> 1.0f;
            };
            return base * item.toolSpeed();
        }
        if (target.preferredTool() == ToolType.NONE) {
            return item.toolType() == ToolType.KNIFE ? 1.4f : 1.0f;
        }
        return item.isTool() ? 0.85f : 0.55f;
    }

    public static boolean canHarvest(ItemStack selectedStack, Registry<ItemType> items, BlockType target) {
        int requiredLevel = requiredToolLevel(target);
        if (requiredLevel <= 0) {
            return true;
        }
        if (selectedStack.isEmpty()) {
            return false;
        }
        ItemType item = items.requireById(selectedStack.itemId());
        return item.isTool()
                && item.toolType() == target.preferredTool()
                && item.toolLevel() >= requiredLevel;
    }

    public static int toolLevel(ItemType item) {
        return item.toolLevel();
    }

    public static int requiredToolLevel(BlockType target) {
        return target.requiredToolLevel();
    }

    public static int toolDamage(ItemStack selectedStack, Registry<ItemType> items, BlockType target) {
        if (selectedStack.isEmpty()) {
            return 0;
        }
        ItemType item = items.requireById(selectedStack.itemId());
        if (!item.isTool()) {
            return 0;
        }
        return item.toolType() == target.preferredTool() ? 1 : 2;
    }

    public static int dropCount(BlockType target, float breakMultiplier) {
        if (target.preferredTool() == ToolType.KNIFE) {
            return breakMultiplier > 5.0f ? 3 : breakMultiplier > 2.0f ? 2 : 1;
        }
        if (target.preferredTool() == ToolType.PICKAXE && isOreLike(target.id()) && breakMultiplier > 5.0f) {
            return 2;
        }
        if (target.preferredTool() == ToolType.AXE && isWoodLike(target.id()) && breakMultiplier > 4.5f) {
            return 2;
        }
        return 1;
    }

    public static double breakDelaySeconds(BlockType target, float breakMultiplier) {
        if (target.hardness() <= 0.0f) {
            return 0.08;
        }
        double delay = target.hardness() * 0.34 / Math.max(0.35f, breakMultiplier);
        return Math.max(0.08, Math.min(0.85, delay));
    }

    public static Optional<BlockInteraction> blockInteraction(BlockType target) {
        return switch (target.id()) {
            case Blocks.WILD_GRASS -> Optional.of(new BlockInteraction("voxel:dry_grass", 1, 0.25, "Gathered dry grass"));
            case Blocks.BERRY_BUSH -> Optional.of(new BlockInteraction("voxel:berries", 2, 0.35, "Harvested berries"));
            case Blocks.HERB_PLANTER -> Optional.of(new BlockInteraction("voxel:wild_herbs", 1, 0.35, "Picked wild herbs"));
            case Blocks.REEDS -> Optional.of(new BlockInteraction("voxel:reed_bundle", 1, 0.35, "Cut reed bundle"));
            case Blocks.GLOW_MUSHROOM -> Optional.of(new BlockInteraction("voxel:glow_mushroom_cap", 1, 0.40, "Gathered glow mushroom cap"));
            case Blocks.SPORE_BLOSSOM -> Optional.of(new BlockInteraction("voxel:spore_blossom", 1, 0.45, "Picked spore blossom"));
            case Blocks.PINE_LOG -> Optional.of(new BlockInteraction("voxel:resin", 1, 0.45, "Collected resin"));
            case Blocks.TREE_STUMP -> Optional.of(new BlockInteraction("voxel:bark_strip", 2, 0.45, "Peeled bark strips"));
            default -> Optional.empty();
        };
    }

    private static boolean isOreLike(short blockId) {
        return blockId == Blocks.COAL_ORE
                || blockId == Blocks.COPPER_ORE
                || blockId == Blocks.IRON_ORE
                || blockId == Blocks.GLOW_CRYSTAL_NODE;
    }

    private static boolean isWoodLike(short blockId) {
        return blockId == Blocks.SKYROOT_LOG
                || blockId == Blocks.PINE_LOG
                || blockId == Blocks.TREE_STUMP
                || blockId == Blocks.SKYROOT_PLANKS;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int floor(double value) {
        return (int) Math.floor(value);
    }

    private static int sign(double value) {
        if (value > 0.0) {
            return 1;
        }
        if (value < 0.0) {
            return -1;
        }
        return 0;
    }

    private static double firstBoundaryT(double origin, double delta, int step) {
        if (step == 0) {
            return Double.POSITIVE_INFINITY;
        }
        double boundary = step > 0 ? Math.floor(origin) + 1.0 : Math.floor(origin);
        return (boundary - origin) / delta;
    }

    private static double deltaT(double delta) {
        return delta == 0.0 ? Double.POSITIVE_INFINITY : Math.abs(1.0 / delta);
    }

    @FunctionalInterface
    public interface BlockOcclusionQuery {
        boolean occludes(int x, int y, int z);
    }

    public record BlockInteraction(String itemKey, int count, double cooldownSeconds, String message) {
        public BlockInteraction {
            if (itemKey == null || itemKey.isBlank()) {
                throw new IllegalArgumentException("Interaction item key is required");
            }
            if (count < 1) {
                throw new IllegalArgumentException("Interaction count must be positive");
            }
            if (cooldownSeconds < 0.0) {
                throw new IllegalArgumentException("Interaction cooldown must be >= 0");
            }
        }
    }
}
