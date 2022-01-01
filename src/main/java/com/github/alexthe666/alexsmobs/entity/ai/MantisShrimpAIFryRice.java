package com.github.alexthe666.alexsmobs.entity.ai;

import com.github.alexthe666.alexsmobs.entity.EntityMantisShrimp;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.*;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.MoveToBlockGoal;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.state.properties.BedPart;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.ChestTileEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IWorldReader;

import java.util.EnumSet;

public class MantisShrimpAIFryRice extends MoveToBlockGoal {

    private EntityMantisShrimp mantisShrimp;
    private ITag<Item> tag;
    private boolean wasLitPrior = false;
    private int cookingTicks = 0;

    public MantisShrimpAIFryRice(EntityMantisShrimp entityMantisShrimp) {
        super(entityMantisShrimp, 1, 8);
        this.mantisShrimp = entityMantisShrimp;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        tag = ItemTags.getAllTags().getTag(AMTagRegistry.SHRIMP_RICE_FRYABLES);
    }

    public void stop(){
        cookingTicks = 0;
        if(!wasLitPrior){
            BlockPos blockpos = this.getMoveToTarget().below();
            BlockState state = mantisShrimp.level.getBlockState(blockpos);
            if(state.getBlock() instanceof AbstractFurnaceBlock && !wasLitPrior){
                mantisShrimp.level.setBlockAndUpdate(blockpos, state.setValue(AbstractFurnaceBlock.LIT, false));
            }
        }
        super.stop();
    }

    public void tick() {
        super.tick();
        BlockPos blockpos = this.getMoveToTarget().below();
        if(this.isReachedTarget()){
            BlockState state = mantisShrimp.level.getBlockState(blockpos);
            if(mantisShrimp.punchProgress == 0){
                mantisShrimp.punch();
            }
            if(state.getBlock() instanceof AbstractFurnaceBlock && !wasLitPrior){
                mantisShrimp.level.setBlockAndUpdate(blockpos, state.setValue(AbstractFurnaceBlock.LIT, true));
            }
            cookingTicks++;
            if(cookingTicks > 200){
                cookingTicks = 0;
                ItemStack rice = new ItemStack(AMItemRegistry.SHRIMP_FRIED_RICE);
                rice.setCount(mantisShrimp.getMainHandItem().getCount());
                mantisShrimp.setItemInHand(Hand.MAIN_HAND, rice);

            }
        }else{
            cookingTicks = 0;
        }
    }

    @Override
    public boolean canUse() {
        return tag.contains(this.mantisShrimp.getMainHandItem().getItem()) && !mantisShrimp.isSitting() && super.canUse();
    }

    public boolean canContinueToUse() {
        return tag.contains(this.mantisShrimp.getMainHandItem().getItem()) && !mantisShrimp.isSitting() && super.canContinueToUse();
    }

    public double acceptedDistance() {
        return 3.9F;
    }

    @Override
    protected boolean isValidTarget(IWorldReader worldIn, BlockPos pos) {
        if (!worldIn.isEmptyBlock(pos.above())) {
            return false;
        } else {
            BlockState blockstate = worldIn.getBlockState(pos);
            if(blockstate.getBlock() instanceof AbstractFurnaceBlock){
                wasLitPrior = blockstate.getValue(AbstractFurnaceBlock.LIT);
                return true;
            }
            return blockstate.getBlock().is(BlockTags.CAMPFIRES);
        }
    }


}
