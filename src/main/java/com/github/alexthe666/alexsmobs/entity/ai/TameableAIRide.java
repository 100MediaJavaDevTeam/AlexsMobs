package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.CreatureEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.vector.Vector3d;

import java.util.EnumSet;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class TameableAIRide extends Goal {

    private CreatureEntity tameableEntity;
    private LivingEntity player;
    private double speed;

    public TameableAIRide(CreatureEntity dragon, double speed) {
        this.tameableEntity = dragon;
        this.speed = speed;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        if (tameableEntity.getControllingPassenger() instanceof PlayerEntity) {
            player = (PlayerEntity) tameableEntity.getControllingPassenger();
            return true;
        }
        return false;
    }

    @Override
    public void start() {
        tameableEntity.getNavigation().stop();
    }

    @Override
    public void tick() {
        tameableEntity.getNavigation().stop();
        tameableEntity.setTarget(null);
        double x = tameableEntity.getX();
        double y = tameableEntity.getY();
        double z = tameableEntity.getZ();
        if (player.zza != 0) {
            Vector3d lookVec = player.getLookAngle();
            if (player.zza < 0) {
                lookVec = lookVec.yRot((float) Math.PI);
            }
            x += lookVec.x * 10;
            z += lookVec.z * 10;
            if(tameableEntity instanceof IFlyingAnimal){
                y += lookVec.y * 10;
            }
        }
        tameableEntity.xxa = player.xxa * 0.35F;
        tameableEntity.maxUpStep = 1;
        tameableEntity.getMoveControl().setWantedPosition(x, y, z, speed);
    }
}
