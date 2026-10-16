package com.timer;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.block.Block;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import org.lwjgl.glfw.GLFW;

public class TimerMod implements ClientModInitializer {
    public static final String MOD_ID = "timer";

    private static KeyBinding openKey;
    private static KeyBinding addKey;
    private static KeyBinding hudKey;

    @Override
    public void onInitializeClient() {
        TimerConfig.load();

        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.timer.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, "category.timer"));
        addKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.timer.add", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, "category.timer"));

        hudKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.timer.hud", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_L, "category.timer"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                client.setScreen(new TimerScreen(client.currentScreen));
            }
            while (hudKey.wasPressed()) {
                client.setScreen(new HudSettingsScreen(client.currentScreen));
            }
            while (addKey.wasPressed()) {
                toggleLookedAt(client);
            }
            TimerManager.tick(client);
        });

        HudRenderCallback.EVENT.register(TimerHud::render);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> TimerManager.clear());
    }

    private static void toggleLookedAt(MinecraftClient client) {
        if (client.world == null || client.player == null) return;
        HitResult hit = client.crosshairTarget;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            Block b = client.world.getBlockState(bhr.getBlockPos()).getBlock();
            boolean added = TimerConfig.get().toggle(b);
            client.player.sendMessage(Text.literal("[Timer] " + (added ? "Śledzę: " : "Usunięto: ")
                    + b.getName().getString()), true);
        } else {
            client.player.sendMessage(Text.literal("[Timer] Patrz na blok."), true);
        }
    }
}
