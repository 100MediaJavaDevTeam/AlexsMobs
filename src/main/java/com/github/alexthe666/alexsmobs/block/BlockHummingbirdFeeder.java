package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import com.google.common.base.Predicates;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.block.material.PushReaction;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.pathfinding.PathType;
import net.minecraft.potion.Potions;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockHummingbirdFeeder extends Block {
    public static final IntegerProperty CONTENTS = IntegerProperty.create("contents", 0, 3);
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    private static final VoxelShape AABB = Block.box(4, 0, 4, 12, 12, 12);
    private static final VoxelShape AABB_HANGING = Block.box(4, 0, 4, 12, 16, 12);

    public BlockHummingbirdFeeder() {
        super(AbstractBlock.Properties.of(Material.METAL).sound(SoundType.LANTERN).harvestTool(ToolType.PICKAXE).strength(3F).randomTicks().noOcclusion());
        this.setRegistryName("alexsmobs:hummingbird_feeder");
        this.registerDefaultState(this.stateDefinition.any().setValue(CONTENTS, 0).setValue(HANGING, false));
    }

    @Deprecated
    public VoxelShape getShape(BlockState state, IBlockReader worldIn, BlockPos pos, ISelectionContext context) {
        return state.getValue(HANGING) ? AABB_HANGING : AABB;
    }

    @Nullable
    public BlockState getStateForPlacement(BlockItemUseContext context) {
        FluidState fluidstate = context.getLevel().getFluidState(context.getClickedPos());
        for(Direction direction : context.getNearestLookingDirections()) {
            if (direction.getAxis() == Direction.Axis.Y) {
                BlockState blockstate = this.defaultBlockState().setValue(HANGING, Boolean.valueOf(direction == Direction.UP));
                if (blockstate.canSurvive(context.getLevel(), context.getClickedPos())) {
                    return blockstate.setValue(WATERLOGGED, Boolean.valueOf(fluidstate.getType() == Fluids.WATER));
                }
            }
        }

        return null;
    }


    protected static Direction getBlockConnected(BlockState state) {
        return state.getValue(HANGING) ? Direction.DOWN : Direction.UP;
    }

    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        int contents = state.getValue(CONTENTS);
        ItemStack waterBottle = AMEffectRegistry.createPotion(Potions.WATER);
        ItemStack itemStack = player.getItemInHand(handIn);
        int setContent = -1;
        if(contents == 0){
            if(itemStack.getItem() == Items.SUGAR){
                setContent = 2;
                useItem(player, itemStack);
            }else if(itemStack.getItem() == waterBottle.getItem() && ItemStack.tagMatches(waterBottle, itemStack)){
                setContent = 1;
                useItem(player, itemStack);
            }
        }else if(contents == 1){
            if(itemStack.getItem() == Items.SUGAR){
                setContent = 3;
                useItem(player, itemStack);
            }
        }else if(contents == 2){
            if(itemStack.getItem() == waterBottle.getItem() && ItemStack.tagMatches(waterBottle, itemStack)){
                setContent = 3;
                useItem(player, itemStack);
            }
        }
        if(setContent >= 0){
            worldIn.setBlockAndUpdate(pos, state.setValue(CONTENTS, setContent));
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    public void useItem(PlayerEntity playerEntity, ItemStack stack){
        if(!playerEntity.isCreative()){
            if(stack.hasContainerItem()){
                playerEntity.addItem(stack.getContainerItem().copy());
            }
            stack.shrink(1);
        }
    }

    public boolean canSurvive(BlockState state, IWorldReader worldIn, BlockPos pos) {
        Direction direction = getBlockConnected(state).getOpposite();
        return Block.canSupportCenter(worldIn, pos.relative(direction), direction.getOpposite());
    }

    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.DESTROY;
    }

    public BlockState updateShape(BlockState stateIn, Direction facing, BlockState facingState, IWorld worldIn, BlockPos currentPos, BlockPos facingPos) {
        if (stateIn.getValue(WATERLOGGED)) {
            worldIn.getLiquidTicks().scheduleTick(currentPos, Fluids.WATER, Fluids.WATER.getTickDelay(worldIn));
        }

        return getBlockConnected(stateIn).getOpposite() == facing && !stateIn.canSurvive(worldIn, currentPos) ? Blocks.AIR.defaultBlockState() : super.updateShape(stateIn, facing, facingState, worldIn, currentPos, facingPos);
    }

    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    public boolean isPathfindable(BlockState state, IBlockReader worldIn, BlockPos pos, PathType type) {
        return false;
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(CONTENTS, HANGING, WATERLOGGED);
    }
}
