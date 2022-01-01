package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityCachalotEcho;
import com.github.alexthe666.alexsmobs.misc.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.google.common.base.Predicates;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ItemEcholocator extends Item {

    public boolean ender;

    public ItemEcholocator(Item.Properties properties, boolean ender) {
        super(properties);
        this.ender = ender;
    }

    private List<BlockPos> getNearbyPortals(BlockPos blockpos, ServerWorld world, int range) {
        if(ender){
            PointOfInterestManager pointofinterestmanager = world.getPoiManager();
            Stream<BlockPos> stream = pointofinterestmanager.findAll(AMPointOfInterestRegistry.END_PORTAL_FRAME.getPredicate(), Predicates.alwaysTrue(), blockpos, range, PointOfInterestManager.Status.ANY);
            return stream.collect(Collectors.toList());
        }else{
            Random random = new Random();
            for(int i = 0; i < 256; i++){
                BlockPos checkPos = blockpos.add(random.nextInt(range) - range/2f, random.nextInt(range)/2f - range/2f, random.nextInt(range) - range/2f);
                if(world.getBlockState(checkPos).getBlock() == Blocks.CAVE_AIR && world.getMaxLocalRawBrightness(checkPos) < 4){
                    return Collections.singletonList(checkPos);
                }
            }
            return Collections.emptyList();
        }

    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity livingEntityIn, Hand handIn) {
        ItemStack stack = livingEntityIn.getItemInHand(handIn);
        boolean left = false;
        if (livingEntityIn.getUsedItemHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == HandSide.RIGHT || livingEntityIn.getUsedItemHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == HandSide.LEFT) {
            left = true;
        }
        EntityCachalotEcho whaleEcho = new EntityCachalotEcho(worldIn, livingEntityIn, !left);
        if (!worldIn.isClientSide && worldIn instanceof ServerWorld) {
            BlockPos playerPos = livingEntityIn.blockPosition();
            List<BlockPos> portals = getNearbyPortals(playerPos, (ServerWorld) worldIn, 128);
            BlockPos pos = null;
            if(ender){
                for (BlockPos portalPos : portals) {
                    if (pos == null || pos.distSqr(playerPos) > portalPos.distSqr(playerPos)) {
                        pos = portalPos;
                    }
                }
            }else{
                CompoundNBT nbt = stack.getOrCreateTag();
                if(nbt.contains("CavePos") && nbt.getBoolean("ValidCavePos")){
                    pos = BlockPos.of(nbt.getLong("CavePos"));
                    if(worldIn.getBlockState(pos).getBlock() != Blocks.CAVE_AIR ||worldIn.getMaxLocalRawBrightness(pos) >= 4 || 1000000 < pos.distSqr(playerPos)){
                        nbt.putBoolean("ValidCavePos", false);
                    }
                }else{
                    for (BlockPos portalPos : portals) {
                        if (pos == null || pos.distSqr(playerPos) < portalPos.distSqr(playerPos)) {
                            pos = portalPos;
                        }
                    }
                    if(pos != null){
                        nbt.putLong("CavePos", pos.asLong());
                        nbt.putBoolean("ValidCavePos", true);
                        stack.setTag(nbt);
                    }
                }

            }
            if (pos != null) {
                double d0 = pos.getX() + 0.5F - whaleEcho.getX();
                double d1 = pos.getY() + 0.5F - whaleEcho.getY();
                double d2 = pos.getZ() + 0.5F - whaleEcho.getZ();
                whaleEcho.tickCount = 15;
                whaleEcho.shoot(d0, d1, d2, 0.4F, 0.3F);
                worldIn.addFreshEntity(whaleEcho);
                worldIn.playSound((PlayerEntity)null, whaleEcho.getX(), whaleEcho.getY(), whaleEcho.getZ(), AMSoundRegistry.CACHALOT_WHALE_CLICK, SoundCategory.PLAYERS, 1.0F, 1.0F);
                stack.hurtAndBreak(1, livingEntityIn, (player) -> {
                    player.broadcastBreakEvent(livingEntityIn.getUsedItemHand());
                });
            }
        }
        livingEntityIn.getCooldowns().addCooldown(this, 5);

        return ActionResult.sidedSuccess(stack, worldIn.isClientSide());
    }
}
