package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.entity.LivingEntity;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class EffectBugPheromones extends Effect {

    public EffectBugPheromones() {
        super(EffectType.BENEFICIAL, 0X78464B);
        this.setRegistryName(AlexsMobs.MODID, "bug_pheromones");
    }

    public void applyEffectTick(LivingEntity entity, int amplifier) {
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.bug_pheromones";
    }

}