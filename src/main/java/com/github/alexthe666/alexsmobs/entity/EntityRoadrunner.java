package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class EntityRoadrunner extends AnimalEntity {

    public float oFlapSpeed;
    public float oFlap;
    public float wingRotDelta = 1.0F;
    public float wingRotation;
    public float destPos;
    public float prevAttackProgress;
    public float attackProgress;
    private static final DataParameter<Integer> ATTACK_TICK = EntityDataManager.defineId(EntityRoadrunner.class, DataSerializers.INT);
    public int timeUntilNextFeather = this.random.nextInt(24000) + 24000;

    protected EntityRoadrunner(EntityType type, World worldIn) {
        super(type, worldIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new PanicGoal(this, 1.1D));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D, Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS)), false));
        this.goalSelector.addGoal(5, new AnimalAIWanderRanged(this, 50, 1.0D, 25, 7));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, EntityRattlesnake.class, 55, true, true, null));
        this.targetSelector.addGoal(2, (new HurtByTargetGoal(this, EntityRattlesnake.class)).setAlertOthers());
    }
    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("FeatherTime")) {
            this.timeUntilNextFeather = compound.getInt("FeatherTime");
        }

    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.roadrunnerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("FeatherTime", this.timeUntilNextFeather);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ROADRUNNER_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ROADRUNNER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ROADRUNNER_HURT;
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_TICK, 0);

    }

    public boolean doHurtTarget(Entity entityIn) {
        this.entityData.set(ATTACK_TICK, 5);
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.CACTUS || super.isInvulnerableTo(source);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.45F).add(Attributes.FOLLOW_RANGE, 10D);
    }

    public void aiStep() {
        super.aiStep();
        this.oFlap = this.wingRotation;
        this.prevAttackProgress = attackProgress;
        this.oFlapSpeed = this.destPos;
        this.destPos = (float) ((double) this.destPos + (double) (this.onGround ? -1 : 4) * 0.3D);
        this.destPos = MathHelper.clamp(this.destPos, 0.0F, 1.0F);
        if (!this.onGround && this.wingRotDelta < 1.0F) {
            this.wingRotDelta = 1.0F;
        }
        if (!this.level.isClientSide && this.isAlive() && !this.isBaby() && --this.timeUntilNextFeather <= 0) {
            this.spawnAtLocation(AMItemRegistry.ROADRUNNER_FEATHER);
            this.timeUntilNextFeather = this.random.nextInt(24000) + 24000;
        }
        this.wingRotDelta = (float) ((double) this.wingRotDelta * 0.9D);
        Vector3d vector3d = this.getDeltaMovement();
        if (!this.onGround && vector3d.y < 0.0D) {
            this.setDeltaMovement(vector3d.multiply(1.0D, 0.6D, 1.0D));
        }
        this.wingRotation += this.wingRotDelta * 2.0F;

        if(this.entityData.get(ATTACK_TICK) > 0){
            if(this.entityData.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 1.3D){
                this.getTarget().hurt(DamageSource.mobAttack(this), 2);
            }
            this.entityData.set(ATTACK_TICK, this.entityData.get(ATTACK_TICK) - 1);
            if(attackProgress < 5F){
                attackProgress++;
            }
        }else{
            if(attackProgress > 0F){
                attackProgress--;
            }
        }
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {
        this.playSound(SoundEvents.CHICKEN_STEP, 0.15F, 1.0F);
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS).contains(stack.getItem());
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.ROADRUNNER.create(p_241840_1_);
    }

    public static boolean canRoadrunnerSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = BlockTags.getAllTags().getTag(AMTagRegistry.ROADRUNNER_SPAWNS).contains(worldIn.getBlockState(pos.below()).getBlock());
        return spawnBlock && worldIn.getRawBrightness(pos, 0) > 8;
    }

}
