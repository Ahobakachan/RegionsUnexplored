package net.regions_unexplored.block.wood;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Supplier;

public class MapleLogBlock extends RotatedPillarBlock {
    private final Supplier<Block> strippedBlock;
    private final Supplier<Block> sappyBlock;

    public MapleLogBlock(Supplier<Block> strippedBlock, Supplier<Block> sappyBlock, Properties properties) {
        super(properties);
        this.strippedBlock = strippedBlock;
        this.sappyBlock = sappyBlock;
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof AxeItem) {
            BlockState target = !level.isClientSide() && level.getRandom().nextFloat() < 0.25F
                    ? sappyBlock.get().defaultBlockState()
                    : strippedBlock.get().defaultBlockState();
            target = target.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));

            if (!level.isClientSide()) {
                level.setBlock(pos, target, 11);
                level.playSound(null, pos, SoundEvents.AXE_STRIP, SoundSource.BLOCKS, 1.0F, 1.0F);
                player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }
}
