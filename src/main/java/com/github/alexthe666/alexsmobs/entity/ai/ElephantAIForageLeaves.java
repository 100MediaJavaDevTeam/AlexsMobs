package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityElephant;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.command.arguments.EntityAnchorArgument;
import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;

import java.util.Random;

public class ElephantAIForageLeaves extends MoveToBlockGoal {

    private EntityElephant elephant;
    private int idleAtLeavesTime = 0;
    private boolean isAboveDestinationBear;

    public ElephantAIForageLeaves(EntityElephant elephant) {
        super(elephant, 0.7D, 32, 5);
        this.elephant = elephant;
    }

    public boolean canUse() {
        return !elephant.isBaby() && elephant.getControllingPassenger() == null && elephant.getControllingVillager() == null && elephant.getMainHandItem().isEmpty() && !elephant.aiItemFlag && super.canUse();
    }

    public void stop() {
        idleAtLeavesTime = 0;
    }

    public double acceptedDistance() {
        return 4D;
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

        if (this.isReachedTarget() && Math.abs(elephant.getY() - blockPos.getY()) <= 3) {
            elephant.lookAt(EntityAnchorArgument.Type.EYES, new Vector3d(blockPos.getX() + 0.5D, blockPos.getY(), blockPos.getZ() + 0.5));
            if (elephant.getY() + 2 < blockPos.getY()) {
                if (elephant.getAnimation() == IAnimatedEntity.NO_ANIMATION) {
                    elephant.setAnimation(EntityElephant.ANIMATION_BREAKLEAVES);
                }
                elephant.setStanding(true);
                elephant.maxStandTime = 15;
            } else {
                elephant.setAnimation(EntityElephant.ANIMATION_BREAKLEAVES);
                elephant.setStanding(false);
            }
            if (this.idleAtLeavesTime >= 10) {
                this.breakLeaves();
            } else {
                ++this.idleAtLeavesTime;
            }
        }

    }

    protected int nextStartTick(CreatureEntity p_203109_1_) {
        return 100 + p_203109_1_.getRandom().nextInt(200);
    }

    private boolean isWithinXZDist(BlockPos blockpos, Vector3d positionVec, double distance) {
        return blockpos.distSqr(positionVec.x(), blockpos.getY(), positionVec.z(), true) < distance * distance;
    }

    protected boolean isReachedTarget() {
        return this.isAboveDestinationBear;
    }

    private void breakLeaves() {
        if (net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(elephant.level, elephant)) {
            BlockState blockstate = elephant.level.getBlockState(this.blockPos);
            if (BlockTags.getAllTags().getTag(AMTagRegistry.ELEPHANT_FOODBLOCKS).contains(blockstate.getBlock())) {
                elephant.level.destroyBlock(blockPos, false);
                Random rand = new Random();
                ItemStack stack = new ItemStack(blockstate.getBlock().asItem());
                ItemEntity itementity = new ItemEntity(elephant.level, blockPos.getX() + rand.nextFloat(), blockPos.getY() + rand.nextFloat(), blockPos.getZ() + rand.nextFloat(), stack);
                itementity.setDefaultPickUpDelay();
                elephant.level.addFreshEntity(itementity);
                if(BlockTags.getAllTags().getTag(AMTagRegistry.DROPS_ACACIA_BLOSSOMS).contains(blockstate.getBlock()) && rand.nextInt(30) == 0){
                    ItemStack banana = new ItemStack(AMItemRegistry.ACACIA_BLOSSOM);
                    ItemEntity itementity2 = new ItemEntity(elephant.level, blockPos.getX() + rand.nextFloat(), blockPos.getY() + rand.nextFloat(), blockPos.getZ() + rand.nextFloat(), banana);
                    itementity2.setDefaultPickUpDelay();
                    elephant.level.addFreshEntity(itementity2);
                }
                stop();
            }
        }
    }

    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        return !elephant.aiItemFlag && BlockTags.getAllTags().getTag(AMTagRegistry.ELEPHANT_FOODBLOCKS).contains(worldIn.getBlockState(pos).getBlock());
    }
}
