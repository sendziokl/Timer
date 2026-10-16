package com.timer.mixin;

import com.timer.TimerManager;
import net.minecraft.block.BlockState;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Przechwytuje aktualizacje blokow wysylane przez serwer (zniknięcie / odrodzenie bloku). */
@Mixin(ClientWorld.class)
public abstract class ClientWorldMixin {
    @Inject(method = "handleBlockUpdate", at = @At("HEAD"))
    private void timer$onBlockUpdate(BlockPos pos, BlockState state, int flags, CallbackInfo ci) {
        ClientWorld self = (ClientWorld) (Object) this;
        BlockState old = self.getBlockState(pos);
        TimerManager.onBlockChange(pos.toImmutable(), old, state);
    }
}
