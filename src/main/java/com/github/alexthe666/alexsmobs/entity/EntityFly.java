package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.FlyingMovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.SpiderEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.FlyingPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.ForgeBlockTagsProvider;

import javax.annotation.Nullable;
import java.util.*;

public class EntityFly extends AnimalEntity implements IFlyingAnimal {

    private int conversionTime = 0;
    private static final DataParameter<Boolean> NO_DESPAWN = EntityDataManager.defineId(EntityFly.class, DataSerializers.BOOLEAN);

    protected EntityFly(EntityType type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new FlyingMovementController(this, 20, true);
        this.setPathfindingMalus(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathNodeType.COCOA, -1.0F);
        this.setPathfindingMalus(PathNodeType.FENCE, -1.0F);
    }

    protected void playStepSound(BlockPos pos, BlockState blockIn) {}

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("NoFlyDespawn", this.isNoDespawn());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setNoDespawn(compound.getBoolean("NoFlyDespawn"));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(NO_DESPAWN, Boolean.valueOf(false));
    }

        public boolean isNoDespawn() {
        return this.entityData.get(NO_DESPAWN).booleanValue();
    }

    public void setNoDespawn(boolean despawn) {
        this.entityData.set(NO_DESPAWN, despawn);
    }


    public static boolean canFlySpawn(EntityType<EntityFly> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return reason == SpawnReason.SPAWNER || pos.getY() > 63 && random.nextInt(4) == 0 && worldIn.getRawBrightness(pos, 0) > 8 && worldIn.getBrightness(LightType.BLOCK, pos) == 0 && (BlockTags.SAND.contains(worldIn.getBlockState(pos.below()).getBlock()) || Tags.Blocks.DIRT.contains(worldIn.getBlockState(pos.below()).getBlock()));
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !requiresCustomPersistence();
    }

    public boolean requiresCustomPersistence() {
        return this.isNoDespawn() || super.requiresCustomPersistence();
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.flySpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean isInNether() {
        return this.level.dimension() == World.NETHER && !this.isNoAi();
    }


    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.FLY_IDLE;
    }

    public int getAmbientSoundInterval() {
        return 30;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.FLY_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.FLY_HURT;
    }

    public int getMaxSpawnClusterSize() {
        return 2;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 2.0D).add(Attributes.FLYING_SPEED, 0.8F).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        return worldIn.getBlockState(pos).isAir() ? 10.0F : 0.0F;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new TemptGoal(this, 1.25D, Ingredient.of(Items.ROTTEN_FLESH, Items.SUGAR), false));
        this.goalSelector.addGoal(3, new FollowParentGoal(this, 1.25D));
        this.goalSelector.addGoal(3, new AvoidEntityGoal<>(this, SpiderEntity.class, 6.0F, 1.0D, 1.2D));
        this.goalSelector.addGoal(4, new AnnoyZombieGoal());
        this.goalSelector.addGoal(5, new WanderGoal());
        this.goalSelector.addGoal(6, new SwimGoal(this));
    }

    protected PathNavigator createNavigation(World worldIn) {
        FlyingPathNavigator flyingpathnavigator = new FlyingPathNavigator(this, worldIn) {
            public boolean isStableDestination(BlockPos pos) {
                return !this.level.getBlockState(pos.below()).isAir();
            }
        };
        flyingpathnavigator.setCanOpenDoors(false);
        flyingpathnavigator.setCanFloat(false);
        flyingpathnavigator.setCanPassDoors(true);
        return flyingpathnavigator;
    }

    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return this.isBaby() ? sizeIn.height * 0.5F : sizeIn.height * 0.5F;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
        fallDistance = 0;
    }

    public void tick(){
        super.tick();

        if(this.isBaby() && this.getEyeHeight() > this.getBbHeight()){
            this.refreshDimensions();
        }
        if(this.isInLove() && !this.isNoDespawn()){
            this.setNoDespawn(true);
        }
        if(isInNether()){
            this.setNoDespawn(true);
            conversionTime++;
            if(conversionTime > 300){
                EntityCrimsonMosquito mosquito = AMEntityRegistry.CRIMSON_MOSQUITO.create(level);
                mosquito.copyPosition(this);
                if(!level.isClientSide){
                    mosquito.finalizeSpawn((IServerWorld)level, level.getCurrentDifficultyAt(this.blockPosition()), SpawnReason.CONVERSION, null, null);
                }
                level.addFreshEntity(mosquito);
                mosquito.onSpawnFromFly();
                this.remove();
            }
        }
    }

    public ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack lvt_3_1_ = p_230254_1_.getItemInHand(p_230254_2_);
        if(lvt_3_1_.getItem() == Items.SUGAR){
            if(!p_230254_1_.isCreative()){
                lvt_3_1_.shrink(1);
            }
            this.setNoDespawn(true);
            this.heal(2);
            return ActionResultType.SUCCESS;
        }
        return super.mobInteract(p_230254_1_, p_230254_2_);

    }

    protected boolean makeFlySound() {
        return true;
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.ARTHROPOD;
    }

    protected void jumpInLiquid(ITag<Fluid> fluidTag) {
        this.setDeltaMovement(this.getDeltaMovement().add(0.0D, 0.01D, 0.0D));
    }

    @OnlyIn(Dist.CLIENT)
    public Vector3d getLeashOffset() {
        return new Vector3d(0.0D, 0.5F * this.getEyeHeight(), this.getBbWidth() * 0.2F);
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.ROTTEN_FLESH;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.FLY.create(p_241840_1_);
    }

    class WanderGoal extends Goal {
        WanderGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            return EntityFly.this.navigation.isDone() && EntityFly.this.random.nextInt(3) == 0;
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        public boolean canContinueToUse() {
            return EntityFly.this.navigation.isInProgress();
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            Vector3d vector3d = this.getRandomLocation();
            if (vector3d != null) {
                EntityFly.this.navigation.moveTo(EntityFly.this.navigation.createPath(new BlockPos(vector3d), 1), 1.0D);
            }

        }

        @Nullable
        private Vector3d getRandomLocation() {
            Vector3d vector3d = EntityFly.this.getViewVector(0.0F);
            int i = 8;
            Vector3d vector3d2 = RandomPositionGenerator.getAboveLandPos(EntityFly.this, 3, 3, vector3d, ((float) Math.PI / 2F), 1, 1);
            return vector3d2 != null ? vector3d2 : RandomPositionGenerator.getAirPos(EntityFly.this, 3, 3, -3, vector3d, (float) Math.PI / 2F);
        }
    }

    private class AnnoyZombieGoal extends Goal {
        protected final Sorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private int cooldown = 0;
        private ITag tag;

        AnnoyZombieGoal() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            tag = EntityTypeTags.getAllTags().getTag(AMTagRegistry.FLY_TARGETS);
            this.theNearestAttackableTargetSorter = new Sorter(EntityFly.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e.isAlive() && tag != null && e.getType().is(tag) && (!(e instanceof LivingEntity) || ((LivingEntity) e).getHealth() >= 2D);
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntityFly.this.isPassenger() || EntityFly.this.isVehicle()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityFly.this.level.getGameTime() % 10;
                if (EntityFly.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityFly.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityFly.this.level.getEntitiesOfClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
            if (list.isEmpty()) {
                return false;
            } else {
                Collections.sort(list, this.theNearestAttackableTargetSorter);
                this.targetEntity = list.get(0);
                this.mustUpdate = false;
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return targetEntity != null;
        }

        public void stop() {
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if(cooldown > 0){
                cooldown--;
            }
            if(targetEntity != null){
                if (EntityFly.this.getNavigation().isDone()) {
                    int i = EntityFly.this.getRandom().nextInt(3) - 1;
                    int k = EntityFly.this.getRandom().nextInt(3) - 1;
                    int l = (int) ((EntityFly.this.getRandom().nextInt(3) - 1) * Math.ceil(targetEntity.getBbHeight()));
                    EntityFly.this.getNavigation().moveTo(this.targetEntity.getX() + i, this.targetEntity.getY() + l, this.targetEntity.getZ() + k, 1);
                }
                if(EntityFly.this.distanceToSqr(targetEntity) < 3.0F){
                    if(targetEntity instanceof LivingEntity && ((LivingEntity) targetEntity).getHealth() > 2D){
                        if(cooldown == 0){
                            targetEntity.hurt(DamageSource.GENERIC, 1);
                            cooldown = 100;
                        }
                    }else{
                        this.stop();
                    }

                }
            }
        }

        protected double getTargetDistance() {
            return 16D;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntityFly.this.getX() + 0.5, EntityFly.this.getY() + 0.5, EntityFly.this.getZ() + 0.5D);
            double renderRadius = 5;
            AxisAlignedBB aabb = new AxisAlignedBB(-renderRadius, -renderRadius, -renderRadius, renderRadius, renderRadius, renderRadius);
            return aabb.move(renderCenter);
        }


        public class Sorter implements Comparator<Entity> {
            private final Entity theEntity;

            public Sorter(Entity theEntityIn) {
                this.theEntity = theEntityIn;
            }

            public int compare(Entity p_compare_1_, Entity p_compare_2_) {
                double d0 = this.theEntity.distanceToSqr(p_compare_1_);
                double d1 = this.theEntity.distanceToSqr(p_compare_2_);
                return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
            }
        }
    }
}
