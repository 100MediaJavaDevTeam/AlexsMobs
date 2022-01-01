package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityCapsid;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.world.GameRules;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;

import net.minecraft.block.AbstractBlock.Properties;

public class BlockCapsid extends ContainerBlock {

    public static final DirectionProperty HORIZONTAL_FACING = HorizontalBlock.FACING;
    public BlockCapsid() {
        super(Properties.of(Material.GLASS).noOcclusion().isValidSpawn(BlockCapsid::spawnOption).isRedstoneConductor(BlockCapsid::isntSolid).sound(SoundType.GLASS).lightLevel((state) -> 5).harvestTool(ToolType.PICKAXE).strength(4.5F));
        this.setRegistryName("alexsmobs:capsid");
    }

    public BlockState rotate(BlockState p_185499_1_, Rotation p_185499_2_) {
        return (BlockState)p_185499_1_.setValue(HORIZONTAL_FACING, p_185499_2_.rotate((Direction)p_185499_1_.getValue(HORIZONTAL_FACING)));
    }

    public BlockState mirror(BlockState p_185471_1_, Mirror p_185471_2_) {
        return p_185471_1_.rotate(p_185471_2_.getRotation((Direction)p_185471_1_.getValue(HORIZONTAL_FACING)));
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(HORIZONTAL_FACING, context.getHorizontalDirection());
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(HORIZONTAL_FACING);
    }

    private static Boolean spawnOption(BlockState state, IBlockReader reader, BlockPos pos, EntityType<?> entity) {
        return (boolean)false;
    }

    private static boolean isntSolid(BlockState state, IBlockReader reader, BlockPos pos) {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public boolean skipRendering(BlockState p_200122_1_, BlockState p_200122_2_, Direction p_200122_3_) {
        return p_200122_2_.getBlock() == this ? true : super.skipRendering(p_200122_1_, p_200122_2_, p_200122_3_);
    }

    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        ItemStack heldItem = player.getItemInHand(handIn);
        if (worldIn.getBlockEntity(pos) instanceof TileEntityCapsid && (!player.isShiftKeyDown()  && heldItem.getItem() != this.asItem())) {
            TileEntityCapsid capsid = (TileEntityCapsid)worldIn.getBlockEntity(pos);
            ItemStack copy = heldItem.copy();
            copy.setCount(1);
            if(capsid.getItem(0).isEmpty()){
                capsid.setItem(0, copy);
                if(!player.isCreative()){
                    heldItem.shrink(1);
                }
                return ActionResultType.SUCCESS;
            }else if(capsid.getItem(0).sameItem(copy) && capsid.getItem(0).getMaxStackSize() > capsid.getItem(0).getCount() + copy.getCount()){
                capsid.getItem(0).grow(1);
                if(!player.isCreative()){
                    heldItem.shrink(1);
                }
                return ActionResultType.SUCCESS;
            }else{
                popResource(worldIn, pos, capsid.getItem(0).copy());
                capsid.setItem(0, ItemStack.EMPTY);
                return ActionResultType.SUCCESS;
            }
        }
        return ActionResultType.PASS;
    }

    public void onRemove(BlockState state, World worldIn, BlockPos pos, BlockState newState, boolean isMoving) {
        TileEntity tileentity = worldIn.getBlockEntity(pos);
        if (tileentity instanceof TileEntityCapsid) {
            InventoryHelper.dropContents(worldIn, pos, (TileEntityCapsid) tileentity);
            worldIn.updateNeighbourForOutputSignal(pos, this);
        }
        super.onRemove(state, worldIn, pos, newState, isMoving);
    }


    public BlockRenderType getRenderShape(BlockState p_149645_1_) {
        return BlockRenderType.MODEL;
    }

    @Nullable
    @Override
    public TileEntity newBlockEntity(IBlockReader worldIn) {
        return new TileEntityCapsid();
    }
}
