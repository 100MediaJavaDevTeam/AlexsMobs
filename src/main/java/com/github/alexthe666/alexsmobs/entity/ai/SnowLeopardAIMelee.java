package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntitySnowLeopard;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

public class SnowLeopardAIMelee extends Goal {
    private EntitySnowLeopard leopard;
    private LivingEntity target;
    private boolean secondPartOfLeap = false;
    private Vector3d leapPos = null;
    private boolean stalk = false;

    public SnowLeopardAIMelee(EntitySnowLeopard snowLeopard) {
        this.leopard = snowLeopard;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Nullable
    private static BlockPos getRandomDelta(Random p_226343_0_, int p_226343_1_, int p_226343_2_, int p_226343_3_, @Nullable Vector3d p_226343_4_, double p_226343_5_) {
        if (p_226343_4_ != null && p_226343_5_ < 3.141592653589793D) {
            double lvt_7_2_ = MathHelper.atan2(p_226343_4_.z, p_226343_4_.x) - 1.5707963705062866D;
            double lvt_9_2_ = lvt_7_2_ + (double) (2.0F * p_226343_0_.nextFloat() - 1.0F) * p_226343_5_;
            double lvt_11_1_ = Math.sqrt(p_226343_0_.nextDouble()) * (double) MathHelper.SQRT_OF_TWO * (double) p_226343_1_;
            double lvt_13_1_ = -lvt_11_1_ * Math.sin(lvt_9_2_);
            double lvt_15_1_ = lvt_11_1_ * Math.cos(lvt_9_2_);
            if (Math.abs(lvt_13_1_) <= (double) p_226343_1_ && Math.abs(lvt_15_1_) <= (double) p_226343_1_) {
                int lvt_17_1_ = p_226343_0_.nextInt(2 * p_226343_2_ + 1) - p_226343_2_ + p_226343_3_;
                return new BlockPos(lvt_13_1_, lvt_17_1_, lvt_15_1_);
            } else {
                return null;
            }
        } else {
            int lvt_7_1_ = p_226343_0_.nextInt(2 * p_226343_1_ + 1) - p_226343_1_;
            int lvt_8_1_ = p_226343_0_.nextInt(2 * p_226343_2_ + 1) - p_226343_2_ + p_226343_3_;
            int lvt_9_1_ = p_226343_0_.nextInt(2 * p_226343_1_ + 1) - p_226343_1_;
            return new BlockPos(lvt_7_1_, lvt_8_1_, lvt_9_1_);
        }
    }

    static BlockPos moveUpToAboveSolid(BlockPos p_226342_0_, int p_226342_1_, int p_226342_2_, Predicate<BlockPos> p_226342_3_) {
        if (p_226342_1_ < 0) {
            throw new IllegalArgumentException("aboveSolidAmount was " + p_226342_1_ + ", expected >= 0");
        } else if (!p_226342_3_.test(p_226342_0_)) {
            return p_226342_0_;
        } else {
            BlockPos lvt_4_1_;
            for (lvt_4_1_ = p_226342_0_.above(); lvt_4_1_.getY() < p_226342_2_ && p_226342_3_.test(lvt_4_1_); lvt_4_1_ = lvt_4_1_.above()) {
            }

            BlockPos lvt_5_1_;
            BlockPos lvt_6_1_;
            for (lvt_5_1_ = lvt_4_1_; lvt_5_1_.getY() < p_226342_2_ && lvt_5_1_.getY() - lvt_4_1_.getY() < p_226342_1_; lvt_5_1_ = lvt_6_1_) {
                lvt_6_1_ = lvt_5_1_.above();
                if (p_226342_3_.test(lvt_6_1_)) {
                    break;
                }
            }

            return lvt_5_1_;
        }
    }

    @Override
    public boolean canUse() {
        return leopard.getTarget() != null && !leopard.isSleeping() && !leopard.isSitting() && (leopard.getTarget().isAlive() || leopard.getTarget() instanceof PlayerEntity)&& !leopard.isBaby();
    }

    public void start() {
        target = leopard.getTarget();
        if(target instanceof PlayerEntity && leopard.getLastHurtByMob() != null && leopard.getLastHurtByMob() == target){
            stalk = this.leopard.distanceTo(target) > 10F;
        }else{
            stalk = this.leopard.distanceTo(target) > 4F;
        }
        secondPartOfLeap = false;
    }

    public void stop() {
        secondPartOfLeap = false;
        stalk = false;
        leapPos = null;
        this.leopard.setTackling(false);
        this.leopard.setSlSneaking(false);
    }

    public void tick() {
        if (stalk) {
            if (secondPartOfLeap) {
                leopard.lookAt(target, 180F, 10F);
                leopard.yBodyRot = leopard.yRot;
                if (leopard.isOnGround()) {
                    this.leopard.setSlSneaking(false);
                    this.leopard.setTackling(true);
                    Vector3d vector3d = this.leopard.getDeltaMovement();
                    Vector3d vector3d1 = new Vector3d(this.target.getX() - this.leopard.getX(), 0.0D, this.target.getZ() - this.leopard.getZ());
                    if (vector3d1.lengthSqr() > 1.0E-7D) {
                        vector3d1 = vector3d1.normalize().scale(0.9D).add(vector3d.scale(0.8D));
                    }
                    this.leopard.setDeltaMovement(vector3d1.x, vector3d1.y + 0.6F, vector3d1.z);
                }
                if (this.leopard.distanceTo(target) < 3F && this.leopard.canSee(target)) {
                    target.hurt(DamageSource.mobAttack(leopard), (float) (leopard.getAttribute(Attributes.ATTACK_DAMAGE).getValue() * 2.5F));
                    this.stalk = false;
                    this.secondPartOfLeap = false;
                }
            } else {
                if (leapPos == null || target.distanceToSqr(leapPos) > 250) {
                    Vector3d vector3d1 = calculateFarPoint(50);
                    if (vector3d1 != null) {
                        leapPos = vector3d1;
                    }
                } else {
                    this.leopard.setSlSneaking(true);
                    this.leopard.getNavigation().moveTo(leapPos.x, leapPos.y, leapPos.z, 1D);
                    if (this.leopard.distanceToSqr(leapPos.x, leapPos.y, leapPos.z) < 9) {
                        if (this.leopard.canSee(target)) {
                            secondPartOfLeap = true;
                            this.leopard.getNavigation().stop();
                        }
                    }
                }
            }
        } else {
            this.leopard.setSlSneaking(false);
            this.leopard.getNavigation().moveTo(target, 1D);
            if (this.leopard.distanceTo(target) < 3F) {
                if(leopard.getAnimation() == IAnimatedEntity.NO_ANIMATION){
                    leopard.setAnimation(leopard.getRandom().nextBoolean() ? EntitySnowLeopard.ANIMATION_ATTACK_R : EntitySnowLeopard.ANIMATION_ATTACK_L);
                }else if(this.leopard.getAnimationTick() == 5){
                    leopard.doHurtTarget(target);
                }
            }
        }
    }

    private Vector3d calculateFarPoint(double dist){
        Vector3d highest = null;
        for(int i = 0; i < 10; i++){
            Vector3d vector3d1 = calculateVantagePoint(target, 8, 3, 1, target.position().subtract(leopard.getX(), leopard.getY(), leopard.getZ()), false, 1.5707963705062866D, leopard::getWalkTargetValue, false, 0, 0, true);
            if(vector3d1 != null && target.distanceToSqr(vector3d1) > dist && (highest == null || highest.y() < vector3d1.y)){
                highest = vector3d1;
            }
        }
        return highest;
    }
    @Nullable
    private Vector3d calculateVantagePoint(LivingEntity creature, int xz, int y, int p_226339_3_, @Nullable Vector3d p_226339_4_, boolean p_226339_5_, double p_226339_6_, ToDoubleFunction<BlockPos> p_226339_8_, boolean p_226339_9_, int p_226339_10_, int p_226339_11_, boolean p_226339_12_) {
        PathNavigator lvt_13_1_ = leopard.getNavigation();
        Random lvt_14_1_ = creature.getRandom();
        boolean lvt_15_2_;
        if (leopard.hasRestriction()) {
            lvt_15_2_ = leopard.getRestrictCenter().closerThan(creature.position(), (double) (leopard.getRestrictRadius() + (float) xz) + 1.0D);
        } else {
            lvt_15_2_ = false;
        }

        boolean lvt_16_1_ = false;
        double lvt_17_1_ = -1.0D / 0.0;
        BlockPos lvt_19_1_ = creature.blockPosition();

        for (int lvt_20_1_ = 0; lvt_20_1_ < 10; ++lvt_20_1_) {
            BlockPos lvt_21_1_ = getRandomDelta(lvt_14_1_, xz, y, p_226339_3_, p_226339_4_, p_226339_6_);
            if (lvt_21_1_ != null) {
                int lvt_22_1_ = lvt_21_1_.getX();
                int lvt_23_1_ = lvt_21_1_.getY();
                int lvt_24_1_ = lvt_21_1_.getZ();
                BlockPos lvt_25_2_;
                if (leopard.hasRestriction() && xz > 1) {
                    lvt_25_2_ = leopard.getRestrictCenter();
                    if (creature.getX() > (double) lvt_25_2_.getX()) {
                        lvt_22_1_ -= lvt_14_1_.nextInt(xz / 2);
                    } else {
                        lvt_22_1_ += lvt_14_1_.nextInt(xz / 2);
                    }

                    if (creature.getZ() > (double) lvt_25_2_.getZ()) {
                        lvt_24_1_ -= lvt_14_1_.nextInt(xz / 2);
                    } else {
                        lvt_24_1_ += lvt_14_1_.nextInt(xz / 2);
                    }
                }

                lvt_25_2_ = new BlockPos((double) lvt_22_1_ + creature.getX(), (double) lvt_23_1_ + creature.getY(), (double) lvt_24_1_ + creature.getZ());
                if (lvt_25_2_.getY() >= 0 && lvt_25_2_.getY() <= creature.level.getMaxBuildHeight() && (!lvt_15_2_ || leopard.isWithinRestriction(lvt_25_2_)) && (!p_226339_12_ || lvt_13_1_.isStableDestination(lvt_25_2_))) {
                    if (p_226339_9_) {
                        lvt_25_2_ = moveUpToAboveSolid(lvt_25_2_, lvt_14_1_.nextInt(p_226339_10_ + 1) + p_226339_11_, creature.level.getMaxBuildHeight(), (p_226341_1_) -> {
                            return creature.level.getBlockState(p_226341_1_).getMaterial().isSolid();
                        });
                    }

                    if (p_226339_5_ || !creature.level.getFluidState(lvt_25_2_).is(FluidTags.WATER)) {
                        PathNodeType lvt_26_1_ = WalkNodeProcessor.getBlockPathTypeStatic(creature.level, lvt_25_2_.mutable());
                        if (leopard.getPathfindingMalus(lvt_26_1_) == 0.0F) {
                            double lvt_27_1_ = p_226339_8_.applyAsDouble(lvt_25_2_);
                            if (lvt_27_1_ > lvt_17_1_) {
                                lvt_17_1_ = lvt_27_1_;
                                lvt_19_1_ = lvt_25_2_;
                                lvt_16_1_ = true;
                            }
                        }
                    }
                }
            }
        }

        if (lvt_16_1_) {
            return Vector3d.atBottomCenterOf(lvt_19_1_);
        } else {
            return null;
        }
    }
}
