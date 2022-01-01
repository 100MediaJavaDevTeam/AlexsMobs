package com.github.alexthe666.alexsmobs.item;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

import net.minecraft.item.Item.Properties;

public class ItemFuel extends Item {

    private int burnTime;

    public ItemFuel(Properties props, int burnTime) {
        super(props);
        this.burnTime = burnTime;
    }

    public int getBurnTime(ItemStack itemStack) {
        return burnTime;
    }
}
