package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class EntitySnowLeopard extends AnimalEntity implements IAnimatedEntity, ITargetsDroppedItems {

    public static final Animation ANIMATION_ATTACK_R = Animation.create(13);
    public static final Animation ANIMATION_ATTACK_L = Animation.create(13);
    private int animationTick;
    private Animation currentAnimation;
    public float prevSneakProgress;
    public float sneakProgress;
    public float prevTackleProgress;
    public float tackleProgress;
    public float prevSitProgress;
    public float sitProgress;
    private static final DataParameter<Boolean> TACKLING = EntityDataManager.defineId(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SLEEPING = EntityDataManager.defineId(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SL_SNEAKING = EntityDataManager.defineId(EntitySnowLeopard.class, DataSerializers.BOOLEAN);
    private boolean hasSlowedDown = false;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    public float prevSleepProgress;
    public float sleepProgress;

    protected EntitySnowLeopard(EntityType type, World worldIn) {
        super(type, worldIn);
        this.maxUpStep = 2F;
    }


    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.snowLeopardSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canSnowLeopardSpawn(EntityType<EntitySnowLeopard> snowleperd, IWorld worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.below());
        return (blockstate.is(BlockTags.BASE_STONE_OVERWORLD) || blockstate.is(Blocks.DIRT) || blockstate.is(Blocks.GRASS_BLOCK)) && worldIn.getRawBrightness(p_223317_3_, 0) > 8;
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == AMItemRegistry.MOOSE_RIBS || stack.getItem() == AMItemRegistry.COOKED_MOOSE_RIBS;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.addGoal(3, new SnowLeopardAIMelee(this));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new RandomWalkingGoal(this,  1.0D, 70));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, LivingEntity.class, 10, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.SNOW_LEOPARD_TARGETS))));
        this.targetSelector.addGoal(3, new CreatureAITargetItems(this, false, 30));
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30D).add(Attributes.ATTACK_DAMAGE, 6.0D).add(Attributes.MOVEMENT_SPEED, 0.35F).add(Attributes.FOLLOW_RANGE, 64F);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SNOW_LEOPARD_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SNOW_LEOPARD_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SNOW_LEOPARD_HURT;
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(SLEEPING, Boolean.valueOf(false));
        this.entityData.define(SL_SNEAKING, Boolean.valueOf(false));
        this.entityData.define(TACKLING, Boolean.valueOf(false));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setSitting(boolean bar) {
        this.entityData.set(SITTING, Boolean.valueOf(bar));
    }

    public boolean isTackling() {
        return this.entityData.get(TACKLING).booleanValue();
    }

    public void setTackling(boolean bar) {
        this.entityData.set(TACKLING, Boolean.valueOf(bar));
    }

    public boolean isSLSneaking() {
        return this.entityData.get(SL_SNEAKING).booleanValue();
    }

    public void setSlSneaking(boolean bar) {
        this.entityData.set(SL_SNEAKING, Boolean.valueOf(bar));
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.SNOW_LEOPARD.create(serverWorld);
    }

    public void tick(){
        super.tick();
        this.prevSitProgress = sitProgress;
        this.prevSneakProgress = sneakProgress;
        this.prevTackleProgress = tackleProgress;
        this.prevSleepProgress = sleepProgress;
        if (this.isSitting() && sitProgress < 5F) {
            sitProgress += 0.5F;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress -= 0.5F;
        }
        if (this.isSLSneaking() && sneakProgress < 5F) {
            sneakProgress++;
        }
        if (!isSLSneaking() && sneakProgress > 0F) {
            sneakProgress--;
        }
        if (this.isTackling() && tackleProgress < 3F) {
            tackleProgress++;
        }
        if (!isTackling() && tackleProgress > 0F) {
            tackleProgress--;
        }
        if (this.isSleeping() && sleepProgress < 5F) {
            sleepProgress += 0.5F;
        }
        if (!isSleeping() && sleepProgress > 0F) {
            sleepProgress -= 0.5F;
        }
        if(isSLSneaking() && !hasSlowedDown){
            hasSlowedDown = true;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25F);
        }
        if(!isSLSneaking() && hasSlowedDown){
            hasSlowedDown = false;
            this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.35F);
        }
        if(isTackling()){
            this.yBodyRot = this.yRot;
        }
        if(!level.isClientSide) {
            if (this.getTarget() != null && (this.isSitting() || this.isSleeping())) {
                this.setSitting(false);
                this.setSleeping(false);
            }
            if ((isSitting() || isSleeping()) && (++sittingTime > maxSitTime || this.getTarget() != null || this.isInLove() || this.isInWaterOrBubble())) {
                this.setSitting(false);
                this.setSleeping(false);
                sittingTime = 0;
                maxSitTime = 100 + random.nextInt(50);
            }
            if (this.getTarget() == null && this.getDeltaMovement().lengthSqr() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && !this.isInWaterOrBubble() && random.nextInt(340) == 0) {
                sittingTime = 0;
                if (this.getRandom().nextInt(2) != 0) {
                    maxSitTime = 200 + random.nextInt(800);
                    this.setSitting(true);
                    this.setSleeping(false);
                } else {
                    maxSitTime = 2000 + random.nextInt(2600);
                    this.setSitting(false);
                    this.setSleeping(true);
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev) {
            sittingTime = 0;
            this.setSleeping(false);
            this.setSitting(false);
        }
        return prev;
    }

    public void travel(Vector3d vec3d) {
        if (this.isSitting() || this.isSleeping()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vector3d.ZERO;
        }
        super.travel(vec3d);
    }

    protected boolean isImmobile() {
        return super.isImmobile();
    }

    public boolean isSleeping() {
        return this.entityData.get(SLEEPING).booleanValue();
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, Boolean.valueOf(sleeping));
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
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK_L, ANIMATION_ATTACK_R};
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
    }
}
