package com.timer;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/** Ustawienia pozycji (rog ekranu) i rozmiaru HUD z zywym podgladem. Otwierane klawiszem L. */
public class HudSettingsScreen extends Screen {
    private static final String[] CORNERS = {"Lewy górny", "Prawy górny", "Lewy dolny", "Prawy dolny"};
    private final Screen parent;

    public HudSettingsScreen(Screen parent) {
        super(Text.literal("Timer - wygląd HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TimerHud.preview = true;
        TimerConfig cfg = TimerConfig.get();
        int cx = width / 2;
        int cy = height / 2 - 40;

        // 4 rogi (2x2)
        for (int i = 0; i < 4; i++) {
            final int corner = i;
            String label = (cfg.hudCorner == i ? "> " : "") + CORNERS[i] + (cfg.hudCorner == i ? " <" : "");
            addDrawableChild(ButtonWidget.builder(Text.literal(label), b -> {
                        cfg.hudCorner = corner;
                        TimerConfig.save();
                        clearAndInit();
                    })
                    .dimensions(cx - 102 + (i % 2) * 104, cy + (i / 2) * 22, 100, 20).build());
        }

        // rozmiar
        addDrawableChild(ButtonWidget.builder(Text.literal("-"), b -> changeScale(-0.1f))
                .dimensions(cx - 102, cy + 52, 30, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Reset"), b -> {
                    cfg.hudScale = 1.0f;
                    TimerConfig.save();
                })
                .dimensions(cx - 68, cy + 52, 136, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("+"), b -> changeScale(0.1f))
                .dimensions(cx + 72, cy + 52, 30, 20).build());

        // margines
        addDrawableChild(ButtonWidget.builder(Text.literal("Margines -"), b -> changeMargin(-2))
                .dimensions(cx - 102, cy + 76, 100, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Margines +"), b -> changeMargin(2))
                .dimensions(cx + 2, cy + 76, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(cx - 50, cy + 104, 100, 20).build());
    }

    private void changeScale(float d) {
        TimerConfig cfg = TimerConfig.get();
        cfg.hudScale = Math.round(Math.max(0.5f, Math.min(3.0f, cfg.hudScale + d)) * 10f) / 10f;
        TimerConfig.save();
    }

    private void changeMargin(int d) {
        TimerConfig cfg = TimerConfig.get();
        cfg.hudX = Math.max(0, Math.min(200, cfg.hudX + d));
        cfg.hudY = cfg.hudX;
        TimerConfig.save();
    }

    @Override
    protected void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // bez przyciemnienia, zeby podglad HUD byl dobrze widoczny
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int cy = height / 2 - 40;
        TimerConfig cfg = TimerConfig.get();
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal("Wygląd HUD - wybierz róg i rozmiar"), cx, cy - 22, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer,
                Text.literal("Rozmiar: " + Math.round(cfg.hudScale * 100) + "%   Margines: " + cfg.hudX + "px"),
                cx, cy + 42, 0xFFFFFF55);
    }

    @Override
    public void removed() {
        TimerHud.preview = false;
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
