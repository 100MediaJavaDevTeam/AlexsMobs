package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityGrizzlyBear;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.BeehiveBlock;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.tags.BlockTags;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;

import java.util.Random;

public class GrizzlyBearAIBeehive extends MoveToBlockGoal {

    private EntityGrizzlyBear bear;
    private int idleAtHiveTime = 0;
    private boolean isAboveDestinationBear;

    public GrizzlyBearAIBeehive(EntityGrizzlyBear bear) {
        super(bear, 1D, 32, 8);
        this.bear = bear;
    }

    public boolean canUse() {
        return !bear.isBaby() && super.canUse();
    }

    public void stop() {
        idleAtHiveTime = 0;
    }

    public double acceptedDistance() {
        return 2D;
    }

    public void tick() {
        super.tick();
        BlockPos blockpos = this.getMoveToTarget();
        if (!isWithinXZDist(blockpos, this.mob.position(), this.acceptedDistance())) {
            this.isAboveDestinationBear = false;
            ++this.tryTicks;
            if (this.shouldRecalculatePath()) {
                this.mob.getNavigation().moveTo((double) ((float) blockpos.getX()) + 0.5D, blockpos.getY(), (double) ((float) blockpos.getZ()) + 0.5D, this.speedModifier);
            }
        } else {
            this.isAboveDestinationBear = true;
            --this.tryTicks;
        }

        if (this.isReachedTarget() && Math.abs(bear.getY() - blockPos.getY()) <= 3) {
            bear.lookAt(EntityAnchorArgument.Type.EYES, new Vector3d(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5));
            if (bear.getY() + 2 < blockPos.getY()) {
                bear.setAnimation(EntityGrizzlyBear.ANIMATION_MAUL);
                bear.maxStandTime = 60;
                bear.setStanding(true);
            } else {
                if (bear.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                    bear.setAnimation(bear.getRandom().nextBoolean() ? EntityGrizzlyBear.ANIMATION_SWIPE_L : EntityGrizzlyBear.ANIMATION_SWIPE_R);

                }
            }
            if (this.idleAtHiveTime >= 20) {
                this.eatHive();
            } else {
                ++this.idleAtHiveTime;
            }
        }

    }

    private boolean isWithinXZDist(BlockPos blockpos, Vector3d positionVec, double distance) {
        return blockpos.distSqr(positionVec.x(), blockpos.getY(), positionVec.z(), true) < distance * distance;
    }

    protected boolean isReachedTarget() {
        return this.isAboveDestinationBear;
    }

    private void eatHive() {
        if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(bear.level, bear)) {
            BlockState blockstate = bear.level.getBlockState(this.blockPos);
            if (BlockTags.getAllTags().getTag(AMTagRegistry.GRIZZLY_BEEHIVE).contains(blockstate.getBlock())) {
                if (bear.level.getBlockEntity(this.blockPos) instanceof BeehiveTileEntity) {
                    Random rand = new Random();
                    BeehiveTileEntity beehivetileentity = (BeehiveTileEntity) bear.level.getBlockEntity(this.blockPos);
                    beehivetileentity.emptyAllLivingFromHive(null, blockstate, BeehiveTileEntity.State.EMERGENCY);
                    bear.level.updateNeighbourForOutputSignal(this.blockPos, blockstate.getBlock());
                    ItemStack stack = new ItemStack(Items.HONEYCOMB);
                    int level = 0;
                    if (blockstate.getBlock() instanceof BeehiveBlock) {
                        level = blockstate.getValue(BeehiveBlock.HONEY_LEVEL);
                    }
                    for (int i = 0; i < level; i++) {
                        ItemEntity itementity = new ItemEntity(bear.level, blockPos.getX() + rand.nextFloat(), blockPos.getY() + rand.nextFloat(), blockPos.getZ() + rand.nextFloat(), stack);
                        itementity.setDefaultPickUpDelay();
                        bear.level.addFreshEntity(itementity);
                    }
                    bear.level.destroyBlock(blockPos, false);
                    if (blockstate.getBlock() instanceof BeehiveBlock) {
                        bear.level.setBlockAndUpdate(blockPos, blockstate.setValue(BeehiveBlock.HONEY_LEVEL, 0));
                    }
                    double d0 = 15;
                    for (BeeEntity bee : bear.level.getEntitiesOfClass(BeeEntity.class, new AxisAlignedBB((double) blockPos.getX() - d0, (double) blockPos.getY() - d0, (double) blockPos.getZ() - d0, (double) blockPos.getX() + d0, (double) blockPos.getY() + d0, (double) blockPos.getZ() + d0))) {
                        bee.setRemainingPersistentAngerTime(100);
                        bee.setTarget(bear);
                        bee.setStayOutOfHiveCountdown(400);
                    }
                    stop();
                }
            }
        }
    }

    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        if (BlockTags.getAllTags().getTag(AMTagRegistry.GRIZZLY_BEEHIVE).contains(worldIn.getBlockState(pos).getBlock())) {
            if (worldIn.getBlockEntity(pos) instanceof BeehiveTileEntity && worldIn.getBlockState(pos).getBlock() instanceof BeehiveBlock) {
                int i = worldIn.getBlockState(pos).getValue(BeehiveBlock.HONEY_LEVEL);
                return i > 0;
            }
        }
        return false;
    }
}
