package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelAncientDart;
import com.github.alexthe666.alexsmobs.client.model.ModelCapuchinMonkey;
import com.github.alexthe666.alexsmobs.client.render.RenderCapuchinMonkey;
import com.github.alexthe666.alexsmobs.entity.EntityCapuchinMonkey;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.vector.Vector3f;

public class LayerCapuchinItem extends LayerRenderer<EntityCapuchinMonkey, ModelCapuchinMonkey> {

    public static final ResourceLocation DART_TEXTURE = new ResourceLocation("alexsmobs:textures/entity/ancient_dart.png");
    public static final ModelAncientDart DART_MODEL = new ModelAncientDart();

    public LayerCapuchinItem(RenderCapuchinMonkey render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityCapuchinMonkey entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.hasDart()){
            matrixStackIn.pushPose();
            if(entitylivingbaseIn.isBaby()){
                matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                matrixStackIn.translate(0.5D, 2.6D, 0.15D);
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(-0.65, -0.75F, -0.1F);
                matrixStackIn.scale(2.8F, 2.8F, 2.8F);

            }else{
                translateToHand(false, matrixStackIn);
            }
            float f = 0.0F;
            if(entitylivingbaseIn.getAnimation() == EntityCapuchinMonkey.ANIMATION_THROW){
                if(entitylivingbaseIn.getAnimationTick() < 6){
                    f = Math.min(3, entitylivingbaseIn.getAnimationTick() + partialTicks) * 60;
                }else{
                    f = (12 - (entitylivingbaseIn.getAnimationTick() + partialTicks)) * 30;
                }
            }
            matrixStackIn.translate(0, 0.5F, 0F);
            matrixStackIn.scale(1.2F, 1.2F, 1.2F);
            matrixStackIn.pushPose();
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(f));
            IVertexBuilder ivertexbuilder = bufferIn.getBuffer(DART_MODEL.renderType(DART_TEXTURE));
            DART_MODEL.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, OverlayTexture.NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.popPose();
            matrixStackIn.popPose();

        }else if(entitylivingbaseIn.getAnimation() == EntityCapuchinMonkey.ANIMATION_THROW && entitylivingbaseIn.getAnimationTick() <= 5) {
            ItemStack itemstack = new ItemStack(Items.COBBLESTONE);
            matrixStackIn.pushPose();
            if (entitylivingbaseIn.isBaby()) {
                matrixStackIn.scale(0.35F, 0.35F, 0.35F);
                matrixStackIn.translate(0.5D, 2.6D, 0.15D);
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(-0.4F, 0.75F, -0.0F);
                matrixStackIn.scale(2.8F, 2.8F, 2.8F);
            } else {
                translateToHand(false, matrixStackIn);
                matrixStackIn.translate(0.125F, 0.5F, 0.1F);
            }
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-2.5F));
            matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-90F));
            Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, itemstack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
            matrixStackIn.popPose();
        }
    }

    protected void translateToHand(boolean left, MatrixStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().body.translateAndRotate(matrixStack);
        this.getParentModel().arm_right.translateAndRotate(matrixStack);
    }
}
