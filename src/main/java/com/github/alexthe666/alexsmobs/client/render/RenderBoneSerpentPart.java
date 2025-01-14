package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentBody;
import com.github.alexthe666.alexsmobs.client.model.ModelBoneSerpentTail;
import com.github.alexthe666.alexsmobs.entity.EntityBoneSerpentPart;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.model.SegmentedModel;
import net.minecraft.util.ResourceLocation;

public class RenderBoneSerpentPart extends LivingRenderer<EntityBoneSerpentPart, SegmentedModel<EntityBoneSerpentPart>> {
    private static final ResourceLocation TEXTURE_BODY = new ResourceLocation("alexsmobs:textures/entity/bone_serpent_mid.png");
    private static final ResourceLocation TEXTURE_TAIL = new ResourceLocation("alexsmobs:textures/entity/bone_serpent_tail.png");
    private ModelBoneSerpentBody bodyModel = new ModelBoneSerpentBody();
    private ModelBoneSerpentTail tailModel = new ModelBoneSerpentTail();

    public RenderBoneSerpentPart(EntityRendererManager renderManagerIn) {
        super(renderManagerIn, new ModelBoneSerpentBody(), 0.3F);
    }

    protected boolean shouldShowName(EntityBoneSerpentPart entity) {
        return super.shouldShowName(entity) && (entity.shouldShowName() || entity.hasCustomName() && entity == this.entityRenderDispatcher.crosshairPickEntity);
    }

    protected void scale(EntityBoneSerpentPart entitylivingbaseIn, MatrixStack matrixStackIn, float partialTickTime) {
        this.model = entitylivingbaseIn.isTail() ? tailModel : bodyModel;
      //  matrixStackIn.scale(1.2F, 1.2F, 1.2F);
    }


    public ResourceLocation getTextureLocation(EntityBoneSerpentPart entity) {
        return entity.isTail() ? TEXTURE_TAIL : TEXTURE_BODY;
    }
}
