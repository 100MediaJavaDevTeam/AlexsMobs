package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityShoebill;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSet;
import net.minecraft.loot.LootTables;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.server.ServerWorld;

import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class ShoebillAIFish extends Goal {

    private EntityShoebill bird;
    private BlockPos waterPos = null;
    private BlockPos targetPos = null;
    private int executionChance = 0;
    private Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    private int idleTime = 0;
    private int navigateTime = 0;

    public ShoebillAIFish(EntityShoebill bird) {
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        this.bird = bird;
    }

    public void stop() {
        targetPos = null;
        waterPos = null;
        idleTime = 0;
        navigateTime = 0;
        this.bird.getNavigation().stop();
    }

    public void tick() {
        if (targetPos != null && waterPos != null) {
            double dist = bird.distanceToSqr(Vector3d.atCenterOf(waterPos));
            if (dist <= 1F) {
                navigateTime = 0;
                double d0 = waterPos.getX() + 0.5D - bird.getX();
                double d2 = waterPos.getZ() + 0.5D - bird.getZ();
                float yaw = (float)(MathHelper.atan2(d2, d0) * (double)(180F / (float)Math.PI)) - 90.0F;
                bird.yRot = yaw;
                bird.yHeadRot = yaw;
                bird.yBodyRot = yaw;
                bird.getNavigation().stop();
                idleTime++;
                if(idleTime > 25){
                    bird.setAnimation(EntityShoebill.ANIMATION_FISH);
                }
                if(idleTime > 45 && bird.getAnimation() == EntityShoebill.ANIMATION_FISH){
                    this.bird.playSound(SoundEvents.GENERIC_SPLASH, 0.7F, 0.5F + bird.getRandom().nextFloat());
                    this.bird.resetFishingCooldown();
                    this.spawnFishingLoot();
                    this.stop();
                }
            }else{
                navigateTime++;
                bird.getNavigation().moveTo(waterPos.getX(), waterPos.getY(), waterPos.getZ(), 1.2D);
            }
            if(navigateTime > 3600){
                this.stop();
            }
        }
    }

    public boolean canContinueToUse() {
        return targetPos != null && bird.fishingCooldown == 0 && bird.revengeCooldown == 0 && !bird.isFlying();
    }

    public void spawnFishingLoot() {
        double luck = 0D + bird.luckLevel * 0.5F;
        LootContext.Builder lootcontext$builder = new LootContext.Builder((ServerWorld) this.bird.level);
        lootcontext$builder.withLuck((float) luck); // Forge: add player & looted bird to LootContext
        LootParameterSet.Builder lootparameterset$builder = new LootParameterSet.Builder();
        List<ItemStack> result = bird.level.getServer().getLootTables().get(LootTables.FISHING).getRandomItems(lootcontext$builder.create(lootparameterset$builder.build()));
        for (ItemStack itemstack : result) {
            ItemEntity item = new ItemEntity(this.bird.level, this.bird.getX() + 0.5F, this.bird.getY(), this.bird.getZ(), itemstack);
            if (!this.bird.level.isClientSide) {
                this.bird.level.addFreshEntity(item);
            }
        }
    }


    @Override
    public boolean canUse() {
        if(!bird.isFlying() && bird.fishingCooldown == 0 && bird.getRandom().nextInt(30) == 0){
            if(bird.isInWater()){
                waterPos = bird.blockPosition();
                targetPos = waterPos;
                return true;
            }else{
                waterPos = generateTarget();
                if (waterPos != null) {
                    targetPos = getLandPos(waterPos);
                    return targetPos != null;
                }
            }

        }
        return false;
    }

    public BlockPos generateTarget() {
        BlockPos blockpos = null;
        Random random = new Random();
        int range = 32;
        for (int i = 0; i < 15; i++) {
            BlockPos blockpos1 = this.bird.blockPosition().offset(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
            while (this.bird.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1) {
                blockpos1 = blockpos1.below();
            }
            if (isConnectedToLand(blockpos1)) {
                blockpos = blockpos1;
            }
        }
        return blockpos;
    }

    public boolean isConnectedToLand(BlockPos pos) {
        if (this.bird.level.getFluidState(pos).is(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.relative(dir);
                if (this.bird.level.getFluidState(offsetPos).isEmpty() && this.bird.level.getFluidState(offsetPos.above()).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    public BlockPos getLandPos(BlockPos pos) {
        if (this.bird.level.getFluidState(pos).is(FluidTags.WATER)) {
            for (Direction dir : HORIZONTALS) {
                BlockPos offsetPos = pos.relative(dir);
                if (this.bird.level.getFluidState(offsetPos).isEmpty() && this.bird.level.getFluidState(offsetPos.above()).isEmpty()) {
                    return offsetPos;
                }
            }
        }
        return null;
    }
}
