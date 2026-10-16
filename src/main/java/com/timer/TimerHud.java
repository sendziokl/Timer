package com.timer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.ArrayList;
import java.util.List;

public final class TimerHud {
    /** Ustawiane przez ekran ustawien HUD - pokazuje przykladowe wiersze do podgladu. */
    public static boolean preview = false;

    private record Row(ItemStack icon, String line1, int color, String line2) {}

    private TimerHud() {}

    public static void render(DrawContext ctx, RenderTickCounter tickCounter) {
        TimerConfig cfg = TimerConfig.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;
        if (!preview && (!cfg.hudEnabled || mc.options.hudHidden)) return;

        boolean auto = cfg.autoActive();
        List<Row> rows = new ArrayList<>();
        int hidden = 0;

        if (preview) {
            rows.add(new Row(new ItemStack(Items.DIAMOND_ORE), "Diamond Ore  0:08", 0xFFFFFF55, "120 64 -33"));
            rows.add(new Row(new ItemStack(Items.IRON_ORE), "Iron Ore  1:25", 0xFFFFFFFF, "118 63 -30"));
            rows.add(new Row(new ItemStack(Items.COAL_ORE), "Coal Ore  uczę się +0:12", 0xFFAAAAAA, "121 65 -31"));
        } else {
            List<TimerManager.Entry> list = TimerManager.sorted();
            if (list.isEmpty() && !auto && cfg.tracked.isEmpty()) return;
            int shown = 0;
            for (TimerManager.Entry e : list) {
                if (shown++ >= cfg.maxRows) {
                    hidden = list.size() - cfg.maxRows;
                    break;
                }
                Double rem = e.remaining();
                String time;
                int color;
                if (rem == null) {
                    time = "uczę się +" + TimerManager.fmt(e.elapsed());
                    color = 0xFFAAAAAA;
                } else if (rem <= 0) {
                    time = "teraz!";
                    color = 0xFF55FF55;
                } else {
                    time = TimerManager.fmt(rem);
                    color = rem <= 10 ? 0xFFFFFF55 : 0xFFFFFFFF;
                }
                rows.add(new Row(new ItemStack(e.block.asItem()),
                        e.block.getName().getString() + "  " + time, color,
                        e.pos.getX() + " " + e.pos.getY() + " " + e.pos.getZ()));
            }
        }

        TextRenderer tr = mc.textRenderer;
        String header = "Timer" + (auto ? " (auto)" : "");
        String more = hidden > 0 ? "+" + hidden + " więcej..." : null;

        // wymiary w "jednostkach HUD" (przed skalowaniem)
        int w = 20 + tr.getWidth(header);
        for (Row r : rows) w = Math.max(w, 20 + Math.max(tr.getWidth(r.line1), tr.getWidth(r.line2)));
        if (more != null) w = Math.max(w, 20 + tr.getWidth(more));
        int h = 20 + rows.size() * 20 + (more != null ? 12 : 0);

        float scale = Math.max(0.5f, Math.min(3.0f, cfg.hudScale));
        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        float pw = w * scale, ph = h * scale;
        boolean right = cfg.hudCorner == 1 || cfg.hudCorner == 3;
        boolean bottom = cfg.hudCorner == 2 || cfg.hudCorner == 3;
        float x = right ? sw - pw - cfg.hudX : cfg.hudX;
        float y = bottom ? sh - ph - cfg.hudY : cfg.hudY;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 0);
        ctx.getMatrices().scale(scale, scale, 1f);

        ctx.drawItem(new ItemStack(Items.CLOCK), 0, 0);
        ctx.drawText(tr, header, 20, 4, 0xFFFFFFFF, true);
        int yy = 20;
        for (Row r : rows) {
            ctx.drawItem(r.icon, 0, yy);
            ctx.drawText(tr, r.line1, 20, yy, r.color, true);
            ctx.drawText(tr, r.line2, 20, yy + 9, 0xFF888888, true);
            yy += 20;
        }
        if (more != null) ctx.drawText(tr, more, 20, yy, 0xFFAAAAAA, true);

        ctx.getMatrices().pop();
    }
}
