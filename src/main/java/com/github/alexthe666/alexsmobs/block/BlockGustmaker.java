package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityGust;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DirectionalBlock;
import net.minecraft.block.material.Material;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.BooleanProperty;
import net.minecraft.state.DirectionProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.state.properties.BlockStateProperties;
import net.minecraft.util.Direction;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Random;

public class BlockGustmaker extends Block {
    public static final DirectionProperty FACING = DirectionalBlock.FACING;
    public static final BooleanProperty TRIGGERED = BlockStateProperties.TRIGGERED;

    public BlockGustmaker() {
        super(AbstractBlock.Properties.of(Material.STONE).requiresCorrectToolForDrops().strength(4.5F));
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TRIGGERED, Boolean.valueOf(false)));
        this.setRegistryName("alexsmobs:gustmaker");
    }

    public static Vector3d getDispensePosition(BlockPos coords, Direction dir) {
        double d0 = coords.getX() + 0.5D + 0.7D * (double) dir.getStepX();
        double d1 = coords.getY() + 0.15D + 0.7D * (double) dir.getStepY();
        double d2 = coords.getZ() + 0.5D + 0.7D * (double) dir.getStepZ();
        return new Vector3d(d0, d1, d2);
    }

    public void neighborChanged(BlockState state, World worldIn, BlockPos pos, Block blockIn, BlockPos fromPos, boolean isMoving) {
        tickGustmaker(state, worldIn, pos, false);
    }

    public void tick(BlockState state, ServerWorld worldIn, BlockPos pos, Random rand) {
        tickGustmaker(state, worldIn, pos, true);
    }

    public void tickGustmaker(BlockState state, World worldIn, BlockPos pos, boolean tickOff) {
        boolean flag = worldIn.hasNeighborSignal(pos) || worldIn.hasNeighborSignal(pos.below()) || worldIn.hasNeighborSignal(pos.above());
        boolean flag1 = state.getValue(TRIGGERED);
        if (flag && !flag1) {
            Vector3d dispensePosition = getDispensePosition(pos, state.getValue(FACING));
            Vector3d gustDir = Vector3d.atLowerCornerOf(state.getValue(FACING).getNormal()).multiply(0.1, 0.1, 0.1);
            EntityGust gust = new EntityGust(worldIn);
            gust.setGustDir((float) gustDir.x, (float) gustDir.y, (float) gustDir.z);
            gust.setPos(dispensePosition.x, dispensePosition.y, dispensePosition.z);
            if(state.getValue(FACING).getAxis() == Direction.Axis.Y){
                gust.setVertical(true);
            }
            if (!worldIn.isClientSide) {
                worldIn.addFreshEntity(gust);
            }
            worldIn.setBlock(pos, state.setValue(TRIGGERED, Boolean.valueOf(true)), 2);
            worldIn.getBlockTicks().scheduleTick(pos, this, 20);
        } else if (flag1) {
            if (tickOff) {
                worldIn.getBlockTicks().scheduleTick(pos, this, 20);
                worldIn.setBlock(pos, state.setValue(TRIGGERED, Boolean.valueOf(false)), 2);
            }
        }
    }

    public BlockState getStateForPlacement(BlockItemUseContext context) {
        return this.defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    public BlockState rotate(BlockState state, Rotation rot) {
        return state.setValue(FACING, rot.rotate(state.getValue(FACING)));
    }

    public BlockState mirror(BlockState state, Mirror mirrorIn) {
        return state.rotate(mirrorIn.getRotation(state.getValue(FACING)));
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FACING, TRIGGERED);
    }
}
