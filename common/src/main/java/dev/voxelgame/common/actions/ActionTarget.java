package dev.voxelgame.common.actions;

public sealed interface ActionTarget permits ActionTarget.None, ActionTarget.Block, ActionTarget.Entity, ActionTarget.Direction {
    ActionTargetType type();

    record None() implements ActionTarget {
        @Override
        public ActionTargetType type() {
            return ActionTargetType.NONE;
        }
    }

    record Block(int x, int y, int z, int faceX, int faceY, int faceZ) implements ActionTarget {
        @Override
        public ActionTargetType type() {
            return ActionTargetType.BLOCK;
        }
    }

    record Entity(long entityId) implements ActionTarget {
        public Entity {
            if (entityId < 0L) {
                throw new IllegalArgumentException("Entity target id must be >= 0");
            }
        }

        @Override
        public ActionTargetType type() {
            return ActionTargetType.ENTITY;
        }
    }

    record Direction(
            double originX,
            double originY,
            double originZ,
            double directionX,
            double directionY,
            double directionZ
    ) implements ActionTarget {
        public Direction {
            requireFinite("originX", originX);
            requireFinite("originY", originY);
            requireFinite("originZ", originZ);
            requireFinite("directionX", directionX);
            requireFinite("directionY", directionY);
            requireFinite("directionZ", directionZ);
            double lengthSquared = directionX * directionX + directionY * directionY + directionZ * directionZ;
            if (lengthSquared <= 0.000001) {
                throw new IllegalArgumentException("Direction target must have a non-zero direction");
            }
        }

        @Override
        public ActionTargetType type() {
            return ActionTargetType.DIRECTION;
        }
    }

    private static void requireFinite(String name, double value) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite");
        }
    }
}
