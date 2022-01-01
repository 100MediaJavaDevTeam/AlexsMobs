package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.entity.EntityMosquitoSpit;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.UseAction;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.math.vector.Vector3f;
import net.minecraft.world.World;

import java.util.Random;
import java.util.function.Predicate;

public class ItemBloodSprayer extends Item {

    public static final Predicate<ItemStack> IS_BLOOD = (stack) -> {
        return stack.getItem() == AMItemRegistry.BLOOD_SAC;
    };

    public ItemBloodSprayer(Item.Properties properties) {
        super(properties);
    }

    public int getUseDuration(ItemStack stack) {
        return isUsable(stack) ? Integer.MAX_VALUE : 0;
    }

    public UseAction getUseAnimation(ItemStack stack) {
        return UseAction.BOW;
    }

    public static boolean isUsable(ItemStack stack) {
        return stack.getDamageValue() < stack.getMaxDamage() - 1;
    }

    @Override
    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {

        ItemStack itemstack = playerIn.getItemInHand(handIn);
        playerIn.startUsingItem(handIn);
        if(!isUsable(itemstack)){
            ItemStack ammo = findAmmo(playerIn);
            boolean flag = playerIn.isCreative();
            if(!ammo.isEmpty()){
                ammo.shrink(1);
                flag = true;
            }
            if(flag){
                itemstack.setDamageValue(0);
            }
        }
        return ActionResult.consume(itemstack);
    }

    public ItemStack findAmmo(PlayerEntity entity) {
        if(entity.isCreative()){
            return ItemStack.EMPTY;
        }
        for(int i = 0; i < entity.inventory.getContainerSize(); ++i) {
            ItemStack itemstack1 = entity.inventory.getItem(i);
            if (IS_BLOOD.test(itemstack1)) {
                return itemstack1;
            }
        }
        return ItemStack.EMPTY;
    }

    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return !oldStack.sameItem(newStack);
    }

    public void onUseTick(World worldIn, LivingEntity livingEntityIn, ItemStack stack, int count) {
        if(isUsable(stack)) {
            if (count % 2 == 0) {
                boolean left = false;
                if (livingEntityIn.getUsedItemHand() == Hand.OFF_HAND && livingEntityIn.getMainArm() == HandSide.RIGHT || livingEntityIn.getUsedItemHand() == Hand.MAIN_HAND && livingEntityIn.getMainArm() == HandSide.LEFT) {
                    left = true;
                }
                EntityMosquitoSpit blood = new EntityMosquitoSpit(worldIn, livingEntityIn, !left);
                Vector3d vector3d = livingEntityIn.getViewVector(1.0F);
                Vector3f vector3f = new Vector3f(vector3d);
                Random rand = new Random();
                livingEntityIn.playSound(SoundEvents.LAVA_POP,1.0F, 1.2F + (rand.nextFloat() - rand.nextFloat()) * 0.2F);
                blood.shoot((double) vector3f.x(), (double) vector3f.y(), (double) vector3f.z(), 1F, 10);
                if (!worldIn.isClientSide) {
                    worldIn.addFreshEntity(blood);
                }
                stack.hurtAndBreak(1, livingEntityIn, (player) -> {
                    player.broadcastBreakEvent(livingEntityIn.getUsedItemHand());
                });
            }
        }else{
            if(livingEntityIn instanceof PlayerEntity){
                ItemStack ammo = findAmmo((PlayerEntity)livingEntityIn);
                boolean flag = ((PlayerEntity) livingEntityIn).isCreative();
                if(!ammo.isEmpty()){
                    ammo.shrink(1);
                    flag = true;
                }
                if(flag){
                    ((PlayerEntity) livingEntityIn).getCooldowns().addCooldown(this, 20);
                    stack.setDamageValue(0);
                }
                livingEntityIn.stopUsingItem();
            }
        }
    }
}
