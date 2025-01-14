package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.client.model.ModelStraddleboard;
import com.github.alexthe666.alexsmobs.entity.EntityStraddleboard;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3f;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class RenderStraddleboard extends EntityRenderer<EntityStraddleboard> {
    private static final ResourceLocation TEXTURE_OVERLAY = new ResourceLocation("alexsmobs:textures/entity/straddleboard_overlay.png");
    private static final ResourceLocation TEXTURE = new ResourceLocation("alexsmobs:textures/entity/straddleboard.png");
    private static ModelStraddleboard BOARD_MODEL = new ModelStraddleboard();

    public RenderStraddleboard(EntityRendererManager renderManager) {
        super(renderManager);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityStraddleboard entity) {
        return TEXTURE;
    }

    @Override
    public void render(EntityStraddleboard entityIn, float entityYaw, float partialTicks, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn) {
        matrixStackIn.pushPose();
        matrixStackIn.mulPose(new Quaternion(Vector3f.XP, 180F, true));
        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.yRotO, entityIn.yRot)));
        matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(MathHelper.lerp(partialTicks, entityIn.xRotO, entityIn.xRot)));
        matrixStackIn.pushPose();
        boolean lava = entityIn.isInLava() || entityIn.isVehicle();
        float f2 = entityIn.getRockingAngle(partialTicks);
        if (!MathHelper.equal(f2, 0.0F)) {
            matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(entityIn.getRockingAngle(partialTicks)));
        }
        int k = entityIn.getColor();
        float r = (float)(k >> 16 & 255) / 255.0F;
        float g = (float)(k >> 8 & 255) / 255.0F;
        float b = (float)(k & 255) / 255.0F;
        matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(entityIn.prevBoardRot + partialTicks * (entityIn.boardRot - entityIn.prevBoardRot)));
        matrixStackIn.translate(0, -1.5F - Math.abs(entityIn.boardRot * 0.007F) - (lava ? 0.25F : 0), 0);
        BOARD_MODEL.animateBoard(entityIn, entityIn.tickCount + partialTicks);
        IVertexBuilder ivertexbuilder2 = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE_OVERLAY));
        BOARD_MODEL.renderToBuffer(matrixStackIn, ivertexbuilder2, packedLightIn, NO_OVERLAY, r, g, b, 1.0F);
        IVertexBuilder ivertexbuilder = bufferIn.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        BOARD_MODEL.renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, NO_OVERLAY, 1.0F, 1.0F, 1.0F, 1.0F);
        matrixStackIn.popPose();
        matrixStackIn.popPose();


    }

}
