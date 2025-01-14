package com.github.alexthe666.alexsmobs.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelShieldOfTheDeep extends EntityModel<Entity> {
	private final ModelRenderer shield;
	private final ModelRenderer handle;

	public ModelShieldOfTheDeep() {
		texWidth = 64;
		texHeight = 64;

		shield = new ModelRenderer(this);
		shield.setPos(-2.0F, 16.0F, 0.0F);
		shield.texOffs(0, 0).addBox(-1.0F, -4.0F, -6.0F, 1.0F, 12.0F, 12.0F, 0.0F, false);
		shield.texOffs(17, 15).addBox(-3.0F, -3.0F, -5.0F, 2.0F, 10.0F, 10.0F, 0.0F, false);
		shield.texOffs(27, 0).addBox(-4.0F, -1.0F, -3.0F, 3.0F, 6.0F, 6.0F, 0.0F, false);

		handle = new ModelRenderer(this);
		handle.setPos(8.0F, 8.0F, -8.0F);
		shield.addChild(handle);
		handle.texOffs(0, 25).addBox(-8.0F, -8.5F, 7.0F, 5.0F, 5.0F, 2.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
		//previously the render function, render code was moved to a method below
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		shield.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}