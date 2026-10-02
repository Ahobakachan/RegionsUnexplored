package net.regions_unexplored.mixin.compat.alexscaves;

import net.minecraft.world.item.ItemStack;
import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.recipe.RecipeCaveMap", remap = false)
public abstract class CaveMapRecipeMixin {
    @Inject(method = {"matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            "canCraftInDimensions"}, at = @At("HEAD"), cancellable = true)
    private void ru$noMapCrafting(CallbackInfoReturnable<Boolean> cir) {
        if (AlexsCavesIntegration.mapsDisabled()) cir.setReturnValue(false);
    }

    @Inject(method = {"assemble(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            "getResultItem", "getDisplayResultFor"}, at = @At("HEAD"), cancellable = true)
    private void ru$noMapResult(CallbackInfoReturnable<ItemStack> cir) {
        if (AlexsCavesIntegration.mapsDisabled()) cir.setReturnValue(ItemStack.EMPTY);
    }
}
