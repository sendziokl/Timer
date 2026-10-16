package com.timer.mixin;

import com.timer.TimerManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Wykrywa blok, ktory TY wlasnie wykopales (klient przewiduje zniknięcie zanim odpowie serwer). */
@Mixin(ClientPlayerInteractionManager.class)
public abstract class InteractionManagerMixin {
    @Inject(method = "breakBlock", at = @At("HEAD"))
    private void timer$onBreak(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.world == null) return;
        TimerManager.onPlayerBreak(pos.toImmutable(), mc.world.getBlockState(pos));
    }
}
