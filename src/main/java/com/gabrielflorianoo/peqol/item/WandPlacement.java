package com.gabrielflorianoo.peqol.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * Shared (client preview + server placement) logic of the Alchemical Construction Wand:
 * extends the clicked face over every connected block of the same state on that plane.
 */
public final class WandPlacement {

    private WandPlacement() {
    }

    /**
     * @return true if blocks of this state can be duplicated by the wand.
     */
    public static boolean canCopy(BlockState state) {
        if (state.isAir() || state.hasBlockEntity()) {
            return false;
        }
        if (state.hasProperty(BlockStateProperties.DOUBLE_BLOCK_HALF) || state.hasProperty(BlockStateProperties.BED_PART)
                || state.hasProperty(BlockStateProperties.CHEST_TYPE)) {
            return false;
        }
        Item item = state.getBlock().asItem();
        return item != Items.AIR;
    }

    /**
     * @return the positions (in front of {@code face}) where copies of the clicked block would be placed, closest first.
     */
    public static List<BlockPos> findTargets(Level level, BlockPos origin, Direction face, int max) {
        List<BlockPos> result = new ArrayList<>();
        BlockState state = level.getBlockState(origin);
        if (max <= 0 || !canCopy(state)) {
            return result;
        }
        Direction[] spread = perpendicular(face);
        Set<BlockPos> visited = new HashSet<>();
        Queue<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);
        int maxRadius = (int) Math.ceil(Math.sqrt(max)) + 1;
        while (!queue.isEmpty() && result.size() < max) {
            BlockPos current = queue.poll();
            if (!level.getBlockState(current).equals(state)) {
                continue;
            }
            BlockPos target = current.relative(face);
            if (!isFree(level, target, state)) {
                continue;
            }
            result.add(target);
            for (Direction direction : spread) {
                BlockPos next = current.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) > maxRadius || Math.abs(next.getY() - origin.getY()) > maxRadius
                        || Math.abs(next.getZ() - origin.getZ()) > maxRadius) {
                    continue;
                }
                if (visited.add(next)) {
                    queue.add(next);
                }
            }
        }
        return result;
    }

    private static boolean isFree(Level level, BlockPos target, BlockState state) {
        if (!level.isInWorldBounds(target) || !level.isLoaded(target)) {
            return false;
        }
        //Air, water, grass, snow layers... anything a player could place into
        if (!level.getBlockState(target).canBeReplaced()) {
            return false;
        }
        return level.isUnobstructed(state, target, CollisionContext.empty());
    }

    private static Direction[] perpendicular(Direction face) {
        List<Direction> result = new ArrayList<>(4);
        for (Direction direction : Direction.values()) {
            if (direction.getAxis() != face.getAxis()) {
                result.add(direction);
            }
        }
        return result.toArray(new Direction[0]);
    }
}
