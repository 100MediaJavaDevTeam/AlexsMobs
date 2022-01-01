package com.github.alexthe666.alexsmobs.client.render.layer;

import com.github.alexthe666.alexsmobs.ClientProxy;
import com.github.alexthe666.alexsmobs.client.model.ModelKangaroo;
import com.github.alexthe666.alexsmobs.client.render.RenderKangaroo;
import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.entity.LivingRenderer;
import net.minecraft.client.renderer.entity.layers.LayerRenderer;
import net.minecraft.client.renderer.entity.model.EntityModel;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.vector.Vector3f;

public class LayerKangarooBaby extends LayerRenderer<EntityKangaroo, ModelKangaroo> {

    public LayerKangarooBaby(RenderKangaroo render) {
        super(render);
    }

    public void render(MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int packedLightIn, EntityKangaroo roo, float limbSwing, float limbSwingAmount, float partialTicks, float ageInTicks, float netHeadYaw, float headPitch) {
        if(roo.isVehicle() && !roo.isBaby()){
            for(Entity passenger : roo.getPassengers()){
                float riderRot = passenger.yRotO + (passenger.yRot - passenger.yRotO) * partialTicks;
                EntityRenderer render = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(passenger);
                EntityModel modelBase = null;
                if (render instanceof LivingRenderer) {
                    modelBase = ((LivingRenderer) render).getModel();
                }
                if(modelBase != null){
                    ClientProxy.currentUnrenderedEntities.remove(passenger.getUUID());
                    matrixStackIn.pushPose();
                    translateToPouch(matrixStackIn);
                    matrixStackIn.translate(0, 1.12F, -0.3F);
                    ModelKangaroo.renderOnlyHead = true;
                    matrixStackIn.mulPose(Vector3f.ZP.rotationDegrees(180F));
                    matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(riderRot + 180F));
                    renderEntity(passenger, 0, 0, 0, 0, partialTicks, matrixStackIn, bufferIn, packedLightIn);
                    ModelKangaroo.renderOnlyHead = false;
                    matrixStackIn.popPose();
                    ClientProxy.currentUnrenderedEntities.add(passenger.getUUID());
                }

            }
        }

    }

    public <E extends Entity> void renderEntity(E entityIn, double x, double y, double z, float yaw, float partialTicks, MatrixStack matrixStack, IRenderTypeBuffer bufferIn, int packedLight) {
        EntityRenderer<? super E> render = null;
        EntityRendererManager manager = Minecraft.getInstance().getEntityRenderDispatcher();
        try {
            render = manager.getRenderer(entityIn);

            if (render != null) {
                try {
                    render.render(entityIn, yaw, partialTicks, matrixStack, bufferIn, packedLight);
                } catch (Throwable throwable1) {
                    throw new ReportedException(CrashReport.forThrowable(throwable1, "Rendering entity in world"));
                }
            }
        } catch (Throwable throwable3) {
            CrashReport crashreport = CrashReport.forThrowable(throwable3, "Rendering entity in world");
            CrashReportCategory crashreportcategory = crashreport.addCategory("Entity being rendered");
            entityIn.fillCrashReportCategory(crashreportcategory);
            CrashReportCategory crashreportcategory1 = crashreport.addCategory("Renderer details");
            crashreportcategory1.setDetail("Assigned renderer", render);
            crashreportcategory1.setDetail("Location", CrashReportCategory.formatLocation(x, y, z));
            crashreportcategory1.setDetail("Rotation", Float.valueOf(yaw));
            crashreportcategory1.setDetail("Delta", Float.valueOf(partialTicks));
            throw new ReportedException(crashreport);
        }
    }

    protected void translateToPouch(MatrixStack matrixStack) {
        this.getParentModel().root.translateAndRotate(matrixStack);
        this.getParentModel().body.translateAndRotate(matrixStack);
    }
}
