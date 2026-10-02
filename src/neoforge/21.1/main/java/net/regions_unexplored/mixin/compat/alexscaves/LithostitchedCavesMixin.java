package net.regions_unexplored.mixin.compat.alexscaves;

import net.minecraft.core.Holder;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.regions_unexplored.compat.AlexsCavesIntegration;
import com.llamalad7.mixinextras.sugar.Local;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource", remap = false)
public abstract class LithostitchedCavesMixin {
    @Inject(method = "getNoiseBiome", at = @At(value = "INVOKE_ASSIGN",
            target = "Lnet/minecraft/world/level/biome/BiomeResolver;getNoiseBiome(IIILnet/minecraft/world/level/biome/Climate$Sampler;)Lnet/minecraft/core/Holder;"),
            cancellable = true)
    private void ru$preserveNativeCaves(int x, int y, int z, Climate.Sampler sampler,
                                        CallbackInfoReturnable<Holder<Biome>> cir,
                                        @Local(ordinal = 0) Holder<Biome> nativeBiome) {
        if (!net.neoforged.fml.ModList.get().isLoaded("alexscaves") || !AlexsCavesIntegration.ENABLED.get()) return;
        if (nativeBiome != null && nativeBiome.unwrapKey().orElseThrow().identifier().getNamespace().equals("alexscaves")) {
            // RU's alternate layout otherwise replaces a cave returned by the native delegate.
            cir.setReturnValue(nativeBiome);
        }
    }
}
