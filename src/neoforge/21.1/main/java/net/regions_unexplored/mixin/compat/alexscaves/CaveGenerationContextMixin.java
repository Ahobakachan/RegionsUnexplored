package net.regions_unexplored.mixin.compat.alexscaves;

import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.util.StaticCache2D;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatusTasks;
import net.minecraft.world.level.chunk.status.ChunkStep;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.regions_unexplored.compat.AlexsCavesBiomeSources;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.concurrent.CompletableFuture;

@Mixin(ChunkStatusTasks.class)
public abstract class CaveGenerationContextMixin {
    @Inject(method = "generateBiomes", at = @At("HEAD"))
    private static void ru$bindWrappedSource(WorldGenContext context, ChunkStep step,
                                             StaticCache2D<GenerationChunkHolder> cache, ChunkAccess chunk,
                                             CallbackInfoReturnable<CompletableFuture<ChunkAccess>> cir) {
        AlexsCavesBiomeSources.configure(context.generator().getBiomeSource(), context.level());
    }
}
