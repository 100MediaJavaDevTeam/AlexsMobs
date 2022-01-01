package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.entity.*;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.stats.Stats;
import net.minecraft.util.ActionResult;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.Hand;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

import net.minecraft.item.Item.Properties;

public class ItemAnimalDictionary extends Item {
    public ItemAnimalDictionary(Properties properties) {
        super(properties);
    }

    private boolean usedOnEntity = false;

    @Override
    public ActionResultType interactLivingEntity(ItemStack stack, PlayerEntity playerIn, LivingEntity target, Hand hand) {
        ItemStack itemStackIn = playerIn.getItemInHand(hand);
        if (playerIn instanceof ServerPlayerEntity) {
            ServerPlayerEntity serverplayerentity = (ServerPlayerEntity)playerIn;
            CriteriaTriggers.CONSUME_ITEM.trigger(serverplayerentity, itemStackIn);
            serverplayerentity.awardStat(Stats.ITEM_USED.get(this));
        }
        if (playerIn.level.isClientSide && Objects.requireNonNull(target.getEncodeId()).contains(AlexsMobs.MODID + ":")) {
            usedOnEntity = true;
            String id = target.getEncodeId().replace(AlexsMobs.MODID + ":", "");
            if(target instanceof EntityBoneSerpent || target instanceof EntityBoneSerpentPart){
                id = "bone_serpent";
            }
            if(target instanceof EntityCentipedeHead || target instanceof EntityCentipedeBody || target instanceof EntityCentipedeTail){
                id = "cave_centipede";
            }
            if(target instanceof EntityVoidWorm || target instanceof EntityVoidWormPart){
                id = "void_worm";
            }
            AlexsMobs.PROXY.openBookGUI(itemStackIn, id);
        }
        return ActionResultType.PASS;
    }

    public ActionResult<ItemStack> use(World worldIn, PlayerEntity playerIn, Hand handIn) {
        ItemStack itemStackIn = playerIn.getItemInHand(handIn);
        if (!usedOnEntity) {
            if (playerIn instanceof ServerPlayerEntity) {
                ServerPlayerEntity serverplayerentity = (ServerPlayerEntity) playerIn;
                CriteriaTriggers.CONSUME_ITEM.trigger(serverplayerentity, itemStackIn);
                serverplayerentity.awardStat(Stats.ITEM_USED.get(this));
            }
            if (worldIn.isClientSide) {
                AlexsMobs.PROXY.openBookGUI(itemStackIn);
            }
        }
        usedOnEntity = false;

        return new ActionResult(ActionResultType.PASS, itemStackIn);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        tooltip.add(new TranslationTextComponent("item.alexsmobs.animal_dictionary.desc").withStyle(TextFormatting.GRAY));
    }
}
