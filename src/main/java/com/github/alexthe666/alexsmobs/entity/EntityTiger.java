package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
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
import net.minecraft.pathfinding.*;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class EntityTiger extends AnimalEntity implements ICustomCollisions, IAnimatedEntity, IAngerable, ITargetsDroppedItems {

    public static final Animation ANIMATION_PAW_R = Animation.create(15);
    public static final Animation ANIMATION_PAW_L = Animation.create(15);
    public static final Animation ANIMATION_TAIL_FLICK = Animation.create(45);
    public static final Animation ANIMATION_LEAP = Animation.create(20);
    private static final DataParameter<Boolean> WHITE = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> RUNNING = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SLEEPING = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> STEALTH_MODE = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> HOLDING = EntityDataManager.defineId(EntityTiger.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ANGER_TIME = EntityDataManager.defineId(EntityTiger.class, DataSerializers.INT);
    private static final DataParameter<Integer> LAST_SCARED_MOB_ID = EntityDataManager.defineId(EntityTiger.class, DataSerializers.INT);
    private static final RangedInteger ANGRY_TIMER = TickRangeConverter.rangeOfSeconds(40, 80);
    private static final Predicate<LivingEntity> NO_BLESSING_EFFECT = (mob) -> {
        return !mob.hasEffect(AMEffectRegistry.TIGERS_BLESSING);
    };
    public float prevSitProgress;
    public float sitProgress;
    public float prevSleepProgress;
    public float sleepProgress;
    public float prevHoldProgress;
    public float holdProgress;
    public float prevStealthProgress;
    public float stealthProgress;
    private int animationTick;
    private Animation currentAnimation;
    private boolean hasSpedUp = false;
    private UUID lastHurtBy;
    private int sittingTime;
    private int maxSitTime;
    private int holdTime = 0;
    private int prevScaredMobId = -1;
    private boolean dontSitFlag = false;

    protected EntityTiger(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, 0);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0);
        this.moveControl = new MovementControllerCustomCollisions(this);
    }

    public static boolean canTigerSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getRawBrightness(pos, 0) > 8;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 50D).add(Attributes.ATTACK_DAMAGE, 12.0D).add(Attributes.MOVEMENT_SPEED, 0.25F).add(Attributes.FOLLOW_RANGE, 86);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.tigerSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getFluidState(pos.below()).isEmpty() && worldIn.getFluidState(pos).is(FluidTags.WATER) ? 0.0F : super.getWalkTargetValue(pos, worldIn);
    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return !worldIn.containsAnyLiquid(this.getBoundingBox());
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("TigerSitting", this.isSitting());
        compound.putBoolean("TigerSleeping", this.isSleeping());
        compound.putBoolean("White", this.isWhite());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setSitting(compound.getBoolean("TigerSitting"));
        this.setSleeping(compound.getBoolean("TigerSleeping"));
        this.setWhite(compound.getBoolean("White"));
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(WHITE, Boolean.valueOf(false));
        this.entityData.define(RUNNING, Boolean.valueOf(false));
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(STEALTH_MODE, Boolean.valueOf(false));
        this.entityData.define(HOLDING, Boolean.valueOf(false));
        this.entityData.define(SLEEPING, Boolean.valueOf(false));
        this.entityData.define(ANGER_TIME, 0);
        this.entityData.define(LAST_SCARED_MOB_ID, -1);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new SwimGoal(this));
        this.goalSelector.addGoal(2, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.addGoal(3, new AIMelee());
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(7, new AnimalAIWanderRanged(this, 60, 1.0D, 14, 7));
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 25F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false, 10));
        this.targetSelector.addGoal(2, (new AngerGoal(this)));
        this.targetSelector.addGoal(3, new AttackPlayerGoal());
        this.targetSelector.addGoal(4, new NearestAttackableTargetGoal(this, LivingEntity.class, 10, false, false, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.TIGER_TARGETS))) {
            public boolean canUse() {
                return !EntityTiger.this.isBaby() && super.canUse();
            }
        });
        this.targetSelector.addGoal(5, new ResetAngerGoal<>(this, true));
    }

    protected SoundEvent getAmbientSound() {
        return isStealth() ? super.getAmbientSound() : getRemainingPersistentAngerTime() > 0 ? AMSoundRegistry.TIGER_ANGRY : AMSoundRegistry.TIGER_IDLE;
    }

    public int getAmbientSoundInterval() {
        return getRemainingPersistentAngerTime() > 0 ? 40 : 80;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.TIGER_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.TIGER_HURT;
    }



    protected float getWaterSlowDown() {
        return 0.99F;
    }

    public boolean shouldMove() {
        return !isSitting() && !isSleeping() && !this.isHolding();
    }

    public double getVisibilityPercent(@Nullable Entity lookingEntity) {
        if (this.isStealth()) {
            return 0.2D;
        }
        return super.getVisibilityPercent(lookingEntity);
    }

    public boolean isFood(ItemStack stack) {
        return ItemTags.getAllTags().getTag(AMTagRegistry.TIGER_BREEDABLES).contains(stack.getItem());
    }

    //killEntity
    public void killed(ServerWorld world, LivingEntity entity) {
        this.heal(5);
        super.killed(world, entity);
    }

    public void travel(Vector3d vec3d) {
        if (!this.shouldMove()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vector3d.ZERO;
        }
        super.travel(vec3d);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new Navigator(this, worldIn);
    }

    public boolean isWhite() {
        return this.entityData.get(WHITE).booleanValue();
    }

    public void setWhite(boolean white) {
        this.entityData.set(WHITE, Boolean.valueOf(white));
    }

    public boolean isRunning() {
        return this.entityData.get(RUNNING).booleanValue();
    }

    public void setRunning(boolean running) {
        this.entityData.set(RUNNING, Boolean.valueOf(running));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setSitting(boolean bar) {
        this.entityData.set(SITTING, Boolean.valueOf(bar));
    }

    public boolean isStealth() {
        return this.entityData.get(STEALTH_MODE).booleanValue();
    }

    public void setStealth(boolean bar) {
        this.entityData.set(STEALTH_MODE, Boolean.valueOf(bar));
    }

    public boolean isHolding() {
        return this.entityData.get(HOLDING).booleanValue();
    }

    public void setHolding(boolean running) {
        this.entityData.set(HOLDING, Boolean.valueOf(running));
    }

    public boolean isSleeping() {
        return this.entityData.get(SLEEPING).booleanValue();
    }

    public void setSleeping(boolean sleeping) {
        this.entityData.set(SLEEPING, Boolean.valueOf(sleeping));
    }

    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(ANGER_TIME);
    }

    public void setRemainingPersistentAngerTime(int time) {
        this.entityData.set(ANGER_TIME, time);
    }

    public UUID getPersistentAngerTarget() {
        return this.lastHurtBy;
    }

    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.lastHurtBy = target;
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(ANGRY_TIMER.randomValue(this.random));
    }

    protected void customServerAiStep() {
        if (!this.level.isClientSide) {
            this.updatePersistentAnger((ServerWorld) this.level, false);
        }
    }


    public void tick() {
        super.tick();
        prevSitProgress = sitProgress;
        prevSleepProgress = sleepProgress;
        prevHoldProgress = holdProgress;
        prevStealthProgress = stealthProgress;
        if (isSitting() && sitProgress < 5F) {
            sitProgress++;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress--;
        }
        if (isSleeping() && sleepProgress < 5F) {
            sleepProgress++;
        }
        if (!isSleeping() && sleepProgress > 0F) {
            sleepProgress--;
        }
        if (isHolding() && holdProgress < 5F) {
            holdProgress++;
        }
        if (!isHolding() && holdProgress > 0F) {
            holdProgress--;
        }
        if (isStealth() && stealthProgress < 10F) {
            stealthProgress += 0.25F;
        }
        if (!isStealth() && stealthProgress > 0F) {
            stealthProgress--;
        }
        if (!level.isClientSide) {
            if (isRunning() && !hasSpedUp) {
                hasSpedUp = true;
                maxUpStep = 1F;
                this.setSprinting(true);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.4F);
            }
            if (!isRunning() && hasSpedUp) {
                hasSpedUp = false;
                maxUpStep = 0.6F;
                this.setSprinting(false);
                this.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(0.25F);
            }
            if ((isSitting() || isSleeping()) && (++sittingTime > maxSitTime || this.getTarget() != null || this.isInLove() || dontSitFlag || this.isInWaterOrBubble())) {
                this.setSitting(false);
                this.setSleeping(false);
                sittingTime = 0;
                maxSitTime = 100 + random.nextInt(50);
            }
            if (this.getTarget() == null && !dontSitFlag && this.getDeltaMovement().lengthSqr() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && !this.isInWaterOrBubble() && random.nextInt(100) == 0) {
                sittingTime = 0;
                if (this.getRandom().nextBoolean()) {
                    maxSitTime = 100 + random.nextInt(550);
                    this.setSitting(true);
                    this.setSleeping(false);
                } else {
                    maxSitTime = 200 + random.nextInt(550);
                    this.setSitting(false);
                    this.setSleeping(true);
                }
            }
            if (this.getDeltaMovement().lengthSqr() < 0.03D && this.getAnimation() == NO_ANIMATION && !this.isSleeping() && !this.isSitting() && random.nextInt(100) == 0) {
                this.setAnimation(ANIMATION_TAIL_FLICK);
            }
        }
        if (this.isHolding()) {
            this.setSprinting(false);
            this.setRunning(false);
            if (!level.isClientSide && this.getTarget() != null && this.getTarget().isAlive()) {
                this.xRot = 0;
                float radius = 1.0F + this.getTarget().getBbWidth() * 0.5F;
                float angle = (0.01745329251F * this.yBodyRot);
                double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
                double extraZ = radius * MathHelper.cos(angle);
                double extraY = -0.5F;
                Vector3d minus = new Vector3d(this.getX() + extraX - this.getTarget().getX(), this.getY() + extraY - this.getTarget().getY(), this.getZ() + extraZ - this.getTarget().getZ());
                this.getTarget().setDeltaMovement(minus);
                if (holdTime % 20 == 0) {
                    this.getTarget().hurt(DamageSource.mobAttack(this), 5 + this.getRandom().nextInt(2));
                }
            }
            holdTime++;
            if (holdTime > 100) {
                holdTime = 0;
                this.setHolding(false);
            }
        } else {
            holdTime = 0;
        }
        if (prevScaredMobId != this.entityData.get(LAST_SCARED_MOB_ID) && level.isClientSide) {
            Entity e = level.getEntity(this.entityData.get(LAST_SCARED_MOB_ID));
            if (e != null) {
                double d2 = this.random.nextGaussian() * 0.1D;
                double d0 = this.random.nextGaussian() * 0.1D;
                double d1 = this.random.nextGaussian() * 0.1D;
                this.level.addParticle(AMParticleRegistry.SHOCKED, e.getX(), e.getEyeY() + e.getBbHeight() * 0.15F + (double) (this.random.nextFloat() * e.getBbHeight() * 0.15F), e.getZ(), d0, d1, d2);
            }
        }
        if(this.getTarget() != null && this.getTarget().hasEffect(AMEffectRegistry.TIGERS_BLESSING)){
            this.setTarget(null);
            this.setLastHurtByMob(null);
        }
        prevScaredMobId = this.entityData.get(LAST_SCARED_MOB_ID);
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev) {
            if (source.getEntity() != null) {
                if (source.getEntity() instanceof LivingEntity) {
                    LivingEntity hurter = (LivingEntity) source.getEntity();
                    if (hurter.hasEffect(AMEffectRegistry.TIGERS_BLESSING)) {
                        hurter.removeEffect(AMEffectRegistry.TIGERS_BLESSING);
                    }
                }
            }
            return prev;
        }
        return prev;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public BlockPos getLightPosition() {
        BlockPos pos = new BlockPos(this.position());
        if (!level.getBlockState(pos).canOcclude()) {
            return pos.above();
        }
        return pos;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        boolean whiteOther = p_241840_2_ instanceof EntityTiger && ((EntityTiger) p_241840_2_).isWhite();
        EntityTiger baby = AMEntityRegistry.TIGER.create(p_241840_1_);
        double whiteChance = 0.1D;
        if (this.isWhite() && whiteOther) {
            whiteChance = 0.8D;
        }
        if (this.isWhite() != whiteOther) {
            whiteChance = 0.4D;
        }
        baby.setWhite(random.nextDouble() < whiteChance);
        return baby;
    }

    public Vector3d collide(Vector3d vec) {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        ISelectionContext iselectioncontext = ISelectionContext.of(this);
        VoxelShape voxelshape = this.level.getWorldBorder().getCollisionShape();
        Stream<VoxelShape> stream = VoxelShapes.joinIsNotEmpty(voxelshape, VoxelShapes.create(axisalignedbb.deflate(1.0E-7D)), IBooleanFunction.AND) ? Stream.empty() : Stream.of(voxelshape);
        Stream<VoxelShape> stream1 = this.level.getEntityCollisions(this, axisalignedbb.expandTowards(vec), (p_233561_0_) -> {
            return true;
        });
        ReuseableStream<VoxelShape> reuseablestream = new ReuseableStream<>(Stream.concat(stream1, stream));
        Vector3d vector3d = vec.lengthSqr() == 0.0D ? vec : collideBoundingBoxHeuristicallyPassable(this, vec, axisalignedbb, this.level, iselectioncontext, reuseablestream);
        boolean flag = vec.x != vector3d.x;
        boolean flag1 = vec.y != vector3d.y;
        boolean flag2 = vec.z != vector3d.z;
        boolean flag3 = this.onGround || flag1 && vec.y < 0.0D;
        if (this.maxUpStep > 0.0F && flag3 && (flag || flag2)) {
            Vector3d vector3d1 = collideBoundingBoxHeuristicallyPassable(this, new Vector3d(vec.x, this.maxUpStep, vec.z), axisalignedbb, this.level, iselectioncontext, reuseablestream);
            Vector3d vector3d2 = collideBoundingBoxHeuristicallyPassable(this, new Vector3d(0.0D, this.maxUpStep, 0.0D), axisalignedbb.expandTowards(vec.x, 0.0D, vec.z), this.level, iselectioncontext, reuseablestream);
            if (vector3d2.y < (double) this.maxUpStep) {
                Vector3d vector3d3 = collideBoundingBoxHeuristicallyPassable(this, new Vector3d(vec.x, 0.0D, vec.z), axisalignedbb.move(vector3d2), this.level, iselectioncontext, reuseablestream).add(vector3d2);
                if (getHorizontalDistanceSqr(vector3d3) > getHorizontalDistanceSqr(vector3d1)) {
                    vector3d1 = vector3d3;
                }
            }

            if (getHorizontalDistanceSqr(vector3d1) > getHorizontalDistanceSqr(vector3d)) {
                return vector3d1.add(collideBoundingBoxHeuristicallyPassable(this, new Vector3d(0.0D, -vector3d1.y + vec.y, 0.0D), axisalignedbb.move(vector3d1), this.level, iselectioncontext, reuseablestream));
            }
        }

        return vector3d;
    }

    @Override
    public boolean canPassThrough(BlockPos mutablePos, BlockState blockstate, VoxelShape voxelshape) {
        return blockstate.getBlock() == Blocks.BAMBOO || blockstate.is(BlockTags.LEAVES);
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
        return new Animation[]{ANIMATION_PAW_R, ANIMATION_PAW_L, ANIMATION_LEAP, ANIMATION_TAIL_FLICK};
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    public void push(Entity entityIn) {
        if (!this.isHolding() || entityIn != this.getTarget()) {
            super.push(entityIn);
        }
    }

    @Override
    protected void doPush(Entity entityIn) {
        if (!this.isHolding() || entityIn != this.getTarget()) {
            super.doPush(entityIn);
        }
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat() && stack.getItem() != Items.ROTTEN_FLESH;
    }

    public double getMaxDistToItem() {
        return 3.0D;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.dontSitFlag = false;
        ItemStack stack = e.getItem();
        if (stack.getItem().isEdible() && stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat() && stack.getItem() != Items.ROTTEN_FLESH) {
            this.playSound(SoundEvents.CAT_EAT, this.getVoicePitch(), this.getSoundVolume());
            this.heal(5);
            if (e.getThrower() != null && random.nextFloat() < getChanceForEffect(stack) && level.getPlayerByUUID(e.getThrower()) != null) {
                PlayerEntity player = level.getPlayerByUUID(e.getThrower());
                player.addEffect(new EffectInstance(AMEffectRegistry.TIGERS_BLESSING, 12000));
                this.setTarget(null);
                this.setLastHurtByMob(null);
            }
        }
    }

    public void onFindTarget(ItemEntity e) {
        this.dontSitFlag = true;
        this.setSitting(false);
        this.setSleeping(false);
    }

    public double getChanceForEffect(ItemStack stack) {
        if (stack.getItem() == Items.PORKCHOP || stack.getItem() == Items.COOKED_PORKCHOP) {
            return 0.4F;
        }
        if (stack.getItem() == Items.CHICKEN || stack.getItem() == Items.COOKED_CHICKEN) {
            return 0.3F;
        }
        return 0.1F;
    }

    protected void jumpFromGround() {
        if (!this.isSleeping() && !this.isSitting()) {
            super.jumpFromGround();
        }
    }

    static class NodeProcessor extends WalkNodeProcessor {

        private NodeProcessor() {
        }

        public static PathNodeType getBlockPathTypeStatic(IBlockReader p_237231_0_, BlockPos.Mutable p_237231_1_) {
            int i = p_237231_1_.getX();
            int j = p_237231_1_.getY();
            int k = p_237231_1_.getZ();
            PathNodeType pathnodetype = getNodes(p_237231_0_, p_237231_1_);
            if (pathnodetype == PathNodeType.OPEN && j >= 1) {
                PathNodeType pathnodetype1 = getNodes(p_237231_0_, p_237231_1_.set(i, j - 1, k));
                pathnodetype = pathnodetype1 != PathNodeType.WALKABLE && pathnodetype1 != PathNodeType.OPEN && pathnodetype1 != PathNodeType.WATER && pathnodetype1 != PathNodeType.LAVA ? PathNodeType.WALKABLE : PathNodeType.OPEN;
                if (pathnodetype1 == PathNodeType.DAMAGE_FIRE) {
                    pathnodetype = PathNodeType.DAMAGE_FIRE;
                }

                if (pathnodetype1 == PathNodeType.DAMAGE_CACTUS) {
                    pathnodetype = PathNodeType.DAMAGE_CACTUS;
                }

                if (pathnodetype1 == PathNodeType.DAMAGE_OTHER) {
                    pathnodetype = PathNodeType.DAMAGE_OTHER;
                }

                if (pathnodetype1 == PathNodeType.STICKY_HONEY) {
                    pathnodetype = PathNodeType.STICKY_HONEY;
                }
            }

            if (pathnodetype == PathNodeType.WALKABLE) {
                pathnodetype = checkNeighbourBlocks(p_237231_0_, p_237231_1_.set(i, j, k), pathnodetype);
            }

            return pathnodetype;
        }

        protected static PathNodeType getNodes(IBlockReader p_237238_0_, BlockPos p_237238_1_) {
            BlockState blockstate = p_237238_0_.getBlockState(p_237238_1_);
            PathNodeType type = blockstate.getAiPathNodeType(p_237238_0_, p_237238_1_);
            if (type != null) return type;
            Block block = blockstate.getBlock();
            Material material = blockstate.getMaterial();
            if (blockstate.isAir(p_237238_0_, p_237238_1_)) {
                return PathNodeType.OPEN;
            } else if (blockstate.getBlock() == Blocks.BAMBOO) {
                return PathNodeType.OPEN;
            } else {
                return getBlockPathTypeRaw(p_237238_0_, p_237238_1_);
            }
        }

        public PathNodeType getBlockPathType(IBlockReader blockaccessIn, int x, int y, int z) {
            return getBlockPathTypeStatic(blockaccessIn, new BlockPos.Mutable(x, y, z));
        }

        protected PathNodeType evaluateBlockPathType(IBlockReader world, boolean b1, boolean b2, BlockPos pos, PathNodeType nodeType) {
            return nodeType == PathNodeType.LEAVES || world.getBlockState(pos).getBlock() == Blocks.BAMBOO ? PathNodeType.OPEN : super.evaluateBlockPathType(world, b1, b2, pos, nodeType);
        }
    }

    class Navigator extends GroundPathNavigatorWide {

        public Navigator(MobEntity mob, World world) {
            super(mob, world, 1.2F);
        }

        protected PathFinder createPathFinder(int i) {
            this.nodeEvaluator = new NodeProcessor();
            return new PathFinder(this.nodeEvaluator, i);
        }

        protected boolean canMoveDirectly(Vector3d posVec31, Vector3d posVec32, int sizeX, int sizeY, int sizeZ) {
            int i = MathHelper.floor(posVec31.x);
            int j = MathHelper.floor(posVec31.z);
            double d0 = posVec32.x - posVec31.x;
            double d1 = posVec32.z - posVec31.z;
            double d2 = d0 * d0 + d1 * d1;
            if (d2 < 1.0E-8D) {
                return false;
            } else {
                double d3 = 1.0D / Math.sqrt(d2);
                d0 = d0 * d3;
                d1 = d1 * d3;
                sizeX = sizeX + 2;
                sizeZ = sizeZ + 2;
                if (!this.isSafeToStandAt(i, MathHelper.floor(posVec31.y), j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
                    return false;
                } else {
                    sizeX = sizeX - 2;
                    sizeZ = sizeZ - 2;
                    double d4 = 1.0D / Math.abs(d0);
                    double d5 = 1.0D / Math.abs(d1);
                    double d6 = (double) i - posVec31.x;
                    double d7 = (double) j - posVec31.z;
                    if (d0 >= 0.0D) {
                        ++d6;
                    }

                    if (d1 >= 0.0D) {
                        ++d7;
                    }

                    d6 = d6 / d0;
                    d7 = d7 / d1;
                    int k = d0 < 0.0D ? -1 : 1;
                    int l = d1 < 0.0D ? -1 : 1;
                    int i1 = MathHelper.floor(posVec32.x);
                    int j1 = MathHelper.floor(posVec32.z);
                    int k1 = i1 - i;
                    int l1 = j1 - j;

                    while (k1 * k > 0 || l1 * l > 0) {
                        if (d6 < d7) {
                            d6 += d4;
                            i += k;
                            k1 = i1 - i;
                        } else {
                            d7 += d5;
                            j += l;
                            l1 = j1 - j;
                        }

                        if (!this.isSafeToStandAt(i, MathHelper.floor(posVec31.y), j, sizeX, sizeY, sizeZ, posVec31, d0, d1)) {
                            return false;
                        }
                    }

                    return true;
                }
            }
        }

        private boolean isPositionClear(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vector3d p_179692_7_, double p_179692_8_, double p_179692_10_) {
            for (BlockPos blockpos : BlockPos.betweenClosed(new BlockPos(x, y, z), new BlockPos(x + sizeX - 1, y + sizeY - 1, z + sizeZ - 1))) {
                double d0 = (double) blockpos.getX() + 0.5D - p_179692_7_.x;
                double d1 = (double) blockpos.getZ() + 0.5D - p_179692_7_.z;
                if (!(d0 * p_179692_8_ + d1 * p_179692_10_ < 0.0D) && !this.level.getBlockState(blockpos).isPathfindable(this.level, blockpos, PathType.LAND) || EntityTiger.this.canPassThrough(blockpos, level.getBlockState(blockpos), null)) {
                    return false;
                }
            }

            return true;
        }

        private boolean isSafeToStandAt(int x, int y, int z, int sizeX, int sizeY, int sizeZ, Vector3d vec31, double p_179683_8_, double p_179683_10_) {
            int i = x - sizeX / 2;
            int j = z - sizeZ / 2;
            if (!this.isPositionClear(i, y, j, sizeX, sizeY, sizeZ, vec31, p_179683_8_, p_179683_10_)) {
                return false;
            } else {
                BlockPos.Mutable mutable = new BlockPos.Mutable();
                for (int k = i; k < i + sizeX; ++k) {
                    for (int l = j; l < j + sizeZ; ++l) {
                        double d0 = (double) k + 0.5D - vec31.x;
                        double d1 = (double) l + 0.5D - vec31.z;
                        if (!(d0 * p_179683_8_ + d1 * p_179683_10_ < 0.0D)) {
                            PathNodeType pathnodetype = this.nodeEvaluator.getBlockPathType(this.level, k, y - 1, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                            mutable.set(k, y - 1, l);
                            if (!this.hasValidPathType(pathnodetype) || EntityTiger.this.canPassThrough(mutable, level.getBlockState(mutable), null)) {
                                return false;
                            }

                            pathnodetype = this.nodeEvaluator.getBlockPathType(this.level, k, y, l, this.mob, sizeX, sizeY, sizeZ, true, true);
                            float f = this.mob.getPathfindingMalus(pathnodetype);
                            if (f < 0.0F || f >= 8.0F) {
                                return false;
                            }

                            if (pathnodetype == PathNodeType.DAMAGE_FIRE || pathnodetype == PathNodeType.DANGER_FIRE || pathnodetype == PathNodeType.DAMAGE_OTHER) {
                                return false;
                            }
                        }
                    }
                }

                return true;
            }
        }

        protected boolean hasValidPathType(PathNodeType p_230287_1_) {
            if (p_230287_1_ == PathNodeType.WATER) {
                return false;
            } else if (p_230287_1_ == PathNodeType.LAVA) {
                return false;
            } else {
                return p_230287_1_ != PathNodeType.OPEN;
            }
        }
    }

    private class AIMelee extends Goal {
        private EntityTiger tiger;
        private int jumpAttemptCooldown = 0;

        public AIMelee() {
            tiger = EntityTiger.this;
        }

        @Override
        public boolean canUse() {
            return tiger.getTarget() != null && tiger.getTarget().isAlive();
        }

        public void tick() {
            if (jumpAttemptCooldown > 0) {
                jumpAttemptCooldown--;
            }
            LivingEntity target = tiger.getTarget();
            if (target != null && target.isAlive()) {
                double dist = tiger.distanceTo(target);
                if (tiger.getLastHurtByMob() != null && tiger.getLastHurtByMob().isAlive() && dist < 10) {
                    tiger.setStealth(false);
                } else {
                    if (dist > 20) {
                        tiger.setRunning(false);
                        tiger.setStealth(true);
                    }
                }
                if (dist <= 20) {
                    tiger.setStealth(false);
                    tiger.setRunning(true);
                    if (tiger.entityData.get(LAST_SCARED_MOB_ID) != target.getId()) {
                        tiger.entityData.set(LAST_SCARED_MOB_ID, target.getId());
                        target.addEffect(new EffectInstance(AMEffectRegistry.FEAR, 100, 0, true, false));
                    }
                }
                if (dist < 12 && tiger.getAnimation() == NO_ANIMATION && tiger.isOnGround() && jumpAttemptCooldown == 0 && !tiger.isHolding()) {
                    tiger.setAnimation(ANIMATION_LEAP);
                    jumpAttemptCooldown = 70;
                }
                if ((jumpAttemptCooldown > 0 || tiger.isInWaterOrBubble()) && !tiger.isHolding() && tiger.getAnimation() == NO_ANIMATION && dist < 4 + target.getBbWidth()) {
                    tiger.setAnimation(tiger.getRandom().nextBoolean() ? ANIMATION_PAW_L : ANIMATION_PAW_R);
                }
                if (dist < 4 + target.getBbWidth() && (tiger.getAnimation() == ANIMATION_PAW_L || tiger.getAnimation() == ANIMATION_PAW_R) && tiger.getAnimationTick() == 8) {
                    target.hurt(DamageSource.mobAttack(tiger), 7 + tiger.getRandom().nextInt(5));
                }
                if (tiger.getAnimation() == ANIMATION_LEAP) {
                    tiger.getNavigation().stop();
                    Vector3d vec = target.position().subtract(tiger.position());
                    tiger.yRot = -((float) MathHelper.atan2(vec.x, vec.z)) * (180F / (float) Math.PI);
                    tiger.yBodyRot = tiger.yRot;

                    if (tiger.getAnimationTick() == 5 && tiger.onGround) {
                        Vector3d vector3d1 = new Vector3d(target.getX() - this.tiger.getX(), 0.0D, target.getZ() - this.tiger.getZ());
                        if (vector3d1.lengthSqr() > 1.0E-7D) {
                            vector3d1 = vector3d1.normalize().scale(Math.min(dist, 15) * 0.2F);
                        }
                        this.tiger.setDeltaMovement(vector3d1.x, vector3d1.y + 0.3F + 0.1F * MathHelper.clamp(target.getEyeY() - this.tiger.getY(), 0, 2), vector3d1.z);
                    }
                    if (dist < target.getBbWidth() + 3 && tiger.getAnimationTick() >= 15) {
                        target.hurt(DamageSource.mobAttack(tiger), 2);
                        tiger.setRunning(false);
                        tiger.setStealth(false);
                        tiger.setHolding(true);
                    }
                } else {
                    if (tiger.isHolding()) {
                        tiger.getNavigation().stop();
                    } else {
                        try{
                            tiger.getNavigation().moveTo(target, tiger.isStealth() ? 0.75F : 1.0F);
                        }catch (Exception e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        public void stop() {
            tiger.setStealth(false);
            tiger.setRunning(false);
            tiger.setHolding(false);
        }
    }

    class AttackPlayerGoal extends NearestAttackableTargetGoal<PlayerEntity> {

        public AttackPlayerGoal() {
            super(EntityTiger.this, PlayerEntity.class, 100, false, true, NO_BLESSING_EFFECT);
        }

        public boolean canUse() {
            if (EntityTiger.this.isBaby()) {
                return false;
            } else {
                return super.canUse();
            }
        }

        protected double getFollowDistance() {
            return 4.0D;
        }
    }

    class AngerGoal extends HurtByTargetGoal {
        AngerGoal(EntityTiger beeIn) {
            super(beeIn);
        }

        public boolean canContinueToUse() {
            return EntityTiger.this.isAngry() && super.canContinueToUse();
        }

        public void start() {
            super.start();
            if (EntityTiger.this.isBaby()) {
                this.alertOthers();
                this.stop();
            }

        }

        protected void alertOther(MobEntity mobIn, LivingEntity targetIn) {
            if (!mobIn.isBaby()) {
                super.alertOther(mobIn, targetIn);
            }
        }
    }
}
