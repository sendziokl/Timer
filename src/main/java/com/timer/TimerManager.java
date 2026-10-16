package com.timer;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TimerManager {
    public static final class Entry {
        public final BlockPos pos;
        public final Block block;
        public final long start;

        Entry(BlockPos pos, Block block, long start) {
            this.pos = pos;
            this.block = block;
            this.start = start;
        }

        public double elapsed() {
            return (System.currentTimeMillis() - start) / 1000.0;
        }

        /** Pozostaly czas w sekundach, albo null gdy czas regeneracji jest jeszcze nieznany. */
        public Double remaining() {
            double d = TimerConfig.get().durationOf(block);
            return d < 0 ? null : d - elapsed();
        }
    }

    private static final long MIN_MS = 700; // krotsze zmiany to odrzucony przez serwer "ghost break"
    private static final int MAX_ENTRIES = 256;
    private static final Map<BlockPos, Entry> PENDING = new LinkedHashMap<>();
    private static int tickCounter = 0;

    private TimerManager() {}

    public static Collection<Entry> entries() {
        return PENDING.values();
    }

    public static void clear() {
        PENDING.clear();
    }

    private static boolean shouldTrack(BlockState state) {
        if (state.isAir()) return false;
        if (state.isOf(Blocks.BEDROCK)) return false;
        if (!state.getFluidState().isEmpty()) return false;
        return TimerConfig.get().isTracked(state.getBlock());
    }

    private static void start(BlockPos pos, Block block) {
        if (PENDING.size() >= MAX_ENTRIES) {
            // wyrzuc najstarszy
            BlockPos first = PENDING.keySet().iterator().next();
            PENDING.remove(first);
        }
        PENDING.put(pos, new Entry(pos, block, System.currentTimeMillis()));
    }

    /** Wywolywane przy kazdej aktualizacji bloku od serwera. */
    public static void onBlockChange(BlockPos pos, BlockState old, BlockState now) {
        Entry e = PENDING.get(pos);
        if (e != null) {
            if (now.getBlock() == e.block) {
                long d = System.currentTimeMillis() - e.start;
                PENDING.remove(pos);
                if (d >= MIN_MS) TimerConfig.get().learn(e.block, d / 1000.0);
            }
            return;
        }
        if (old.getBlock() == now.getBlock()) return;
        if (!shouldTrack(old)) return;
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) return;
        int r = TimerConfig.get().radius;
        if (p.squaredDistanceTo(Vec3d.ofCenter(pos)) > (double) r * r) return;
        start(pos, old.getBlock());
    }

    /** Wywolywane gdy gracz sam kopie blok (zanim serwer odpowie). */
    public static void onPlayerBreak(BlockPos pos, BlockState state) {
        if (PENDING.containsKey(pos)) return;
        if (!shouldTrack(state)) return;
        start(pos, state.getBlock());
    }

    public static void tick(MinecraftClient client) {
        if (client.world == null) {
            if (!PENDING.isEmpty()) PENDING.clear();
            return;
        }
        if (++tickCounter % 20 != 0) return;
        PENDING.values().removeIf(e -> {
            double d = TimerConfig.get().durationOf(e.block);
            double limit = d > 0 ? d * 3 + 30 : 600;
            return e.elapsed() > limit;
        });
    }

    public static List<Entry> sorted() {
        List<Entry> list = new ArrayList<>(PENDING.values());
        list.sort((a, b) -> {
            Double ra = a.remaining(), rb = b.remaining();
            if (ra == null && rb == null) return Double.compare(b.elapsed(), a.elapsed());
            if (ra == null) return 1;
            if (rb == null) return -1;
            return Double.compare(ra, rb);
        });
        return list;
    }

    public static String fmt(double seconds) {
        long t = (long) Math.ceil(Math.max(0, seconds));
        return String.format("%d:%02d", t / 60, t % 60);
    }
}
