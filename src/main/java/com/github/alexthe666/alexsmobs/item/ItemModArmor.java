package com.github.alexthe666.alexsmobs.item;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.citadel.server.item.CustomArmorMaterial;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.client.renderer.entity.model.BipedModel;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.attributes.Attribute;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.ForgeMod;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public class ItemModArmor extends ArmorItem {
    private static final UUID[] ARMOR_MODIFIERS = new UUID[]{UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"), UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150")};
    private Multimap<Attribute, AttributeModifier> attributeMapCroc;
    private Multimap<Attribute, AttributeModifier> attributeMapMoose;

    public ItemModArmor(CustomArmorMaterial armorMaterial, EquipmentSlotType slot) {
        super(armorMaterial, slot, new Item.Properties().tab(AlexsMobs.TAB));

    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable World worldIn, List<ITextComponent> tooltip, ITooltipFlag flagIn) {
        if (this.material == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
            tooltip.add(new TranslationTextComponent("item.alexsmobs.centipede_leggings.desc").withStyle(TextFormatting.GRAY));
        }
        if (this.material == AMItemRegistry.EMU_ARMOR_MATERIAL) {
            tooltip.add(new TranslationTextComponent("item.alexsmobs.emu_leggings.desc").withStyle(TextFormatting.GRAY));
        }
        super.appendHoverText(stack, worldIn, tooltip, flagIn);
        if (this.material == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            tooltip.add(new TranslationTextComponent("item.alexsmobs.roadrunner_boots.desc").withStyle(TextFormatting.BLUE));
        }
        if (this.material == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            tooltip.add(new TranslationTextComponent("item.alexsmobs.frontier_cap.desc").withStyle(TextFormatting.BLUE));
        }
    }

    private void buildCrocAttributes(CustomArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[slot.getIndex()];
        builder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", materialIn.getDefenseForSlot(slot), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        builder.put(ForgeMod.SWIM_SPEED.get(), new AttributeModifier(uuid, "Swim speed", 1, AttributeModifier.Operation.ADDITION));
        if (this.knockbackResistance > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        attributeMapCroc = builder.build();
    }

    private void buildMooseAttributes(CustomArmorMaterial materialIn) {
        ImmutableMultimap.Builder<Attribute, AttributeModifier> builder = ImmutableMultimap.builder();
        UUID uuid = ARMOR_MODIFIERS[slot.getIndex()];
        builder.put(Attributes.ARMOR, new AttributeModifier(uuid, "Armor modifier", materialIn.getDefenseForSlot(slot), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(uuid, "Armor toughness", materialIn.getToughness(), AttributeModifier.Operation.ADDITION));
        builder.put(Attributes.ATTACK_KNOCKBACK, new AttributeModifier(uuid, "Knockback", 2, AttributeModifier.Operation.ADDITION));
        if (this.knockbackResistance > 0) {
            builder.put(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(uuid, "Armor knockback resistance", this.knockbackResistance, AttributeModifier.Operation.ADDITION));
        }
        attributeMapMoose = builder.build();
    }


    public Multimap<Attribute, AttributeModifier> getDefaultAttributeModifiers(EquipmentSlotType equipmentSlot) {
        if (getMaterial() == AMItemRegistry.CROCODILE_ARMOR_MATERIAL && equipmentSlot == this.slot) {
            if (attributeMapCroc == null) {
                buildCrocAttributes(AMItemRegistry.CROCODILE_ARMOR_MATERIAL);
            }
            return attributeMapCroc;
        }
        if (getMaterial() == AMItemRegistry.MOOSE_ARMOR_MATERIAL && equipmentSlot == this.slot) {
            if (attributeMapMoose == null) {
                buildMooseAttributes(AMItemRegistry.MOOSE_ARMOR_MATERIAL);
            }
            return attributeMapMoose;
        }
        return super.getDefaultAttributeModifiers(equipmentSlot);
    }

    @Nullable
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlotType slot, String type) {
        if (this.material == AMItemRegistry.CROCODILE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/crocodile_chestplate.png";
        } else if (this.material == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/roadrunner_boots.png";
        } else if (this.material == AMItemRegistry.CENTIPEDE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/centipede_leggings.png";
        } else if (this.material == AMItemRegistry.MOOSE_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/moose_headgear.png";
        } else if (this.material == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/frontier_cap.png";
        } else if (this.material == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/sombrero.png";
        } else if (this.material == AMItemRegistry.SPIKED_TURTLE_SHELL_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/spiked_turtle_shell.png";
        } else if (this.material == AMItemRegistry.FEDORA_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/fedora.png";
        } else if (this.material == AMItemRegistry.EMU_ARMOR_MATERIAL) {
            return "alexsmobs:textures/armor/emu_leggings.png";
        }
        return super.getArmorTexture(stack, entity, slot, type);
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    public <A extends BipedModel<?>> A getArmorModel(LivingEntity entity, ItemStack itemStack, EquipmentSlotType armorSlot, A _default) {
        if (this.material == AMItemRegistry.ROADRUNNER_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(0, entity);
        } else if (this.material == AMItemRegistry.MOOSE_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(1, entity);
        } else if (this.material == AMItemRegistry.RACCOON_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(2, entity);
        } else if (this.material == AMItemRegistry.SOMBRERO_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(3, entity);
        } else if (this.material == AMItemRegistry.SPIKED_TURTLE_SHELL_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(4, entity);
        } else if (this.material == AMItemRegistry.FEDORA_ARMOR_MATERIAL) {
            return (A) AlexsMobs.PROXY.getArmorModel(5, entity);
        } else {
            return super.getArmorModel(entity, itemStack, armorSlot, _default);
        }
    }
}
