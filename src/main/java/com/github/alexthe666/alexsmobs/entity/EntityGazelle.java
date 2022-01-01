package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHerdPanic;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.Blocks;
import net.minecraft.entity.AgeableEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;

public class EntityGazelle extends AnimalEntity implements IAnimatedEntity, IHerdPanic {

    private int animationTick;
    private Animation currentAnimation;
    public static final Animation ANIMATION_FLICK_EARS = Animation.create(20);
    public static final Animation ANIMATION_FLICK_TAIL = Animation.create(14);
    public static final Animation ANIMATION_EAT_GRASS = Animation.create(30);
    private boolean hasSpedUp = false;
    private int revengeCooldown = 0;
    private static final DataParameter<Boolean> RUNNING = EntityDataManager.defineId(EntityGazelle.class, DataSerializers.BOOLEAN);

    protected EntityGazelle(EntityType type, World worldIn) {
        super(type, worldIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new AnimalAIHerdPanic(this, 1.1D));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D, Ingredient.of(Items.WHEAT), false));
        this.goalSelector.addGoal(5, new AnimalAIWanderRanged(this, 100, 1.0D, 25, 7));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.addGoal(7, new LookRandomlyGoal(this));
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GAZELLE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GAZELLE_HURT;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.gazelleSpawnRolls, this.getRandom(), spawnReasonIn) && super.checkSpawnRules(worldIn, spawnReasonIn);
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if(prev){
            double range = 15;
            int fleeTime = 100 + getRandom().nextInt(150);
            this.revengeCooldown = fleeTime;
            List<EntityGazelle> list = this.level.getEntitiesOfClass(this.getClass(), this.getBoundingBox().inflate(range, range/2, range));
            for(EntityGazelle gaz : list){
                gaz.revengeCooldown = fleeTime;

            }
        }
        return prev;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(RUNNING, Boolean.valueOf(false));
    }

    public boolean isRunning() {
        return this.entityData.get(RUNNING).booleanValue();
    }

    public void setRunning(boolean running) {
        this.entityData.set(RUNNING, Boolean.valueOf(running));
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.WHEAT || stack.getItem() == AMItemRegistry.ACACIA_BLOSSOM;
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    public void tick() {
        super.tick();
        if(!level.isClientSide && this.getAnimation() == NO_ANIMATION && getRandom().nextInt(70) == 0 && (this.getLastHurtByMob() == null || this.distanceTo(this.getLastHurtByMob()) > 30)){
            if(level.getBlockState(this.blockPosition().below()).is(Blocks.GRASS_BLOCK) && getRandom().nextInt(3) == 0){
                this.setAnimation(ANIMATION_EAT_GRASS);
            }else{
                this.setAnimation(getRandom().nextBoolean()  ? ANIMATION_FLICK_EARS : ANIMATION_FLICK_TAIL);
            }
        }
        if(!this.level.isClientSide){
            if(revengeCooldown >= 0){
                revengeCooldown--;
            }
            if(revengeCooldown == 0 && this.getLastHurtByMob() != null){
                this.setLastHurtByMob(null);
            }
            this.setRunning(revengeCooldown > 0);
            if(isRunning() && !hasSpedUp){
                hasSpedUp = true;
                this.setSprinting(true);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.475F);
            }
            if(!isRunning() && hasSpedUp){
                hasSpedUp = false;
                this.setSprinting(false);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25F);
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("GazelleRunning", this.isRunning());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setRunning(compound.getBoolean("GazelleRunning"));
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
        return new Animation[]{ANIMATION_FLICK_EARS, ANIMATION_FLICK_TAIL, ANIMATION_EAT_GRASS};
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.GAZELLE.create(p_241840_1_);
    }

    @Override
    public void onPanic() {
    }

    @Override
    public boolean canPanic() {
        return true;
    }
}
