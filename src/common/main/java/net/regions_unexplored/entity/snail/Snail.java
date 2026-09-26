package net.regions_unexplored.entity.snail;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.pathfinder.PathType;
import net.regions_unexplored.registry.RUItems;

public class Snail extends Animal {
    public Snail(EntityType<? extends Snail> type, Level level) {
        super(type, level);
        setPathfindingMalus(PathType.DANGER_OTHER, 0.0F);
        setPathfindingMalus(PathType.DAMAGE_OTHER, 0.0F);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new BreedGoal(this, 0.5D));
        goalSelector.addGoal(1, new TemptGoal(this, 0.5D, Ingredient.of(RUItems.FOUL_BERRIES.get()), false));
        goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 0.5D));
        goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 18.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.25D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(RUItems.FOUL_BERRIES.get());
    }

    @Override
    public AgeableMob getBreedOffspring(net.minecraft.server.level.ServerLevel level, AgeableMob other) {
        return RUEntityTypes.SNAIL.get().create(level);
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, boolean recentlyHit) {
        super.dropCustomDeathLoot(level, source, recentlyHit);
        if (level.getGameRules().getBoolean(net.minecraft.world.level.GameRules.RULE_DOMOBLOOT)) {
            spawnAtLocation(RUItems.SNAIL_SHELL_PIECE.get());
        }
    }
}
