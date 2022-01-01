package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelCockroach;
import com.github.alexthe666.alexsmobs.client.model.ModelSombrero;
import com.github.alexthe666.alexsmobs.client.render.RenderCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class LayerCockroachMaracas extends LayerRenderer<EntityCockroach, ModelCockroach> {

    private ItemStack stack;
    private ModelSombrero sombrero;
    private static final ResourceLocation SOMBRERO_TEX = new ResourceLocation("alexsmobs:textures/armor/sombrero.png");

    public LayerCockroachMaracas(RenderCockroach render) {
        super(render);
        stack = new ItemStack(AMItemRegistry.MARACA);
        this.sombrero = new ModelSombrero(0);

    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityCockroach entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.hasMaracas()){
            matrixStackIn.pushPose();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.65F, 0.65F, 0.65F);
                matrixStackIn.translate(0.0D, 0.815D, 0.125D);
            }
            matrixStackIn.pushPose();
            translateToHand(0, matrixStackIn);
            matrixStackIn.translate(-0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-90F));
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(60F));
            Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, stack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(1, matrixStackIn);
            matrixStackIn.translate(0.25F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(90F));
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(-120F));
            Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, stack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(2, matrixStackIn);
            matrixStackIn.translate(-0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-90F));
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(60F));
            Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, stack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            matrixStackIn.pushPose();
            translateToHand(3, matrixStackIn);
            matrixStackIn.translate(0.35F, 0.0F, 0);
            matrixStackIn.scale(1.4F, 1.4F, 1.4F);
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(90F));
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(-120F));
            Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, stack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
            if(!entitylivingbaseIn.isHeadless()){
                matrixStackIn.pushPose();
                translateToHand(4, matrixStackIn);
                matrixStackIn.translate(0F, -0.4F, -0.01F);
                matrixStackIn.translate(0F, entitylivingbaseIn.danceProgress * 0.045F, entitylivingbaseIn.danceProgress * -0.09F);
                matrixStackIn.scale(0.8F, 0.8F, 0.8F);
                matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(60F * entitylivingbaseIn.danceProgress * 0.2F));
                IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.entityCutoutNoCull(SOMBRERO_TEX));
                sombrero.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, LivingRenderer.getOverlayCoords(entitylivingbaseIn, 0.0F), 1.0F, 1.0F, 1.0F, 1.0F);
                matrixStackIn.popPose();
            }
            matrixStackIn.popPose();
        }
    }

    protected void translateToHand(int hand, MatrixStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().abdomen.translateAndRotate(matrixStack);
        if (hand == 0) {
            this.getParentModel().right_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 1) {
            this.getParentModel().left_leg_front.translateAndRotate(matrixStack);
        } else if (hand == 2) {
            this.getParentModel().right_leg_mid.translateAndRotate(matrixStack);
        } else if (hand == 3) {
            this.getParentModel().left_leg_mid.translateAndRotate(matrixStack);
        }else{
            this.getParentModel().neck.translateAndRotate(matrixStack);
            this.getParentModel().head.translateAndRotate(matrixStack);
        }
    }
}
