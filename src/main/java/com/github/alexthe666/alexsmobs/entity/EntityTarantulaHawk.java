package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIHurtByTargetNotBaby;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlyingAIFollowOwner;
import com.github.alexthe666.alexsmobs.message.MessageTarantulaHawkSting;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ExperienceOrbEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.potion.EffectInstance;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceContext;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.BiomeDictionary;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityTarantulaHawk extends TameableEntity implements IFollower {

    public static final int STING_DURATION = 2400;
    protected static final EntitySize FLIGHT_SIZE = EntitySize.fixed(0.9F, 1.5F);
    private static final DataParameter<Float> FLY_ANGLE = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> NETHER = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DRAGGING = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FLYING = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DIGGING = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SCARED = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ATTACK_TICK = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.INT);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.INT);
    private static final DataParameter<Boolean> ANGRY = EntityDataManager.defineId(EntityTarantulaHawk.class, DataSerializers.BOOLEAN);
    public float prevFlyAngle;
    public float prevSitProgress;
    public float sitProgress;
    public float prevDragProgress;
    public float dragProgress;
    public float prevFlyProgress;
    public float flyProgress;
    public float prevAttackProgress;
    public float attackProgress;
    public float prevDigProgress;
    public float digProgress;
    private boolean isLandNavigator;
    private boolean flightSize = false;
    private int timeFlying = 0;
    private boolean bredBuryFlag = false;
    private int spiderFeedings = 0;
    private int dragTime = 0;

    protected EntityTarantulaHawk(EntityType type, World worldIn) {
        super(type, worldIn);
        switchNavigator(false);
    }

    public static boolean canTarantulaHawkSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = BlockTags.SAND.contains(worldIn.getBlockState(pos.below()).getBlock());
        return (spawnBlock) && worldIn.getRawBrightness(pos, 0) > 8 || isBiomeNether(worldIn, pos) || AMConfig.fireproofTarantulaHawk;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 18.0D).add(Attributes.ARMOR, 4.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 5);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.tarantulaHawkSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        if(this.isBiomeNether(worldIn, this.blockPosition())){
            this.setNether(true);
        }
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    private static boolean isBiomeNether(IWorld worldIn, BlockPos position) {
        RegistryKey<Biome> biomeKey = RegistryKey.create(Registry.BIOME_REGISTRY, worldIn.getBiome(position).getRegistryName());
        return BiomeDictionary.hasType(biomeKey, BiomeDictionary.Type.NETHER);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new FlyingAIFollowOwner(this, 1.0D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new AIFleeRoadrunners());
        this.goalSelector.addGoal(4, new AIMelee());
        this.goalSelector.addGoal(5, new AIBury());
        this.goalSelector.addGoal(6, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(7, new TemptGoal(this, 1.1D, Ingredient.of(Items.SPIDER_EYE, Items.FERMENTED_SPIDER_EYE), false));
        this.goalSelector.addGoal(8, new AIWalkIdle());
        this.goalSelector.addGoal(9, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.addGoal(4, new EntityAINearestTarget3D(this, SpiderEntity.class, 15, true, true, null) {
            public boolean canUse() {
                return super.canUse() && !EntityTarantulaHawk.this.isBaby() && !EntityTarantulaHawk.this.isSitting();
            }
        });
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TARANTULA_HAWK_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TARANTULA_HAWK_HURT;
    }

    public boolean fireImmune() {
        return isNether() || AMConfig.fireproofTarantulaHawk;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigator(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveController();
            this.navigation = new DirectPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLY_ANGLE, 0F);
        this.entityData.define(NETHER, false);
        this.entityData.define(FLYING, false);
        this.entityData.define(SITTING, false);
        this.entityData.define(DRAGGING, false);
        this.entityData.define(DIGGING, false);
        this.entityData.define(SCARED, false);
        this.entityData.define(ANGRY, false);
        this.entityData.define(ATTACK_TICK, 0);
        this.entityData.define(COMMAND, 0);
    }

    public boolean hurt(DamageSource source, float amount) {
        if (source.getEntity() instanceof LivingEntity && ((LivingEntity) source.getEntity()).getMobType() == CreatureAttribute.ARTHROPOD && ((LivingEntity) source.getEntity()).hasEffect(AMEffectRegistry.DEBILITATING_STING)) {
            return false;
        }
        return super.hurt(source, amount);
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.ARTHROPOD;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("HawkSitting", this.isSitting());
        compound.putBoolean("Nether", this.isNether());
        compound.putBoolean("Digging", this.isDigging());
        compound.putBoolean("Flying", this.isFlying());
        compound.putInt("Command", this.getCommand());
        compound.putInt("SpiderFeedings", this.spiderFeedings);
        compound.putBoolean("BreedFlag", this.bredBuryFlag);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setOrderedToSit(compound.getBoolean("HawkSitting"));
        this.setNether(compound.getBoolean("Nether"));
        this.setDigging(compound.getBoolean("Digging"));
        this.setFlying(compound.getBoolean("Flying"));
        this.setCommand(compound.getInt("Command"));
        this.spiderFeedings = compound.getInt("SpiderFeedings");
        this.bredBuryFlag = compound.getBoolean("BreedFlag");
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

    public float getFlyAngle() {
        return this.entityData.get(FLY_ANGLE);
    }

    public void setFlyAngle(float progress) {
        this.entityData.set(FLY_ANGLE, progress);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        if (flying && isBaby()) {
            return;
        }
        this.entityData.set(FLYING, flying);
    }

    public boolean isNether() {
        return this.entityData.get(NETHER).booleanValue();
    }

    public void setNether(boolean sit) {
        this.entityData.set(NETHER, Boolean.valueOf(sit));
    }

    public boolean isScared() {
        return this.entityData.get(SCARED).booleanValue();
    }

    public void setScared(boolean sit) {
        this.entityData.set(SCARED, Boolean.valueOf(sit));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean isDragging() {
        return this.entityData.get(DRAGGING).booleanValue();
    }

    public void setDragging(boolean sit) {
        this.entityData.set(DRAGGING, Boolean.valueOf(sit));
    }

    public boolean isDigging() {
        return this.entityData.get(DIGGING).booleanValue();
    }

    public void setDigging(boolean sit) {
        this.entityData.set(DIGGING, Boolean.valueOf(sit));
    }

    public EntitySize getDimensions(Pose poseIn) {
        return isFlying() && !isBaby() ? FLIGHT_SIZE : super.getDimensions(poseIn);
    }

    public void tick() {
        prevFlyAngle = this.getFlyAngle();
        super.tick();
        prevAttackProgress = attackProgress;
        prevFlyProgress = flyProgress;
        prevSitProgress = sitProgress;
        prevDragProgress = dragProgress;
        prevDigProgress = digProgress;
        if (this.isFlying() && flyProgress < 5F) {
            flyProgress++;
        }
        if (!this.isFlying() && flyProgress > 0F) {
            flyProgress--;
        }
        if (this.isSitting() && sitProgress < 5F) {
            sitProgress++;
        }
        if (!this.isSitting() && sitProgress > 0F) {
            sitProgress--;
        }
        if (this.isDragging() && dragProgress < 5F) {
            dragProgress++;
        }
        if (!this.isDragging() && dragProgress > 0F) {
            dragProgress--;
        }
        if (this.isDigging() && digProgress < 5F) {
            digProgress++;
        }
        if (!this.isDigging() && digProgress > 0F) {
            digProgress--;
        }
        if (flightSize && !isFlying()) {
            this.refreshDimensions();
            flightSize = false;
        }
        if (!flightSize && isFlying()) {
            this.refreshDimensions();
            flightSize = true;
        }
        float threshold = 0.015F;
        if (isFlying() && this.yRotO - this.yRot > threshold) {
            this.setFlyAngle(this.getFlyAngle() + 5);
        } else if (isFlying() && this.yRotO - this.yRot < -threshold) {
            this.setFlyAngle(this.getFlyAngle() - 5);
        } else if (this.getFlyAngle() > 0) {
            this.setFlyAngle(Math.max(this.getFlyAngle() - 4, 0));
        } else if (this.getFlyAngle() < 0) {
            this.setFlyAngle(Math.min(this.getFlyAngle() + 4, 0));
        }
        this.setFlyAngle(MathHelper.clamp(this.getFlyAngle(), -30, 30));
        if (!level.isClientSide) {
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (isFlying()) {
                if(timeFlying % 25 == 0){
                    this.playSound(AMSoundRegistry.TARANTULA_HAWK_WING, this.getSoundVolume(), this.getVoicePitch());
                }
                timeFlying++;
                this.setNoGravity(true);
                if (this.isSitting() || this.isPassenger() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if (this.getTarget() != null && this.getTarget() instanceof PlayerEntity && !this.isTame()) {
                this.entityData.set(ANGRY, true);
            } else {
                this.entityData.set(ANGRY, false);
            }
        }
        if (this.entityData.get(ATTACK_TICK) > 0) {
            this.entityData.set(ATTACK_TICK, this.entityData.get(ATTACK_TICK) - 1);
            if (attackProgress < 5F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }
        if (isDigging() && level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).canOcclude()) {
            BlockPos posit = this.getBlockPosBelowThatAffectsMyMovement();
            BlockState understate = level.getBlockState(posit);
            for (int i = 0; i < 4 + random.nextInt(2); i++) {
                double particleX = posit.getX() + random.nextFloat();
                double particleY = posit.getY() + 1F;
                double particleZ = posit.getZ() + random.nextFloat();
                double motX = this.random.nextGaussian() * 0.02D;
                double motY = 0.1F + random.nextFloat() * 0.2F;
                double motZ = this.random.nextGaussian() * 0.02D;
                level.addParticle(new BlockParticleData(ParticleTypes.BLOCK, understate), particleX, particleY, particleZ, motX, motY, motZ);
            }
        }
        if(this.tickCount > 0 && tickCount % 300 == 0 && this.getHealth() < this.getMaxHealth()){
            this.heal(1);
        }
        if(!level.isClientSide && this.isDragging() && this.getPassengers().isEmpty() && !this.isDigging()){
            dragTime++;
            if(dragTime > 5000){
                dragTime = 0;
                for(Entity e : this.getPassengers()){
                    e.hurt(DamageSource.mobAttack(this), 10);
                }
                this.ejectPassengers();
                this.setDragging(false);
            }
        }
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if (!isTame() && item == Items.SPIDER_EYE) {
            this.usePlayerItem(player, itemstack);
            this.playSound(SoundEvents.STRIDER_EAT, this.getSoundVolume(), this.getVoicePitch());
            spiderFeedings++;
            if (spiderFeedings >= 15 && getRandom().nextInt(6) == 0 || spiderFeedings > 25) {
                this.tame(player);
                this.level.broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level.broadcastEntityEvent(this, (byte) 6);
            }
            return ActionResultType.SUCCESS;
        }
        if (isTame() && item.is(ItemTags.FLOWERS)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.usePlayerItem(player, itemstack);
                this.playSound(SoundEvents.STRIDER_EAT, this.getSoundVolume(), this.getVoicePitch());
                this.heal(5);
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.PASS;

        }
        if (type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player)) {
            if (player.isShiftKeyDown()) {
                if (this.getMainHandItem().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setItemInHand(Hand.MAIN_HAND, cop);
                    itemstack.shrink(1);
                    return ActionResultType.SUCCESS;
                } else {
                    this.spawnAtLocation(this.getMainHandItem().copy());
                    this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    return ActionResultType.SUCCESS;
                }
            } else if (!isFood(itemstack)) {
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 3) {
                    this.setCommand(0);
                }
                player.displayClientMessage(new TranslationTextComponent("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.setOrderedToSit(true);
                    return ActionResultType.SUCCESS;
                } else {
                    this.setOrderedToSit(false);
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return type;
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return isTame() && item == Items.FERMENTED_SPIDER_EYE;
    }


    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.CACTUS || super.isInvulnerableTo(source);
    }

    @Override
    public void spawnChildFromBreeding(ServerWorld world, AnimalEntity animalEntity) {
        bredBuryFlag = true;
        ServerPlayerEntity serverplayerentity = this.getLoveCause();
        if (serverplayerentity == null && animalEntity.getLoveCause() != null) {
            serverplayerentity = animalEntity.getLoveCause();
        }

        if (serverplayerentity != null) {
            serverplayerentity.awardStat(Stats.ANIMALS_BRED);
            CriteriaTriggers.BRED_ANIMALS.trigger(serverplayerentity, this, animalEntity, this);
        }

        this.setAge(6000);
        animalEntity.setAge(6000);
        this.resetLove();
        animalEntity.resetLove();
        world.broadcastEntityEvent(this, (byte) 7);
        world.broadcastEntityEvent(this, (byte) 18);
        if (world.getGameRules().getBoolean(GameRules.RULE_DOMOBLOOT)) {
            world.addFreshEntity(new ExperienceOrbEntity(world, this.getX(), this.getY(), this.getZ(), this.getRandom().nextInt(7) + 1));
        }

    }

    public void followEntity(TameableEntity tameable, LivingEntity owner, double followSpeed) {
        if (this.distanceTo(owner) > 5) {
            this.setFlying(true);
            this.getMoveControl().setWantedPosition(owner.getX(), owner.getY() + owner.getBbHeight(), owner.getZ(), followSpeed);
        } else {
            if (this.onGround) {
                this.setFlying(false);
            }
            if (this.isFlying() && !this.isOverWater()) {
                BlockPos vec = this.getCrowGround(this.blockPosition());
                if (vec != null) {
                    this.getMoveControl().setWantedPosition(vec.getX(), vec.getY(), vec.getZ(), followSpeed);
                }
            } else {
                this.getNavigation().moveTo(owner, followSpeed);
            }
        }
    }

    public void positionRider(Entity passenger) {
        this.xRot = 0;
        float radius = 1.0F + passenger.getBbWidth() * 0.5F;
        float angle = (0.01745329251F * (this.yBodyRot - 180));
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        double extraY = 0;
        passenger.setPos(this.getX() + extraX, this.getY() + extraY, this.getZ() + extraZ);
    }

    private boolean isOverWater() {
        BlockPos position = this.blockPosition();
        while (position.getY() > 0 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return !level.getFluidState(position).isEmpty() || position.getY() <= 0;
    }

    public Vector3d getBlockInViewAway(Vector3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getCrowGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 4 + this.getRandom().nextInt(10);
        BlockPos newPos = ground.above(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(6) + 1);
        if (!this.isTargetBlocked(Vector3d.atCenterOf(newPos)) && this.distanceToSqr(Vector3d.atCenterOf(newPos)) > 1) {
            return Vector3d.atCenterOf(newPos);
        }
        return null;
    }

    private BlockPos getCrowGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), this.getY(), in.getZ());
        while (position.getY() > 2 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return position;
    }

    public Vector3d getBlockGrounding(Vector3d fleePos) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, getY(), fleePos.z() + extraZ);
        BlockPos ground = this.getCrowGround(radialPos);
        if (ground.getY() == 0) {
            return this.position();
        } else {
            ground = this.blockPosition();
            while (ground.getY() > 2 && level.isEmptyBlock(ground)) {
                ground = ground.below();
            }
        }
        if (!this.isTargetBlocked(Vector3d.atCenterOf(ground.above()))) {
            return Vector3d.atCenterOf(ground);
        }
        return null;
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    private Vector3d getOrbitVec(Vector3d vector3d, float gatheringCircleDist, boolean orbitClockwise) {
        float angle = (0.01745329251F * (float) 2 * (orbitClockwise ? -tickCount : tickCount));
        double extraX = gatheringCircleDist * MathHelper.sin((angle));
        double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if (vector3d != null) {
            Vector3d pos = new Vector3d(vector3d.x() + extraX, vector3d.y() + random.nextInt(2) + 4, vector3d.z() + extraZ);
            if (this.level.isEmptyBlock(new BlockPos(pos))) {
                return pos;
            }
        }
        return null;
    }

    public int getCommand() {
        return this.entityData.get(COMMAND).intValue();
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, Integer.valueOf(command));
    }

    private BlockPos genSandPos(BlockPos parent) {
        IWorld world = this.level;
        Random random = new Random();
        int range = 24;
        for (int i = 0; i < 15; i++) {
            BlockPos sandAir = parent.offset(random.nextInt(range) - range / 2, -5, random.nextInt(range) - range / 2);
            while (!world.isEmptyBlock(sandAir) && sandAir.getY() < 255) {
                sandAir = sandAir.above();
            }
            BlockState state = world.getBlockState(sandAir.below());
            if (BlockTags.SAND.contains(state.getBlock())) {
                return sandAir.below();
            }
        }
        return null;
    }

    @Override
    public boolean shouldFollow() {
        return getCommand() == 1 && !this.isDragging() && !this.isDigging() && (this.getTarget() == null || !this.getTarget().isAlive());
    }

    public boolean isAngry() {
        return entityData.get(ANGRY);
    }

    class MoveController extends MovementController {
        private final MobEntity parentEntity;


        public MoveController() {
            super(EntityTarantulaHawk.this);
            this.parentEntity = EntityTarantulaHawk.this;
        }

        public void tick() {
            if (this.operation == MovementController.Action.MOVE_TO) {
                Vector3d vector3d = new Vector3d(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d0 = vector3d.length();
                double width = parentEntity.getBoundingBox().getSize();
                if (d0 < width) {
                    this.operation = MovementController.Action.WAIT;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
                } else {
                    float angle = (0.01745329251F * (parentEntity.yBodyRot + 90));
                    float radius = (float) Math.sin(parentEntity.tickCount * 0.2F) * 2;
                    double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
                    double extraZ = radius * MathHelper.cos(angle);
                    Vector3d vector3d1 = vector3d.scale(this.speedModifier * 0.05D / d0);
                    Vector3d strafPlus = new Vector3d(extraX, 0, extraZ).scale(0.003D * Math.min(d0, 100));
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(strafPlus));
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(vector3d1));
                    parentEntity.yRot = -((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * (180F / (float) Math.PI);
                    if (!EntityTarantulaHawk.this.isDragging()) {
                        parentEntity.yBodyRot = parentEntity.yRot;
                    }
                }

            }
        }
    }

    private class AIMelee extends Goal {
        private EntityTarantulaHawk hawk;
        private int orbitCooldown = 0;
        private boolean clockwise = false;
        private Vector3d orbitVec = null;
        private BlockPos sandPos = null;

        public AIMelee() {
            hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canUse() {
            return hawk.getTarget() != null && !hawk.isSitting() && !hawk.isScared() && hawk.getTarget().isAlive() && !hawk.isDragging() && !hawk.isDigging() && !hawk.getTarget().noPhysics && !hawk.getTarget().isPassenger();
        }

        @Override
        public void start() {
            hawk.setDragging(false);
            clockwise = random.nextBoolean();
        }

        @Override
        public void tick() {
            LivingEntity target = hawk.getTarget();
            boolean paralized = target != null && target.getMobType() == CreatureAttribute.ARTHROPOD && !target.noPhysics && target.hasEffect(AMEffectRegistry.DEBILITATING_STING);
            boolean paralizedWithChild = paralized && target.getEffect(AMEffectRegistry.DEBILITATING_STING).getAmplifier() > 0;
            if (sandPos == null || !BlockTags.SAND.contains(level.getBlockState(sandPos).getBlock())) {
                sandPos = hawk.genSandPos(target.blockPosition());
            }
            if (orbitCooldown > 0) {
                orbitCooldown--;
                hawk.setFlying(true);
                if (target != null) {
                    if (orbitVec == null || hawk.distanceToSqr(orbitVec) < 4F || !hawk.getMoveControl().hasWanted()) {
                        orbitVec = hawk.getOrbitVec(target.position().add(0, target.getBbHeight(), 0), 10 + random.nextInt(2), false);
                        if (orbitVec != null) {
                            hawk.getMoveControl().setWantedPosition(orbitVec.x, orbitVec.y, orbitVec.z, 1F);
                        }
                    }
                }
            } else if (((paralized && !hawk.isTame()) || (paralizedWithChild && hawk.bredBuryFlag)) && sandPos != null) {
                if (hawk.isOnGround()) {
                    hawk.setFlying(false);
                    hawk.getNavigation().moveTo(target, 1);
                } else {
                    Vector3d vector3d = hawk.getBlockGrounding(hawk.position());
                    if (vector3d != null && hawk.isFlying()) {
                        hawk.getMoveControl().setWantedPosition(vector3d.x, vector3d.y, vector3d.z, 1F);
                    }
                }
                if (hawk.distanceTo(target) < target.getBbWidth() + 1.5F && !target.isPassenger()) {
                    hawk.setDragging(true);
                    hawk.setFlying(false);
                    target.startRiding(hawk, true);
                }
            } else {
                if (target != null && !paralizedWithChild) {
                    double dist = hawk.distanceTo(target);
                    if (dist < 10 && !hawk.isFlying()) {
                        if (hawk.isOnGround()) {
                            hawk.setFlying(false);
                        }
                        hawk.getNavigation().moveTo(target, 1);
                    } else {
                        hawk.setFlying(true);
                        hawk.getMoveControl().setWantedPosition(target.getX(), target.getEyeY(), target.getZ(), 1F);
                    }
                    if (dist < target.getBbWidth() + 2.5F) {
                        if (hawk.entityData.get(ATTACK_TICK) == 0 && hawk.attackProgress == 0) {
                            hawk.entityData.set(ATTACK_TICK, 7);
                        }
                        if (hawk.attackProgress == 5F) {
                            hawk.doHurtTarget(target);
                            if(hawk.bredBuryFlag){
                                if(target.getHealth() <= 1.0F){
                                    target.heal(5);
                                }
                            }
                            target.addEffect(new EffectInstance(AMEffectRegistry.DEBILITATING_STING, target.getMobType() == CreatureAttribute.ARTHROPOD ? EntityTarantulaHawk.STING_DURATION : 600, hawk.bredBuryFlag ? 1 : 0));
                            if (!hawk.level.isClientSide && target.getMobType() == CreatureAttribute.ARTHROPOD) {
                                AlexsMobs.sendMSGToAll(new MessageTarantulaHawkSting(hawk.getId(), target.getId()));
                            }
                            orbitCooldown = target.getMobType() == CreatureAttribute.ARTHROPOD ? 200 + random.nextInt(200) : 10 + random.nextInt(20);
                        }
                    }
                }
            }


        }

        @Override
        public void stop() {
            orbitCooldown = 0;
            hawk.bredBuryFlag = false;
            clockwise = random.nextBoolean();
            orbitVec = null;
            if(hawk.getPassengers().isEmpty()){
                hawk.setTarget(null);
            }
        }
    }

    private class AIWalkIdle extends Goal {
        protected final EntityTarantulaHawk hawk;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE));
            this.hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canUse() {
            if (this.hawk.isVehicle() || hawk.isScared() || hawk.isDragging() || EntityTarantulaHawk.this.getCommand() == 1 || (hawk.getTarget() != null && hawk.getTarget().isAlive()) || this.hawk.isPassenger() || this.hawk.isSitting()) {
                return false;
            } else {
                if (this.hawk.getRandom().nextInt(30) != 0 && !hawk.isFlying()) {
                    return false;
                }
                if (this.hawk.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && hawk.timeFlying < 200;
                }
                Vector3d lvt_1_1_ = this.getPosition();
                if (lvt_1_1_ == null) {
                    return false;
                } else {
                    this.x = lvt_1_1_.x;
                    this.y = lvt_1_1_.y;
                    this.z = lvt_1_1_.z;
                    return true;
                }
            }
        }

        public void tick() {
            if (flightTarget) {
                hawk.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                this.hawk.getNavigation().moveTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && hawk.onGround) {
                hawk.setFlying(false);
            }
            if (isFlying() && hawk.onGround && hawk.timeFlying > 10) {
                hawk.setFlying(false);
            }
        }

        @Nullable
        protected Vector3d getPosition() {
            Vector3d vector3d = hawk.position();
            if (hawk.isOverWater()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (hawk.timeFlying < 50 || hawk.isOverWater()) {
                    return hawk.getBlockInViewAway(vector3d, 0);
                } else {
                    return hawk.getBlockGrounding(vector3d);
                }
            } else {

                return RandomPositionGenerator.getPos(this.hawk, 10, 7);
            }
        }

        public boolean canContinueToUse() {
            if (hawk.isSitting() || EntityTarantulaHawk.this.getCommand() == 1) {
                return false;
            }
            if (flightTarget) {
                return hawk.isFlying() && hawk.distanceToSqr(x, y, z) > 2F;
            } else {
                return (!this.hawk.getNavigation().isDone()) && !this.hawk.isVehicle();
            }
        }

        public void start() {
            if (flightTarget) {
                hawk.setFlying(true);
                hawk.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                this.hawk.getNavigation().moveTo(this.x, this.y, this.z, 1F);
            }
        }

        public void stop() {
            this.hawk.getNavigation().stop();
            super.stop();
        }
    }

    private class AIBury extends Goal {
        private EntityTarantulaHawk hawk;
        private BlockPos buryPos = null;
        private int digTime = 0;
        private double stageX;
        private double stageY;
        private double stageZ;

        private AIBury() {
            hawk = EntityTarantulaHawk.this;
        }

        @Override
        public boolean canUse() {
            if (hawk.isDragging() && hawk.getTarget() != null) {
                BlockPos pos = hawk.genSandPos(hawk.blockPosition());
                if (pos != null) {
                    buryPos = pos;
                    return true;
                }
            }
            return false;
        }

        public boolean canContinueToUse() {
            return hawk.isDragging() && digTime < 200 && hawk.getTarget() != null && buryPos != null && BlockTags.SAND.contains(level.getBlockState(buryPos).getBlock());
        }

        public void start() {
            digTime = 0;
            stageX = hawk.getX();
            stageY = hawk.getY();
            stageZ = hawk.getZ();
        }

        public void stop() {
            digTime = 0;
            hawk.setDigging(false);
            hawk.setDragging(false);
            hawk.setTarget(null);
            hawk.setLastHurtByMob(null);
        }

        public void tick() {
            hawk.setFlying(false);
            hawk.setDragging(true);
            LivingEntity target = hawk.getTarget();
            if (hawk.distanceToSqr(Vector3d.atCenterOf(buryPos)) < 9) {
                if (!hawk.isDigging()) {
                    hawk.setDigging(true);
                    stageX = target.getX();
                    stageY = target.getY();
                    stageZ = target.getZ();
                }
            }
            if (hawk.isDigging()) {
                target.noPhysics = true;
                digTime++;
                hawk.ejectPassengers();
                target.setPos(stageX, stageY - Math.min(3, digTime * 0.05F), stageZ);
                hawk.getNavigation().moveTo(stageX, stageY, stageZ, 0.85F);
            } else {
                hawk.getNavigation().moveTo(buryPos.getX(), buryPos.getY(), buryPos.getZ(), 0.5F);
            }
        }
    }

    private class AIFleeRoadrunners extends Goal {
        private int searchCooldown = 0;
        private LivingEntity fear = null;
        private Vector3d fearVec = null;

        @Override
        public boolean canUse() {
            if (searchCooldown <= 0) {
                searchCooldown = 100 + EntityTarantulaHawk.this.random.nextInt(100);
                List<EntityRoadrunner> list = EntityTarantulaHawk.this.level.getEntitiesOfClass(EntityRoadrunner.class, EntityTarantulaHawk.this.getBoundingBox().inflate(15, 32, 15));
                for (EntityRoadrunner roadrunner : list) {
                    if (fear == null || EntityTarantulaHawk.this.distanceTo(fear) > EntityTarantulaHawk.this.distanceTo(roadrunner)) {
                        fear = roadrunner;
                    }
                }
            } else {
                searchCooldown--;
            }
            return EntityTarantulaHawk.this.isAlive() && fear != null;
        }

        @Override
        public boolean canContinueToUse() {
            return fear != null && fear.isAlive() && EntityTarantulaHawk.this.distanceTo(fear) < 32F;
        }

        @Override
        public void start() {
            super.start();
            EntityTarantulaHawk.this.setScared(true);
        }

        public void tick() {
            if (fear != null) {
                if (fearVec == null || EntityTarantulaHawk.this.distanceToSqr(fearVec) < 4) {
                    fearVec = EntityTarantulaHawk.this.getBlockInViewAway(fearVec == null ? fear.position() : fearVec, 12);
                }
                if (fearVec != null) {
                    EntityTarantulaHawk.this.setFlying(true);
                    EntityTarantulaHawk.this.getMoveControl().setWantedPosition(fearVec.x, fearVec.y, fearVec.z, 1.1F);
                }
            }
        }

        public void stop() {
            EntityTarantulaHawk.this.setScared(false);
            fear = null;
            fearVec = null;
        }
    }
}
