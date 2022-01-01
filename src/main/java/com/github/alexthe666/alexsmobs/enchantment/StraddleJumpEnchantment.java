package com.github.alexthe666.alexsmobs.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

import net.minecraft.enchantment.Enchantment.Rarity;

public class StraddleJumpEnchantment extends Enchantment {

    protected StraddleJumpEnchantment(Rarity p_i46729_1_, EnchantmentType p_i46729_2_, EquipmentSlotType... p_i46729_3_) {
        super(p_i46729_1_, p_i46729_2_, p_i46729_3_);
    }

    public int getMinCost(int p_77321_1_) {
        return 15 + (p_77321_1_ - 1) * 9;
    }

    public int getMaxCost(int p_223551_1_) {
        return super.getMinCost(p_223551_1_) + 50;
    }

    public int getMaxLevel() {
        return 3;
    }
}
