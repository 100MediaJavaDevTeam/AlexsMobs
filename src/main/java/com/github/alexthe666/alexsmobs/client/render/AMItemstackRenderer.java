package com.github.alexthe666.alexsmobs.client.render;

import com.github.alexthe666.alexsmobs.client.model.ModelMysteriousWorm;
import com.github.alexthe666.alexsmobs.client.model.ModelShieldOfTheDeep;
import com.github.alexthe666.alexsmobs.entity.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.ItemTabIcon;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.datafixers.util.Pair;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHelper;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererManager;
import net.minecraft.client.renderer.model.ItemCameraTransforms;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.tileentity.ItemStackTileEntityRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.DyeColor;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY;

public class AMItemstackRenderer extends ItemStackTileEntityRenderer {

    private static List<Pair<EntityType, Float>> MOB_ICONS = Util.make(Lists.newArrayList(), (list) -> {
        list.add(new Pair<>(AMEntityRegistry.GRIZZLY_BEAR, 0.6F));
        list.add(new Pair<>(AMEntityRegistry.ROADRUNNER, 0.8F));
        list.add(new Pair<>(AMEntityRegistry.BONE_SERPENT, 0.55F));
        list.add(new Pair<>(AMEntityRegistry.GAZELLE, 0.6F));
        list.add(new Pair<>(AMEntityRegistry.CROCODILE, 0.3F));
        list.add(new Pair<>(AMEntityRegistry.FLY, 1.3F));
        list.add(new Pair<>(AMEntityRegistry.HUMMINGBIRD, 1.5F));
        list.add(new Pair<>(AMEntityRegistry.ORCA, 0.2F));
        list.add(new Pair<>(AMEntityRegistry.SUNBIRD, 0.3F));
        list.add(new Pair<>(AMEntityRegistry.GORILLA, 0.85F));
        list.add(new Pair<>(AMEntityRegistry.CRIMSON_MOSQUITO, 0.6F));
        list.add(new Pair<>(AMEntityRegistry.RATTLESNAKE, 0.6F));
        list.add(new Pair<>(AMEntityRegistry.ENDERGRADE, 0.8F));
        list.add(new Pair<>(AMEntityRegistry.HAMMERHEAD_SHARK, 0.5F));
        list.add(new Pair<>(AMEntityRegistry.LOBSTER, 0.85F));
        list.add(new Pair<>(AMEntityRegistry.KOMODO_DRAGON, 0.5F));
        list.add(new Pair<>(AMEntityRegistry.CAPUCHIN_MONKEY, 0.85F));
        list.add(new Pair<>(AMEntityRegistry.CENTIPEDE_HEAD, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.WARPED_TOAD, 0.6F));
        list.add(new Pair<>(AMEntityRegistry.MOOSE, 0.5F));
        list.add(new Pair<>(AMEntityRegistry.MIMICUBE, 0.95F));
        list.add(new Pair<>(AMEntityRegistry.RACCOON, 0.8F));
        list.add(new Pair<>(AMEntityRegistry.BLOBFISH, 1F));
        list.add(new Pair<>(AMEntityRegistry.SEAL, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.COCKROACH, 1F));
        list.add(new Pair<>(AMEntityRegistry.SHOEBILL, 0.8F));
        list.add(new Pair<>(AMEntityRegistry.ELEPHANT, 0.45F));
        list.add(new Pair<>(AMEntityRegistry.SOUL_VULTURE, 0.8F));
        list.add(new Pair<>(AMEntityRegistry.SNOW_LEOPARD, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.SPECTRE, 0.3F));
        list.add(new Pair<>(AMEntityRegistry.CROW, 1.3F));
        list.add(new Pair<>(AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.MUNGUS, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.MANTIS_SHRIMP, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.GUSTER, 0.55F));
        list.add(new Pair<>(AMEntityRegistry.WARPED_MOSCO, 0.45F));
        list.add(new Pair<>(AMEntityRegistry.STRADDLER, 0.38F));
        list.add(new Pair<>(AMEntityRegistry.STRADPOLE, 0.9F));
        list.add(new Pair<>(AMEntityRegistry.EMU, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.PLATYPUS, 1F));
        list.add(new Pair<>(AMEntityRegistry.DROPBEAR, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.TASMANIAN_DEVIL, 1.2F));
        list.add(new Pair<>(AMEntityRegistry.KANGAROO, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.CACHALOT_WHALE, 0.1F));
        list.add(new Pair<>(AMEntityRegistry.LEAFCUTTER_ANT, 1.2F));
        list.add(new Pair<>(AMEntityRegistry.ENDERIOPHAGE, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.BALD_EAGLE, 0.85F));
        list.add(new Pair<>(AMEntityRegistry.TIGER, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.TARANTULA_HAWK, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.VOID_WORM, 0.3F));
        list.add(new Pair<>(AMEntityRegistry.FRILLED_SHARK, 0.65F));
        list.add(new Pair<>(AMEntityRegistry.MIMIC_OCTOPUS, 0.7F));
        list.add(new Pair<>(AMEntityRegistry.SEAGULL, 1.2F));
    });
    public static int ticksExisted = 0;
    private static final ModelShieldOfTheDeep SHIELD_OF_THE_DEEP_MODEL = new ModelShieldOfTheDeep();
    private static final ResourceLocation SHIELD_OF_THE_DEEP_TEXTURE = new ResourceLocation("alexsmobs:textures/armor/shield_of_the_deep.png");
    private static final ModelMysteriousWorm MYTERIOUS_WORM_MODEL = new ModelMysteriousWorm();
    private static final ResourceLocation MYTERIOUS_WORM_TEXTURE = new ResourceLocation("alexsmobs:textures/item/mysterious_worm_model.png");
    private Map<String, Entity> renderedEntites = new HashMap();

    public static void incrementTick() {
        ticksExisted++;
    }

    private static float getScaleFor(EntityType type) {
        for (Pair<EntityType, Float> pair : MOB_ICONS) {
            if (pair.getFirst() == type) {
                return pair.getSecond();
            }
        }
        return 1.0F;
    }

    public static void drawEntityOnScreen(MatrixStack matrixstack, int posX, int posY, float scale, boolean follow, double xRot, double yRot, double zRot, float mouseX, float mouseY, Entity entity) {
        float f = (float) Math.atan(-mouseX / 40.0F);
        float f1 = (float) Math.atan(mouseY / 40.0F);

        matrixstack.scale(scale, scale, scale);
        entity.setOnGround(false);
        float partialTicks = Minecraft.getInstance().getFrameTime();
        Quaternion quaternion = Vector3f.ZP.rotationDegrees(180.0F);
        Quaternion quaternion1 = Vector3f.XP.rotationDegrees(20.0F);
        float partialTicksForRender = Minecraft.getInstance().isPaused() || entity instanceof EntityMimicOctopus ? 0 : partialTicks;
        int tick = Minecraft.getInstance().player.tickCount;
        if(Minecraft.getInstance().isPaused()){
            tick = ticksExisted;
        }
        if (follow) {
            float yaw = f * 45.0F;
            entity.yRot = yaw;
            entity.tickCount = tick;
            if (entity instanceof LivingEntity) {
                ((LivingEntity) entity).yBodyRot = yaw;
                ((LivingEntity) entity).yBodyRotO = yaw;
                ((LivingEntity) entity).yHeadRot = yaw;
                ((LivingEntity) entity).yHeadRotO = yaw;
            }

            quaternion1 = Vector3f.XP.rotationDegrees(f1 * 20.0F);
            quaternion.mul(quaternion1);
        }

        matrixstack.mulPose(quaternion);
        matrixstack.mulPose(Vector3f.XP.rotationDegrees((float) (-xRot)));
        matrixstack.mulPose(Vector3f.YP.rotationDegrees((float) yRot));
        matrixstack.mulPose(Vector3f.ZP.rotationDegrees((float) zRot));
        EntityRendererManager entityrenderermanager = Minecraft.getInstance().getEntityRenderDispatcher();
        quaternion1.conj();
        entityrenderermanager.overrideCameraOrientation(quaternion1);
        entityrenderermanager.setRenderShadow(false);
        IRenderTypeBuffer.Impl irendertypebuffer$impl = Minecraft.getInstance().renderBuffers().bufferSource();
        RenderSystem.runAsFancy(() -> {
            entityrenderermanager.render(entity, 0.0D, 0.0D, 0.0D, f, partialTicksForRender, matrixstack, irendertypebuffer$impl, 15728880);
        });
        irendertypebuffer$impl.endBatch();
        entityrenderermanager.setRenderShadow(true);
        entity.yRot = 0.0F;
        entity.xRot = 0.0F;
        if (entity instanceof LivingEntity) {
            ((LivingEntity) entity).yBodyRot = 0.0F;
            ((LivingEntity) entity).yHeadRotO = 0.0F;
            ((LivingEntity) entity).yHeadRot = 0.0F;
        }
    }

    @Override
    public void renderByItem(ItemStack itemStackIn, ItemCameraTransforms.TransformType p_239207_2_, MatrixStack matrixStackIn, IRenderTypeBuffer bufferIn, int combinedLightIn, int combinedOverlayIn) {
        if(itemStackIn.getItem() == AMItemRegistry.SHIELD_OF_THE_DEEP){
            matrixStackIn.pushPose();
            matrixStackIn.translate(0.4F, -0.75F, 0.5F);
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-180));
            SHIELD_OF_THE_DEEP_MODEL.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.entityCutoutNoCull(SHIELD_OF_THE_DEEP_TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.popPose();
        }
        if(itemStackIn.getItem() == AMItemRegistry.MYSTERIOUS_WORM){
            matrixStackIn.pushPose();
            matrixStackIn.translate(0, -2F, 0);
            matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(-180));
            MYTERIOUS_WORM_MODEL.animateStack(itemStackIn);
            MYTERIOUS_WORM_MODEL.renderToBuffer(matrixStackIn, bufferIn.getBuffer(RenderType.itemEntityTranslucentCull(MYTERIOUS_WORM_TEXTURE)), combinedLightIn, combinedOverlayIn, 1.0F, 1.0F, 1.0F, 1.0F);
            matrixStackIn.popPose();
        }
        if(itemStackIn.getItem() == AMItemRegistry.FALCONRY_GLOVE){
            matrixStackIn.translate(0.5F, 0.5f, 0.5f);
            if(p_239207_2_ == ItemCameraTransforms.TransformType.THIRD_PERSON_LEFT_HAND || p_239207_2_ == ItemCameraTransforms.TransformType.THIRD_PERSON_RIGHT_HAND || p_239207_2_ == ItemCameraTransforms.TransformType.FIRST_PERSON_RIGHT_HAND || p_239207_2_ == ItemCameraTransforms.TransformType.FIRST_PERSON_LEFT_HAND){
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(AMItemRegistry.FALCONRY_GLOVE_HAND), p_239207_2_, combinedLightIn, combinedOverlayIn, matrixStackIn, bufferIn);
            }else{
                Minecraft.getInstance().getItemRenderer().renderStatic(new ItemStack(AMItemRegistry.FALCONRY_GLOVE_INVENTORY), p_239207_2_, 240, combinedOverlayIn, matrixStackIn, bufferIn);
            }
        }
        if (itemStackIn.getItem() == AMItemRegistry.TAB_ICON) {
            Entity fakeEntity = null;
            int entityIndex = (Minecraft.getInstance().player.tickCount / 40) % (MOB_ICONS.size());
            float scale = 1.0F;
            int flags = 0;
            if (ItemTabIcon.hasCustomEntityDisplay(itemStackIn)) {
                flags = itemStackIn.getTag().getInt("DisplayMobFlags");
                String index = ItemTabIcon.getCustomDisplayEntityString(itemStackIn);
                EntityType local = ItemTabIcon.getEntityType(itemStackIn.getTag());
                scale = getScaleFor(local);
                if(itemStackIn.getTag().getFloat("DisplayMobScale") > 0){
                    scale = itemStackIn.getTag().getFloat("DisplayMobScale");
                }
                if (this.renderedEntites.get(index) == null) {
                    Entity entity = local.create(Minecraft.getInstance().level);
                    if (entity instanceof EntityBlobfish) {
                        ((EntityBlobfish) entity).setDepressurized(true);
                    }
                    fakeEntity = this.renderedEntites.putIfAbsent(local.getDescriptionId(), entity);
                } else {
                    fakeEntity = this.renderedEntites.get(local.getDescriptionId());
                }
            } else {
                EntityType type = MOB_ICONS.get(entityIndex).getFirst();
                scale = MOB_ICONS.get(entityIndex).getSecond();
                if (type != null) {
                    if (this.renderedEntites.get(type.getDescriptionId()) == null) {
                        Entity entity = type.create(Minecraft.getInstance().level);
                        if (entity instanceof EntityBlobfish) {
                            ((EntityBlobfish) entity).setDepressurized(true);
                        }
                        fakeEntity = this.renderedEntites.putIfAbsent(type.getDescriptionId(), entity);
                    } else {
                        fakeEntity = this.renderedEntites.get(type.getDescriptionId());
                    }
                }
            }
            if (fakeEntity instanceof EntityCockroach) {
                if (flags == 99) {
                    matrixStackIn.translate(0, 0.25F, 0);
                    matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(-80));
                    ((EntityCockroach) fakeEntity).setMaracas(true);
                } else {
                    ((EntityCockroach) fakeEntity).setMaracas(false);
                }
            }
            if (fakeEntity instanceof EntityElephant) {
                if (flags == 99) {
                    ((EntityElephant) fakeEntity).setTusked(true);
                    ((EntityElephant) fakeEntity).setColor(null);
                } else if (flags == 98) {
                    ((EntityElephant) fakeEntity).setTusked(false);
                    ((EntityElephant) fakeEntity).setColor(DyeColor.BROWN);
                } else {
                    ((EntityElephant) fakeEntity).setTusked(false);
                    ((EntityElephant) fakeEntity).setColor(null);
                }
            }
            if (fakeEntity instanceof EntityBaldEagle) {
                if (flags == 98) {
                    ((EntityBaldEagle) fakeEntity).setCap(true);
                } else {
                    ((EntityBaldEagle) fakeEntity).setCap(false);
                }
            }
            if(fakeEntity instanceof EntityVoidWorm){
                matrixStackIn.translate(0, 0.5F, 0);
            }
            if(fakeEntity instanceof EntityMimicOctopus){
                matrixStackIn.translate(0, 0.5F, 0);
            }
            if (fakeEntity != null) {
                MouseHelper mouseHelper = Minecraft.getInstance().mouseHandler;
                double mouseX = (mouseHelper.xpos() * (double) Minecraft.getInstance().getWindow().getGuiScaledWidth()) / (double) Minecraft.getInstance().getWindow().getScreenWidth();
                double mouseY = mouseHelper.ypos() * (double) Minecraft.getInstance().getWindow().getGuiScaledHeight() / (double) Minecraft.getInstance().getWindow().getScreenHeight();
                matrixStackIn.translate(0.5F, 0F, 0);
                matrixStackIn.mulPose(Vector3f.XP.rotationDegrees(180F));
                matrixStackIn.mulPose(Vector3f.YP.rotationDegrees(180F));
                if (p_239207_2_ != ItemCameraTransforms.TransformType.GUI) {
                    mouseX = 0;
                    mouseY = 0;
                }
                drawEntityOnScreen(matrixStackIn, 0, 0, scale, true, 0, -45, 0, (float) mouseX, (float) mouseY, fakeEntity);
            }
        }
    }

}
