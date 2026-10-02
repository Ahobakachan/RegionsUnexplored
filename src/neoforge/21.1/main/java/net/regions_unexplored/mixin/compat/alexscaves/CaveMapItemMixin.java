package net.regions_unexplored.mixin.compat.alexscaves;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.regions_unexplored.compat.AlexsCavesIntegration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "com.github.alexmodguy.alexscaves.server.item.CaveMapItem", remap = false)
public abstract class CaveMapItemMixin {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void ru$disableMapUse(Level level, Player player, InteractionHand hand,
                                 CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        if (AlexsCavesIntegration.mapsDisabled())
            cir.setReturnValue(InteractionResultHolder.fail(player.getItemInHand(hand)));
    }

    @Inject(method = "inventoryTick", at = @At("HEAD"), cancellable = true)
    private void ru$noMapRegeneration(CallbackInfo ci) {
        if (AlexsCavesIntegration.mapsDisabled()) ci.cancel();
    }

    @Inject(method = "isFilled", at = @At("HEAD"), cancellable = true)
    private static void ru$hideOldMap(CallbackInfoReturnable<Boolean> cir) {
        if (AlexsCavesIntegration.mapsDisabled()) cir.setReturnValue(false);
    }
}
