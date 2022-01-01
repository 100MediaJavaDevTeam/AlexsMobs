package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class EntityGorilla extends TameableEntity implements IAnimatedEntity, ITargetsDroppedItems {
    public static final Animation ANIMATION_BREAKBLOCK_R = Animation.create(20);
    public static final Animation ANIMATION_BREAKBLOCK_L = Animation.create(20);
    public static final Animation ANIMATION_POUNDCHEST = Animation.create(40);
    public static final Animation ANIMATION_ATTACK = Animation.create(20);
    protected static final EntitySize SILVERBACK_SIZE = EntitySize.scalable(1.15F, 1.85F);
    private static final DataParameter<Boolean> SILVERBACK = EntityDataManager.defineId(EntityGorilla.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STANDING = EntityDataManager.defineId(EntityGorilla.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityGorilla.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> EATING = EntityDataManager.defineId(EntityGorilla.class, DataSerializers.BOOLEAN);
    public int maxStandTime = 75;
    public float prevStandProgress;
    public float prevSitProgress;
    public float standProgress;
    public float sitProgress;
    public boolean forcedSit = false;
    private int animationTick;
    private Animation currentAnimation;
    private int standingTime = 0;
    private int eatingTime;
    @Nullable
    private EntityGorilla caravanHead;
    @Nullable
    private EntityGorilla caravanTail;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    @Nullable
    private UUID bananaThrowerID = null;
    private boolean hasSilverbackAttributes = false;

    protected EntityGorilla(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, -1.0F);
        this.setPathfindingMalus(PathNodeType.LEAVES, 0.0F);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 7.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.5F).add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    public static boolean isBanana(ItemStack stack) {
        return ItemTags.getAllTags().getTag(AMTagRegistry.BANANAS).contains(stack.getItem());
    }

    public static boolean canGorillaSpawn(EntityType<EntityGorilla> gorilla, IWorld worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.below());
        return (blockstate.is(BlockTags.LEAVES) || blockstate.is(Blocks.GRASS_BLOCK) || blockstate.is(BlockTags.LOGS) || blockstate.is(Blocks.AIR)) && worldIn.getRawBrightness(p_223317_3_, 0) > 8;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.gorillaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return isTame() && isBanana(stack);
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && this.isTame() && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amount = (amount + 1.0F) / 2.0F;
            }
            return super.hurt(source, amount);
        }
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.2D, true));
        this.goalSelector.addGoal(2, new GorillaAIFollowCaravan(this, 0.8D));
        this.goalSelector.addGoal(4, new TameableAITempt(this, 1.1D, Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.BANANAS)), false));
        this.goalSelector.addGoal(4, new AnimalAIRideParent(this, 1.25D));
        this.goalSelector.addGoal(6, new AIWalkIdle(this, 0.8D));
        this.goalSelector.addGoal(5, new GorillaAIForageLeaves(this));
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.GORILLA_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.GORILLA_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.GORILLA_HURT;
    }

    public boolean doHurtTarget(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    public void travel(Vector3d vec3d) {
        if (this.isSitting()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vector3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        if (spawnDataIn instanceof AgeableEntity.AgeableData) {
            AgeableEntity.AgeableData lvt_6_1_ = (AgeableEntity.AgeableData) spawnDataIn;
            if (lvt_6_1_.getGroupSize() == 0) {
                this.setSilverback(true);
            }
        } else {
            this.setSilverback(this.getRandom().nextBoolean());
        }

        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Nullable
    public EntityGorilla getNearestSilverback(IWorld world, double dist) {
        List<EntityGorilla> list = world.getEntitiesOfClass(this.getClass(), this.getBoundingBox().inflate(dist, dist / 2, dist));
        if (list.isEmpty()) {
            return null;
        }
        EntityGorilla gorilla = null;
        double d0 = Double.MAX_VALUE;
        for (EntityGorilla gorrila2 : list) {
            if (gorrila2.isSilverback()) {
                double d1 = this.distanceToSqr(gorrila2);
                if (!(d1 > d0)) {
                    d0 = d1;
                    gorilla = gorrila2;
                }
            }
        }
        return gorilla;
    }

    public EntitySize getDimensions(Pose poseIn) {
        return isSilverback() && !isBaby() ? SILVERBACK_SIZE.scale(this.getScale()) : super.getDimensions(poseIn);
    }

    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            this.setOrderedToSit(false);
            passenger.yRot = this.yRot;
            if (passenger instanceof EntityGorilla) {
                EntityGorilla babyGorilla = (EntityGorilla) passenger;
                babyGorilla.setStanding(this.isStanding());
                babyGorilla.setOrderedToSit(this.isSitting());
            }
            float sitAdd = -0.03F * this.sitProgress;
            float standAdd = -0.03F * this.standProgress;
            float radius = standAdd + sitAdd;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + extraZ);
        }
    }

    public boolean canBeControlledByRider() {
        return false;
    }

    public double getPassengersRidingOffset() {
        return (double) this.getBbHeight() * 0.55F * getGorillaScale() * (isSilverback() ? 0.75F : 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SILVERBACK, Boolean.valueOf(false));
        this.entityData.define(STANDING, Boolean.valueOf(false));
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(EATING, Boolean.valueOf(false));
    }

    public boolean isSilverback() {
        return this.entityData.get(SILVERBACK).booleanValue();
    }

    public void setSilverback(boolean silver) {
        this.entityData.set(SILVERBACK, silver);
    }

    public boolean isStanding() {
        return this.entityData.get(STANDING).booleanValue();
    }

    public void setStanding(boolean standing) {
        this.entityData.set(STANDING, Boolean.valueOf(standing));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean isEating() {
        return this.entityData.get(EATING).booleanValue();
    }

    public void setEating(boolean eating) {
        this.entityData.set(EATING, Boolean.valueOf(eating));
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Silverback", this.isSilverback());
        compound.putBoolean("Standing", this.isStanding());
        compound.putBoolean("GorillaSitting", this.isSitting());
        compound.putBoolean("ForcedToSit", this.forcedSit);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setSilverback(compound.getBoolean("Silverback"));
        this.setStanding(compound.getBoolean("Standing"));
        this.setOrderedToSit(compound.getBoolean("GorillaSitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (itemstack.getItem() == Items.NAME_TAG) {
            return super.mobInteract(player, hand);
        }
        if (isTame() && isBanana(itemstack) && this.getHealth() < this.getMaxHealth()) {
            this.heal(5);
            this.usePlayerItem(player, itemstack);
            this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
            return ActionResultType.SUCCESS;
        }
        ActionResultType type = super.mobInteract(player, hand);
        if (type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player) && !isFood(itemstack)) {
            if (this.isSitting()) {
                this.forcedSit = false;
                this.setOrderedToSit(false);
                return ActionResultType.SUCCESS;
            } else {
                this.forcedSit = true;
                this.setOrderedToSit(true);
                return ActionResultType.SUCCESS;
            }
        }
        return type;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
        if (animation == ANIMATION_POUNDCHEST) {
            this.maxStandTime = 45;
            this.setStanding(true);
        }
        if (animation == ANIMATION_ATTACK) {
            this.maxStandTime = 10;
            this.setStanding(true);
        }
    }

    public void tick() {
        super.tick();
        if (!this.getItemInHand(Hand.MAIN_HAND).isEmpty() && this.canTargetItem(this.getItemInHand(Hand.MAIN_HAND))) {
            this.setEating(true);
            this.setOrderedToSit(true);
            this.setStanding(false);
        }
        if (isEating() && !this.canTargetItem(this.getItemInHand(Hand.MAIN_HAND))) {
            this.setEating(false);
            eatingTime = 0;
            if (!forcedSit) {
                this.setOrderedToSit(true);
            }
        }
        if (isEating()) {
            eatingTime++;
            if (!ItemTags.LEAVES.contains(this.getMainHandItem().getItem())) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItemInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
                }
            }
            if (eatingTime % 5 == 0) {
                this.playSound(SoundEvents.PANDA_EAT, this.getSoundVolume(), this.getVoicePitch());
            }
            if (eatingTime > 100) {
                ItemStack stack = this.getItemInHand(Hand.MAIN_HAND);
                if (!stack.isEmpty()) {
                    this.heal(4);
                    if (isBanana(stack) && bananaThrowerID != null) {
                        if (getRandom().nextFloat() < 0.3F) {
                            this.setTame(true);
                            this.setOwnerUUID(this.bananaThrowerID);
                            PlayerEntity player = level.getPlayerByUUID(bananaThrowerID);
                            if (player instanceof ServerPlayerEntity) {
                                CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
                            }
                            this.level.broadcastEntityEvent(this, (byte) 7);
                        } else {
                            this.level.broadcastEntityEvent(this, (byte) 6);
                        }
                    }
                    if (stack.hasContainerItem()) {
                        this.spawnAtLocation(stack.getContainerItem());
                    }
                    stack.shrink(1);
                }
                eatingTime = 0;
            }
        }
        prevSitProgress = sitProgress;
        prevStandProgress = standProgress;
        if (this.isSitting() && sitProgress < 10) {
            sitProgress += 1;
        }
        if (!this.isSitting() && sitProgress > 0) {
            sitProgress -= 1;
        }
        if (this.isStanding() && standProgress < 10) {
            standProgress += 1;
        }
        if (!this.isStanding() && standProgress > 0) {
            standProgress -= 1;
        }
        if (this.isPassenger() && this.getVehicle() instanceof EntityGorilla && !this.isBaby()) {
            this.removeVehicle();
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if (isSitting() && !forcedSit && ++sittingTime > maxSitTime) {
            this.setOrderedToSit(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!forcedSit && this.isSitting() && (this.getTarget() != null || this.isStanding()) && !this.isEating()) {
            this.setOrderedToSit(false);
        }
        if (!level.isClientSide && this.getAnimation() == NO_ANIMATION && !this.isStanding() && !this.isSitting() && random.nextInt(1500) == 0) {
            maxSitTime = 300 + random.nextInt(250);
            this.setOrderedToSit(true);
        }
        if (this.forcedSit && !this.isVehicle() && this.isTame()) {
            this.setOrderedToSit(true);
        }
        if (this.isSilverback() && random.nextInt(600) == 0 && this.getAnimation() == NO_ANIMATION && !this.isSitting() && sitProgress == 0 && !this.isNoAi() && this.getMainHandItem().isEmpty()) {
            this.setAnimation(ANIMATION_POUNDCHEST);
        }
        if (!level.isClientSide && this.getTarget() != null && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 10) {
            float f1 = this.yRot * ((float) Math.PI / 180F);
            this.setDeltaMovement(this.getDeltaMovement().add(-MathHelper.sin(f1) * 0.02F, 0.0D, MathHelper.cos(f1) * 0.02F));
            getTarget().knockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().hurt(DamageSource.mobAttack(this), (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        }
        if (isSilverback() && !isBaby() && !hasSilverbackAttributes) {
            hasSilverbackAttributes = true;
            refreshDimensions();
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(50F);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(10F);
            this.heal(50F);
        }
        if (!isSilverback() && !isBaby() && hasSilverbackAttributes) {
            hasSilverbackAttributes = false;
            refreshDimensions();
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(30F);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(8F);
            this.heal(30F);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    public boolean canTargetItem(ItemStack stack) {
        return ItemTags.getAllTags().getTag(AMTagRegistry.GORILLA_FOODSTUFFS).contains(stack.getItem());
    }

    @Override
    public void onGetItem(ItemEntity targetEntity) {
        ItemStack duplicate = targetEntity.getItem().copy();
        duplicate.setCount(1);
        if (!this.getItemInHand(Hand.MAIN_HAND).isEmpty() && !this.level.isClientSide) {
            this.spawnAtLocation(this.getItemInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setItemInHand(Hand.MAIN_HAND, duplicate);
        if (EntityGorilla.isBanana(targetEntity.getItem()) && !this.isTame()) {
            bananaThrowerID = targetEntity.getThrower();
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_BREAKBLOCK_R, ANIMATION_BREAKBLOCK_L, ANIMATION_POUNDCHEST, ANIMATION_ATTACK};
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.GORILLA.create(p_241840_1_);
    }

    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityGorilla caravanHeadIn) {
        this.caravanHead = caravanHeadIn;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTrail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityGorilla getCaravanHead() {
        return this.caravanHead;
    }

    public float getGorillaScale() {
        return isBaby() ? 0.5F : isSilverback() ? 1.3F : 1.0F;
    }

    public boolean isDonkeyKong() {
        String s = TextFormatting.stripFormatting(this.getName().getString());
        return s != null && (s.toLowerCase().contains("donkey") && s.toLowerCase().contains("kong") || s.toLowerCase().equals("dk"));
    }

    public boolean isFunkyKong() {
        String s = TextFormatting.stripFormatting(this.getName().getString());
        return s != null && (s.toLowerCase().contains("funky") && s.toLowerCase().contains("kong"));
    }

    private class AIWalkIdle extends RandomWalkingGoal {
        public AIWalkIdle(EntityGorilla entityGorilla, double v) {
            super(entityGorilla, v);
        }

        public boolean canUse() {
            this.interval = EntityGorilla.this.isSilverback() ? 10 : 120;
            return super.canUse();
        }

        @Nullable
        protected Vector3d getPosition() {
            return RandomPositionGenerator.getPos(this.mob, EntityGorilla.this.isSilverback() ? 25 : 10, 7);
        }

    }
}
