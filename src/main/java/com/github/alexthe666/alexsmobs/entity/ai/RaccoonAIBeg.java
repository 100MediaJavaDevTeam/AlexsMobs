package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.entity.EntityPredicate;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class RaccoonAIBeg extends Goal {
    private static final EntityPredicate ENTITY_PREDICATE = (new EntityPredicate()).range(32D).allowInvulnerable().allowSameTeam().allowNonAttackable().allowUnseeable();
    protected final EntityRaccoon raccoon;
    private final double speed;
    protected PlayerEntity closestPlayer;
    private int delayTemptCounter;
    private boolean isRunning;

    public RaccoonAIBeg(EntityRaccoon raccoon, double speed) {
        this.raccoon = raccoon;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        if (this.delayTemptCounter > 0) {
            --this.delayTemptCounter;
            return false;
        } else {
            if(!this.raccoon.getMainHandItem().isEmpty()){
                return false;
            }
            this.closestPlayer = this.raccoon.level.getNearestPlayer(ENTITY_PREDICATE, this.raccoon);
            if (this.closestPlayer == null) {
                return false;
            } else {
                boolean food =  EntityRaccoon.isRaccoonFood(this.closestPlayer.getMainHandItem()) || EntityRaccoon.isRaccoonFood(this.closestPlayer.getOffhandItem());
                return food;
            }
        }
    }


    public boolean canContinueToUse() {
        return this.raccoon.getMainHandItem().isEmpty() && this.canUse();
    }

    public void start() {
        this.isRunning = true;
    }

    public void stop() {
        this.closestPlayer = null;
        this.raccoon.getNavigation().stop();
        this.delayTemptCounter = 100;
        this.raccoon.setBegging(false);
        this.isRunning = false;
    }

    public void tick() {
        this.raccoon.getLookControl().setLookAt(this.closestPlayer, (float)(this.raccoon.getMaxHeadYRot() + 20), (float)this.raccoon.getMaxHeadXRot());
        if (this.raccoon.distanceToSqr(this.closestPlayer) < 12D) {
            this.raccoon.getNavigation().stop();
            this.raccoon.setBegging(true);
        } else {
            this.raccoon.getNavigation().moveTo(this.closestPlayer, this.speed);
        }

    }

    public boolean isRunning() {
        return this.isRunning;
    }
}
