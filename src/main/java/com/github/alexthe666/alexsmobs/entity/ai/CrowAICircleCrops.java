package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityCrow;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.CropsBlock;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;

public class CrowAICircleCrops extends MoveToBlockGoal {

    private EntityCrow crow;
    private int idleAtFlowerTime = 0;
    private boolean isAboveDestinationBear;
    float circlingTime = 0;
    float circleDistance = 2;
    float maxCirclingTime = 80;
    float yLevel = 2;
    boolean clockwise = false;
    boolean circlePhase = false;

    public CrowAICircleCrops(EntityCrow bird) {
        super(bird, 1D, 32, 8);
        this.crow = bird;
    }

    public void start() {
        super.start();
        circlePhase = true;
        clockwise = crow.getRandom().nextBoolean();
        yLevel = 1 + crow.getRandom().nextInt(3);
        circleDistance = 1 + crow.getRandom().nextInt(3);
    }

    public boolean canUse() {
        return !crow.isBaby() && (crow.getTarget() == null || !crow.getTarget().isAlive()) && !crow.isTame() && crow.fleePumpkinFlag == 0 && !crow.aiItemFlag && super.canUse();
    }

    public boolean canContinueToUse() {
        return blockPos != null && (crow.getTarget() == null || !crow.getTarget().isAlive()) && !crow.isTame() && !crow.aiItemFlag && crow.fleePumpkinFlag == 0 && super.canContinueToUse();
    }

    public void stop() {
        idleAtFlowerTime = 0;
        circlingTime = 0;
        tryTicks = 0;
        blockPos = null;
    }

    public double acceptedDistance() {
        return 1D;
    }

    public void tick() {
        BlockPos blockpos = this.getMoveToTarget();
        if(circlePhase){
            this.tryTicks = 0;
            BlockPos circlePos = getVultureCirclePos(blockpos);
            if (circlePos != null) {
                crow.setFlying(true);
                crow.getMoveControl().setWantedPosition(circlePos.getX() + 0.5D, circlePos.getY() + 0.5D, circlePos.getZ() + 0.5D, 0.7F);
            }
            circlingTime++;
            if(circlingTime > 200){
                circlingTime = 0;
                circlePhase = false;
            }
        }else{
            super.tick();
            if(crow.isOnGround()){
                crow.setFlying(false);
            }
            if (!isWithinXZDist(blockpos, this.mob.position(), this.acceptedDistance())) {
                this.isAboveDestinationBear = false;
                ++this.tryTicks;
                this.mob.getNavigation().moveTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY() - 0.5D, (double) ((float) blockpos.getZ()) + 0.5D, 1);
            } else {
                this.isAboveDestinationBear = true;
                --this.tryTicks;
            }

            if (this.isReachedTarget()) {
                crow.lookAt(EntityAnchorArgument.Type.EYES, new Vector3d(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5));
                if (this.idleAtFlowerTime >= 5) {
                    this.pollinate();
                    this.stop();
                } else {
                    crow.peck();
                    ++this.idleAtFlowerTime;
                }
            }
        }
    }

    public BlockPos getVultureCirclePos(BlockPos target) {
        float angle = (0.01745329251F * 8 * (clockwise ? -circlingTime : circlingTime));
        double extraX = circleDistance * MathHelper.sin((angle));
        double extraZ = circleDistance * MathHelper.cos(angle);
        BlockPos pos = new BlockPos(target.getX() + 0.5F + extraX, target.getY() + 1 + yLevel, target.getZ() + 0.5F + extraZ);
        if (crow.level.isEmptyBlock(pos)) {
            return pos;
        }
        return null;
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vector3d positionVec, double distance) {
        return blockpos.distSqr(positionVec.x(), positionVec.y(), positionVec.z(), true) < distance * distance;
    }

    protected boolean isReachedTarget() {
        return this.isAboveDestinationBear;
    }

    private void pollinate() {
        if(crow.level.getBlockState(blockPos).getBlock() instanceof CropsBlock){
            CropsBlock block = (CropsBlock)crow.level.getBlockState(blockPos).getBlock();
            int cropAge = crow.level.getBlockState(blockPos).getValue(block.getAgeProperty());
            if(cropAge > 0){
                crow.level.setBlockAndUpdate(blockPos, crow.level.getBlockState(blockPos).setValue(block.getAgeProperty(), cropAge - 1));
            }else{
                crow.level.destroyBlock(blockPos, true);
            }
            stop();
        }else{
            crow.level.destroyBlock(blockPos, true);
            stop();
        }
        tryTicks = 1200;
    }

    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        return BlockTags.getAllTags().getTag(AMTagRegistry.CROW_FOODBLOCKS).contains(worldIn.getBlockState(pos).getBlock());
    }
}
