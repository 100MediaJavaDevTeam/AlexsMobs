package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.AMEntityRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityEnderiophage;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;
import net.minecraft.util.DamageSource;

public class EffectEnderFlu extends Effect {

    private int lastDuration = -1;

    public EffectEnderFlu() {
        super(EffectType.HARMFUL, 0X6836AA);
        this.setRegistryName(AlexsMobs.MODID, "ender_flu");
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
        if (lastDuration == 1) {
            int phages = amplifier + 1;
            entity.hurt(DamageSource.MAGIC, phages * 10);
            for (int i = 0; i < phages; i++) {
                EntityEnderiophage phage = AMEntityRegistry.ENDERIOPHAGE.create(entity.level);
                phage.copyPosition(entity);
                phage.onSpawnFromEffect();
                phage.setSkinForDimension();
                if (!entity.level.isClientSide) {
                    phage.setStandardFleeTime();
                    entity.level.addFreshEntity(phage);
                }
            }
        }
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        lastDuration = duration;
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.ender_flu";
    }

}