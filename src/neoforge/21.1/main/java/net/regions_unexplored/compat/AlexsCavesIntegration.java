package net.regions_unexplored.compat;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;

/** Optional exploration profile for Alex's Caves on NeoForge 1.21.1. */
public final class AlexsCavesIntegration {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue DISABLE_MAPS;
    public static final ModConfigSpec.BooleanValue BROADEN_CLIMATE;
    public static final ModConfigSpec.DoubleValue RADIUS;
    public static final ModConfigSpec.IntValue SEPARATION;
    public static final float[] LAND = {-0.11F, 1.0F};
    public static final float[] OCEAN = {-1.0F, -0.19F};

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        ENABLED = builder.comment("Use the natural exploration profile when Alex's Caves is installed.")
                .define("enabled", true);
        DISABLE_MAPS = builder.comment("Disable cave maps and underground cabin locator maps, including old maps.")
                .define("disable_maps", true);
        BROADEN_CLIMATE = builder.comment("Allow land caves below ordinary land biomes and Abyssal Chasm below oceans.",
                        "Depth, biome disable flags and dimension restrictions from Alex's Caves still apply.")
                .define("broaden_climate", true);
        RADIUS = builder.comment("Average cave biome radius in blocks. Requires a server restart; affects new chunks.")
                .defineInRange("radius", 224.0, 64.0, 1024.0);
        SEPARATION = builder.comment("Gap parameter in blocks. Cell spacing is radius + separation.",
                        "Defaults aim for rare discoveries a few thousand blocks apart, shared across six cave types.")
                .defineInRange("separation", 1600, 256, 16384);
        SPEC = builder.build();
    }

    private AlexsCavesIntegration() {}

    public static void register(ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, SPEC, "regions_unexplored-alexscaves.toml");
        container.getEventBus().addListener(AlexsCavesIntegration::hideMaps);
    }

    public static boolean mapsDisabled() {
        return ENABLED.get() && DISABLE_MAPS.get();
    }

    private static void hideMaps(BuildCreativeModeTabContentsEvent event) {
        if (mapsDisabled()) {
            java.util.function.Predicate<net.minecraft.world.item.ItemStack> map = stack -> {
                var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                return id.getNamespace().equals("alexscaves") && id.getPath().startsWith("cave_map");
            };
            event.getParentEntries().removeIf(map);
            event.getSearchEntries().removeIf(map);
        }
    }
}
