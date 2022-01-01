package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.entity.ai.goal.MeleeAttackGoal;

public class OrcaAIMelee extends MeleeAttackGoal {

    public OrcaAIMelee(EntityOrca orca, double v, boolean b) {
        super(orca, v, b);
    }

    public boolean canUse(){
        if(this.mob.getTarget() == null || ((EntityOrca)this.mob).shouldUseJumpAttack(this.mob.getTarget())){
            return false;
        }
        return super.canUse();
    }
}
