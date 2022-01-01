package com.github.alexthe666.alexsmobs.client.event;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.ClientProxy;
import com.github.alexthe666.alexsmobs.client.model.ModelWanderingVillagerRider;
import com.github.alexthe666.alexsmobs.client.render.AMItemstackRenderer;
import com.github.alexthe666.alexsmobs.client.render.LavaVisionFluidRenderer;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityBaldEagle;
import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.message.MessageUpdateEagleControls;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.client.event.EventGetOutlineColor;
import com.google.common.base.MoreObjects;
import com.mojang.blaze3d.matrix.MatrixStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.settings.PointOfView;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.CreatureAttribute;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.WanderingTraderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.*;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClientEvents {

    private static final ResourceLocation RADIUS_TEXTURE = new ResourceLocation("alexsmobs:textures/falconry_radius.png");
    private boolean previousLavaVision = false;
    private FluidBlockRenderer previousFluidRenderer;

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onOutlineEntityColor(EventGetOutlineColor event){
        if(event.getEntityIn() instanceof ItemEntity && ItemTags.getAllTags().getTag(AMTagRegistry.VOID_WORM_DROPS).contains(((ItemEntity) event.getEntityIn()).getItem().getItem())){
            int fromColor = 0;
            int toColor = 0X21E5FF;
            float startR = (float) (fromColor >> 16 & 255) / 255.0F;
            float startG = (float) (fromColor >> 8 & 255) / 255.0F;
            float startB = (float) (fromColor & 255) / 255.0F;
            float endR = (float) (toColor >> 16 & 255) / 255.0F;
            float endG = (float) (toColor >> 8 & 255) / 255.0F;
            float endB = (float) (toColor & 255) / 255.0F;
            float f = (float) (Math.cos(0.4F * (event.getEntityIn().tickCount + Minecraft.getInstance().getFrameTime())) + 1.0F) * 0.5F;
            float r = (endR - startR) * f + startR;
            float g = (endG - startG) * f + startG;
            float b = (endB - startB) * f + startB;
            int j =  ((((int)(r*255)) & 0xFF) << 16) |
                    ((((int)(g*255)) & 0xFF) << 8)  |
                    ((((int)(b*255)) & 0xFF) << 0);
            event.setColor(j);
            event.setResult(Event.Result.ALLOW);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onFogDensity(EntityViewRenderEvent.FogDensity event) {
        FluidState fluidstate = event.getInfo().getFluidInCamera();
        if (Minecraft.getInstance().player.hasEffect(AMEffectRegistry.LAVA_VISION)) {
            if (fluidstate.is(FluidTags.LAVA)) {
                event.setDensity(0.05F);
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPreRenderEntity(RenderLivingEvent.Pre event) {
        if (event.getEntity() instanceof WanderingTraderEntity) {
            if (event.getEntity().getVehicle() instanceof EntityElephant) {
                if (!(event.getRenderer().model instanceof ModelWanderingVillagerRider)) {
                    event.getRenderer().model = new ModelWanderingVillagerRider();
                }
            }
        }
        if (event.getEntity().hasEffect(AMEffectRegistry.CLINGING) && event.getEntity().getEyeHeight() < event.getEntity().getBbHeight() * 0.45F || event.getEntity().hasEffect(AMEffectRegistry.DEBILITATING_STING) && event.getEntity().getMobType() == CreatureAttribute.ARTHROPOD && event.getEntity().getBbWidth() > event.getEntity().getBbHeight()) {
            event.getMatrixStack().pushPose();
            event.getMatrixStack().translate(0.0D, event.getEntity().getBbHeight() + 0.1F, 0.0D);
            event.getMatrixStack().mulPose(Vector3f.ZP.rotationDegrees(180.0F));
            event.getEntity().yBodyRotO = -event.getEntity().yBodyRotO;
            event.getEntity().yBodyRot = -event.getEntity().yBodyRot;
            event.getEntity().yHeadRotO = -event.getEntity().yHeadRotO;
            event.getEntity().yHeadRot = -event.getEntity().yHeadRot;
        }
        if (event.getEntity().hasEffect(AMEffectRegistry.ENDER_FLU)) {
            event.getMatrixStack().pushPose();
            event.getMatrixStack().mulPose(Vector3f.YP.rotationDegrees((float) (Math.cos((double) event.getEntity().tickCount * 7F) * Math.PI * (double) 1.2F)));
            float vibrate = 0.05F;
            event.getMatrixStack().translate((event.getEntity().getRandom().nextFloat() - 0.5F) * vibrate, (event.getEntity().getRandom().nextFloat() - 0.5F) * vibrate, (event.getEntity().getRandom().nextFloat() - 0.5F) * vibrate);
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onPostRenderEntity(RenderLivingEvent.Post event) {
        if (event.getEntity().hasEffect(AMEffectRegistry.ENDER_FLU)) {
            event.getMatrixStack().popPose();
        }
        if (event.getEntity().hasEffect(AMEffectRegistry.CLINGING) && event.getEntity().getEyeHeight() < event.getEntity().getBbHeight() * 0.45F || event.getEntity().hasEffect(AMEffectRegistry.DEBILITATING_STING) && event.getEntity().getMobType() == CreatureAttribute.ARTHROPOD && event.getEntity().getBbWidth() > event.getEntity().getBbHeight()) {
            event.getMatrixStack().popPose();
            event.getEntity().yBodyRotO = -event.getEntity().yBodyRotO;
            event.getEntity().yBodyRot = -event.getEntity().yBodyRot;
            event.getEntity().yHeadRotO = -event.getEntity().yHeadRotO;
            event.getEntity().yHeadRot = -event.getEntity().yHeadRot;
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderHand(RenderHandEvent event) {
        if (Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle) {
            event.setCanceled(true);
        }
        if (!Minecraft.getInstance().player.getPassengers().isEmpty() && event.getHand() == Hand.MAIN_HAND) {
            PlayerEntity player = Minecraft.getInstance().player;
            boolean leftHand = false;
            if (player.getItemInHand(Hand.MAIN_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE) {
                leftHand = player.getMainArm() == HandSide.LEFT;
            } else if (player.getItemInHand(Hand.OFF_HAND).getItem() == AMItemRegistry.FALCONRY_GLOVE) {
                leftHand = player.getMainArm() != HandSide.LEFT;
            }
            for (Entity entity : player.getPassengers()) {
                if (entity instanceof EntityBaldEagle) {
                    float yaw = player.yBodyRotO + (player.yBodyRot - player.yBodyRotO) * event.getPartialTicks();
                    ClientProxy.currentUnrenderedEntities.remove(entity.getUUID());
                    MatrixStack matrixStackIn = event.getMatrixStack();
                    matrixStackIn.pushPose();
                    matrixStackIn.scale(0.5F, 0.5F, 0.5F);
                    matrixStackIn.translate(leftHand ? -0.8F : 0.8F, -0.6F, -1F);
                    matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(yaw));
                    if (leftHand) {
                        matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(90));
                    } else {
                        matrixStackIn.mulPose(Vector3f.YN.rotationDegrees(90));
                    }
                    renderEntity(entity, 0, 0, 0, 0, event.getPartialTicks(), matrixStackIn, event.getBuffers(), event.getLight());
                    matrixStackIn.popPose();
                    ClientProxy.currentUnrenderedEntities.add(entity.getUUID());
                }
            }
        }
        if (Minecraft.getInstance().player.getUseItem().getItem() == AMItemRegistry.DIMENSIONAL_CARVER && event.getItemStack().getItem() == AMItemRegistry.DIMENSIONAL_CARVER) {
            MatrixStack matrixStackIn = event.getMatrixStack();
            matrixStackIn.pushPose();
            FirstPersonRenderer renderer = Minecraft.getInstance().getItemInHandRenderer();
            Hand hand = MoreObjects.firstNonNull(Minecraft.getInstance().player.swingingArm, Hand.MAIN_HAND);
            float f = Minecraft.getInstance().player.getAttackAnim(event.getPartialTicks());
            float f1 = MathHelper.lerp(event.getPartialTicks(), Minecraft.getInstance().player.xRotO, Minecraft.getInstance().player.xRot);
            float f5 = -0.4F * MathHelper.sin(MathHelper.sqrt(f) * (float) Math.PI);
            float f6 = 0.2F * MathHelper.sin(MathHelper.sqrt(f) * ((float) Math.PI * 2F));
            float f10 = -0.2F * MathHelper.sin(f * (float) Math.PI);
            HandSide handside = hand == Hand.MAIN_HAND ? Minecraft.getInstance().player.getMainArm() : Minecraft.getInstance().player.getMainArm().getOpposite();
            boolean flag3 = handside == HandSide.RIGHT;
            int l = flag3 ? 1 : -1;
            matrixStackIn.translate((float) l * f5, f6, f10);
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

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderNameplate(RenderNameplateEvent event) {
        if (Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle && event.getEntity() == Minecraft.getInstance().player) {
            if (Minecraft.getInstance().hasSingleplayerServer()) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderWorldLastEvent(RenderWorldLastEvent event) {
        AMItemstackRenderer.incrementTick();
        if (!AMConfig.shadersCompat) {
            if (Minecraft.getInstance().player.hasEffect(AMEffectRegistry.LAVA_VISION)) {
                if (!previousLavaVision) {
                    RenderType lavaType = RenderType.translucent();
                    RenderTypeLookup.setRenderLayer(Fluids.LAVA, lavaType);
                    RenderTypeLookup.setRenderLayer(Fluids.FLOWING_LAVA, lavaType);
                    previousFluidRenderer = Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer;
                    Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer = new LavaVisionFluidRenderer();
                    updateAllChunks();
                }
            } else {
                if (previousLavaVision) {
                    if (previousFluidRenderer != null) {
                        RenderType lavaType = RenderType.solid();
                        RenderTypeLookup.setRenderLayer(Fluids.LAVA, lavaType);
                        RenderTypeLookup.setRenderLayer(Fluids.FLOWING_LAVA, lavaType);
                        Minecraft.getInstance().getBlockRenderer().liquidBlockRenderer = previousFluidRenderer;
                    }
                    updateAllChunks();
                }
            }
            previousLavaVision = Minecraft.getInstance().player.hasEffect(AMEffectRegistry.LAVA_VISION);
            if(AMConfig.clingingFlipEffect){
                if (Minecraft.getInstance().player.hasEffect(AMEffectRegistry.CLINGING) && Minecraft.getInstance().player.getEyeHeight() < Minecraft.getInstance().player.getBbHeight() * 0.45F) {
                    Minecraft.getInstance().gameRenderer.loadEffect(new ResourceLocation("shaders/post/flip.json"));
                } else if (Minecraft.getInstance().gameRenderer.currentEffect() != null && Minecraft.getInstance().gameRenderer.currentEffect().getName().equals("minecraft:shaders/post/flip.json")) {
                    Minecraft.getInstance().gameRenderer.shutdownEffect();
                }
            }
        }
        if (Minecraft.getInstance().getCameraEntity() instanceof EntityBaldEagle) {
            EntityBaldEagle eagle = (EntityBaldEagle) Minecraft.getInstance().getCameraEntity();
            ClientPlayerEntity playerEntity = Minecraft.getInstance().player;

            if (((EntityBaldEagle) Minecraft.getInstance().getCameraEntity()).shouldHoodedReturn() || eagle.removed) {
                Minecraft.getInstance().setCameraEntity(playerEntity);
                Minecraft.getInstance().options.setCameraType(PointOfView.values()[AlexsMobs.PROXY.getPreviousPOV()]);
            } else {
                float rotX = MathHelper.wrapDegrees(playerEntity.yRot + playerEntity.yHeadRot);
                float rotY = playerEntity.xRot;
                Entity over = null;
                if (Minecraft.getInstance().hitResult instanceof EntityRayTraceResult) {
                    over = ((EntityRayTraceResult) Minecraft.getInstance().hitResult).getEntity();
                } else {
                    Minecraft.getInstance().hitResult = null;
                }
                boolean loadChunks = playerEntity.level.getDayTime() % 10 == 0;
                ((EntityBaldEagle) Minecraft.getInstance().getCameraEntity()).directFromPlayer(rotX, rotY, false, over);
                AlexsMobs.NETWORK_WRAPPER.sendToServer(new MessageUpdateEagleControls(Minecraft.getInstance().getCameraEntity().getId(), rotX, rotY, loadChunks, over == null ? -1 : over.getId()));
            }
        }
    }

    private void updateAllChunks() {
        if (Minecraft.getInstance().levelRenderer.viewArea != null) {
            int length = Minecraft.getInstance().levelRenderer.viewArea.chunks.length;
            for (int i = 0; i < length; i++) {
                Minecraft.getInstance().levelRenderer.viewArea.chunks[i].dirty = true;
            }
        }
    }
}
