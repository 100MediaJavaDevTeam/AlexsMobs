package com.github.alexthe666.alexsmobs.effect;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.potion.Effect;
import net.minecraft.potion.EffectType;

public class EffectOrcaMight extends Effect {

    public EffectOrcaMight() {
        super(EffectType.BENEFICIAL, 0X4A4A52);
        this.setRegistryName(AlexsMobs.MODID, "orcas_might");
        this.addAttributeModifier(Attributes.ATTACK_SPEED, "03C3C89D-7037-4B42-869F-B146BCB64D3A", 3D, AttributeModifier.Operation.ADDITION);
    }

    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration > 0;
    }

    public String getDescriptionId() {
        return "alexsmobs.potion.orcas_might";
    }

}