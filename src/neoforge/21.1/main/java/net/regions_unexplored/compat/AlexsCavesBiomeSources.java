package net.regions_unexplored.compat;

import dev.worldgen.lithostitched.impl.worldgen.biomeinjector.internal.InjectorBiomeSource;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.neoforged.fml.ModList;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.stream.Collectors;

/** Bind Alex's native delegate to the same world as Lithostitched's outer biome source. */
public final class AlexsCavesBiomeSources {
    private static final Map<ServerLevel, Map<ResourceKey<Biome>, Holder<Biome>>> REGISTRIES = new WeakHashMap<>();
    private static final Map<BiomeSource, Map<ResourceKey<Biome>, Holder<Biome>>> CONFIGURED = new WeakHashMap<>();

    private AlexsCavesBiomeSources() {}

    public static BiomeSource root(BiomeSource source) {
        return source instanceof InjectorBiomeSource injector ? injector.rootDelegate() : source;
    }

    public static void configure(BiomeSource source, ServerLevel level) {
        if (!ModList.get().isLoaded("alexscaves") || !AlexsCavesIntegration.ENABLED.get()) return;
        BiomeSource delegate = root(source);
        if (!Bridge.CONTEXT.isInstance(delegate)) return;
        try {
            synchronized (REGISTRIES) {
                Map<ResourceKey<Biome>, Holder<Biome>> biomes = REGISTRIES.computeIfAbsent(level, key -> {
                    Map<ResourceKey<Biome>, Holder<Biome>> result = new HashMap<>();
                    key.registryAccess().registryOrThrow(Registries.BIOME).holders()
                            .forEach(holder -> result.put(holder.key(), holder));
                    return Map.copyOf(result);
                });
                if (CONFIGURED.get(source) != biomes) {
                    Bridge.MAP.invoke(delegate, biomes);
                    Bridge.MAP.invoke(source, biomes);
                    if (level.dimension().equals(Level.OVERWORLD)) {
                        Set<Holder<Biome>> caves = biomes.entrySet().stream()
                                .filter(entry -> entry.getKey().identifier().getNamespace().equals("alexscaves"))
                                .map(Map.Entry::getValue).collect(Collectors.toUnmodifiableSet());
                        Bridge.EXPAND.invoke(delegate, caves);
                        Bridge.EXPAND.invoke(source, caves);
                    }
                    CONFIGURED.put(source, biomes);
                }
            }
            Bridge.SEED.invoke(delegate, level.getSeed());
            Bridge.DIMENSION.invoke(delegate, level.dimension());
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported Alex's Caves biome-source interface", error);
        }
    }

    // Resolve optional mod classes once, and only if the mod is installed. Reflection is never in the sample loop.
    private static final class Bridge {
        private static final Class<?> CONTEXT;
        private static final Method SEED;
        private static final Method DIMENSION;
        private static final Method MAP;
        private static final Method EXPAND;

        static {
            try {
                String prefix = "com.github.alexmodguy.alexscaves.server.level.biome.";
                CONTEXT = Class.forName(prefix + "MultiNoiseBiomeSourceAccessor");
                SEED = CONTEXT.getMethod("setLastSampledSeed", long.class);
                DIMENSION = CONTEXT.getMethod("setLastSampledDimension", ResourceKey.class);
                Class<?> biomes = Class.forName(prefix + "BiomeSourceAccessor");
                MAP = biomes.getMethod("setResourceKeyMap", Map.class);
                EXPAND = biomes.getMethod("expandBiomesWith", Set.class);
            } catch (ReflectiveOperationException error) {
                throw new ExceptionInInitializerError(error);
            }
        }
    }
}
