package com.gtinfinityascension.gtialib.perf;

import com.gregtechceu.gtceu.api.machine.IMachineBlockEntity;
import com.gregtechceu.gtceu.api.machine.MachineDefinition;
import com.gregtechceu.gtceu.api.machine.WorkableTieredMachine;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;

import com.gtinfinityascension.gtialib.GTIALib;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;

import java.util.ArrayList;
import java.util.List;

/**
 * {@code /gtialib bench grid <machine> <count>} places that many machines in a force-loaded grid, and
 * {@code /gtialib bench mspt <ticks>} logs the average server tick time over the next ticks as
 * {@code BENCH mspt=<ms>}. Used by the modpack's perf_bench.py to compare optimizations; needs permission level 2.
 */
public final class BenchCommand {

    private static final int ROW = 32;
    private static long tickStart = -1;
    private static int remaining;
    private static int measured;
    private static long totalNanos;
    private static long worstNanos;
    /** Machines kept running by {@code grid ... <item>}: refilled with power and input, emptied of output. */
    private static final List<WorkableTieredMachine> WORKING = new ArrayList<>();
    private static ItemStack workingInput = ItemStack.EMPTY;

    private BenchCommand() {}

    public static void register(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(GTIALib.MOD_ID).requires(s -> s.hasPermission(2))
                .then(Commands.literal("bench")
                        .then(Commands.literal("grid")
                                .then(Commands.argument("machine", ResourceLocationArgument.id())
                                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 100_000))
                                                .executes(ctx -> grid(ctx, ItemStack.EMPTY))
                                                .then(Commands
                                                        .argument("input", ItemArgument.item(event.getBuildContext()))
                                                        .executes(ctx -> grid(ctx, ItemArgument.getItem(ctx, "input")
                                                                .createItemStack(64, false)))))))
                        .then(Commands.literal("mspt")
                                .then(Commands.argument("ticks", IntegerArgumentType.integer(20, 72_000))
                                        .executes(BenchCommand::mspt)))));
    }

    private static int grid(CommandContext<CommandSourceStack> ctx,
                            ItemStack input) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ResourceLocation id = ResourceLocationArgument.getId(ctx, "machine");
        int count = IntegerArgumentType.getInteger(ctx, "count");
        MachineDefinition def = GTRegistries.MACHINES.get(id);
        if (def == null) {
            ctx.getSource().sendFailure(Component.literal("unknown machine " + id));
            return 0;
        }
        ServerLevel level = ctx.getSource().getLevel();
        BlockPos origin = new BlockPos(0, 200, 0);
        for (int i = 0; i < count; i++) {
            // Every other block, so each machine has free faces like in a real factory.
            BlockPos pos = origin.offset((i % ROW) * 2, (i / (ROW * ROW)) * 2, ((i / ROW) % ROW) * 2);
            ChunkPos chunk = new ChunkPos(pos);
            level.setChunkForced(chunk.x, chunk.z, true);
            level.setBlockAndUpdate(pos, def.getBlock().defaultBlockState());
            if (!input.isEmpty() && level.getBlockEntity(pos) instanceof IMachineBlockEntity be &&
                    be.getMetaMachine() instanceof WorkableTieredMachine machine)
                WORKING.add(machine);
        }
        workingInput = input;
        GTIALib.LOGGER.info("BENCH grid {} x{}", id, count);
        ctx.getSource().sendSuccess(() -> Component.literal("placed " + count + " " + id), false);
        return count;
    }

    private static int mspt(CommandContext<CommandSourceStack> ctx) {
        remaining = IntegerArgumentType.getInteger(ctx, "ticks");
        tickStart = -1;
        measured = 0;
        totalNanos = 0;
        worstNanos = 0;
        return 1;
    }

    private static void keepWorking() {
        for (WorkableTieredMachine machine : WORKING) {
            machine.energyContainer.setEnergyStored(machine.energyContainer.getEnergyCapacity());
            var in = machine.importItems.storage;
            if (in.getStackInSlot(0).getCount() < 16) in.setStackInSlot(0, workingInput.copy());
            var out = machine.exportItems.storage;
            for (int i = 0; i < out.getSlots(); i++) out.setStackInSlot(i, ItemStack.EMPTY);
        }
    }

    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END && !WORKING.isEmpty()) {
            if (remaining > 0 && tickStart >= 0) {
                // Measured before the refill, so the bench's own bookkeeping is not counted.
                long took = System.nanoTime() - tickStart;
                record(took);
                tickStart = -1;
            }
            keepWorking();
            return;
        }
        if (remaining <= 0) return;
        if (event.phase == TickEvent.Phase.START) {
            tickStart = System.nanoTime();
            return;
        }
        // EN: The command runs mid-tick, so the first END has no matching START; skip it.
        // PT: O comando roda no meio de um tick, então o primeiro END não tem START; ele é ignorado.
        // ZH: 命令在 tick 中途执行，第一个 END 没有对应的 START，予以跳过。
        if (tickStart < 0) return;
        record(System.nanoTime() - tickStart);
    }

    private static void record(long took) {
        totalNanos += took;
        worstNanos = Math.max(worstNanos, took);
        measured++;
        if (--remaining == 0) {
            GTIALib.LOGGER.info(String.format(java.util.Locale.ROOT, "BENCH mspt=%.3f worst=%.3f ticks=%d",
                    totalNanos / 1e6 / measured,
                    worstNanos / 1e6, measured));
        }
    }
}
