package com.github.alexthe666.alexsmobs.client.model;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.client.renderer.model.ModelRenderer;
import net.minecraft.entity.Entity;

public class ModelAncientDart extends EntityModel<Entity> {
	private final ModelRenderer root;
	private final ModelRenderer main;
	private final ModelRenderer feathers;
	private final ModelRenderer cube_r1;
	private final ModelRenderer cube_r2;

	public ModelAncientDart() {
		texWidth = 32;
		texHeight = 32;

		root = new ModelRenderer(this);
		root.setPos(0.0F, 0.0F, 0.0F);


		main = new ModelRenderer(this);
		main.setPos(0.0F, -1.0F, 0.0F);
		root.addChild(main);
		main.texOffs(11, 0).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 2.0F, 2.0F, 0.0F, false);
		main.texOffs(0, 0).addBox(-0.5F, -0.5F, -5.0F, 1.0F, 1.0F, 4.0F, 0.0F, false);

		feathers = new ModelRenderer(this);
		feathers.setPos(0.0F, 1.0F, 1.0F);
		main.addChild(feathers);


		cube_r1 = new ModelRenderer(this);
		cube_r1.setPos(0.0F, -1.0F, 0.5F);
		feathers.addChild(cube_r1);
		setRotationAngle(cube_r1, 0.0F, 0.0F, 0.7854F);
		cube_r1.texOffs(0, 6).addBox(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 3.0F, 0.0F, false);

		cube_r2 = new ModelRenderer(this);
		cube_r2.setPos(0.0F, -1.0F, 0.5F);
		feathers.addChild(cube_r2);
		setRotationAngle(cube_r2, 0.0F, 0.0F, -0.7854F);
		cube_r2.texOffs(7, 6).addBox(0.0F, -1.5F, -0.5F, 0.0F, 3.0F, 3.0F, 0.0F, false);
	}

	@Override
	public void setupAnim(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch){
	}

	@Override
	public void renderToBuffer(MatrixStack matrixStack, IVertexBuilder buffer, int packedLight, int packedOverlay, float red, float green, float blue, float alpha){
		root.render(matrixStack, buffer, packedLight, packedOverlay);
	}

	public void setRotationAngle(ModelRenderer modelRenderer, float x, float y, float z) {
		modelRenderer.xRot = x;
		modelRenderer.yRot = y;
		modelRenderer.zRot = z;
	}
}