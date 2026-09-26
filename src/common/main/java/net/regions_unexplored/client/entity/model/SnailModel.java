package net.regions_unexplored.client.entity.model;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.regions_unexplored.entity.snail.Snail;

@OnlyIn(Dist.CLIENT)
public class SnailModel<T extends Snail> extends EntityModel<T> {
    private final ModelPart body, shell, rightEye, leftEye, rightTentacle, leftTentacle;
    private float hideAmount;

    public SnailModel(ModelPart root) {
        body = root.getChild("body");
        shell = root.getChild("shell");
        rightEye = root.getChild("right_eye");
        leftEye = root.getChild("left_eye");
        rightTentacle = root.getChild("right_tentacle");
        leftTentacle = root.getChild("left_tentacle");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-4, 0, 0, 8, 18, 6), PartPose.offsetAndRotation(0, 24, -9, ((float)Math.PI / 2F), 0, 0));
        root.addOrReplaceChild("shell", CubeListBuilder.create().texOffs(0, 24).addBox(-4.5F, 0, 0, 9, 14, 14), PartPose.offsetAndRotation(0, 7, -1, -0.22F, 0, 0));
        root.addOrReplaceChild("right_eye", CubeListBuilder.create().texOffs(28, 0).addBox(-1, -6, -1, 2, 7, 2), PartPose.offset(2.5F, 18, -7));
        root.addOrReplaceChild("left_eye", CubeListBuilder.create().texOffs(28, 0).addBox(-1, -6, -1, 2, 7, 2), PartPose.offset(-2.5F, 18, -7));
        root.addOrReplaceChild("right_tentacle", CubeListBuilder.create().texOffs(28, 9).addBox(-1, -1, -2, 2, 2, 2), PartPose.offset(3, 22, -9));
        root.addOrReplaceChild("left_tentacle", CubeListBuilder.create().texOffs(28, 9).addBox(-1, -1, -2, 2, 2, 2), PartPose.offset(-3, 22, -9));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {
        hideAmount = entity.isPanicking() ? 1.0F : 0.0F;
        float f = 3.0F * hideAmount;
        rightEye.setPos(2.5F, 18.0F + Math.min(5.0F, 10.0F * hideAmount), -7.0F + f);
        leftEye.setPos(-2.5F, 18.0F + Math.min(5.0F, 10.0F * hideAmount), -7.0F + f);
        rightTentacle.setPos(3, 22, -9 + f);
        leftTentacle.setPos(-3, 22, -9 + f);
        shell.setPos(0, 7, -1 - f);
        rightEye.visible = leftEye.visible = hideAmount < 1.0F;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        float f = 1.0F - hideAmount;
        rightEye.xRot = f * (headPitch * ((float)Math.PI / 180F) * 0.5F + 0.3F + Mth.cos(ageInTicks * 0.04F) * 0.15F);
        leftEye.xRot = f * (headPitch * ((float)Math.PI / 180F) * 0.5F + 0.3F + Mth.sin(ageInTicks * 0.04F) * 0.15F);
        rightTentacle.yRot = Mth.cos(ageInTicks * 0.6F) * 0.1F;
        leftTentacle.yRot = -rightTentacle.yRot;
    }

    @Override
    public void renderToBuffer(PoseStack pose, VertexConsumer buffer, int light, int overlay, int color) {
        if (young) {
            pose.pushPose();
            pose.scale(0.7F, 0.7F, 0.7F);
            pose.translate(0, 13F / 16F, 2.5F / 16F);
            ImmutableList.of(leftEye, rightEye).forEach(p -> p.render(pose, buffer, light, overlay, color));
            pose.popPose();
            pose.pushPose();
            pose.scale(0.5F, 0.5F, 0.5F);
            pose.translate(0, 24F / 16F, 0);
            ImmutableList.of(rightTentacle, leftTentacle, shell, body).forEach(p -> p.render(pose, buffer, light, overlay, color));
            pose.popPose();
        } else {
            ImmutableList.of(rightEye, leftEye, rightTentacle, leftTentacle, shell, body).forEach(p -> p.render(pose, buffer, light, overlay, color));
        }
    }
}
