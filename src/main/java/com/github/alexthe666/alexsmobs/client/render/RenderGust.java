package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.client.model.ModelGuster;
import com.github.alexthe666.alexsmobs.entity.EntityGust;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.settings.ParticleStatus;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;

public class RenderGust extends EntityRenderer<EntityGust> {
    private static final ResourceLocation TEXTURE = new ResourceLocation("alexsmobs:textures/entity/guster.png");
    private final ModelGuster model = new ModelGuster();

    public RenderGust(EntityRendererManager renderManagerIn) {
        super(renderManagerIn);
    }

    public void render(EntityGust entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        if(true || Minecraft.getInstance().options.particles == ParticleStatus.MINIMAL){
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.0D, (double)0.5F, 0.0D);
            if(!entityIn.getVertical()){
                matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(180F));
            }else{
                matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-180F));

            }
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.yRot) - 90.0F));
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.entityTranslucent(TEXTURE));
            this.model.hideEyes();
            this.model.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            this.model.animateGust(entityIn, 0, 0, entityIn.tickCount + partialTicks);
            this.model.showEyes();
            matrixStackIn.popPose();
        }
        super.render(entityIn, entityYaw, partialTicks, matrixStackIn, bufferIn, packedLightIn);
    }

    public ResourceLocation getTextureLocation(EntityGust entity) {
        return TEXTURE;
    }
}
