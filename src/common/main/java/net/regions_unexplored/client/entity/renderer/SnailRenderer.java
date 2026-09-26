package net.regions_unexplored.client.entity.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;
import net.regions_unexplored.RegionsUnexplored;
import net.regions_unexplored.client.entity.model.RUEntityModelLayers;
import net.regions_unexplored.client.entity.model.SnailModel;
import net.regions_unexplored.entity.snail.Snail;

public class SnailRenderer extends MobRenderer<Snail, SnailModel<Snail>> {
    private static final Identifier TEXTURE = RegionsUnexplored.id("textures/entity/snail/snail.png");

    public SnailRenderer(EntityRendererProvider.Context context) {
        super(context, new SnailModel<>(context.bakeLayer(RUEntityModelLayers.SNAIL_MAIN)), 0.35F);
    }

    @Override
    public Identifier getTextureLocation(Snail entity) {
        return TEXTURE;
    }
}
