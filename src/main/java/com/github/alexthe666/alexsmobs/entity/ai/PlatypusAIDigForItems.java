package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityPlatypus;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.loot.LootContext;
import net.minecraft.loot.LootParameterSets;
import net.minecraft.loot.LootParameters;
import net.minecraft.loot.LootTable;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.server.ServerWorld;

import java.util.List;
import java.util.Random;

public class PlatypusAIDigForItems extends Goal {

    public static final ResourceLocation PLATYPUS_REWARD = new ResourceLocation("alexsmobs", "gameplay/platypus_reward");
    public static final ResourceLocation PLATYPUS_REWARD_CHARGED = new ResourceLocation("alexsmobs", "gameplay/platypus_supercharged_reward");
    private EntityPlatypus platypus;
    private BlockPos digPos;
    private int generatePosCooldown = 0;
    private int digTime = 0;
    private int maxDroppedItems = 3;

    public PlatypusAIDigForItems(EntityPlatypus platypus) {
        this.platypus = platypus;
    }

    private static List<ItemStack> getItemStacks(EntityPlatypus platypus) {
        LootTable loottable = platypus.level.getServer().getLootTables().get(platypus.superCharged ? PLATYPUS_REWARD_CHARGED : PLATYPUS_REWARD);
        return loottable.getRandomItems((new LootContext.Builder((ServerWorld) platypus.level)).withParameter(LootParameters.THIS_ENTITY, platypus).withRandom(platypus.level.random).create(LootParameterSets.PIGLIN_BARTER));
    }

    @Override
    public boolean canUse() {
        if (!platypus.isSensing()) {
            return false;
        }
        if(generatePosCooldown == 0){
            generatePosCooldown = 20 + platypus.getRandom().nextInt(20);
            digPos = genDigPos();
            maxDroppedItems = 2 + platypus.getRandom().nextInt(5);
            return digPos != null;
        }else{
            generatePosCooldown--;
            return false;
        }

    }

    public boolean canContinueToUse() {
        return platypus.getTarget() == null && platypus.isSensing() && platypus.getLastHurtByMob() == null && digPos != null && platypus.level.getBlockState(digPos).getBlock() == Blocks.CLAY && platypus.level.getFluidState(digPos.above()).is(FluidTags.WATER);
    }

    public void tick() {
        double dist = platypus.distanceToSqr(Vector3d.atCenterOf(digPos.above()));
        double d0 = digPos.getX() + 0.5 - this.platypus.getX();
        double d1 = digPos.getY() + 0.5 - this.platypus.getEyeY();
        double d2 = digPos.getZ() + 0.5 - this.platypus.getZ();
        float f = (float) (MathHelper.atan2(d2, d0) * 57.2957763671875D) - 90.0F;
        if (dist < 2) {
            platypus.setDeltaMovement(platypus.getDeltaMovement().add(0, -0.01F, 0));
            platypus.getNavigation().stop();
            digTime++;
            if (digTime % 5 == 0) {
                SoundEvent sound = platypus.level.getBlockState(digPos).getSoundType().getHitSound();
                platypus.playSound(sound, 1, 0.5F + platypus.getRandom().nextFloat() * 0.5F);
            }
            int itemDivis = (int) Math.floor(100F / maxDroppedItems);
            if(digTime % itemDivis == 0){
                List<ItemStack> lootList = getItemStacks(platypus);
                if (lootList.size() > 0) {
                    for (ItemStack stack : lootList) {
                        ItemEntity e = this.platypus.spawnAtLocation(stack.copy());
                        e.hasImpulse = true;
                        e.setDeltaMovement(e.getDeltaMovement().multiply(0.2, 0.2, 0.2));
                    }
                }
            }
            if (digTime >= 100) {
                platypus.setSensing(false);
                platypus.setDigging(false);
                digTime = 0;
            } else {
                platypus.setDigging(true);
            }
        } else {
            platypus.setDigging(false);

            platypus.getNavigation().moveTo(digPos.getX(), digPos.getY() + 1, digPos.getZ(), 1);

            platypus.yRot = f;
        }

    }

    public void stop() {
        generatePosCooldown = 0;
        platypus.setSensing(false);
        platypus.setDigging(false);
        digPos = null;
        digTime = 0;
    }

    private BlockPos genSeafloorPos(BlockPos parent) {
        IWorld world = platypus.level;
        Random random = new Random();
        int range = 15;
        for (int i = 0; i < 15; i++) {
            BlockPos seafloor = parent.offset(random.nextInt(range) - range / 2, 0, random.nextInt(range) - range / 2);
            while (world.getFluidState(seafloor).is(FluidTags.WATER) && seafloor.getY() > 1) {
                seafloor = seafloor.below();
            }
            BlockState state = world.getBlockState(seafloor);
            if (state.getBlock() == Blocks.CLAY) {
                return seafloor;
            }
        }
        return null;
    }

    private BlockPos genDigPos() {
        Random random = new Random();
        int range = 15;
        if (platypus.isInWater()) {
            return genSeafloorPos(this.platypus.blockPosition());
        } else {
            for (int i = 0; i < 15; i++) {
                BlockPos blockpos1 = this.platypus.blockPosition().offset(random.nextInt(range) - range / 2, 3, random.nextInt(range) - range / 2);
                while (this.platypus.level.isEmptyBlock(blockpos1) && blockpos1.getY() > 1) {
                    blockpos1 = blockpos1.below();
                }
                if (this.platypus.level.getFluidState(blockpos1).is(FluidTags.WATER)) {
                    BlockPos pos3 = genSeafloorPos(blockpos1);
                    if (pos3 != null) {
                        return pos3;
                    }
                }
            }
        }
        return null;
    }
}
