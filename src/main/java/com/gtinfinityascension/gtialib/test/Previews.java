package com.gtinfinityascension.gtialib.test;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MultiblockMachineDefinition;
import com.gregtechceu.gtceu.api.machine.multiblock.MultiblockControllerMachine;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** Builds a multiblock in a GameTest from its own JEI/EMI preview, the same shape a player sees. */
public final class Previews {

    private Previews() {}

    /** A structure placed by {@link #place}; {@link #close()} clears every block it set. */
    public record Placed(GameTestHelper helper, @Nullable MultiblockControllerMachine controller,
                         List<BlockPos> blocks)
            implements AutoCloseable {

        /** Runs the structure check now and returns whether the machine formed. */
        public boolean forms() {
            return controller != null && controller.checkPatternWithLock();
        }

        /** Why the last structure check failed, for test messages. */
        public String error() {
            if (controller == null) return "no controller in the preview";
            var error = controller.getMultiblockState().error;
            return error == null ? "no error reported" : error.getErrorInfo().getString() + " at " + error.getPos();
        }

        @Override
        public void close() {
            for (BlockPos pos : blocks) helper.setBlock(pos, Blocks.AIR);
        }
    }

    /** Size of the first preview shape as {x, y, z}, or null when the machine has no preview. */
    public static int @Nullable [] size(MultiblockMachineDefinition def) {
        var shapes = def.getMatchingShapes();
        if (shapes.isEmpty()) return null;
        var blocks = shapes.get(0).getBlocks();
        return new int[] { blocks.length, blocks[0].length, blocks[0][0].length };
    }

    /** Places the first preview shape with its lowest corner at {@code origin} (test-relative). */
    public static Placed place(GameTestHelper helper, MultiblockMachineDefinition def, BlockPos origin) {
        var shape = def.getMatchingShapes().get(0).getBlocks();
        List<BlockPos> placed = new ArrayList<>();
        MultiblockControllerMachine controller = null;
        for (int x = 0; x < shape.length; x++)
            for (int y = 0; y < shape[x].length; y++)
                for (int z = 0; z < shape[x][y].length; z++) {
                    var info = shape[x][y][z];
                    if (info == null || info.getBlockState().isAir()) continue;
                    BlockPos pos = origin.offset(x, y, z);
                    helper.setBlock(pos, info.getBlockState());
                    placed.add(pos);
                    if (info.getBlockState().is(def.getBlock()) &&
                            helper.getBlockEntity(pos) instanceof IMachineBlockEntity be &&
                            be.getMetaMachine() instanceof MultiblockControllerMachine m)
                        controller = m;
                }
        return new Placed(helper, controller, placed);
    }
}
