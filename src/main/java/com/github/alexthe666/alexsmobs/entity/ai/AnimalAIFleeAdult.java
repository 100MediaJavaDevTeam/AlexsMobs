package com.github.alexthe666.alexsmobs.entity.ai;

import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.pathfinding.Path;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;

import java.util.List;

public class AnimalAIFleeAdult extends Goal {
    private final AnimalEntity childAnimal;
    private AnimalEntity parentAnimal;
    private final double moveSpeed;
    private double fleeDistance;
    private int delayCounter;
    private Path path;

    public AnimalAIFleeAdult(AnimalEntity animal, double speed, double fleeDistance) {
        this.childAnimal = animal;
        this.moveSpeed = speed;
        this.fleeDistance = fleeDistance;
    }

    public boolean canUse() {
        if (this.childAnimal.getAge() >= 0) {
            return false;
        } else {
            List<AnimalEntity> list = this.childAnimal.level.getEntitiesOfClass(this.childAnimal.getClass(), this.childAnimal.getBoundingBox().inflate(fleeDistance, 4.0D, fleeDistance));
            AnimalEntity animalentity = null;
            double d0 = Double.MAX_VALUE;

            for(AnimalEntity animalentity1 : list) {
                if (animalentity1.getAge() >= 0) {
                    double d1 = this.childAnimal.distanceToSqr(animalentity1);
                    if (!(d1 > d0)) {
                        d0 = d1;
                        animalentity = animalentity1;
                    }
                }
            }

            if (animalentity == null) {
                return false;
            } else if (d0 > 19.0D) {
                return false;
            } else {
                this.parentAnimal = animalentity;
                Vector3d vec3d = RandomPositionGenerator.getPosAvoid(this.childAnimal, (int) fleeDistance, 7, new Vector3d(this.parentAnimal.getX(), this.parentAnimal.getY(), this.parentAnimal.getZ()));
                if (vec3d == null) {
                    return false;
                } else if (this.parentAnimal.distanceToSqr(vec3d.x, vec3d.y, vec3d.z) < this.parentAnimal.distanceToSqr(this.childAnimal)) {
                    return false;
                } else {
                    this.path = childAnimal.getNavigation().createPath(new BlockPos(vec3d.x, vec3d.y, vec3d.z), 0);
                    return this.path != null;
                }
            }
        }
    }

    public boolean canContinueToUse() {
        if (this.childAnimal.getAge() >= 0) {
            return false;
        } else if (!this.parentAnimal.isAlive()) {
            return false;
        } else {
            return !childAnimal.getNavigation().isDone();
        }
    }


    public void start() {
        childAnimal.getNavigation().moveTo(this.path, moveSpeed);
    }
    public void stop() {
        this.parentAnimal = null;
        this.childAnimal.getNavigation().stop();
        this.path = null;
    }

    public void tick() {
    }
}
