package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.block.AMBlockRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TurtleEntity;
import net.minecraft.entity.passive.fish.AbstractFishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Predicate;

public class EntityShoebill extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_FISH = Animation.create(40);
    public static final Animation ANIMATION_BEAKSHAKE = Animation.create(20);
    public static final Animation ANIMATION_ATTACK = Animation.create(20);
    private static final DataParameter<Boolean> FLYING = EntityDataManager.defineId(EntityShoebill.class, DataSerializers.BOOLEAN);
    public float prevFlyProgress;
    public float flyProgress;
    public int revengeCooldown = 0;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isLandNavigator;
    public int fishingCooldown = 1200 + random.nextInt(1200);
    public int lureLevel = 0;
    public int luckLevel = 0;
    public static final Predicate<LivingEntity> TARGET_BABY  = (animal) -> {
        return animal.isBaby();
    };

    protected EntityShoebill(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.shoebillSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10D).add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SHOEBILL_HURT;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SHOEBILL_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SHOEBILL_HURT;
    }

    public boolean isFood(ItemStack stack) {
        return false;
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev && source.getEntity() != null && !(source.getEntity() instanceof AbstractFishEntity)) {
            double range = 15;
            int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            List<EntityShoebill> list = this.level.getEntitiesOfClass(this.getClass(), this.getBoundingBox().inflate(range, range / 2, range));
            for (EntityShoebill gaz : list) {
                gaz.revengeCooldown = fleeTime;
            }
        }
        return prev;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigatorWide(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.7F);
            this.navigation = new DirectPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new AnimalAIWadeSwimming(this));
        this.goalSelector.addGoal(1, new ShoebillAIFish(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(4, new ShoebillAIFlightFlee(this));
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1D, Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.SHOEBILL_FOODSTUFFS)), false));
        this.goalSelector.addGoal(6, new RandomWalkingGoal(this, 1D, 1400));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(1, new EntityAINearestTarget3D(this, AbstractFishEntity.class, 30, false, true, null));
        this.targetSelector.addGoal(2, new CreatureAITargetItems(this, false, 10));
        this.targetSelector.addGoal(3, (new HurtByTargetGoal(this, PlayerEntity.class)).setAlertOthers());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, EntityAlligatorSnappingTurtle.class, 40, false, false, TARGET_BABY));
        this.targetSelector.addGoal(5, new NearestAttackableTargetGoal(this, TurtleEntity.class, 40, false, false, TARGET_BABY));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, EntityCrocodile.class, 40, false, false, TARGET_BABY));
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public void tick() {
        super.tick();
        if(this.isInWater()){
            maxUpStep = 1.2F;
        }else{
            maxUpStep = 0.6F;
        }
        prevFlyProgress = flyProgress;
        if (isFlying() && flyProgress < 5F) {
            flyProgress++;
        }
        if (!isFlying() && flyProgress > 0F) {
            flyProgress--;
        }
        if (revengeCooldown > 0) {
            revengeCooldown--;
        }
        if (revengeCooldown == 0 && this.getLastHurtByMob() != null) {
            this.setLastHurtByMob(null);
        }
        if (!level.isClientSide) {
            if(fishingCooldown > 0){
                fishingCooldown--;
            }
            if(this.getAnimation() == NO_ANIMATION && this.getRandom().nextInt(700) == 0){
                this.setAnimation(ANIMATION_BEAKSHAKE);
            }
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (this.revengeCooldown > 0 && !this.isFlying()) {
                if (this.onGround || this.isInWater()) {
                    this.setFlying(false);
                }
            }
            if (isFlying()) {
                this.setNoGravity(true);
            } else {
                this.setNoGravity(false);
            }
        }
        if (!level.isClientSide && this.getTarget() != null && this.canSee(this.getTarget()) && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 9) {
            float f1 = this.yRot * ((float) Math.PI / 180F);
            getTarget().knockback(0.3F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().hurt(DamageSource.mobAttack(this), (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }


    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("FishingTimer", this.fishingCooldown);
        compound.putInt("FishingLuck", this.luckLevel);
        compound.putInt("FishingLure", this.lureLevel);
        compound.putInt("RevengeCooldownTimer", this.revengeCooldown);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.fishingCooldown = compound.getInt("FishingTimer");
        this.luckLevel = compound.getInt("FishingLuck");
        this.lureLevel = compound.getInt("FishingLure");
        this.revengeCooldown = compound.getInt("RevengeCooldownTimer");

    }


    protected float getWaterSlowDown() {
        return 0.98F;
    }

    public boolean doHurtTarget(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        this.entityData.set(FLYING, flying);
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
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_FISH, ANIMATION_BEAKSHAKE, ANIMATION_ATTACK};
    }

    public ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack lvt_3_1_ = p_230254_1_.getItemInHand(p_230254_2_);
         if (lvt_3_1_.getItem() == AMItemRegistry.BLOBFISH && this.isAlive()) {
             if(this.luckLevel < 10) {
                 luckLevel = MathHelper.clamp(luckLevel + 1, 0, 10);
                 for (int i = 0; i < 6 + random.nextInt(3); i++) {
                     double d2 = this.random.nextGaussian() * 0.02D;
                     double d0 = this.random.nextGaussian() * 0.02D;
                     double d1 = this.random.nextGaussian() * 0.02D;
                     this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, lvt_3_1_), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
                 }
                 this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
                 lvt_3_1_.shrink(1);
                 return net.minecraft.util.ActionResultType.sidedSuccess(this.level.isClientSide);
             }else{
                 if(this.getAnimation() == NO_ANIMATION){
                     this.setAnimation(ANIMATION_BEAKSHAKE);
                 }
                 return ActionResultType.SUCCESS;
             }
         } else if (lvt_3_1_.getItem() == AMBlockRegistry.CROCODILE_EGG.asItem() && this.isAlive()) {
             if(this.lureLevel < 10){
                 lureLevel = MathHelper.clamp(lureLevel + 1, 0, 10);
                 fishingCooldown = MathHelper.clamp(fishingCooldown - 200, 200, 2400);
                 for (int i = 0; i < 6 + random.nextInt(3); i++) {
                     double d2 = this.random.nextGaussian() * 0.02D;
                     double d0 = this.random.nextGaussian() * 0.02D;
                     double d1 = this.random.nextGaussian() * 0.02D;
                     this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, lvt_3_1_), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
                 }
                 lvt_3_1_.shrink(1);
                 this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
                 return net.minecraft.util.ActionResultType.sidedSuccess(this.level.isClientSide);
             }else{
                 if(this.getAnimation() == NO_ANIMATION){
                     this.setAnimation(ANIMATION_BEAKSHAKE);
                 }
                 return ActionResultType.SUCCESS;
             }

         } else {
            return super.mobInteract(p_230254_1_, p_230254_2_);
        }
    }


    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.SHOEBILL.create(serverWorld);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return ItemTags.getAllTags().getTag(AMTagRegistry.SHOEBILL_FOODSTUFFS).contains(stack.getItem()) || stack.getItem() == AMItemRegistry.BLOBFISH && luckLevel < 10 || stack.getItem() == AMBlockRegistry.CROCODILE_EGG.asItem() && lureLevel < 10;
    }

    public void resetFishingCooldown(){
        fishingCooldown = Math.max(1200 + random.nextInt(1200) - lureLevel * 120, 200);
    }
    @Override
    public void onGetItem(ItemEntity e) {
        this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
        if(e.getItem().getItem() == AMItemRegistry.BLOBFISH){
            luckLevel = MathHelper.clamp(luckLevel + 1, 0, 10);
        }
        if(e.getItem().getItem() == AMBlockRegistry.CROCODILE_EGG.asItem()){
            lureLevel = MathHelper.clamp(lureLevel + 1, 0, 10);
        }
        this.heal(5);
    }
}
