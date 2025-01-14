package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;

public class BoneSerpentAIRandomSwimming extends RandomWalkingGoal {
    public BoneSerpentAIRandomSwimming(CreatureEntity creature, double speed, int chance) {
        super(creature, speed, chance, false);
    }

    public boolean canUse() {
        if (this.mob.isVehicle() || mob.getTarget() != null) {
            return false;
        } else {
            if (!this.forceTrigger) {
                if (this.mob.getRandom().nextInt(this.interval) != 0) {
                    return false;
                }
            }
            Vector3d vector3d = this.getPosition();
            if (vector3d == null) {
                return false;
            } else {
                this.wantedX = vector3d.x;
                this.wantedY = vector3d.y;
                this.wantedZ = vector3d.z;
                this.forceTrigger = false;
                return true;
            }
        }
    }

    @Nullable
    protected Vector3d getPosition() {
        if(this.mob.getRandom().nextFloat() < (this.mob.isInLava() ? 0.7F : 0.3F)){
            Vector3d vector3d = findSurfaceTarget(this.mob, 32, 16);
            if(vector3d != null){
                return vector3d;
            }
        }
        Vector3d vector3d = RandomPositionGenerator.getPos(this.mob, 32, 16);

        for(int i = 0; vector3d != null && !this.mob.level.getBlockState(new BlockPos(vector3d)).isPathfindable(this.mob.level, new BlockPos(vector3d), PathType.WATER) && i++ < 10; vector3d = RandomPositionGenerator.getPos(this.mob, 10, 7)) {
        }

        return vector3d;
    }

    private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
        BlockPos blockpos = pos.offset(dx * scale, 0, dz * scale);
        return (this.mob.level.getFluidState(blockpos).is(FluidTags.WATER) && !this.mob.level.getBlockState(blockpos).getMaterial().blocksMotion() || this.mob.level.getFluidState(blockpos).is(FluidTags.LAVA));
    }

    private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
        return this.mob.level.getBlockState(pos.offset(dx * scale, 1, dz * scale)).isAir() && this.mob.level.getBlockState(pos.offset(dx * scale, 2, dz * scale)).isAir();
    }

    private Vector3d findSurfaceTarget(CreatureEntity creature, int i, int i1) {
        Vector3d creaturePos = creature.position();
        BlockPos upPos = creature.blockPosition();
        while(creature.level.getFluidState(upPos).is(FluidTags.LAVA) || creature.level.getFluidState(upPos).is(FluidTags.WATER)){
            upPos = upPos.above();
        }
        if(isAirAbove(upPos.below(), 0, 0, 0) && canJumpTo(upPos.below(), 0, 0, 0)){
            return new Vector3d(upPos.getX() + 0.5F, upPos.getY() + 3.5F, upPos.getZ() + 0.5F);
        }
        return null;
    }
}
