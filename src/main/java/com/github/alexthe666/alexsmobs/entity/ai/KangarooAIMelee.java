package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityKangaroo;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import java.util.Random;

public class KangarooAIMelee extends MeleeAttackGoal {

    private EntityKangaroo kangaroo;
    private BlockPos waterPos;
    private int waterCheckTick = 0;
    private int waterTimeout = 0;
    public KangarooAIMelee(EntityKangaroo kangaroo, double speedIn, boolean useLongMemory) {
        super(kangaroo, speedIn, useLongMemory);
        this.kangaroo = kangaroo;
    }

    public boolean canUse() {
        return super.canUse();
    }

    public void tick() {
        boolean dontSuper = false;
        LivingEntity target = kangaroo.getTarget();
        if (target != null) {
            if (target == kangaroo.getLastHurtByMob()) {
                if (target.distanceTo(kangaroo) < kangaroo.getBbWidth() + 1F && target.isInWater()) {
                    target.setDeltaMovement(target.getDeltaMovement().add(0, -0.09, 0));
                    target.setAirSupply(target.getAirSupply() - 30);
                }
                if (waterPos == null || !kangaroo.level.getFluidState(waterPos).is(FluidTags.WATER)) {
                    kangaroo.setVisualFlag(0);
                    waterCheckTick++;
                    waterPos = generateWaterPos();
                } else {
                    kangaroo.setPathfindingMalus(PathNodeType.WATER, 0);
                    kangaroo.setPathfindingMalus(PathNodeType.WATER_BORDER, 0);
                    double localSpeed = MathHelper.clamp(kangaroo.distanceToSqr(waterPos.getX(), waterPos.getY(), waterPos.getZ()) * 0.5F, 1D, 2.3D);
                    kangaroo.getMoveControl().setWantedPosition(waterPos.getX(), waterPos.getY(), waterPos.getZ(), localSpeed);
                    if (kangaroo.isInWater()){
                        waterTimeout++;
                    }
                    if(waterTimeout < 1400){
                        dontSuper = true;
                        checkAndPerformAttack(target, kangaroo.distanceToSqr(target));
                    }
                    if (kangaroo.isInWater() || kangaroo.distanceToSqr(Vector3d.atCenterOf(waterPos)) < 10) {
                        kangaroo.totalMovingProgress = 0;
                    }
                    if(kangaroo.distanceToSqr(Vector3d.atCenterOf(waterPos)) > 10){
                        kangaroo.setVisualFlag(0);
                    }
                    if (kangaroo.distanceToSqr(Vector3d.atCenterOf(waterPos)) < 3 && kangaroo.isInWater()) {
                        kangaroo.setStanding(true);
                        kangaroo.maxStandTime = 100;
                        kangaroo.getLookControl().setLookAt(target, 360, 180);
                        kangaroo.setVisualFlag(1);
                    }
                }
            }else{

            }
            if (!dontSuper) {
                super.tick();
            }
        }
    }

    public boolean canContinueToUse() {
        return waterPos != null && this.kangaroo.getTarget() != null || super.canContinueToUse();
    }

    public void stop() {
        super.stop();
        waterCheckTick = 0;
        waterTimeout = 0;
        waterPos = null;
        kangaroo.setVisualFlag(0);
        kangaroo.setPathfindingMalus(PathNodeType.WATER, 8);
        kangaroo.setPathfindingMalus(PathNodeType.WATER_BORDER, 8);
    }

    public BlockPos generateWaterPos() {
        BlockPos blockpos = null;
        Random random = new Random();
        int range = 15;
        for (int i = 0; i < 15; i++) {
            BlockPos blockpos1 = this.kangaroo.blockPosition().offset(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.kangaroo.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1) {
                blockpos1 = blockpos1.below();
            }
            if (this.kangaroo.level.getFluidState(blockpos1).is(FluidTags.WATER)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    protected void checkAndPerformAttack(LivingEntity enemy, double distToEnemySqr) {
        double d0 = this.getAttackReachSqr(enemy) + 5D;
        if (distToEnemySqr <= d0) {
            if(kangaroo.isInWater()){
                float f1 = kangaroo.yRot * ((float)Math.PI / 180F);
                kangaroo.setDeltaMovement(kangaroo.getDeltaMovement().add((double)(-MathHelper.sin(f1) * 0.3F), 0.0D, (double)(MathHelper.cos(f1) * 0.3F)));
                enemy.knockback(1F, enemy.getX() - kangaroo.getX(), enemy.getZ() - kangaroo.getZ());

            }
            this.resetAttackCooldown();
            if(kangaroo.getAnimation() == IAnimatedEntity.NO_ANIMATION){
                if(kangaroo.getRandom().nextBoolean()){
                    kangaroo.setAnimation(EntityKangaroo.ANIMATION_KICK);
                }else{
                    if(!kangaroo.getMainHandItem().isEmpty()){
                        kangaroo.setAnimation(kangaroo.isLeftHanded() ? EntityKangaroo.ANIMATION_PUNCH_L : EntityKangaroo.ANIMATION_PUNCH_R);
                    }else{
                        kangaroo.setAnimation(kangaroo.getRandom().nextBoolean() ? EntityKangaroo.ANIMATION_PUNCH_R : EntityKangaroo.ANIMATION_PUNCH_L);
                    }
                }
            }
        }
    }

}