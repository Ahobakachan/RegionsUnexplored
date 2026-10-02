package net.regions_unexplored.mixin.compat.alexscaves;

import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.config.BiomeGenerationNoiseCondition", remap = false)
public abstract class CaveClimateMixin {
    @Shadow @Final private float[] continentalness;
    @Shadow @Final private int alexscavesRarityOffset;

    @Redirect(method = "test", at = @At(value = "FIELD", opcode = Opcodes.GETFIELD,
            target = "Lcom/github/alexmodguy/alexscaves/server/config/BiomeGenerationNoiseCondition;continentalness:[F"))
    private float[] ru$explorableClimate(@Coerce Object condition) {
        if (AlexsCavesIntegration.ENABLED.get() && AlexsCavesIntegration.BROADEN_CLIMATE.get()) {
            return alexscavesRarityOffset == 3 ? AlexsCavesIntegration.OCEAN : AlexsCavesIntegration.LAND;
        }
        return continentalness;
    }

    @Inject(method = "isFarEnoughFromSpawn", at = @At("HEAD"), cancellable = true)
    private static void ru$distanceInBlocks(int quartX, int quartZ, double distance,
                                           CallbackInfoReturnable<Boolean> cir) {
        if (AlexsCavesIntegration.ENABLED.get()) {
            // Fix the upstream fromSection/toBlock mismatch and integer overflow far from origin.
            double x = quartX * 4.0;
            double z = quartZ * 4.0;
            cir.setReturnValue(x * x + z * z >= distance * distance);
        }
    }
}
