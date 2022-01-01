package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.AttributeModifierManager;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.Direction;
import net.minecraft.util.math.BlockPos;

public class EffectClinging extends Effect {

    public EffectClinging() {
        super(EffectType.BENEFICIAL, 0XBD4B4B);
        this.setRegistryName(AlexsMobs.MODID, "clinging");
    }

    private static BlockPos getPositionUnderneath(Entity e) {
        return new BlockPos(e.getX(), e.getBoundingBox().maxY + 1.51F, e.getZ());
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        entity.refreshDimensions();
        entity.setNoGravity(false);

        if (isUpsideDown(entity)) {
            entity.fallDistance = 0;
            if (!entity.isShiftKeyDown()) {
                if (!entity.horizontalCollision) {
                    entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.3F, 0));
                }
                entity.setDeltaMovement(entity.getDeltaMovement().multiply(0.998F, 1F, 0.998F));
            }
        }
    }

    public static boolean isUpsideDown(LivingEntity entity){
        BlockPos pos = getPositionUnderneath(entity);
        BlockState ground = entity.level.getBlockState(pos);
        return (entity.verticalCollision || ground.isFaceSturdy(entity.level, pos, Direction.DOWN)) && !entity.isOnGround();
    }
    public void removeAttributeModifiers(LivingEntity entityLivingBaseIn, AttributeModifierManager attributeMapIn, int amplifier) {
        super.removeAttributeModifiers(entityLivingBaseIn, attributeMapIn, amplifier);
        entityLivingBaseIn.refreshDimensions();
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.clinging";
    }

}