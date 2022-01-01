package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.SemiAquaticPathNavigator;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.SquidEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.passive.fish.AbstractGroupFishEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

public class EntityHammerheadShark extends WaterMobEntity {

    private static final Predicate<LivingEntity> INJURED_PREDICATE = (mob) -> {
        return mob.getHealth() <= mob.getMaxHealth() / 2D;
    };

    protected EntityHammerheadShark(EntityType type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new AquaticMoveController(this, 1F);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.hammerheadSharkSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new SemiAquaticPathNavigator(this, worldIn);
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.COD_HURT;
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new FindWaterGoal(this));
        this.goalSelector.addGoal(1, new CirclePreyGoal(this, 1F));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 0.6F, 7));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.goalSelector.addGoal(9, new AvoidEntityGoal<>(this, GuardianEntity.class, 8.0F, 1.0D, 1.0D));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, LivingEntity.class, 50, false, true, INJURED_PREDICATE));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, SquidEntity.class, 50, false, true, null));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, EntityMimicOctopus.class, 80, false, true, null));
        this.targetSelector.addGoal(3, new EntityAINearestTarget3D(this, AbstractGroupFishEntity.class, 70, false, true, null));
    }


    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() == RayTraceResult.Type.BLOCK;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.MOVEMENT_SPEED, 0.5F);
    }

    public static <T extends MobEntity> boolean canHammerheadSharkSpawn(EntityType<EntityHammerheadShark> p_223364_0_, IWorld p_223364_1_, SpawnReason reason, BlockPos p_223364_3_, Random p_223364_4_) {
        if (p_223364_3_.getY() > 45 && p_223364_3_.getY() < p_223364_1_.getSeaLevel()) {
            Optional<RegistryKey<Biome>> optional = p_223364_1_.getBiomeName(p_223364_3_);
            return (!Objects.equals(optional, Optional.of(Biomes.OCEAN)) || !Objects.equals(optional, Optional.of(Biomes.DEEP_OCEAN))) && p_223364_1_.getFluidState(p_223364_3_).is(FluidTags.WATER);
        } else {
            return false;
        }
    }

    private static class CirclePreyGoal extends Goal {
        EntityHammerheadShark shark;
        float speed;
        float circlingTime = 0;
        float circleDistance = 5;
        float maxCirclingTime = 80;
        boolean clockwise = false;

        public CirclePreyGoal(EntityHammerheadShark shark, float speed) {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
            this.shark = shark;
            this.speed = speed;
        }

        @Override
        public boolean canUse() {
            return this.shark.getTarget() != null;
        }

        @Override
        public boolean canContinueToUse() {
            return this.shark.getTarget() != null;
        }

        public void start(){
            circlingTime = 0;
            maxCirclingTime = 360 + this.shark.random.nextInt(80);
            circleDistance = 5 + this.shark.random.nextFloat() * 5;
            clockwise = this.shark.random.nextBoolean();
        }

        public void stop(){
            circlingTime = 0;
            maxCirclingTime = 360 + this.shark.random.nextInt(80);
            circleDistance = 5 + this.shark.random.nextFloat() * 5;
            clockwise = this.shark.random.nextBoolean();
        }

        public void tick(){
            LivingEntity prey = this.shark.getTarget();
            if(prey != null){
                double dist = this.shark.distanceTo(prey);
                if(circlingTime >= maxCirclingTime){
                    shark.lookAt(prey, 30.0F, 30.0F);
                    shark.getNavigation().moveTo(prey, 1.5D);
                    if(dist < 2D){
                        shark.doHurtTarget(prey);
                        if(shark.random.nextFloat() < 0.3F){
                            shark.spawnAtLocation(new ItemStack(AMItemRegistry.SHARK_TOOTH));
                        }
                        stop();
                    }
                }else{
                    if(dist <= 25){
                        circlingTime++;
                        BlockPos circlePos = getSharkCirclePos(prey);
                        if(circlePos != null){
                            shark.getNavigation().moveTo(circlePos.getX() + 0.5D, circlePos.getY() + 0.5D, circlePos.getZ() + 0.5D, 0.6D);
                        }
                    }else{
                        shark.lookAt(prey, 30.0F, 30.0F);
                        shark.getNavigation().moveTo(prey, 0.8D);
                    }
                }
            }
        }

        public BlockPos getSharkCirclePos(LivingEntity target) {
            float angle = (0.01745329251F * (clockwise ? -circlingTime : circlingTime));
            double extraX = circleDistance * MathHelper.sin((angle));
            double extraZ = circleDistance * MathHelper.cos(angle);
            BlockPos ground = new BlockPos(target.getX() + 0.5F + extraX, shark.getY(), target.getZ() + 0.5F + extraZ);
            if(shark.level.getFluidState(ground).is(FluidTags.WATER)){
                return ground;

            }
            return null;
    }
    }
}
