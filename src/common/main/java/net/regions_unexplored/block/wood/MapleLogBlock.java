package net.regions_unexplored.block.wood;

import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;
import net.neoforged.neoforge.common.ItemAbility;

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
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ItemAbility toolAction, boolean simulate) {
        if (toolAction == ItemAbilities.AXE_STRIP) {
            Level level = context.getLevel();
            BlockState target = !level.isClientSide() && level.getRandom().nextFloat() < 0.25F
                    ? sappyBlock.get().defaultBlockState()
                    : strippedBlock.get().defaultBlockState();
            return target.setValue(RotatedPillarBlock.AXIS, state.getValue(RotatedPillarBlock.AXIS));
        }
        return super.getToolModifiedState(state, context, toolAction, simulate);
    }
}
