package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.EntityRaccoon;
import net.minecraft.block.Block;
import net.minecraft.block.ContainerBlock;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorldReader;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AnimalAILootChests extends MoveToBlockGoal {

    private final AnimalEntity entity;
    private final ILootsChests chestLooter;
    private boolean hasOpenedChest = false;

    public AnimalAILootChests(AnimalEntity entity, int range) {
        super(entity, 1.0F, range);
        this.entity = entity;
        this.chestLooter = (ILootsChests) entity;
    }

    public boolean isChestRaidable(IWorldReader world, BlockPos pos) {
        if (world.getBlockState(pos).getBlock() instanceof ContainerBlock) {
            Block block = world.getBlockState(pos).getBlock();
            boolean listed = false;
            TileEntity entity = world.getBlockEntity(pos);
            if (entity instanceof IInventory) {
                IInventory inventory = (IInventory) entity;
                try {
                    if (!inventory.isEmpty() && chestLooter.isLootable(inventory)) {
                        return true;
                    }
                } catch (Exception e) {
                    AlexsMobs.LOGGER.warn("Alex's Mobs stopped a " + entity.getClass().getSimpleName() + " from causing a crash during access");
                    e.printStackTrace();
                }
            }
        }
        return false;
    }

    @Override
    public boolean canUse() {
        if (this.entity instanceof TameableEntity && ((TameableEntity) entity).isTame()) {
            return false;
        }
        if (!AMConfig.raccoonsStealFromChests) {
            return false;
        }
        if (!this.entity.getItemInHand(Hand.MAIN_HAND).isEmpty()) {
            return false;
        }
        if (this.nextStartTick <= 0) {
            if (!net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(this.entity.level, this.entity)) {
                return false;
            }
        }
        return super.canUse();
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && this.entity.getItemInHand(Hand.MAIN_HAND).isEmpty();
    }

    public boolean canSeeChest() {
        RayTraceResult raytraceresult = entity.level.clip(new RayTraceContext(entity.getEyePosition(1.0F), new Vector3d(blockPos.getX() + 0.5, blockPos.getY() + 0.5, blockPos.getZ() + 0.5), RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, entity));
        if (raytraceresult instanceof BlockRayTraceResult) {
            BlockRayTraceResult blockRayTraceResult = (BlockRayTraceResult) raytraceresult;
            BlockPos pos = blockRayTraceResult.getBlockPos();
            return pos.equals(blockPos) || entity.level.isEmptyBlock(pos) || this.entity.level.getBlockEntity(pos) == this.entity.level.getBlockEntity(blockPos);
        }
        return true;
    }

    public ItemStack getFoodFromInventory(IInventory inventory, Random random) {
        List<ItemStack> items = new ArrayList<ItemStack>();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (chestLooter.shouldLootItem(stack)) {
                items.add(stack);
            }
        }
        if (items.isEmpty()) {
            return ItemStack.EMPTY;
        } else if (items.size() == 1) {
            return items.get(0);
        } else {
            return items.get(random.nextInt(items.size() - 1));
        }
    }


    @Override
    public void tick() {
        super.tick();
        if (this.blockPos != null) {
            TileEntity te = this.entity.level.getBlockEntity(this.blockPos);
            if (te instanceof IInventory) {
                IInventory feeder = (IInventory) te;
                double distance = this.entity.distanceToSqr(this.blockPos.getX() + 0.5F, this.blockPos.getY() + 0.5F, this.blockPos.getZ() + 0.5F);
                if (canSeeChest()) {
                    if (this.isReachedTarget() && distance <= 3) {
                        toggleChest(feeder, false);
                        ItemStack stack = getFoodFromInventory(feeder, this.entity.level.random);
                        if (stack == ItemStack.EMPTY) {
                            this.stop();
                        } else {
                            ItemStack duplicate = stack.copy();
                            duplicate.setCount(1);
                            if (!this.entity.getItemInHand(Hand.MAIN_HAND).isEmpty() && !this.entity.level.isClientSide) {
                                this.entity.spawnAtLocation(this.entity.getItemInHand(Hand.MAIN_HAND), 0.0F);
                            }
                            this.entity.setItemInHand(Hand.MAIN_HAND, duplicate);
                            if (entity instanceof EntityRaccoon) {
                                ((EntityRaccoon) entity).lookForWaterBeforeEatingTimer = 10;
                            }
                            stack.shrink(1);
                            this.stop();

                        }
                    } else {
                        if (distance < 5 && !hasOpenedChest) {
                            hasOpenedChest = true;
                            toggleChest(feeder, true);
                        }
                    }
                }

            }

        }
    }


    public void stop() {
        super.stop();
        if (this.blockPos != null) {
            TileEntity te = this.entity.level.getBlockEntity(this.blockPos);
            if (te instanceof IInventory) {
                toggleChest((IInventory) te, false);
            }
        }
        this.blockPos = null;
        this.hasOpenedChest = false;
    }


    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        return pos != null && isChestRaidable(worldIn, pos);
    }

    public void toggleChest(IInventory te, boolean open) {
        if (te instanceof ChestTileEntity) {
            ChestTileEntity chest = (ChestTileEntity) te;
            if (open) {
                this.entity.level.blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, 1);
            } else {
                this.entity.level.blockEvent(this.blockPos, chest.getBlockState().getBlock(), 1, 0);
            }
            chest.openCount = open ? 1 : 0;
            this.entity.level.updateNeighborsAt(blockPos, chest.getBlockState().getBlock());
            this.entity.level.updateNeighborsAt(blockPos.below(), chest.getBlockState().getBlock());
        }
    }
}
