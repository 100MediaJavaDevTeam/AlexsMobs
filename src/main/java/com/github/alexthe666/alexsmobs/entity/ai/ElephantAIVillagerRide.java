package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;

public class ElephantAIVillagerRide  extends Goal {

    private EntityElephant elephant;
    private AbstractVillagerEntity villager;
    private double speed;

    public ElephantAIVillagerRide(EntityElephant dragon, double speed) {
        elephant = dragon;
        this.speed = speed;
    }

    @Override
    public boolean canUse() {
        if(elephant.getControllingVillager() != null){
           villager = elephant.getControllingVillager();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
    }

    @Override
    public void tick() {
        if(this.villager.getNavigation().isInProgress()){
            this.elephant.getNavigation().moveTo(this.villager.getNavigation().getPath(), 1.6D);
        }
    }
}
