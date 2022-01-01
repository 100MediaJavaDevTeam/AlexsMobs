package com.github.alexthe666.alexsmobs.client.particle;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.item.ItemDimensionalCarver;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.IVertexBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.*;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Quaternion;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public class ParticleInvertDig extends SimpleAnimatedParticle {

    private Entity creator;

    protected ParticleInvertDig(ClientWorld world, double x, double y, double z, IAnimatedSprite spriteWithAge, double creatorId) {
        super(world, x, y, z, spriteWithAge, 0);
        this.xd = 0;
        this.yd = 0;
        this.zd = 0;
        this.quadSize = 0.1F;
        this.alpha = 1F;
        this.lifetime = ItemDimensionalCarver.MAX_TIME;
        this.hasPhysics = false;
        this.creator = world.getEntity((int) creatorId);
    }

    public int getLightColor(float p_189214_1_) {
        return 240;
    }

    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        boolean live = false;
        this.quadSize = 0.1F + Math.min((age / (float)lifetime), 0.5F) * 0.5F;
        if (this.age++ >= lifetime || creator == null) {
            this.remove();
        } else {
            if (creator instanceof PlayerEntity) {
                ItemStack item = ((PlayerEntity) creator).getUseItem();
                if (item.getItem() == AMItemRegistry.DIMENSIONAL_CARVER) {
                    this.age = MathHelper.clamp(lifetime - ((PlayerEntity) creator).getUseItemRemainingTicks(), 0, lifetime);
                    live = true;
                }
            }
        }
        if(!live){
            this.remove();
        }
        this.setSpriteFromAge(this.sprites);
    }

    @OnlyIn(Dist.CLIENT)
    public static class Factory implements IParticleFactory<BasicParticleType> {
        private final IAnimatedSprite spriteSet;

        public Factory(IAnimatedSprite spriteSet) {
            this.spriteSet = spriteSet;
        }

        public Particle createParticle(BasicParticleType typeIn, ClientWorld worldIn, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            ParticleInvertDig heartparticle = new ParticleInvertDig(worldIn, x, y, z, this.spriteSet, xSpeed);
            heartparticle.setSpriteFromAge(this.spriteSet);
            return heartparticle;
        }
    }
}
