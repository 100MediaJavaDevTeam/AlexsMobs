package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelMantisShrimp;
import com.github.alexthe666.alexsmobs.client.render.RenderMantisShrimp;
import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.vector.Vector3f;

public class LayerMantisShrimpItem extends LayerRenderer<EntityMantisShrimp, ModelMantisShrimp> {

    public LayerMantisShrimpItem(RenderMantisShrimp render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityMantisShrimp entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack itemstack = entitylivingbaseIn.getItemBySlot(EquipmentSlotType.MAINHAND);
        matrixStackIn.pushPose();
        boolean left = entitylivingbaseIn.isLeftHanded();
        if(entitylivingbaseIn.isBaby()){
            matrixStackIn.scale(0.5F, 0.5F, 0.5F);
            matrixStackIn.translate(0.0D, 1.5D, 0D);
        }
        matrixStackIn.pushPose();
        translateToHand(matrixStackIn, left);
        matrixStackIn.translate(left ? 0.075F : -0.075F, 0.45F, -0.125F);
        if(!Minecraft.getInstance().getItemRenderer().getItemModelShaper().getItemModel(itemstack).isGui3d()){
            matrixStackIn.translate(0F, 0F, 0.05F);
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(left ? -40F : 40F));
        }
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-2.5F));
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-180F));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180F));
        matrixStackIn.scale(1.2F, 1.2F, 1.2F);
        Minecraft.getInstance().getItemInHandRenderer().renderItem(entitylivingbaseIn, itemstack, ItemCameraTransforms.TransformType.GROUND, false, matrixStackIn, bufferIn, packedLightIn);
        matrixStackIn.popPose();
        matrixStackIn.popPose();
    }

    protected void translateToHand(MatrixStack matrixStack, boolean left) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().body.translateAndRotate(matrixStack);
        this.getParentModel().head.translateAndRotate(matrixStack);
        if(left){
            this.getParentModel().arm_left.translateAndRotate(matrixStack);
            this.getParentModel().fist_left.translateAndRotate(matrixStack);
        }else{
            this.getParentModel().arm_right.translateAndRotate(matrixStack);
            this.getParentModel().fist_right.translateAndRotate(matrixStack);
        }
    }
}
