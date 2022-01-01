package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.passive.AnimalEntity;

public class AnimalAIHurtByTargetNotBaby extends HurtByTargetGoal {

    private AnimalEntity animal;

    public AnimalAIHurtByTargetNotBaby(AnimalEntity creatureIn, Class<?>... excludeReinforcementTypes) {
        super(creatureIn, excludeReinforcementTypes);
        this.animal = creatureIn;
    }

    public void start() {
        super.start();
        if (animal.isBaby()) {
            this.alertOthers();
            this.stop();
        }

    }

    protected void alertOther(MobEntity mobIn, LivingEntity targetIn) {
        if (!mobIn.isBaby()) {
            super.alertOther(mobIn, targetIn);
        }
    }
}
