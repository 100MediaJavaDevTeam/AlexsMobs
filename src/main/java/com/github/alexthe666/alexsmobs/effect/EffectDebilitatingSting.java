package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityTarantulaHawk;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IServerWorld;

public class EffectDebilitatingSting extends Effect {

    private int lastDuration = -1;

    protected EffectDebilitatingSting() {
        super(EffectType.NEUTRAL, 0XFFF385);
        this.addAttributeModifier(Attributes.MOVEMENT_SPEED, "7107DE5E-7CE8-4030-940E-514C1F160890", -1.0F, AttributeModifier.Operation.MULTIPLY_BASE);
        this.setRegistryName(AlexsMobs.MODID, "debilitating_sting");
    }

    public void removeAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeModifierManager attributeMapIn, int amplifier) {
        if (entityLivingBaseIn.getMobType() == CreatureAttribute.ARTHROPOD) {
            super.removeAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }

    public void addAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeModifierManager attributeMapIn, int amplifier) {
        if (entityLivingBaseIn.getMobType() == CreatureAttribute.ARTHROPOD) {
            super.addAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        }
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (entity.getMobType() != CreatureAttribute.ARTHROPOD) {
            if (entity.getHealth() > entity.getMaxHealth() * 0.5F) {
                entity.hurt(DamageSource.MAGIC, 1.0F);
            }
        } else {
            boolean suf = isEntityInsideOpaqueBlock(entity);
            if (suf) {
                entity.setDeltaMovement(Vector3d.ZERO);
                entity.noPhysics = true;
            }
            entity.setNoGravity(suf);
            entity.setJumping(false);
            if (!entity.isPassenger() && entity instanceof MobEntity && !(((MobEntity) entity).getMoveControl().getClass() == MovementController.class)) {
                entity.setDeltaMovement(new Vector3d(0, -1, 0));
            }
            if (lastDuration == 1) {
                entity.hurt(DamageSource.MAGIC, (amplifier + 1) * 30);
                if (amplifier > 0) {
                    BlockPos surface = entity.blockPosition();
                    while (!entity.level.isEmptyBlock(surface) && surface.getY() < 256) {
                        surface = surface.above();
                    }
                    EntityTarantulaHawk baby = AMEntityRegistry.TARANTULA_HAWK.create(entity.level);
                    baby.setBaby(true);
                    baby.setPos(entity.getX(), surface.getY() + 0.1F, entity.getZ());
                    if (!entity.level.isClientSide) {
                        baby.finalizeSpawn((IServerWorld) entity.level, entity.level.getCurrentDifficultyAt(entity.blockPosition()), SpawnReason.BREEDING, null, null);
                        entity.level.addFreshEntity(baby);
                    }
                }
                entity.setNoGravity(false);
                entity.noPhysics = false;
            }
        }
    }

    public boolean isEntityInsideOpaqueBlock(Entity entity) {
        float f = 0.1F;
        float f1 = entity.getDimensions(entity.getPose()).width * 0.8F;
        AxisAlignedBB axisalignedbb = AxisAlignedBB.ofSize(f1, 0.1F, f1).move(entity.getX(), entity.getEyeY(), entity.getZ());
        return entity.level.getBlockCollisions(entity, axisalignedbb, (p_241338_1_, p_241338_2_) -> {
            return p_241338_1_.isSuffocating(entity.level, p_241338_2_);
        }).findAny().isPresent();
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.debilitating_sting";
    }
}
