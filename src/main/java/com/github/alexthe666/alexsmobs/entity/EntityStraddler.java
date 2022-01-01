package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.StraddlerAIShoot;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.google.common.collect.Sets;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FlowingFluidBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.StriderEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

import java.util.Random;
import java.util.Set;

public class EntityStraddler extends MonsterEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_LAUNCH = Animation.create(30);
    private static final DataParameter<Integer> STRADPOLE_COUNT = EntityDataManager.defineId(EntityStraddler.class, DataSerializers.INT);
    private int animationTick;
    private Animation currentAnimation;

    protected EntityStraddler(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.LAVA, 0.0F);
        this.setPathfindingMalus(PathNodeType.DANGER_FIRE, 0.0F);
        this.setPathfindingMalus(PathNodeType.DAMAGE_FIRE, 0.0F);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.STRADDLER_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.STRADDLER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.STRADDLER_HURT;
    }

    public static boolean canStraddlerSpawn(EntityType animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = BlockTags.BASE_STONE_NETHER.contains(worldIn.getBlockState(pos.below()).getBlock());
        return spawnBlock;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 28.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.8D).add(Attributes.ARMOR, 5.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.3F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STRADPOLE_COUNT, 0);
    }

    public int getStradpoleCount() {
        return this.entityData.get(STRADPOLE_COUNT);
    }

    public void setStradpoleCount(int index) {
        this.entityData.set(STRADPOLE_COUNT, index);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.straddlerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StraddlerAIShoot(this, 0.5F, 30, 16));
        this.goalSelector.addGoal(7, new RandomWalkingGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(9, new LookAtGoal(this, StriderEntity.class, 8.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillagerEntity.class, true));
    }

    protected void checkFallDamage(double p_184231_1_, boolean p_184231_3_, BlockState p_184231_4_, BlockPos p_184231_5_) {
        this.checkInsideBlocks();
        if (this.isInLava()) {
            this.fallDistance = 0.0F;
        } else {
            super.checkFallDamage(p_184231_1_, p_184231_3_, p_184231_4_, p_184231_5_);
        }
    }

    public void travel(Vector3d travelVector) {
        this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED) * (this.getAnimation() == ANIMATION_LAUNCH ? 0.5F : 1F) * (isInLava() ? 0.2F : 1F));
        if (this.isEffectiveAi() && (this.isInWater() || this.isInLava())) {
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

    private void floatStrider() {
        if (this.isInLava()) {
            ISelectionContext lvt_1_1_ = ISelectionContext.of(this);
            if (lvt_1_1_.isAbove(FlowingFluidBlock.STABLE_SHAPE, this.blockPosition().below(), true) && !this.level.getFluidState(this.blockPosition().above()).is(FluidTags.LAVA)) {
                this.onGround = true;
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.5D).add(0.0D, random.nextFloat() * 0.5, 0.0D));
            }
        }

    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    protected float nextStep() {
        return this.moveDist + 0.6F;
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        if (worldIn.getBlockState(pos).getFluidState().is(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    public Vector3d getDismountLocationForPassenger(LivingEntity livingEntity) {
        Vector3d[] avector3d = new Vector3d[]{getCollisionHorizontalEscapeVector(this.getBbWidth(), livingEntity.getBbWidth(), livingEntity.yRot), getCollisionHorizontalEscapeVector(this.getBbWidth(), livingEntity.getBbWidth(), livingEntity.yRot - 22.5F), getCollisionHorizontalEscapeVector(this.getBbWidth(), livingEntity.getBbWidth(), livingEntity.yRot + 22.5F), getCollisionHorizontalEscapeVector(this.getBbWidth(), livingEntity.getBbWidth(), livingEntity.yRot - 45.0F), getCollisionHorizontalEscapeVector(this.getBbWidth(), livingEntity.getBbWidth(), livingEntity.yRot + 45.0F)};
        Set<BlockPos> set = Sets.newLinkedHashSet();
        double d0 = this.getBoundingBox().maxY;
        double d1 = this.getBoundingBox().minY - 0.5D;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (Vector3d vector3d : avector3d) {
            blockpos$mutable.set(this.getX() + vector3d.x, d0, this.getZ() + vector3d.z);

            for (double d2 = d0; d2 > d1; --d2) {
                set.add(blockpos$mutable.immutable());
                blockpos$mutable.move(Direction.DOWN);
            }
        }

        for (BlockPos blockpos : set) {
            if (!this.level.getFluidState(blockpos).is(FluidTags.LAVA)) {
                double d3 = this.level.getBlockFloorHeight(blockpos);
                if (TransportationHelper.isBlockFloorValid(d3)) {
                    Vector3d vector3d1 = Vector3d.upFromBottomCenterOf(blockpos, d3);

                    for (Pose pose : livingEntity.getDismountPoses()) {
                        AxisAlignedBB axisalignedbb = livingEntity.getLocalBoundsForPose(pose);
                        if (TransportationHelper.canDismountTo(this.level, livingEntity, axisalignedbb.move(vector3d1))) {
                            livingEntity.setPose(pose);
                            return vector3d1;
                        }
                    }
                }
            }
        }

        return new Vector3d(this.getX(), this.getBoundingBox().maxY, this.getZ());
    }

    public boolean isOnFire() {
        return false;
    }

    public boolean canStandOnFluid(Fluid p_230285_1_) {
        return p_230285_1_.is(FluidTags.LAVA);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("StradpoleCount", getStradpoleCount());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setStradpoleCount(compound.getInt("StradpoleCount"));
    }

    public void tick() {
        super.tick();
        this.floatStrider();
        this.checkInsideBlocks();
        if (this.getAnimation() == ANIMATION_LAUNCH && this.isAlive()){
            if(this.getAnimationTick() == 2){
                this.playSound(SoundEvents.CROSSBOW_LOADING_MIDDLE, 2F, 1F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            }
        }
        if (this.getAnimation() == ANIMATION_LAUNCH && this.isAlive() && this.getAnimationTick() == 20 && this.getTarget() != null) {
            EntityStradpole pole = AMEntityRegistry.STRADPOLE.create(level);
            pole.setParentId(this.getUUID());
            pole.setPos(this.getX(), this.getEyeY(), this.getZ());
            double d0 = this.getTarget().getEyeY() - (double)1.1F;
            double d1 = this.getTarget().getX() - this.getX();
            double d2 = d0 - pole.getY();
            double d3 = this.getTarget().getZ() - this.getZ();
            float f = MathHelper.sqrt(d1 * d1 + d3 * d3) * 0.4F;
            float f3 = MathHelper.sqrt(d1 * d1 + d2 * d2 + d3 * d3) * 0.2F;
            this.playSound(SoundEvents.CROSSBOW_LOADING_END, 2F, 1F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
            pole.shoot(d1, d2 + (double)f3, d3, 2F, 0F);
            pole.yRot = this.yRot % 360.0F;
            pole.xRot = MathHelper.clamp(this.yRot, -90.0F, 90.0F) % 360.0F;
            if(!level.isClientSide){
                this.level.addFreshEntity(pole);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_LAUNCH};
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new LavaPathNavigator(this, worldIn);
    }

    public boolean shouldShoot() {
        return true;
    }

    static class LavaPathNavigator extends GroundPathNavigator {
        LavaPathNavigator(EntityStraddler p_i231565_1_, World p_i231565_2_) {
            super(p_i231565_1_, p_i231565_2_);
        }

        protected PathFinder createPathFinder(int p_179679_1_) {
            this.nodeEvaluator = new WalkNodeProcessor();
            return new PathFinder(this.nodeEvaluator, p_179679_1_);
        }

        protected boolean hasValidPathType(PathNodeType p_230287_1_) {
            return p_230287_1_ == PathNodeType.LAVA || p_230287_1_ == PathNodeType.DAMAGE_FIRE || p_230287_1_ == PathNodeType.DANGER_FIRE || super.hasValidPathType(p_230287_1_);
        }

        public boolean isStableDestination(BlockPos pos) {
            return this.level.getBlockState(pos).is(Blocks.LAVA) || super.isStableDestination(pos);
        }
    }
}
