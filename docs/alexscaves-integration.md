# Alex's Caves: natural exploration on Minecraft 1.21.1

This fork includes an optional NeoForge integration with Alex's Caves. Install RU,
Lithostitched 1.7.9, Alex's Caves and Citadel on both client and server. Alex's Caves
already generates its own biomes naturally; maps never create the biomes. RU keeps
that generator, including cave carving, surface materials, structures and mobs.

The compatibility hooks only load on NeoForge. RU also works without Alex's Caves.
Fabric cannot load the NeoForge Alex's Caves build.

## Exploration profile

All six biomes can appear in newly generated Overworld chunks. Land caves accept
ordinary inland climates, and Abyssal Chasm accepts ocean climates. Upstream depth,
dimension, distance and per-biome disable settings remain effective. The X distance
conversion is corrected and distance calculations cannot overflow near the world border.

The default average radius is **224 blocks**, with a **1600-block separation
parameter** (1824-block Voronoi cell spacing). Suitable climate, depth and cave type
further reduce actual occurrence. This aims at discoveries a few thousand blocks
apart, comparable to looking for rare deep underground landmarks. It is not a fixed
Warden spawn chance: Wardens are summoned by shriekers, and their availability also
depends on terrain. Individual cave types are rarer than the six types combined.

Existing chunks keep their terrain. Create a new world or explore new chunks.

## Maps

By default, cave maps cannot be crafted, used, regenerated or used to launch search
workers. Old filled cave maps are treated as unfilled. Maps and their display sprites
are hidden from creative tabs and creative search. Cartographers and wandering
traders no longer offer new underground cabin maps, and the cabin-map loot modifier
does not generate maps or search for structures. Item registrations stay in place so
existing inventories can still load. Previously acquired vanilla cabin maps remain
vanilla map items; this integration does not erase inventory items or existing trades.

Tablets, codices, research, the guide book, structures and other gameplay remain available.

## Configuration

`config/regions_unexplored-alexscaves.toml` is created automatically:

```toml
enabled = true
disable_maps = true
broaden_climate = true
radius = 224.0
separation = 1600
```

Use the same configuration on client and server. Restart after changing generation
settings. `enabled = false` restores Alex's Caves behaviour; `disable_maps = false`
restores map behaviour independently; `broaden_climate = false` honours the original
continentalness ranges. Other Alex's Caves configuration files are not rewritten.

## Build and follow progress

The **Alex's Caves integration** workflow runs on pushes, pull requests and manual
dispatch. It builds RU and pinned upstream NeoForge 1.21.1 source checkouts:

- [Citadel 2.7.1](https://github.com/AlexModGuy/Citadel/tree/badebf6505f56650fd770887329d82b1a88f3f79).
- [Alex's Caves 2.0.2, 1.21.1 branch](https://github.com/AlexModGuy/AlexsCaves/tree/0ab3c214b72c846c2c1ea713a37e2b4b3a00a9f5).

The only upstream build adaptation points Alex's Caves at the actual Citadel 2.7.1
jar instead of its hardcoded local 2.7.0 filename. These are development branch
builds, not the released Forge 1.20.1 jars. Do not mix Minecraft versions or loaders.

After building, the workflow boots dedicated servers with and without Alex's Caves.
It checks map restrictions, real biome-source samples across three seeds, all six cave
types, coexistence with RU and Deep Dark, above-ground and dimension isolation,
different layouts across seeds, and generated chunks at discovered cave locations.
Its sampling guard rejects no caves or more than 12% cave coverage at Y=-32; the
reported fraction is a sample statistic, not a guarantee for every world.

Download **minecraft-1.21.1-neoforge-mods** only after both server checks pass. Add
Lithostitched 1.7.9 separately. The artifact also includes upstream source for review
and licence compliance. Assertion reports and server logs are available in the
`checks-with-caves` and `checks-ru-only` artifacts and the Actions run summary.

Local checks use Java 21, `bash gradlew neoforge211RemapJar -x
downloadNeoforge211Assets`, the two pinned upstream checkouts under `_upstream/`,
`python3 ci/prepare_upstream.py _upstream/AlexsCaves`, their Gradle builds, and
`python3 ci/stage_mods.py build artifacts/mods`. Stage either test mode into
`ci/integration/mods` and run `gradle -p ci/integration runServer
-PcheckMode=with-caves` using Gradle 8.10.2; verify with `ci/check_report.py`.
