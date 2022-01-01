package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ArmorStandEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Random;
import java.util.function.Predicate;

public class EntityAlligatorSnappingTurtle extends AnimalEntity implements ISemiAquatic, IShearable, net.minecraftforge.common.IForgeShearable {

    public static final Predicate<LivingEntity> TARGET_PRED = (animal) -> {
        return !(animal instanceof EntityAlligatorSnappingTurtle) && EntityPredicates.NO_CREATIVE_OR_SPECTATOR.test(animal)  && !(animal instanceof ArmorStandEntity) && animal.isAlive();
    };
    private static final DataParameter<Byte> CLIMBING = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.BYTE);
    private static final DataParameter<Integer> MOSS = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.INT);
    private static final DataParameter<Boolean> WAITING = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> ATTACK_TARGET_FLAG = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> LUNGE_FLAG = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> TURTLE_SCALE = EntityDataManager.defineId(EntityAlligatorSnappingTurtle.class, DataSerializers.FLOAT);
    public float openMouthProgress;
    public float prevOpenMouthProgress;
    public float attackProgress;
    public float prevAttackProgress;
    public int chaseTime = 0;
    private int biteTick = 0;
    private int waitTime = 0;
    private int timeUntilWait = 0;
    private int mossTime = 0;

    protected EntityAlligatorSnappingTurtle(EntityType<? extends AnimalEntity> type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0.0F);
        maxUpStep = 1F;
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ALLIGATOR_SNAPPING_TURTLE_HURT;
    }


    public static boolean canTurtleSpawn(EntityType type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        boolean spawnBlock = BlockTags.getAllTags().getTag(AMTagRegistry.ALLIGATOR_SNAPPING_TURTLE_SPAWNS).contains(worldIn.getBlockState(pos.below()).getBlock());
        return spawnBlock && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.alligatorSnappingTurtleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.7D).add(Attributes.ARMOR, 8D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    public float getScale() {
        return this.isBaby() ? 0.3F : 1.0F;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.3D, false));
        this.goalSelector.addGoal(2, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(2, new AnimalAILeaveWater(this));
        this.goalSelector.addGoal(3, new BottomFeederAIWander(this, 1.0D, 120, 150, 10));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this) {
            public boolean canContinueToUse() {
                return chaseTime >= 0 && super.canContinueToUse();
            }
        }));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, LivingEntity.class, 2, false, true, TARGET_PRED) {
            protected AxisAlignedBB getTargetSearchArea(double targetDistance) {
                return this.mob.getBoundingBox().inflate(0.5D, 2D, 0.5D);
            }
        });
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COD;
    }

    public boolean onClimbable() {
        return this.isBesideClimbableBlock();
    }

    public boolean doHurtTarget(Entity entityIn) {
        return true;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CLIMBING, (byte) 0);
        this.entityData.define(MOSS, 0);
        this.entityData.define(TURTLE_SCALE, 1F);
        this.entityData.define(WAITING, false);
        this.entityData.define(ATTACK_TARGET_FLAG, false);
        this.entityData.define(LUNGE_FLAG, false);
    }

    public void tick() {
        super.tick();
        prevOpenMouthProgress = openMouthProgress;
        prevAttackProgress = attackProgress;
        boolean attack = this.entityData.get(LUNGE_FLAG);
        boolean open = this.isWaiting() || this.entityData.get(ATTACK_TARGET_FLAG) && !attack;
        if (attack && attackProgress < 5) {
            attackProgress++;
        }
        if (!attack && attackProgress > 0) {
            attackProgress--;
        }
        if (open && openMouthProgress < 5) {
            openMouthProgress++;
        }
        if (!open && openMouthProgress > 0) {
            openMouthProgress--;
        }
        if (this.attackProgress == 4 && this.isAlive() && this.getTarget() != null && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 2.3F) {
            float dmg = this.isBaby() ? 1F : (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue();
            this.getTarget().hurt(DamageSource.mobAttack(this), dmg);
        }
        if (this.attackProgress > 4) {
            biteTick = 5;
        }
        if (biteTick > 0) {
            biteTick--;
        }
        if (chaseTime < 0) {
            chaseTime++;
        }
        if (!this.level.isClientSide) {
            this.setBesideClimbableBlock(this.horizontalCollision && this.isInWater());
            if (this.isWaiting()) {
                waitTime++;
                timeUntilWait = 1500;
                if (waitTime > 1500 || this.getTarget() != null) {
                    this.setWaiting(false);
                }
            } else {
                timeUntilWait--;
                waitTime = 0;
            }
            if ((this.getTarget() == null || !this.getTarget().isAlive()) && timeUntilWait <= 0 && this.isInWater()) {
                this.setWaiting(true);
            }
            if (this.getTarget() != null && biteTick == 0) {
                this.setWaiting(false);
                chaseTime++;
                this.entityData.set(ATTACK_TARGET_FLAG, true);
                this.lookAt(this.getTarget(), 360, 40);
                this.yBodyRot = this.yRot;
                if (this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 2.3F && openMouthProgress > 4) {
                    this.entityData.set(LUNGE_FLAG, true);
                }
                if (this.distanceTo(this.getTarget()) > (this.getTarget() instanceof PlayerEntity ? 5 : 10) && chaseTime > 40) {
                    chaseTime = -50;
                    this.setTarget(null);
                    this.setLastHurtByMob(null);
                    this.setLastHurtMob(null);
                    this.lastHurtByPlayer = null;
                }
            } else {
                this.entityData.set(ATTACK_TARGET_FLAG, false);
                this.entityData.set(LUNGE_FLAG, false);
            }
            mossTime++;
            if (this.isInWater() && mossTime > 12000) {
                mossTime = 0;
                this.setMoss(Math.min(10, this.getMoss() + 1));
            }
        }
    }

    @Nullable
    public LivingEntity getTarget() {
        return this.chaseTime < 0 ? null : super.getTarget();
    }

    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if (this.chaseTime >= 0) {
            super.setTarget(entitylivingbaseIn);
        } else {
            super.setTarget(null);
        }
    }

    @Nullable
    public LivingEntity getLastHurtByMob() {
        return this.chaseTime < 0 ? null : super.getLastHurtByMob();
    }

    public void setLastHurtByMob(@Nullable LivingEntity entitylivingbaseIn) {
        if (this.chaseTime >= 0) {
            super.setLastHurtByMob(entitylivingbaseIn);
        } else {
            super.setLastHurtByMob(null);
        }
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setMoss(random.nextInt(6));
        this.setTurtleScale(0.8F + random.nextFloat() * 0.2F);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public float getTurtleScale() {
        return this.entityData.get(TURTLE_SCALE);
    }

    public void setTurtleScale(float scale) {
        this.entityData.set(TURTLE_SCALE, scale);
    }


    protected PathNavigator createNavigation(World worldIn) {
        SemiAquaticPathNavigator flyingpathnavigator = new SemiAquaticPathNavigator(this, worldIn) {
            public boolean isStableDestination(BlockPos pos) {
                return this.level.getBlockState(pos).getFluidState().isEmpty();
            }
        };
        return flyingpathnavigator;
    }

    public boolean isWaiting() {
        return this.entityData.get(WAITING).booleanValue();
    }

    public void setWaiting(boolean sit) {
        this.entityData.set(WAITING, Boolean.valueOf(sit));
    }

    public int getMoss() {
        return this.entityData.get(MOSS).intValue();
    }

    public void setMoss(int moss) {
        this.entityData.set(MOSS, Integer.valueOf(moss));
    }

    protected void updateAir(int air) {

    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Waiting", this.isWaiting());
        compound.putInt("MossLevel", this.getMoss());
        compound.putFloat("TurtleScale", this.getTurtleScale());
        compound.putInt("MossTime", this.mossTime);
        compound.putInt("WaitTime", this.waitTime);
        compound.putInt("WaitTime2", this.timeUntilWait);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setWaiting(compound.getBoolean("Waiting"));
        this.setMoss(compound.getInt("MossLevel"));
        this.setTurtleScale(compound.getFloat("TurtleScale"));
        this.mossTime = compound.getInt("MossTime");
        this.waitTime = compound.getInt("WaitTime");
        this.timeUntilWait = compound.getInt("WaitTime2");
    }

    @Override
    public boolean shouldEnterWater() {
        return true;
    }

    @Override
    public boolean shouldLeaveWater() {
        return false;
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isWaiting();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getFluidState(pos.below()).isEmpty() && worldIn.getFluidState(pos).is(FluidTags.WATER) ? 10.0F : super.getWalkTargetValue(pos, worldIn);
    }

    public boolean isBesideClimbableBlock() {
        return (this.entityData.get(CLIMBING) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b0 = this.entityData.get(CLIMBING);
        if (climbing) {
            b0 = (byte) (b0 | 1);
        } else {
            b0 = (byte) (b0 & -2);
        }

        this.entityData.set(CLIMBING, b0);
    }


    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            if (this.jumping) {
                this.setDeltaMovement(this.getDeltaMovement().scale(1D));
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.72D, 0.0D));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().scale(0.4D));
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.08D, 0.0D));
            }

        } else {
            super.travel(travelVector);
        }

    }

    public boolean readyForShearing() {
        return this.isAlive() && this.getMoss() > 0;
    }

    @Override
    public boolean isShearable(@javax.annotation.Nonnull ItemStack item, World world, BlockPos pos) {
        return readyForShearing();
    }

    @Override
    public void shear(SoundCategory category) {
        level.playSound(null, this, SoundEvents.SHEEP_SHEAR, category, 1.0F, 1.0F);
        if (!level.isClientSide()) {
            if (random.nextFloat() < this.getMoss() * 0.05F) {
                this.spawnAtLocation(AMItemRegistry.SPIKED_SCUTE);
            } else {
                this.spawnAtLocation(Items.SEAGRASS);
            }
            this.setMoss(0);
        }
    }

    @javax.annotation.Nonnull
    @Override
    public java.util.List<ItemStack> onSheared(@javax.annotation.Nullable PlayerEntity player, @javax.annotation.Nonnull ItemStack item, World world, BlockPos pos, int fortune) {
        world.playSound(null, this, SoundEvents.SHEEP_SHEAR, player == null ? SoundCategory.BLOCKS : SoundCategory.PLAYERS, 1.0F, 1.0F);
        if (!world.isClientSide()) {
            if (random.nextFloat() < this.getMoss() * 0.05F) {
                this.setMoss(0);
                return Collections.singletonList(new ItemStack(AMItemRegistry.SPIKED_SCUTE));
            } else {
                this.setMoss(0);
                return Collections.singletonList(new ItemStack(Items.SEAGRASS));
            }
        }
        return java.util.Collections.emptyList();
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.ALLIGATOR_SNAPPING_TURTLE.create(p_241840_1_);
    }
}
