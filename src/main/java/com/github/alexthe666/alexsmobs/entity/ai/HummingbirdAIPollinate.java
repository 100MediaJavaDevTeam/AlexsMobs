package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityHummingbird;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.CropsBlock;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.item.BoneMealItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.server.ServerWorld;

public class HummingbirdAIPollinate  extends MoveToBlockGoal {

    private EntityHummingbird bird;
    private int idleAtFlowerTime = 0;
    private boolean isAboveDestinationBear;

    public HummingbirdAIPollinate(EntityHummingbird bird) {
        super(bird, 1D, 32, 8);
        this.bird = bird;
    }

    public boolean canUse() {
        return !bird.isBaby() && bird.pollinateCooldown == 0 && super.canUse();
    }

    public void stop() {
        idleAtFlowerTime = 0;
    }

    public double acceptedDistance() {
        return 3D;
    }

    public void tick() {
        super.tick();
        BlockPos blockpos = this.getMoveToTarget();
        if (!isWithinXZDist(blockpos, this.mob.position(), this.acceptedDistance())) {
            this.isAboveDestinationBear = false;
            ++this.tryTicks;
            double speedLoc = speedModifier;
            if(this.mob.distanceToSqr(blockpos.getX() + 0.5D, blockpos.getY() + 0.5D, blockpos.getZ() + 0.5D) >= 3){
                speedLoc = speedModifier * 0.3D;
            }
            this.mob.getMoveControl().setWantedPosition((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, speedLoc);

        } else {
            this.isAboveDestinationBear = true;
            --this.tryTicks;
        }

        if (this.isReachedTarget() && Math.abs(bird.getY() - blockPos.getY()) <= 2) {
            bird.lookAt(EntityAnchorArgument.Type.EYES, new Vector3d(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5));
            if (this.idleAtFlowerTime >= 20) {
                this.pollinate();
                this.stop();
            } else {
                ++this.idleAtFlowerTime;
            }
        }

    }

    private boolean isGrowable(BlockPos pos, ServerWorld world) {
        BlockState blockstate = world.getBlockState(pos);
        Block block = blockstate.getBlock();
        return block instanceof CropsBlock && !((CropsBlock)block).isMaxAge(blockstate);
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vector3d positionVec, double distance) {
        return blockpos.distSqr(positionVec.x(), positionVec.y(), positionVec.z(), true) < distance * distance;
    }

    protected boolean isReachedTarget() {
        return this.isAboveDestinationBear;
    }

    private void pollinate() {
        bird.level.levelEvent(2005, blockPos, 0);
        bird.setCropsPollinated(bird.getCropsPollinated() + 1);
        bird.pollinateCooldown = 200;
        if(bird.getCropsPollinated() > 3){
            if(isGrowable(blockPos, (ServerWorld) bird.level)){
                BoneMealItem.growCrop(new ItemStack(Items.BONE_MEAL), bird.level, blockPos);
            }
            bird.setCropsPollinated(0);
        }
    }

    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        if (worldIn.getBlockState(pos).getBlock().is(BlockTags.BEE_GROWABLES) || worldIn.getBlockState(pos).getBlock().is(BlockTags.FLOWERS)) {
            return bird.pollinateCooldown == 0 && bird.canBlockBeSeen(pos);
        }
        return false;
    }
}
