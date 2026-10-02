package net.regions_unexplored.mixin.compat.alexscaves;

import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.level.biome.ACBiomeRarity", remap = false)
public abstract class CaveRarityMixin {
    @Shadow private static double biomeSize;
    @Shadow private static double seperationDistance;

    @Inject(method = "init", at = @At("TAIL"))
    private static void ru$explorationSpacing(CallbackInfo ci) {
        if (AlexsCavesIntegration.ENABLED.get()) {
            // Alex's Caves samples quart coordinates (one quart is four blocks).
            biomeSize = AlexsCavesIntegration.RADIUS.get() * 0.25;
            seperationDistance = biomeSize + AlexsCavesIntegration.SEPARATION.get() * 0.25;
        }
    }
}
