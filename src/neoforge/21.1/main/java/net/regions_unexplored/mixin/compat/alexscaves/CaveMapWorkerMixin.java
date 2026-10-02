package net.regions_unexplored.mixin.compat.alexscaves;

import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.level.storage.ACWorldData", remap = false)
public abstract class CaveMapWorkerMixin {
    @Inject(method = "fillOutCaveMap", at = @At("HEAD"), cancellable = true)
    private void ru$noMapWorkers(CallbackInfo ci) {
        if (AlexsCavesIntegration.mapsDisabled()) ci.cancel();
    }
}
