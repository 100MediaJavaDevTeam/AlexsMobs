package com.github.alexthe666.alexsmobs.enchantment;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentType;
import net.minecraft.inventory.EquipmentSlotType;

import net.minecraft.enchantment.Enchantment.Rarity;

public class StraddleEnchantment extends Enchantment {

    protected StraddleEnchantment(Rarity r, EnchantmentType type, EquipmentSlotType... types) {
        super(r, type, types);
    }

    public int getMinCost(int i) {
        return 12 + (i + 1) * 9;
    }

    public int getMaxCost(int i) {
        return super.getMinCost(i) + 30;
    }

    public int getMaxLevel() {
        return 1;
    }
}
