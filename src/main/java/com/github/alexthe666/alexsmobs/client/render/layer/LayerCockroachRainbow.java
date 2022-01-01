package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.client.model.ModelCockroach;
import com.github.alexthe666.alexsmobs.client.render.AMRenderTypes;
import com.github.alexthe666.alexsmobs.client.render.RenderCockroach;
import com.github.alexthe666.alexsmobs.entity.EntityCockroach;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;

public class LayerCockroachRainbow extends LayerRenderer<EntityCockroach, ModelCockroach> {


    public LayerCockroachRainbow(RenderCockroach render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityCockroach entitylivingbaseIn, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(entitylivingbaseIn.isRainbow()) {
           IVertexBuilder ivertexbuilder = bufferIn.getBuffer(AMRenderTypes.RAINBOW_GLINT);

            this.getParentModel().renderToBuffer(matrixStackIn, ivertexbuilder, packedLightIn, LivingRenderer.getOverlayCoords(entitylivingbaseIn, 0), 1, 1, 1, 1.0F);

        }
    }
}
