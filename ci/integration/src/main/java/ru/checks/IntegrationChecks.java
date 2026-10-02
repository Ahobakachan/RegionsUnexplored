package ru.checks;

import com.google.gson.GsonBuilder;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.QuartPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.PacketSendListener;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ClientInformation;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.MultiNoiseBiomeSource;
import net.minecraft.world.level.levelgen.NoiseBasedChunkGenerator;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Runs against packaged jars in an ordinary Overworld, then shuts the CI server down. */
@Mod("ru_integration_checks")
public final class IntegrationChecks {
    private static final String AC = "com.github.alexmodguy.alexscaves.";
    private static final List<String> CAVES = List.of("magnetic_caves", "primordial_caves", "toxic_caves",
            "abyssal_chasm", "forlorn_hollows", "candy_cavity");
    private final Map<String, Object> report = new LinkedHashMap<>();
    private BlockPos magneticExample;

    public IntegrationChecks() {
        NeoForge.EVENT_BUS.addListener(this::started);
    }

    private void started(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        server.execute(() -> {
            try {
                boolean withCaves = System.getProperty("ru.check.mode").equals("with-caves");
                require(ModList.get().isLoaded("alexscaves") == withCaves, "Wrong test mod set");
                report.put("mode", withCaves ? "with-caves" : "ru-only");
                report.put("minecraft", "1.21.1");
                if (withCaves) {
                    checkMaps(server.overworld());
                    checkSpacing();
                    checkGeneration(server);
                    checkLocateAndTeleport(server);
                    checkDimensions(server);
                } else {
                    require(server.registryAccess().registryOrThrow(Registries.BIOME).registryKeySet().stream()
                            .anyMatch(key -> key.location().getNamespace().equals("regions_unexplored")),
                            "RU biomes missing without Alex's Caves");
                }
                report.put("success", true);
            } catch (Throwable error) {
                report.put("success", false);
                report.put("error", error.toString());
                error.printStackTrace();
            } finally {
                try {
                    Files.createDirectories(Path.of("../build"));
                    Files.writeString(Path.of("../build/compat-report.json"),
                            new GsonBuilder().setPrettyPrinting().create().toJson(report));
                } catch (Exception error) {
                    throw new RuntimeException(error);
                } finally {
                    server.halt(false);
                }
            }
        });
    }

    private void checkMaps(ServerLevel level) throws Exception {
        var recipeClass = Class.forName(AC + "server.recipe.RecipeCaveMap");
        var recipe = (CraftingRecipe) recipeClass.getConstructor(CraftingBookCategory.class)
                .newInstance(CraftingBookCategory.MISC);
        var codex = BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexscaves:cave_codex"));
        List<ItemStack> ingredients = new ArrayList<>();
        for (int i = 0; i < 9; i++) ingredients.add(new ItemStack(i == 4 ? codex : Items.PAPER));
        var input = CraftingInput.of(3, 3, ingredients);
        require(!recipe.matches(input, level), "Map recipe still matches");
        require(recipe.assemble(input, level.registryAccess()).isEmpty(), "Map recipe still crafts");
        require(recipe.getResultItem(level.registryAccess()).isEmpty(), "Map recipe still advertises a result");
        require(!recipe.canCraftInDimensions(3, 3), "Map recipe still enabled");

        var map = new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse("alexscaves:cave_map")));
        var player = FakePlayerFactory.getMinecraft(level);
        player.setItemInHand(InteractionHand.MAIN_HAND, map);
        require(map.getItem().use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL,
                "Map use starts a search");
        CompoundTag oldMap = new CompoundTag();
        oldMap.putBoolean("Filled", true);
        oldMap.putUUID("MapUUID", UUID.randomUUID());
        map.set(DataComponents.CUSTOM_DATA, CustomData.of(oldMap));
        require(!(boolean) map.getItem().getClass().getMethod("isFilled", ItemStack.class).invoke(null, map),
                "An old map still reveals its target");
        map.getItem().inventoryTick(map, level, player, 0, true);
        var worldDataClass = Class.forName(AC + "server.level.storage.ACWorldData");
        Object data = worldDataClass.getMethod("get", Level.class).invoke(null, level);
        worldDataClass.getMethod("fillOutCaveMap", UUID.class, ItemStack.class, ServerLevel.class,
                BlockPos.class, net.minecraft.world.entity.player.Player.class)
                .invoke(data, UUID.randomUUID(), map, level, BlockPos.ZERO, player);
        require(!(boolean) worldDataClass.getMethod("isCaveMapTicking").invoke(data), "A map worker was scheduled");

        var events = Class.forName(AC + "server.event.CommonEvents").getConstructor().newInstance();
        var trades = new Int2ObjectOpenHashMap<List<VillagerTrades.ItemListing>>();
        for (int i = 1; i <= 5; i++) trades.put(i, new ArrayList<>());
        events.getClass().getMethod("onVillagerTradeSetup", VillagerTradesEvent.class)
                .invoke(events, new VillagerTradesEvent(trades, VillagerProfession.CARTOGRAPHER, level.registryAccess()));
        require(trades.values().stream().allMatch(List::isEmpty), "Cartographers still sell cabin maps");
        var generic = new ArrayList<VillagerTrades.ItemListing>();
        events.getClass().getMethod("onWanderingTradeSetup", WandererTradesEvent.class)
                .invoke(events, new WandererTradesEvent(generic, new ArrayList<>(), level.registryAccess()));
        require(generic.isEmpty(), "Wandering traders still sell cabin maps");

        var lootClass = Class.forName(AC + "server.misc.CabinMapLootModifier");
        var constructor = lootClass.getDeclaredConstructor(LootItemCondition[].class);
        constructor.setAccessible(true);
        var modifier = constructor.newInstance((Object) new LootItemCondition[0]);
        var apply = lootClass.getDeclaredMethod("doApply", ObjectArrayList.class, LootContext.class);
        apply.setAccessible(true);
        var loot = new ObjectArrayList<ItemStack>();
        loot.add(new ItemStack(Items.DIAMOND));
        require(apply.invoke(modifier, loot, null) == loot && loot.size() == 1,
                "Cabin map loot modifier changes loot or starts a structure search");

        CreativeModeTabs.tryRebuildTabContents(level.enabledFeatures(), true, level.registryAccess());
        BuiltInRegistries.CREATIVE_MODE_TAB.forEach(tab -> {
            require(tab.getDisplayItems().stream().noneMatch(IntegrationChecks::isMap), "Map remains in a creative tab");
            require(tab.getSearchTabDisplayItems().stream().noneMatch(IntegrationChecks::isMap), "Map remains in creative search");
        });
        report.put("maps", "crafting, use, old maps, workers, trades, loot and creative tabs disabled");
    }

    private static boolean isMap(ItemStack stack) {
        var id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return id.getNamespace().equals("alexscaves") && id.getPath().startsWith("cave_map");
    }

    private void checkSpacing() throws Exception {
        Class<?> rarity = Class.forName(AC + "server.level.biome.ACBiomeRarity");
        require(readDouble(rarity, "biomeSize") * 4 == 224, "Wrong cave radius");
        require(readDouble(rarity, "seperationDistance") * 4 == 1824, "Wrong cell spacing");
        var condition = Class.forName(AC + "server.config.BiomeGenerationNoiseCondition");
        var distance = condition.getDeclaredMethod("isFarEnoughFromSpawn", int.class, int.class, double.class);
        distance.setAccessible(true);
        require(!(boolean) distance.invoke(null, 100, 0, 500.0), "X distance uses section coordinates");
        require(!(boolean) distance.invoke(null, 0, 100, 500.0), "Z distance uses section coordinates");
        require((boolean) distance.invoke(null, 9_000_000, 0, 500.0), "Distance overflows near world border");
        report.put("radius_blocks", 224);
        report.put("cell_spacing_blocks", 1824);
    }

    private void checkGeneration(MinecraftServer server) throws Exception {
        ServerLevel level = server.overworld();
        var generator = (NoiseBasedChunkGenerator) level.getChunkSource().getGenerator();
        var source = generator.getBiomeSource();
        Object nativeSource = source.getClass().getMethod("rootDelegate").invoke(source);
        Class<?> accessor = Class.forName(AC + "server.level.biome.MultiNoiseBiomeSourceAccessor");
        var setSeed = accessor.getMethod("setLastSampledSeed", long.class);
        var setDimension = accessor.getMethod("setLastSampledDimension", net.minecraft.resources.ResourceKey.class);
        setDimension.invoke(nativeSource, Level.OVERWORLD);
        List<Object> seeds = new ArrayList<>();
        Map<String, Integer> total = new LinkedHashMap<>();
        Map<String, BlockPos> examples = new LinkedHashMap<>();
        long lastHash = 0;
        for (long seed : new long[]{level.getSeed()}) {
            setSeed.invoke(nativeSource, seed);
            var random = level.getChunkSource().randomState();
            Map<String, Integer> counts = new LinkedHashMap<>();
            long hash = 1;
            int samples = 0;
            double nearestCave = Double.MAX_VALUE;
            for (int x = -12288; x <= 12288; x += 128) {
                for (int z = -12288; z <= 12288; z += 128) {
                    var holder = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(-32),
                            QuartPos.fromBlock(z), random.sampler());
                    String id = holder.unwrapKey().orElseThrow().location().toString();
                    counts.merge(id, 1, Integer::sum);
                    total.merge(id, 1, Integer::sum);
                    hash = hash * 31 + id.hashCode();
                    samples++;
                    if (seed == level.getSeed() && id.startsWith("alexscaves:"))
                        examples.putIfAbsent(id, new BlockPos(x, -32, z));
                    if (id.startsWith("alexscaves:")) {
                        nearestCave = Math.min(nearestCave, Math.hypot(x, z));
                        require(!source.getNoiseBiome(QuartPos.fromBlock(x), 80, QuartPos.fromBlock(z), random.sampler())
                                .unwrapKey().orElseThrow().location().getNamespace().equals("alexscaves"),
                                "A cave biome leaks above ground");
                    }
                }
            }
            require(hash != lastHash, "Different world seeds produce the same biome layout");
            lastHash = hash;
            int caveCount = counts.entrySet().stream().filter(e -> e.getKey().startsWith("alexscaves:"))
                    .mapToInt(Map.Entry::getValue).sum();
            double fraction = (double) caveCount / samples;
            require(fraction > 0.001 && fraction < 0.12, "Caves absent or too common: " + fraction);
            require(nearestCave <= 4096, "Caves only appear far from spawn: nearest sampled cave " + nearestCave);
            seeds.add(Map.of("seed", seed, "samples", samples, "cave_fraction", fraction,
                    "nearest_cave_blocks", nearestCave, "layout_hash", Long.toString(hash), "counts", counts));
        }
        setSeed.invoke(nativeSource, level.getSeed());
        for (String cave : CAVES) require(total.getOrDefault("alexscaves:" + cave, 0) > 0, "Missing cave: " + cave);
        magneticExample = examples.get("alexscaves:magnetic_caves");
        require(total.getOrDefault("minecraft:deep_dark", 0) > 0, "Deep Dark has disappeared");
        require(total.keySet().stream().anyMatch(id -> id.startsWith("regions_unexplored:")), "RU caves have disappeared");
        for (var example : examples.entrySet()) {
            BlockPos pos = example.getValue();
            var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            require(chunk
                            .getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2)
                            .unwrapKey().orElseThrow().location().toString().equals(example.getKey()),
                    "Generated chunk disagrees with biome source: " + example);
            require(chunk.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                            pos.getX() & 15, pos.getZ() & 15) > level.getMinBuildHeight() + 16,
                    "Located cave chunk has no terrain: " + example);
            int caveBlocks = 0;
            for (int x = 0; x < 16; x++) for (int z = 0; z < 16; z++) {
                for (int y = level.getMinBuildHeight(); y < 64; y++) {
                    var state = chunk.getBlockState(new BlockPos((pos.getX() & ~15) + x, y, (pos.getZ() & ~15) + z));
                    if (BuiltInRegistries.BLOCK.getKey(state.getBlock()).getNamespace().equals("alexscaves")) caveBlocks++;
                }
            }
            require(caveBlocks > 0, "Cave biome has no Alex's Caves blocks/features: " + example);
        }
        report.put("generation", seeds);
        report.put("generated_cave_chunks", examples.keySet());
    }

    private void checkLocateAndTeleport(MinecraftServer server) {
        ServerLevel level = server.overworld();
        var border = level.getWorldBorder();
        double originalSize = border.getSize();
        double originalX = border.getCenterX();
        double originalZ = border.getCenterZ();
        List<Object> locations = new ArrayList<>();
        ServerPlayer player = teleportProbe(level);
        try {
            border.setCenter(0, 0);
            border.setSize(8192);
            for (String name : CAVES) {
                var id = ResourceLocation.parse("alexscaves:" + name);
                // These are precisely the search radius and steps used by vanilla /locate biome.
                var found = level.findClosestBiome3d(holder -> holder.unwrapKey().orElseThrow().location().equals(id),
                        new BlockPos(0, 64, 0), 6400, 32, 64);
                if (found == null) {
                    locations.add(Map.of("biome", name, "inside_8192_border", false));
                    // Rare individual types need not occur inside every border. Seed 0 is our fixed regression case.
                    require(level.getSeed() != 0 || !name.equals("magnetic_caves"),
                            "Magnetic Caves missing from the seed-0 locate regression case");
                    continue;
                }
                BlockPos pos = found.getFirst();
                require(border.isWithinBounds(pos), "Locate returned a position outside world border: " + pos);
                require(!level.isOutsideBuildHeight(pos), "Locate returned an out-of-bounds height: " + pos);
                var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
                require(chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2)
                                .unwrapKey().orElseThrow().location().equals(id), "Locate and generated terrain disagree: " + name);
                player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
                require(player.blockPosition().equals(pos), "Teleport did not reach locate coordinates: " + pos);
                require(border.isWithinBounds(player.blockPosition()) && !level.isOutsideBuildHeight(player.blockPosition()),
                        "Teleport ended outside playable world");
                locations.add(Map.of("biome", name, "position", List.of(pos.getX(), pos.getY(), pos.getZ()),
                        "inside_8192_border", true, "generated_biome_verified", true));
            }
            border.setSize(1024);
            var anyCave = level.findClosestBiome3d(holder -> holder.unwrapKey().orElseThrow().location()
                    .getNamespace().equals("alexscaves"), new BlockPos(0, 64, 0), 6400, 32, 64);
            require(anyCave == null || border.isWithinBounds(anyCave.getFirst()), "Locate escapes a small world border");
            report.put("locate_and_teleport", locations);
            report.put("small_world_border", "An absent cave returns no result instead of an unreachable location");

            // Positive locate/teleport coverage for Magnetic Caves on every seed, including an off-centre border.
            // Start at a genuinely generated cave, using the same radius/steps as vanilla /locate biome.
            border.setCenter(magneticExample.getX(), magneticExample.getZ());
            var magnetic = ResourceLocation.parse("alexscaves:magnetic_caves");
            var found = level.findClosestBiome3d(holder -> holder.unwrapKey().orElseThrow().location().equals(magnetic),
                    magneticExample, 6400, 32, 64);
            require(found != null, "Locate cannot find a known generated Magnetic Cave");
            BlockPos pos = found.getFirst();
            require(border.isWithinBounds(pos) && !level.isOutsideBuildHeight(pos), "Locate escapes an off-centre border");
            var chunk = level.getChunk(pos.getX() >> 4, pos.getZ() >> 4);
            require(chunk.getNoiseBiome(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2)
                    .unwrapKey().orElseThrow().location().equals(magnetic), "Magnetic locate disagrees with generated terrain");
            player.teleportTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
            require(player.blockPosition().equals(pos), "Magnetic teleport did not reach locate coordinates: " + pos);
            require(border.isWithinBounds(player.blockPosition()) && !level.isOutsideBuildHeight(player.blockPosition()),
                    "Magnetic teleport escapes an off-centre border");
            report.put("magnetic_off_centre_border", Map.of("position", List.of(pos.getX(), pos.getY(), pos.getZ()),
                    "border_diameter", 1024, "generated_biome_verified", true, "teleport_verified", true));
        } finally {
            border.setCenter(originalX, originalZ);
            border.setSize(originalSize);
        }
    }

    private static ServerPlayer teleportProbe(ServerLevel level) {
        var profile = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "RU_TeleportCheck");
        var player = new ServerPlayer(level.getServer(), level, profile, ClientInformation.createDefault());
        // FakePlayer's network handler overrides teleport with a no-op. Use the ordinary server teleport
        // implementation and suppress only outbound packets because CI has no connected graphical client.
        player.connection = new ServerGamePacketListenerImpl(level.getServer(), new Connection(PacketFlow.SERVERBOUND),
                player, CommonListenerCookie.createInitial(profile, false)) {
            @Override public void send(Packet<?> packet) {}
            @Override public void send(Packet<?> packet, PacketSendListener listener) {}
        };
        return player;
    }

    private void checkDimensions(MinecraftServer server) throws Exception {
        for (var dimension : List.of(Level.NETHER, Level.END)) {
            ServerLevel level = server.getLevel(dimension);
            var source = level.getChunkSource().getGenerator().getBiomeSource();
            Object nativeSource = source;
            if (source.getClass().getName().contains("InjectorBiomeSource"))
                nativeSource = source.getClass().getMethod("rootDelegate").invoke(source);
            var sampler = level.getChunkSource().randomState().sampler();
            if (nativeSource instanceof MultiNoiseBiomeSource) {
                Class<?> accessor = Class.forName(AC + "server.level.biome.MultiNoiseBiomeSourceAccessor");
                accessor.getMethod("setLastSampledSeed", long.class).invoke(nativeSource, level.getSeed());
                accessor.getMethod("setLastSampledDimension", net.minecraft.resources.ResourceKey.class).invoke(nativeSource, dimension);
            }
            for (int x = -2048; x <= 2048; x += 128) {
                for (int z = -2048; z <= 2048; z += 128) {
                    require(!source.getNoiseBiome(x / 4, -8, z / 4, sampler).unwrapKey().orElseThrow()
                            .location().getNamespace().equals("alexscaves"), "Cave generated in " + dimension.location());
                }
            }
        }
        report.put("dimensions", "No Alex's Caves in Nether or End");
    }

    private static double readDouble(Class<?> type, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getDouble(null);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
