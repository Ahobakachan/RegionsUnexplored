package net.regions_unexplored.block.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.regions_unexplored.registry.RUItems;

import java.util.function.Supplier;

public class MapleSappyLogBlock extends RotatedPillarBlock {
    private final Supplier<Block> saplessBlock;

    public MapleSappyLogBlock(Supplier<Block> saplessBlock, Properties properties) {
        super(properties);
        this.saplessBlock = saplessBlock;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.is(Items.GLASS_BOTTLE)) {
            stack.shrink(1);
            ItemStack sap = new ItemStack(RUItems.MAPLE_SAP_BOTTLE.get());
            if (stack.isEmpty()) {
                player.setItemInHand(hand, sap);
            } else if (!player.getInventory().add(sap)) {
                player.drop(sap, false);
            }

            level.playSound(player, pos, SoundEvents.BOTTLE_FILL, SoundSource.NEUTRAL, 1.0F, 1.0F);
            level.gameEvent(player, GameEvent.FLUID_PICKUP, pos);
            if (!level.isClientSide()) {
                player.awardStat(Stats.ITEM_USED.get(Items.GLASS_BOTTLE));
            }

            level.setBlockAndUpdate(pos, state.setValue(AXIS, state.getValue(AXIS)));
            level.setBlockAndUpdate(pos, saplessBlock.get().defaultBlockState().setValue(AXIS, state.getValue(AXIS)));
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
