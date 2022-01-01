package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityEnderiophageRocket;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUseContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Direction;
import net.minecraft.util.Hand;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;

public class ItemEnderiophageRocket extends Item {

    public ItemEnderiophageRocket(Item.Properties group) {
        super(group);
    }

    public ActionResultType useOn(ItemUseContext context) {
        World world = context.getLevel();
        if (!world.isClientSide) {
            ItemStack itemstack = context.getItemInHand();
            Vector3d vector3d = context.getClickLocation();
            Direction direction = context.getClickedFace();
            FireworkRocketEntity fireworkrocketentity = new EntityEnderiophageRocket(world, context.getPlayer(), vector3d.x + (double)direction.getStepX() * 0.15D, vector3d.y + (double)direction.getStepY() * 0.15D, vector3d.z + (double)direction.getStepZ() * 0.15D, itemstack);
            world.addFreshEntity(fireworkrocketentity);
            if(!context.getPlayer().isCreative()){
                itemstack.shrink(1);
            }
        }
        return ActionResultType.sidedSuccess(world.isClientSide);
    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        if (playerIn.isFallFlying()) {
            ItemStack itemstack = playerIn.getItemInHand(handIn);
            if (!worldIn.isClientSide) {
                worldIn.addFreshEntity(new EntityEnderiophageRocket(worldIn, itemstack, playerIn));
                if (!playerIn.abilities.instabuild) {
                    itemstack.shrink(1);
                }
            }

            return ActionResult.sidedSuccess(playerIn.getItemInHand(handIn), worldIn.isClientSide());
        } else {
            return ActionResult.pass(playerIn.getItemInHand(handIn));
        }
    }

}
