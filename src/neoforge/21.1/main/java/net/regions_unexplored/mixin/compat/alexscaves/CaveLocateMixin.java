package net.regions_unexplored.mixin.compat.alexscaves;

import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.neoforged.fml.ModList;
import net.regions_unexplored.compat.AlexsCavesIntegration;
import net.regions_unexplored.compat.AlexsCavesBiomeSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Predicate;

/** Keep vanilla /locate cave results in the playable world and use the queried world's seed. */
@Mixin(ServerLevel.class)
public abstract class CaveLocateMixin {
    @Inject(method = "findClosestBiome3d", at = @At("HEAD"), cancellable = true)
    private void ru$locateAccessibleCave(Predicate<Holder<Biome>> predicate, BlockPos origin, int radius,
                                         int horizontalStep, int verticalStep,
                                         CallbackInfoReturnable<Pair<BlockPos, Holder<Biome>>> cir) {
        if (!ModList.get().isLoaded("alexscaves") || !AlexsCavesIntegration.ENABLED.get()) return;
        ServerLevel level = (ServerLevel) (Object) this;
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        AlexsCavesBiomeSources.configure(source, level);
        if (!(AlexsCavesBiomeSources.root(source) instanceof MultiNoiseBiomeSource) || source.possibleBiomes().stream().noneMatch(holder ->
                holder.unwrapKey().orElseThrow().identifier().getNamespace().equals("alexscaves") && predicate.test(holder))) return;
        var sampler = level.getChunkSource().randomState().sampler();
        int startY = Mth.clamp(origin.getY(), level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - 1);
        int[] heights = Mth.outFromOrigin(startY, level.getMinBuildHeight() + 1,
                level.getMaxBuildHeight() - 1, verticalStep).toArray();
        for (var offset : BlockPos.spiralAround(BlockPos.ZERO, Math.floorDiv(radius, horizontalStep), Direction.EAST, Direction.SOUTH)) {
            int x = origin.getX() + offset.getX() * horizontalStep;
            int z = origin.getZ() + offset.getZ() * horizontalStep;
            if (!level.getWorldBorder().isWithinBounds(x, z)) continue;
            for (int y : heights) {
                var biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), sampler);
                if (predicate.test(biome)) {
                    cir.setReturnValue(Pair.of(new BlockPos(x, y, z), biome));
                    return;
                }
            }
        }
        cir.setReturnValue(null);
    }
}
