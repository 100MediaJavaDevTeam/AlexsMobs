package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.message.MessageCrowMountPlayer;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class CrowAIFollowOwner extends Goal {
    private final EntityCrow crow;
    private final IWorldReader world;
    private final double followSpeed;
    private final PathNavigator navigator;
    private final float maxDist;
    private final float minDist;
    private final boolean teleportToLeaves;
    float circlingTime = 0;
    float circleDistance = 1;
    float yLevel = 2;
    boolean clockwise = false;
    private LivingEntity owner;
    private int timeToRecalcPath;
    private float oldWaterCost;
    private int maxCircleTime;

    public CrowAIFollowOwner(EntityCrow p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
        this.crow = p_i225711_1_;
        this.world = p_i225711_1_.level;
        this.followSpeed = p_i225711_2_;
        this.navigator = p_i225711_1_.getNavigation();
        this.minDist = p_i225711_4_;
        this.maxDist = p_i225711_5_;
        this.teleportToLeaves = p_i225711_6_;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    public boolean canUse() {
        LivingEntity lvt_1_1_ = this.crow.getOwner();
        if (lvt_1_1_ == null) {
            return false;
        } else if (lvt_1_1_.isSpectator()) {
            return false;
        } else if (this.crow.isSitting() || crow.isPassenger()) {
            return false;
        } else if (crow.getCommand() != 1) {
            return false;
        } else if (this.crow.distanceToSqr(lvt_1_1_) < (double) (this.minDist * this.minDist)) {
            return false;
        } else {
            this.owner = lvt_1_1_;
            return (crow.getTarget() == null || !crow.getTarget().isAlive());
        }
    }

    public boolean canContinueToUse() {
        if (this.crow.isSitting()) {
            return false;
        } else {
            return crow.getCommand() == 1 && !crow.isPassenger() && (crow.getTarget() == null || !crow.getTarget().isAlive());
        }
    }

    public void start() {
        this.timeToRecalcPath = 0;
        this.oldWaterCost = this.crow.getPathfindingMalus(PathNodeType.WATER);
        this.crow.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        clockwise = crow.getRandom().nextBoolean();
        yLevel = crow.getRandom().nextInt(1);
        circlingTime = 0;
        maxCircleTime = 20 + crow.getRandom().nextInt(100);
        circleDistance = 1F + crow.getRandom().nextFloat() * 2F;
    }

    public void stop() {
        this.owner = null;
        this.navigator.stop();
        circlingTime = 0;
        this.crow.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
    }

    public void tick() {
        this.crow.getLookControl().setLookAt(this.owner, 10.0F, (float) this.crow.getMaxHeadXRot());
        if (!this.crow.isLeashed() && !this.crow.isPassenger()) {
            double dist = this.crow.distanceToSqr(this.owner);
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                if (dist >= 144.0D && !crow.aiItemFlag) {
                    crow.setFlying(true);
                    crow.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + owner.getEyeHeight() + 0.2F, owner.getZ(), 1F);
                    this.tryToTeleportNearEntity();
                    circlingTime = 0;
                }
            }

            if (!crow.aiItemFlag) {
                if (this.crow.isFlying()) {
                    circlingTime++;
                }
                if(circlingTime > maxCircleTime && crow.getRidingCrows(owner) < 2){
                    crow.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + owner.getEyeHeight() + 0.2F, owner.getZ(), 0.7F);
                    if(crow.distanceTo(owner) < 2){
                        crow.startRiding(owner, true);
                        if (!crow.level.isClientSide) {
                            AlexsMobs.sendMSGToAll(new MessageCrowMountPlayer(crow.getId(), owner.getId()));
                        }
                    }
                }else{
                    Vector3d circlePos = getVultureCirclePos(owner.position());
                    if (circlePos == null) {
                        circlePos = owner.position();
                    }
                    crow.setFlying(true);
                    crow.getMoveControl().setWantedPosition(circlePos.x(), circlePos.y() + owner.getEyeHeight() + 0.2F, circlePos.z(), 0.7F);

                }

            }
        }
    }

    public Vector3d getVultureCirclePos(Vector3d target) {
        float angle = (0.01745329251F * 8 * (clockwise ? -circlingTime : circlingTime));
        double extraX = circleDistance * MathHelper.sin((angle));
        double extraZ = circleDistance * MathHelper.cos(angle);
        Vector3d pos = new Vector3d(target.x() + extraX, target.y() + yLevel, target.z() + extraZ);
        if (crow.level.isEmptyBlock(new BlockPos(pos))) {
            return pos;
        }
        return null;
    }


    private void tryToTeleportNearEntity() {
        BlockPos lvt_1_1_ = this.owner.blockPosition();

        for (int lvt_2_1_ = 0; lvt_2_1_ < 10; ++lvt_2_1_) {
            int lvt_3_1_ = this.getRandomNumber(-3, 3);
            int lvt_4_1_ = this.getRandomNumber(-1, 1);
            int lvt_5_1_ = this.getRandomNumber(-3, 3);
            boolean lvt_6_1_ = this.tryToTeleportToLocation(lvt_1_1_.getX() + lvt_3_1_, lvt_1_1_.getY() + lvt_4_1_, lvt_1_1_.getZ() + lvt_5_1_);
            if (lvt_6_1_) {
                return;
            }
        }

    }

    private boolean tryToTeleportToLocation(int p_226328_1_, int p_226328_2_, int p_226328_3_) {
        if (Math.abs((double) p_226328_1_ - this.owner.getX()) < 2.0D && Math.abs((double) p_226328_3_ - this.owner.getZ()) < 2.0D) {
            return false;
        } else if (!this.isTeleportFriendlyBlock(new BlockPos(p_226328_1_, p_226328_2_, p_226328_3_))) {
            return false;
        } else {
            this.crow.moveTo((double) p_226328_1_ + 0.5D, p_226328_2_, (double) p_226328_3_ + 0.5D, this.crow.yRot, this.crow.xRot);
            this.navigator.stop();
            return true;
        }
    }

    private boolean isTeleportFriendlyBlock(BlockPos p_226329_1_) {
        PathNodeType lvt_2_1_ = WalkNodeProcessor.getBlockPathTypeStatic(this.world, p_226329_1_.mutable());
        if (lvt_2_1_ != PathNodeType.WALKABLE) {
            return false;
        } else {
            BlockState lvt_3_1_ = this.world.getBlockState(p_226329_1_.below());
            if (!this.teleportToLeaves && lvt_3_1_.getBlock() instanceof LeavesBlock) {
                return false;
            } else {
                BlockPos lvt_4_1_ = p_226329_1_.subtract(this.crow.blockPosition());
                return this.world.noCollision(this.crow, this.crow.getBoundingBox().move(lvt_4_1_));
            }
        }
    }

    private int getRandomNumber(int p_226327_1_, int p_226327_2_) {
        return this.crow.getRandom().nextInt(p_226327_2_ - p_226327_1_ + 1) + p_226327_1_;
    }
}
