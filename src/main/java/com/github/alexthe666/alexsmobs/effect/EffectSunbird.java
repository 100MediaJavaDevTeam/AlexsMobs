package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.math.vector.Vector3d;

public class EffectSunbird extends Effect {
    public boolean curse;

    public EffectSunbird(boolean curse) {
        super(curse ? EffectType.HARMFUL : EffectType.BENEFICIAL, 0XFFEAB9);
        this.setRegistryName(AlexsMobs.MODID, curse ? "sunbird_curse" : "sunbird_blessing");
        this.curse = curse;
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (curse) {
            if (entity.isFallFlying()) {
                if (entity instanceof PlayerEntity) {
                    ((PlayerEntity) entity).stopFallFlying();
                }
            }
            boolean forceFall = false;
            if (entity instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) entity;
                if (!player.isCreative() || !player.abilities.flying) {
                    forceFall = true;
                }
            }
            if ((forceFall || !(entity instanceof PlayerEntity)) && !entity.isOnGround()) {
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, -0.2F, 0));
            }
        } else {
            entity.fallDistance = 0.0F;
            if (entity.isFallFlying()) {
                if (entity.xRot < -10) {
                    float pitchMulti = Math.abs(entity.xRot) / 90F;
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.02 + pitchMulti * 0.02, 0));
                }
            } else if (!entity.isOnGround() && !entity.isCrouching()) {
                Vector3d vector3d = entity.getDeltaMovement();
                if (vector3d.y < 0.0D) {
                    entity.setDeltaMovement(vector3d.multiply(1.0D, 0.6D, 1.0D));
                }
            }

        }
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return curse ? "alexsmobs.potion.sunbird_curse" : "alexsmobs.potion.sunbird_blessing";
    }

}