package com.timer;

import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class TimerScreen extends Screen {
    private static final int ROWS = 7;

    private final Screen parent;
    private TextFieldWidget idField;
    private TextFieldWidget secField;
    private String status = "";
    private int offset = 0;

    public TimerScreen(Screen parent) {
        super(Text.literal("Timer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        TimerConfig cfg = TimerConfig.get();
        int cx = width / 2;
        int top = 30;

        idField = new TextFieldWidget(textRenderer, cx - 150, top, 150, 20, Text.literal("id"));
        idField.setMaxLength(100);
        idField.setPlaceholder(Text.literal("np. diamond_ore"));
        addDrawableChild(idField);

        secField = new TextFieldWidget(textRenderer, cx + 4, top, 50, 20, Text.literal("s"));
        secField.setMaxLength(6);
        secField.setPlaceholder(Text.literal("auto"));
        addDrawableChild(secField);

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj"), b -> addFromField())
                .dimensions(cx + 58, top, 92, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Dodaj blok z ręki"), b -> addHeld())
                .dimensions(cx - 150, top + 24, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("Auto na serwerach: " + (cfg.autoOnServers ? "TAK" : "NIE")),
                        b -> {
                            cfg.autoOnServers = !cfg.autoOnServers;
                            TimerConfig.save();
                            clearAndInit();
                        })
                .dimensions(cx + 4, top + 24, 146, 20).build());

        addDrawableChild(ButtonWidget.builder(
                        Text.literal("HUD: " + (cfg.hudEnabled ? "TAK" : "NIE")),
                        b -> {
                            cfg.hudEnabled = !cfg.hudEnabled;
                            TimerConfig.save();
                            clearAndInit();
                        })
                .dimensions(cx - 150, top + 48, 70, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Wygląd HUD"),
                        b -> client.setScreen(new HudSettingsScreen(this)))
                .dimensions(cx - 76, top + 48, 76, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Wyczyść aktywne timery"), b -> {
                    TimerManager.clear();
                    status = "Wyczyszczono.";
                })
                .dimensions(cx + 4, top + 48, 146, 20).build());

        // lista sledzonych blokow
        List<String> keys = new ArrayList<>(cfg.tracked.keySet());
        if (offset > Math.max(0, keys.size() - ROWS)) offset = Math.max(0, keys.size() - ROWS);
        int listY = top + 84;
        for (int i = 0; i < ROWS && offset + i < keys.size(); i++) {
            String key = keys.get(offset + i);
            addDrawableChild(ButtonWidget.builder(Text.literal("X"), b -> {
                        cfg.tracked.remove(key);
                        TimerConfig.save();
                        clearAndInit();
                    })
                    .dimensions(cx + 126, listY + i * 22, 24, 20).build());
        }

        addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
                    offset = Math.max(0, offset - ROWS);
                    clearAndInit();
                })
                .dimensions(cx - 150, height - 28, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
                    if (offset + ROWS < cfg.tracked.size()) offset += ROWS;
                    clearAndInit();
                })
                .dimensions(cx - 122, height - 28, 24, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Gotowe"), b -> close())
                .dimensions(cx + 50, height - 28, 100, 20).build());
    }

    private void addFromField() {
        String text = idField.getText().trim().toLowerCase();
        Identifier id = Identifier.tryParse(text.contains(":") ? text : "minecraft:" + text);
        if (id == null || !Registries.BLOCK.containsId(id) || Registries.BLOCK.get(id).getDefaultState().isAir()) {
            status = "Nie ma takiego bloku: " + text;
            return;
        }
        double sec = 0;
        String s = secField.getText().trim().replace(',', '.');
        if (!s.isEmpty()) {
            try {
                sec = Math.max(0, Double.parseDouble(s));
            } catch (NumberFormatException e) {
                status = "Zły czas (podaj sekundy lub zostaw puste = auto)";
                return;
            }
        }
        TimerConfig.get().tracked.put(id.toString(), sec);
        TimerConfig.save();
        status = "Dodano: " + id;
        idField.setText("");
        secField.setText("");
        clearAndInit();
    }

    private void addHeld() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.player.getMainHandStack().getItem() instanceof BlockItem bi) {
            TimerConfig.get().tracked.putIfAbsent(TimerConfig.idOf(bi.getBlock()), 0.0);
            TimerConfig.save();
            status = "Dodano: " + TimerConfig.idOf(bi.getBlock());
            clearAndInit();
        } else {
            status = "Weź do ręki jakiś blok.";
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double hAmount, double vAmount) {
        int size = TimerConfig.get().tracked.size();
        int newOff = offset - (int) Math.signum(vAmount);
        newOff = Math.max(0, Math.min(newOff, Math.max(0, size - ROWS)));
        if (newOff != offset) {
            offset = newOff;
            clearAndInit();
        }
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;

        ctx.drawItem(new ItemStack(net.minecraft.item.Items.CLOCK), cx - 40, 8);
        ctx.drawText(textRenderer, "Timer", cx - 20, 12, 0xFFFFFFFF, true);
        ctx.drawText(textRenderer, "sek.", cx + 4, 20, 0xFFAAAAAA, false);

        TimerConfig cfg = TimerConfig.get();
        List<String> keys = new ArrayList<>(cfg.tracked.keySet());
        int listY = 30 + 84;
        if (keys.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer,
                    Text.literal("Brak bloków. Dodaj blok albo wejdź na serwer z trybem auto."),
                    cx, listY + 8, 0xFFAAAAAA);
        }
        for (int i = 0; i < ROWS && offset + i < keys.size(); i++) {
            String key = keys.get(offset + i);
            Identifier id = Identifier.tryParse(key);
            Block b = id != null ? Registries.BLOCK.get(id) : null;
            int y = listY + i * 22;
            if (b != null) ctx.drawItem(new ItemStack(b.asItem()), cx - 150, y + 2);

            double manual = cfg.tracked.get(key);
            Double learned = cfg.learned.get(key);
            String info = manual > 0 ? "ręcznie " + manual + "s"
                    : learned != null ? "wykryto " + learned + "s" : "uczy się...";
            String name = b != null ? b.getName().getString() : key;
            ctx.drawText(textRenderer, name + " - " + info, cx - 130, y + 6, 0xFFFFFFFF, true);
        }

        ctx.drawText(textRenderer, "Auto-serwery: " + String.join(", ", cfg.autoServers)
                + "  (edytuj w config/timer.json)", cx - 150, height - 44, 0xFF888888, false);
        if (!status.isEmpty()) {
            ctx.drawText(textRenderer, status, cx - 150, 30 + 72, 0xFFFFFF55, true);
        }
    }

    @Override
    public void close() {
        MinecraftClient.getInstance().setScreen(parent);
    }
}
