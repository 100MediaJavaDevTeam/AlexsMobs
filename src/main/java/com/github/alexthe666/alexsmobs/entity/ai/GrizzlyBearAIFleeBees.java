package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.google.common.base.Predicate;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;

public class GrizzlyBearAIFleeBees extends Goal {
    private final double farSpeed;
    private final double nearSpeed;
    private final float avoidDistance;
    private final Predicate<BeeEntity> avoidTargetSelector;
    protected EntityGrizzlyBear entity;
    protected BeeEntity closestLivingEntity;
    private Path path;

    public GrizzlyBearAIFleeBees(EntityGrizzlyBear entityIn, float avoidDistanceIn, double farSpeedIn, double nearSpeedIn) {
        this.avoidTargetSelector = new Predicate<BeeEntity>() {
            public boolean apply(@Nullable BeeEntity p_apply_1_) {
                return p_apply_1_.isAlive() && GrizzlyBearAIFleeBees.this.entity.getSensing().canSee(p_apply_1_) && !GrizzlyBearAIFleeBees.this.entity.isAlliedTo(p_apply_1_) && p_apply_1_.getRemainingPersistentAngerTime() > 0;
            }
        };
        this.entity = entityIn;
        this.avoidDistance = avoidDistanceIn;
        this.farSpeed = farSpeedIn;
        this.nearSpeed = nearSpeedIn;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
    }

    public boolean canUse() {
        if (this.entity.isTame()) {
            return false;
        }
        if(this.entity.isSitting() && !entity.forcedSit){
            this.entity.setOrderedToSit(false);
        }
        if(this.entity.isSitting()){
            return false;
        }
        List<BeeEntity> beeEntities = this.entity.level.getEntitiesOfClass(BeeEntity.class, this.entity.getBoundingBox().inflate((double) avoidDistance, 8.0D, (double) avoidDistance), this.avoidTargetSelector);
        if (beeEntities.isEmpty()) {
            return false;
        } else {
            this.closestLivingEntity = beeEntities.get(0);
            Vector3d vec3d = RandomPositionGenerator.getPosAvoid(this.entity, 16, 7, new Vector3d(this.closestLivingEntity.getX(), this.closestLivingEntity.getY(), this.closestLivingEntity.getZ()));
            if (vec3d == null) {
                return false;
            } else if (this.closestLivingEntity.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < this.closestLivingEntity.distanceToSqr(this.entity)) {
                return false;
            } else {
                this.path = entity.getNavigation().createPath(new BlockPos(vec3d.x, vec3d.y, vec3d.z), 0);
                return this.path != null;
            }
        }
    }

    public boolean canContinueToUse() {
        return !entity.getNavigation().isDone();
    }

    public void start() {
        entity.getNavigation().moveTo(this.path, farSpeed);
    }

    public void stop() {
        this.entity.getNavigation().stop();
        this.closestLivingEntity = null;
    }

    public void tick() {
        if(closestLivingEntity != null && closestLivingEntity.getRemainingPersistentAngerTime() <= 0){
            this.stop();
        }
        this.entity.getNavigation().setSpeedModifier(getRunSpeed());
    }

    public double getRunSpeed() {
        return 0.7F;
    }
}

