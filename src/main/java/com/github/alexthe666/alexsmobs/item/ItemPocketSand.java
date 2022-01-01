package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntitySandShot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import java.util.function.Predicate;

import net.minecraft.item.Item.Properties;

public class ItemPocketSand extends Item {

    public static final Predicate<ItemStack> IS_SAND = (stack) -> {
        return ItemTags.SAND.contains(stack.getItem());
    };

    public ItemPocketSand(Properties properties) {
        super(properties);
    }

    public ItemStack findAmmo(PlayerEntity entity) {
        if(entity.isCreative()){
            return ItemStack.EMPTY;
        }
        for(int i = 0; i < entity.inventory.getContainerSize(); ++i) {
            ItemStack itemstack1 = entity.inventory.getItem(i);
            if (IS_SAND.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity livingEntityIn, Hand handIn) {
        ItemStack itemstack = livingEntityIn.getItemInHand(handIn);
        ItemStack ammo = findAmmo(livingEntityIn);
        if(livingEntityIn.isCreative()){
            ammo = new ItemStack(Items.SAND);
        }
        if (!worldIn.isClientSide && !ammo.isEmpty()) {
            worldIn.playSound((PlayerEntity)null, livingEntityIn.getX(), livingEntityIn.getY(), livingEntityIn.getZ(), SoundEvents.SAND_BREAK, SoundCategory.PLAYERS, 0.5F, 0.4F + (random.nextFloat() * 0.4F + 0.8F));
            boolean left = false;
            if (livingEntityIn.getUsedItemHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == HandSide.RIGHT || livingEntityIn.getUsedItemHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == HandSide.LEFT) {
                left = true;
            }
            EntitySandShot blood = new EntitySandShot(worldIn, livingEntityIn, !left);
            Vector3d vector3d = livingEntityIn.getViewVector(1.0F);
            Vector3f vector3f = new Vector3f(vector3d);
            blood.shoot((double) vector3f.x(), (double) vector3f.y(), (double) vector3f.z(), 1.2F, 11);
            if (!worldIn.isClientSide) {
                worldIn.addFreshEntity(blood);
            }
            livingEntityIn.getCooldowns().addCooldown(this, 2);
            ammo.shrink(1);
            itemstack.hurtAndBreak(1, livingEntityIn, (player) -> {
                player.broadcastBreakEvent(livingEntityIn.getUsedItemHand());
            });
        }
        livingEntityIn.awardStat(Stats.ITEM_USED.get(this));
        return ActionResult.sidedSuccess(itemstack, worldIn.isClientSide());
    }


}
