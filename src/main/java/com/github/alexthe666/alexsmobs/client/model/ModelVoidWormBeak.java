package com.github.alexthe666.alexsmobs.client.model;

import com.github.alexthe666.alexsmobs.tileentity.TileEntityVoidWormBeak;
import com.github.alexthe666.citadel.client.model.AdvancedEntityModel;
import com.github.alexthe666.citadel.client.model.AdvancedModelBox;
import com.google.common.collect.ImmutableList;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;

public class ModelVoidWormBeak extends AdvancedEntityModel<Entity> {
    private final AdvancedModelBox root;
    private final AdvancedModelBox left;
    private final AdvancedModelBox right;

    public ModelVoidWormBeak() {
        texWidth = 64;
        texHeight = 64;
        root = new AdvancedModelBox(this);
        root.setPos(0.0F, 24.0F, 0.0F);
        left = new AdvancedModelBox(this);
        left.setPos(0.0F, 0.0F, 0.0F);
        root.addChild(left);
        left.texOffs(0, 0).addBox(-0.1F, -12.9F, -3.5F, 7.0F, 13.0F, 7.0F, -0.1F, false);
        right = new AdvancedModelBox(this);
        right.setPos(0.0F, 0.0F, 0.0F);
        root.addChild(right);
        right.texOffs(0, 21).addBox(-7.0F, -13.0F, -3.5F, 7.0F, 13.0F, 7.0F, 0.0F, false);
        this.updateDefaultPose();
    }

    @Override
    public Iterable<ModelRenderer> parts() {
        return ImmutableList.of(root);
    }

    @Override
    public Iterable<AdvancedModelBox> getAllParts() {
        return ImmutableList.of(root, left, right);
    }

    @Override
    public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
    }

    public void renderBeak(TileEntityVoidWormBeak beak, float partialTick) {
        this.resetToDefaultPose();
        float amount = beak.getChompProgress(partialTick) * 0.2F;
        float ageInTicks = beak.ticksExisted + partialTick;
        this.flap(left, 0.5F, 0.5F, false, 0F, 0.3F, ageInTicks, amount);
        this.flap(right, 0.5F, -0.5F, false, 0F, -0.3F, ageInTicks, amount);
        float rotation = MathHelper.cos(ageInTicks * 0.5F) * 0.5F * amount + 0.3F * amount;
        left.y -= rotation * 4.5F;
        right.y -= rotation * 4.5F;
    }

    public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
        modelRenderer.xRot = x;
        modelRenderer.yRot = y;
        modelRenderer.zRot = z;
    }
}