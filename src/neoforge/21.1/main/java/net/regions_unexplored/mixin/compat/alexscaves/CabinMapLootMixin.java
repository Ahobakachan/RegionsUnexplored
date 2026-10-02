package net.regions_unexplored.mixin.compat.alexscaves;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.misc.CabinMapLootModifier", remap = false)
public abstract class CabinMapLootMixin {
    @Inject(method = "doApply", at = @At("HEAD"), cancellable = true)
    private void ru$noCabinMapLoot(ObjectArrayList<ItemStack> loot, LootContext context,
                                  CallbackInfoReturnable<ObjectArrayList<ItemStack>> cir) {
        if (AlexsCavesIntegration.mapsDisabled()) cir.setReturnValue(loot);
    }
}
