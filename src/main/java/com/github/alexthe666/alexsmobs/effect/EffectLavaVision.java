package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class EffectLavaVision extends Effect {

    public EffectLavaVision() {
        super(EffectType.BENEFICIAL, 0XFF6A00);
        this.setRegistryName(AlexsMobs.MODID, "lava_vision");

    }

    public void applyEffectTick(LivingEntity LivingEntityIn, int amplifier) {
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.lava_vision";
    }

}