package com.github.alexthe666.alexsmobs.block;

import com.github.alexthe666.alexsmobs.entity.EntityLeafcutterAnt;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import com.google.common.base.Predicates;
import net.minecraft.block.*;
import net.minecraft.block.material.Material;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.state.IntegerProperty;
import net.minecraft.state.StateContainer;
import net.minecraft.tileentity.BeehiveTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.ToolType;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BlockLeafcutterAntChamber extends Block {
    public static final IntegerProperty FUNGUS = IntegerProperty.create("fungus", 0, 5);

    public BlockLeafcutterAntChamber() {
        super(AbstractBlock.Properties.of(Material.GRASS).sound(SoundType.GRAVEL).harvestTool(ToolType.SHOVEL).strength(4F).randomTicks());
        this.setRegistryName("alexsmobs:leafcutter_ant_chamber");
        this.registerDefaultState(this.stateDefinition.any().setValue(FUNGUS, 0));
    }

    public ActionResultType use(BlockState state, World worldIn, BlockPos pos, PlayerEntity player, Hand handIn, BlockRayTraceResult hit) {
        int fungalLevel = state.getValue(FUNGUS);
        if (fungalLevel == 5) {
            boolean shroomlight = false;
            for (BlockPos blockpos : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
                if(worldIn.getBlockState(blockpos).getBlock() == Blocks.SHROOMLIGHT){
                    shroomlight = true;
                }
            }
            if(!shroomlight){
                this.angerNearbyAnts(worldIn, pos);
            }
            worldIn.setBlockAndUpdate(pos, state.setValue(FUNGUS, 0));
            if(!worldIn.isClientSide){
                if(worldIn.random.nextInt(2) == 0){
                    Direction dir = Direction.getRandom(worldIn.random);
                    if(worldIn.getBlockState(pos.above()).getBlock() == AMBlockRegistry.LEAFCUTTER_ANTHILL){
                        dir = Direction.DOWN;
                    }
                    BlockPos offset = pos.relative(dir);
                    if(Tags.Blocks.DIRT.contains(worldIn.getBlockState(offset).getBlock()) && !worldIn.canSeeSky(offset)){
                        worldIn.setBlockAndUpdate(offset, this.defaultBlockState());
                    }
                }
                popResource(worldIn, pos, new ItemStack(AMItemRegistry.GONGYLIDIA));
            }
            return ActionResultType.SUCCESS;
        }
        return ActionResultType.FAIL;
    }

    public void randomTick(BlockState state, ServerWorld worldIn, BlockPos pos, Random random) {
            if (!worldIn.isAreaLoaded(pos, 3))
                return; // Forge: prevent loading unloaded chunks when checking neighbor's light and spreading
        if(worldIn.canSeeSky(pos.above())){
            worldIn.setBlockAndUpdate(pos, Blocks.DIRT.defaultBlockState());
        }
    }

    public void playerDestroy(World worldIn, PlayerEntity player, BlockPos pos, BlockState state, @Nullable TileEntity te, ItemStack stack) {
        super.playerDestroy(worldIn, player, pos, state, te, stack);
        this.angerNearbyAnts(worldIn, pos);
    }

    private void angerNearbyAnts(World world, BlockPos pos) {
        List<EntityLeafcutterAnt> list = world.getEntitiesOfClass(EntityLeafcutterAnt.class, (new AxisAlignedBB(pos)).inflate(20D, 6.0D, 20D));
        PlayerEntity player = null;
        List<PlayerEntity> list1 = world.getEntitiesOfClass(PlayerEntity.class, (new AxisAlignedBB(pos)).inflate(20D, 6.0D, 20D));
        if (list1.isEmpty()) return; //Forge: Prevent Error when no players are around.
        int i = list1.size();
        player = list1.get(world.random.nextInt(i));
        if (!list.isEmpty()) {
            for (EntityLeafcutterAnt beeentity : list) {
                if (beeentity.getTarget() == null) {
                    beeentity.setTarget(player);
                }
            }
        }
        if(!world.isClientSide){
            PointOfInterestManager pointofinterestmanager = ((ServerWorld) world).getPoiManager();
            Stream<BlockPos> stream = pointofinterestmanager.findAll(AMPointOfInterestRegistry.LEAFCUTTER_ANT_HILL.getPredicate(), Predicates.alwaysTrue(), pos, 50, PointOfInterestManager.Status.ANY);
            List<BlockPos> listOfHives = stream.collect(Collectors.toList());
            for (BlockPos pos2 : listOfHives) {
                if(world.getBlockEntity(pos2) instanceof TileEntityLeafcutterAnthill){
                    TileEntityLeafcutterAnthill beehivetileentity = (TileEntityLeafcutterAnthill) world.getBlockEntity(pos2);
                    beehivetileentity.angerAnts(player, world.getBlockState(pos2), BeehiveTileEntity.State.EMERGENCY);
                }

            }
        }
    }

    protected void createBlockStateDefinition(StateContainer.Builder<Block, BlockState> builder) {
        builder.add(FUNGUS);
    }
}
