package com.github.alexthe666.alexsmobs.client.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.ItemRenderer;
import net.minecraft.client.renderer.RenderState;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Util;

import net.minecraft.client.renderer.RenderState.TextureState;

public class AMRenderTypes extends RenderType {

    protected static final RenderState.TexturingState RAINBOW_GLINT_TEXTURING = new RenderState.TexturingState("rainbow_glint_texturing", () -> {
        setupRainbowRendering(0F);
    }, () -> {
        RenderSystem.matrixMode(5890);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(5888);
    });


    protected static final RenderState.TexturingState FRILLED_SHARK_TEETH_GLINT_TEXTURING = new RenderState.TexturingState("frilled_shark_teeth_glint_texturing", () -> {
        setupFrilledSharkGlintTexturing(8.0F);
    }, () -> {
        RenderSystem.matrixMode(5890);
        RenderSystem.popMatrix();
        RenderSystem.matrixMode(5888);

    });


    public static final RenderType RAINBOW_GLINT = create("rainbow_glint", DefaultVertexFormats.POSITION_COLOR_TEX_LIGHTMAP, 7, 256, RenderType.State.builder().setTextureState(new RenderState.TextureState(new ResourceLocation("alexsmobs:textures/entity/rainbow_glint.png"), true, false)).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(RAINBOW_GLINT_TEXTURING).setDiffuseLightingState(DIFFUSE_LIGHTING).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setShadeModelState(SMOOTH_SHADE).createCompositeState(false));

    public static final RenderType FRILLED_SHARK_TEETH_GLINT =  create("frilled_shark_glint", DefaultVertexFormats.POSITION_TEX, 7, 256, RenderType.State.builder().setTextureState(new TextureState(ItemRenderer.ENCHANT_GLINT_LOCATION, true, false)).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setDepthTestState(EQUAL_DEPTH_TEST).setTransparencyState(GLINT_TRANSPARENCY).setTexturingState(ENTITY_GLINT_TEXTURING).setLayeringState(VIEW_OFFSET_Z_LAYERING).createCompositeState(false));

    protected static final RenderState.TransparencyState WORM_TRANSPARANCY = new RenderState.TransparencyState("translucent_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderState.TransparencyState MIMICUBE_TRANSPARANCY = new RenderState.TransparencyState("mimicube_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    protected static final RenderState.TransparencyState GHOST_TRANSPARANCY = new RenderState.TransparencyState("translucent_ghost_transparency", () -> {
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
    }, () -> {
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
    });

    public AMRenderTypes(String p_i225992_1_, VertexFormat p_i225992_2_, int p_i225992_3_, int p_i225992_4_, boolean p_i225992_5_, boolean p_i225992_6_, Runnable p_i225992_7_, Runnable p_i225992_8_) {
        super(p_i225992_1_, p_i225992_2_, p_i225992_3_, p_i225992_4_, p_i225992_5_, p_i225992_6_, p_i225992_7_, p_i225992_8_);
    }

    public static RenderType getTransparentMimicube(ResourceLocation texture) {
        RenderType.State lvt_1_1_ = RenderType.State.builder().setTextureState(new TextureState(texture, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setOutputState(TRANSLUCENT_TARGET).setDiffuseLightingState(DIFFUSE_LIGHTING).setAlphaState(DEFAULT_ALPHA).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).setWriteMaskState(RenderState.COLOR_DEPTH_WRITE).setCullState(RenderState.NO_CULL).createCompositeState(true);
        return create("mimicube", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, lvt_1_1_);
    }

    public static RenderType getEyesFlickering(ResourceLocation p_228652_0_, float lightLevel) {
        RenderState.TextureState lvt_1_1_ = new RenderState.TextureState(p_228652_0_, false, false);
        return create("eye_flickering", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setAlphaState(DEFAULT_ALPHA).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    }

    public static RenderType getFullBright(ResourceLocation p_228652_0_) {
        RenderState.TextureState lvt_1_1_ = new RenderState.TextureState(p_228652_0_, false, false);
        return create("full_bright", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setAlphaState(DEFAULT_ALPHA).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    }


    public static RenderType getFrilledSharkTeeth(ResourceLocation p_228652_0_) {
        RenderState.TextureState lvt_1_1_ = new RenderState.TextureState(p_228652_0_, false, false);
        return create("sharkteeth", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setDiffuseLightingState(NO_DIFFUSE_LIGHTING).setTransparencyState(NO_TRANSPARENCY).setAlphaState(DEFAULT_ALPHA).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(false));
    }

    public static RenderType getEyesNoCull(ResourceLocation p_228652_0_) {
        TextureState lvt_1_1_ = new TextureState(p_228652_0_, false, false);
        return create("eyes_no_cull", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setTransparencyState(ADDITIVE_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).setCullState(NO_CULL).setFogState(BLACK_FOG).createCompositeState(false));
    }

    public static RenderType getSpectreBones(ResourceLocation p_228652_0_) {
        TextureState lvt_1_1_ = new TextureState(p_228652_0_, false, false);
        return create("spectre_bones", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setTransparencyState(ADDITIVE_TRANSPARENCY).setWriteMaskState(COLOR_WRITE).setDiffuseLightingState(RenderState.NO_DIFFUSE_LIGHTING).setFogState(BLACK_FOG).setCullState(NO_CULL).createCompositeState(false));
    }


    public static RenderType getGhost(ResourceLocation p_228652_0_) {
        TextureState lvt_1_1_ = new TextureState(p_228652_0_, false, false);
        return create("ghost_am", DefaultVertexFormats.NEW_ENTITY, 7, 262144, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setWriteMaskState(COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).setAlphaState(DEFAULT_ALPHA).setDiffuseLightingState(RenderState.NO_DIFFUSE_LIGHTING).setLightmapState(NO_LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(GHOST_TRANSPARANCY).setFogState(FOG).setCullState(RenderState.NO_CULL).createCompositeState(true));
    }

    public static RenderType getSnappingTurtleMoss(ResourceLocation LocationIn, float alphaIn) {
        RenderType.State rendertype$state = RenderType.State.builder().setTextureState(new RenderState.TextureState(LocationIn, false, false)).setTransparencyState(TRANSLUCENT_TRANSPARENCY).setDiffuseLightingState(DIFFUSE_LIGHTING).setAlphaState(new RenderState.AlphaState(alphaIn)).setCullState(NO_CULL).setLightmapState(LIGHTMAP).setOverlayState(OVERLAY).createCompositeState(true);
        return create("snapping_turtle_moss", DefaultVertexFormats.NEW_ENTITY, 7, 256, true, true, rendertype$state);
    }


    private static void setupRainbowRendering(float scaleIn) {
        RenderSystem.matrixMode(5890);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        long i = Util.getMillis() * 8L;
        float f = (float)(i % 110000L) / 110000.0F;
        float f1 = (float)(i % 10000L) / 10000.0F;
        RenderSystem.translatef(0, f1, 0.0F);
        RenderSystem.rotatef(10.0F, 0.0F, 0.0F, 1.0F);
        RenderSystem.scalef(scaleIn, scaleIn, scaleIn);
        RenderSystem.matrixMode(5888);
    }

    private static void setupFrilledSharkGlintTexturing(float scaleIn) {
        RenderSystem.matrixMode(5890);
        RenderSystem.pushMatrix();
        RenderSystem.loadIdentity();
        long i = Util.getMillis() * 64L;
        float f1 = (float)(i % 110000) / 110000.0F;
        RenderSystem.translatef(f1, f1, 0.0F);
        RenderSystem.rotatef(0, 0.0F, 0.0F, 1.0F);
        RenderSystem.scalef(scaleIn, scaleIn, scaleIn);
        RenderSystem.matrixMode(5888);
    }

    public static RenderType getEyesAlphaEnabled(ResourceLocation locationIn) {
        RenderState.TextureState renderstate$texturestate = new RenderState.TextureState(locationIn, false, false);
        return create("eyes", DefaultVertexFormats.NEW_ENTITY, 7, 256, false, true, RenderType.State.builder().setTextureState(renderstate$texturestate).setTransparencyState(WORM_TRANSPARANCY).setWriteMaskState(COLOR_WRITE).setCullState(RenderState.NO_CULL).setFogState(BLACK_FOG).createCompositeState(false));
    }

    public static RenderType getMungusBeam(ResourceLocation guardianBeamTexture) {
        TextureState lvt_1_1_ = new TextureState(guardianBeamTexture, false, false);
        return create("mungus_beam", DefaultVertexFormats.NEW_ENTITY, 7, 262144, false, true, RenderType.State.builder().setTextureState(lvt_1_1_).setWriteMaskState(COLOR_DEPTH_WRITE).setDepthTestState(LEQUAL_DEPTH_TEST).setAlphaState(DEFAULT_ALPHA).setDiffuseLightingState(RenderState.NO_DIFFUSE_LIGHTING).setLightmapState(NO_LIGHTMAP).setOverlayState(OVERLAY).setTransparencyState(GHOST_TRANSPARANCY).setFogState(FOG).setCullState(RenderState.NO_CULL).createCompositeState(true));
    }
}
