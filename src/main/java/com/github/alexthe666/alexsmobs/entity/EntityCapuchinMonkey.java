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
import com.google.common.collect.ImmutableList;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntityCapuchinMonkey extends TameableEntity implements IAnimatedEntity, IFollower, ITargetsDroppedItems {

    public static final Animation ANIMATION_THROW = Animation.create(12);
    public static final Animation ANIMATION_HEADTILT = Animation.create(15);
    public static final Animation ANIMATION_SCRATCH = Animation.create(20);

    protected static final DataParameter<Boolean> DART = EntityDataManager.defineId(EntityCapuchinMonkey.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityCapuchinMonkey.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityCapuchinMonkey.class, DataSerializers.INT);
    private static final DataParameter<Integer> DART_TARGET = EntityDataManager.defineId(EntityCapuchinMonkey.class, DataSerializers.INT);
    public float prevSitProgress;
    public float sitProgress;
    public boolean forcedSit = false;
    public boolean attackDecision = false;//true for ranged, false for melee
    private int animationTick;
    private Animation currentAnimation;
    private int sittingTime = 0;
    private int maxSitTime = 75;
    private Ingredient temptationItems = Ingredient.fromValues(Stream.of(new Ingredient.TagList(ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS)), new Ingredient.SingleItemList(new ItemStack(Items.EGG))));
    private boolean hasSlowed = false;
    private int rideCooldown = 0;

    protected EntityCapuchinMonkey(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.LEAVES, 0.0F);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.4F);
    }

    public static <T extends MobEntity> boolean canCapuchinSpawn(EntityType<EntityCapuchinMonkey> gorilla, IWorld worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.below());
        return (blockstate.is(BlockTags.LEAVES) || blockstate.is(Blocks.GRASS_BLOCK) || blockstate.is(BlockTags.LOGS) || blockstate.is(Blocks.AIR)) && worldIn.getRawBrightness(p_223317_3_, 0) > 8;
    }

    public int getMaxSpawnClusterSize() {
        return 8;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.capuchinMonkeySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            if (entity != null && this.isTame() && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amount = (amount + 1.0F) / 4.0F;
            }
            return super.hurt(source, amount);
        }
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new SitGoal(this));
        this.goalSelector.addGoal(3, new CapuchinAIMelee(this, 1, true));
        this.goalSelector.addGoal(3, new CapuchinAIRangedAttack(this, 1, 20, 15));
        this.goalSelector.addGoal(6, new TameableAIFollowOwner(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.1D, Ingredient.merge(ImmutableList.of(Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.BANANAS)))), true) {
            public void tick() {
                super.tick();
                if (this.mob.distanceToSqr(this.player) < 6.25D && this.mob.getRandom().nextInt(14) == 0) {
                    ((EntityCapuchinMonkey) this.mob).setAnimation(ANIMATION_HEADTILT);
                }
            }
        });
        this.goalSelector.addGoal(7, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(8, new RandomWalkingGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, (new HurtByTargetGoal(this, EntityCapuchinMonkey.class, EntityTossedItem.class)).setAlertOthers());
        this.targetSelector.addGoal(5, new CapuchinAITargetBalloons(this, true));
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CAPUCHIN_MONKEY_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CAPUCHIN_MONKEY_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CAPUCHIN_MONKEY_HURT;
    }

    public boolean isAlliedTo(Entity entityIn) {
        if (this.isTame()) {
            LivingEntity livingentity = this.getOwner();
            if (entityIn == livingentity) {
                return true;
            }
            if (entityIn instanceof TameableEntity) {
                return ((TameableEntity) entityIn).isOwnedBy(livingentity);
            }
            if (livingentity != null) {
                return livingentity.isAlliedTo(entityIn);
            }
        }

        return super.isAlliedTo(entityIn);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("MonkeySitting", this.isSitting());
        compound.putBoolean("HasDart", this.hasDart());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putInt("Command", this.getCommand());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setOrderedToSit(compound.getBoolean("MonkeySitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.setCommand(compound.getInt("Command"));
        this.setDart(compound.getBoolean("HasDart"));
    }

    public void tick() {
        super.tick();
        this.prevSitProgress = this.sitProgress;
        if (this.isSitting() && sitProgress < 10) {
            sitProgress += 1;
        }
        if (!this.isSitting() && sitProgress > 0) {
            sitProgress -= 1;
        }
        if (isSitting() && !forcedSit && ++sittingTime > maxSitTime) {
            this.setOrderedToSit(false);
            sittingTime = 0;
            maxSitTime = 75 + random.nextInt(50);
        }
        if (!level.isClientSide && this.getAnimation() == NO_ANIMATION && !this.isSitting() && this.getCommand() != 1 && random.nextInt(1500) == 0) {
            maxSitTime = 300 + random.nextInt(250);
            this.setOrderedToSit(true);
        }
        this.maxUpStep = 2;
        if (!forcedSit && this.isSitting() && (this.getDartTarget() != null || this.getCommand() == 1)) {
            this.setOrderedToSit(false);
        }
        if (!level.isClientSide && this.getTarget() != null && this.getAnimation() == ANIMATION_SCRATCH && this.getAnimationTick() == 10) {
            float f1 = this.yRot * ((float) Math.PI / 180F);
            this.setDeltaMovement(this.getDeltaMovement().add(-MathHelper.sin(f1) * 0.3F, 0.0D, MathHelper.cos(f1) * 0.3F));
            getTarget().knockback(1F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().hurt(DamageSource.mobAttack(this), (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
            this.setAttackDecision(this.getTarget());
        }
        if (!level.isClientSide && this.getDartTarget() != null && this.getDartTarget().isAlive() && this.getAnimation() == ANIMATION_THROW && this.getAnimationTick() == 5) {
            Vector3d vector3d = this.getDartTarget().getDeltaMovement();
            double d0 = this.getDartTarget().getX() + vector3d.x - this.getX();
            double d1 = this.getDartTarget().getEyeY() - (double) 1.1F - this.getY();
            double d2 = this.getDartTarget().getZ() + vector3d.z - this.getZ();
            float f = MathHelper.sqrt(d0 * d0 + d2 * d2);
            EntityTossedItem tossedItem = new EntityTossedItem(this.level, this);
            tossedItem.setDart(this.hasDart());
            tossedItem.xRot -= -20.0F;
            tossedItem.shoot(d0, d1 + (double) (f * 0.2F), d2, hasDart() ? 1.15F : 0.75F, 8.0F);
            if (!this.isSilent()) {
                this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.WITCH_THROW, this.getSoundSource(), 1.0F, 0.8F + this.random.nextFloat() * 0.4F);
            }
            this.level.addFreshEntity(tossedItem);
            this.setAttackDecision(this.getDartTarget());
        }
        if (rideCooldown > 0) {
            rideCooldown--;
        }
        if (!level.isClientSide && getAnimation() == NO_ANIMATION && this.getRandom().nextInt(300) == 0) {
            setAnimation(ANIMATION_HEADTILT);
        }
        if (!level.isClientSide && this.isSitting()) {
            this.getNavigation().stop();
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean doHurtTarget(Entity entityIn) {
        if (this.getAnimation() == NO_ANIMATION) {
            this.setAnimation(ANIMATION_SCRATCH);
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

    protected void dropEquipment() {
        super.dropEquipment();
        if (hasDart()) {
            this.spawnAtLocation(AMItemRegistry.ANCIENT_DART);
        }
    }

    public void rideTick() {
        Entity entity = this.getVehicle();
        if (this.isPassenger() && !entity.isAlive()) {
            this.stopRiding();
        } else if (isTame() && entity instanceof LivingEntity && isOwnedBy((LivingEntity) entity)) {
            this.setDeltaMovement(0, 0, 0);
            this.tick();
            if (this.isPassenger()) {
                Entity mount = this.getVehicle();
                if (mount instanceof PlayerEntity) {
                    this.yBodyRot = ((LivingEntity) mount).yBodyRot;
                    this.yRot = ((LivingEntity) mount).yRot;
                    this.yHeadRot = ((LivingEntity) mount).yHeadRot;
                    this.yRotO = ((LivingEntity) mount).yHeadRot;
                    float radius = 0F;
                    float angle = (0.01745329251F * (((LivingEntity) mount).yBodyRot - 180F));
                    double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
                    double extraZ = radius * MathHelper.cos(angle);
                    this.setPos(mount.getX() + extraX, Math.max(mount.getY() + mount.getBbHeight() + 0.1, mount.getY()), mount.getZ() + extraZ);
                    attackDecision = true;
                    if (!mount.isAlive() || rideCooldown == 0 && mount.isShiftKeyDown()) {
                        this.removeVehicle();
                        attackDecision = false;
                    }
                }

            }
        } else {
            super.rideTick();
        }

    }

    public void setAttackDecision(Entity target) {
        if (target instanceof MonsterEntity || this.hasDart()) {
            attackDecision = true;
        } else {
            attackDecision = !attackDecision;
        }
    }

    public int getCommand() {
        return this.entityData.get(COMMAND).intValue();
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, Integer.valueOf(command));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean hasDartTarget() {
        return this.entityData.get(DART_TARGET) != -1 && this.hasDart();
    }


    public void setDartTarget(Entity entity) {
        this.entityData.set(DART_TARGET, entity == null ? -1 : entity.getId());
        if(entity instanceof LivingEntity){
            this.setTarget((LivingEntity)entity);
        }
    }

    @Nullable
    public Entity getDartTarget() {
        if (!this.hasDartTarget()) {
            return this.getTarget();
        } else {
            Entity entity = this.level.getEntity(this.entityData.get(DART_TARGET));
            if(entity == null || !entity.isAlive()){
                return this.getTarget();
            }else{
                return entity;
            }
        }
    }


    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(COMMAND, Integer.valueOf(0));
        this.entityData.define(DART_TARGET, -1);
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(DART, false);
    }

    public boolean hasDart() {
        return this.entityData.get(DART);
    }

    public void setDart(boolean dart) {
        this.entityData.set(DART, dart);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.CAPUCHIN_MONKEY.create(p_241840_1_);
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
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || super.isInvulnerableTo(source);
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if (!isTame() && EntityGorilla.isBanana(itemstack)) {
            this.usePlayerItem(player, itemstack);
            if (getRandom().nextInt(5) == 0) {
                this.tame(player);
                this.level.broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level.broadcastEntityEvent(this, (byte) 6);
            }
            return ActionResultType.SUCCESS;
        }
        if (isTame() && (EntityGorilla.isBanana(itemstack) || temptationItems.test(itemstack) && !isFood(itemstack)) && this.getHealth() < this.getMaxHealth()) {
            this.usePlayerItem(player, itemstack);
            this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
            this.heal(5);
            return ActionResultType.SUCCESS;
        }
        if (type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player) && !isFood(itemstack) && !EntityGorilla.isBanana(itemstack) && !temptationItems.test(itemstack)) {
            if (!this.hasDart() && itemstack.getItem() == AMItemRegistry.ANCIENT_DART) {
                this.setDart(true);
                this.usePlayerItem(player, itemstack);
                return ActionResultType.CONSUME;
            }
            if (this.hasDart() && itemstack.getItem() == Items.SHEARS) {
                this.setDart(false);
                itemstack.hurtAndBreak(1, this, (p_233654_0_) -> {
                });
                return ActionResultType.SUCCESS;
            }
            if (player.isShiftKeyDown() && player.getPassengers().isEmpty()) {
                this.startRiding(player);
                rideCooldown = 20;
                return ActionResultType.SUCCESS;
            } else {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.displayClientMessage(new TranslationTextComponent("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.forcedSit = true;
                    this.setOrderedToSit(true);
                    return ActionResultType.SUCCESS;
                } else {
                    this.forcedSit = false;
                    this.setOrderedToSit(false);
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return type;
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_THROW, ANIMATION_SCRATCH};
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return temptationItems.test(stack) || EntityGorilla.isBanana(stack);
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return isTame() && ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS).contains(stack.getItem());
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
        this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
        if (EntityGorilla.isBanana(e.getItem())) {
            if (getRandom().nextInt(4) == 0) {
                this.spawnAtLocation(new ItemStack(AMBlockRegistry.BANANA_PEEL));
            }
            if (e.getThrower() != null && !this.isTame()) {
                if (getRandom().nextInt(5) == 0) {
                    this.setTame(true);
                    this.setOwnerUUID(e.getThrower());
                    this.level.broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level.broadcastEntityEvent(this, (byte) 6);
                }
            }
        }
    }

}
