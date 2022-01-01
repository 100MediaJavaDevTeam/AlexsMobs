package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AquaticMoveController;
import com.github.alexthe666.alexsmobs.entity.ai.BoneSerpentPathNavigator;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.ai.goal.RandomWalkingGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.WaterMobEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.fluid.Fluid;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class EntityStradpole extends WaterMobEntity {

    private static final DataParameter<Boolean> FROM_BUCKET = EntityDataManager.defineId(EntityStradpole.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DESPAWN_SOON = EntityDataManager.defineId(EntityStradpole.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> LAUNCHED = EntityDataManager.defineId(EntityStradpole.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<UUID>> PARENT_UUID = EntityDataManager.defineId(EntityStradpole.class, DataSerializers.OPTIONAL_UUID);
    public float swimPitch = 0;
    public float prevSwimPitch = 0;
    private int despawnTimer = 0;
    private int ricochetCount = 0;
    protected EntityStradpole(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.LAVA, 0.0F);
        this.moveControl = new AquaticMoveController(this, 1.4F);
    }

    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return SoundEvents.COD_HURT;
    }

    public int getMaxSpawnClusterSize() {
        return 2;
    }

    protected ItemStack getFishBucket(){
        ItemStack stack = new ItemStack(AMItemRegistry.STRADPOLE_BUCKET);
        if (this.hasCustomName()) {
            stack.setHoverName(this.getCustomName());
        }
        return stack;
    }

    protected ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack itemstack = p_230254_1_.getItemInHand(p_230254_2_);
        if(itemstack.getItem() == AMItemRegistry.MOSQUITO_LARVA){
            if(!p_230254_1_.isCreative()){
                itemstack.shrink(1);
            }
            if(random.nextFloat() < 0.45F){
                EntityStraddler straddler = AMEntityRegistry.STRADDLER.create(level);
                straddler.copyPosition(this);
                if(!level.isClientSide){
                    level.addFreshEntity(straddler);
                }
                this.remove();
            }
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        if (itemstack.getItem() == Items.LAVA_BUCKET && this.isAlive()) {
            this.playSound(SoundEvents.BUCKET_FILL_FISH, 1.0F, 1.0F);
            itemstack.shrink(1);
            ItemStack itemstack1 = this.getFishBucket();
            if (!this.level.isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity)p_230254_1_, itemstack1);
            }

            if (itemstack.isEmpty()) {
                p_230254_1_.setItemInHand(p_230254_2_, itemstack1);
            } else if (!p_230254_1_.inventory.add(itemstack1)) {
                p_230254_1_.drop(itemstack1, false);
            }

            this.remove();
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        } else {
            return super.mobInteract(p_230254_1_, p_230254_2_);
        }
    }


    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.3F);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(DESPAWN_SOON, false);
        this.entityData.define(LAUNCHED, false);
        this.entityData.define(FROM_BUCKET, false);
    }

    private boolean isFromBucket() {
        return this.entityData.get(FROM_BUCKET);
    }

    public void setFromBucket(boolean p_203706_1_) {
        this.entityData.set(FROM_BUCKET, p_203706_1_);
    }


    @Nullable
    public UUID getParentId() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        if (this.getParentId() != null) {
            compound.putUUID("ParentUUID", this.getParentId());
        }
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("DespawnSoon", this.isDespawnSoon());
    }

    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.isFromBucket();
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.stradpoleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canStradpoleSpawn(EntityType<EntityStradpole> p_234314_0_, IWorld p_234314_1_, SpawnReason p_234314_2_, BlockPos p_234314_3_, Random p_234314_4_) {
        if(p_234314_1_.getFluidState(p_234314_3_).is(FluidTags.LAVA)){
            if(!p_234314_1_.getFluidState(p_234314_3_.below()).is(FluidTags.LAVA)){

                return p_234314_1_.isEmptyBlock(p_234314_3_.above());
            }
        }
        return false;
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("ParentUUID")) {
            this.setParentId(compound.getUUID("ParentUUID"));
        }
        this.setFromBucket(compound.getBoolean("FromBucket"));
        this.setDespawnSoon(compound.getBoolean("DespawnSoon"));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new StradpoleAISwim(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        if (!worldIn.getBlockState(pos).getFluidState().isEmpty()) {
            return 15.0F;
        } else {
            return Float.NEGATIVE_INFINITY;
        }
    }

    public boolean isDespawnSoon() {
        return this.entityData.get(DESPAWN_SOON);
    }

    public void setDespawnSoon(boolean despawnSoon) {
        this.entityData.set(DESPAWN_SOON, despawnSoon);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new BoneSerpentPathNavigator(this, worldIn);
    }

    public void tick() {
        float f = 1.0F;
        if (entityData.get(LAUNCHED)) {
            this.yBodyRot = this.yRot;
            RayTraceResult raytraceresult = ProjectileHelper.getHitResult(this, this::canHitEntity);
            if (raytraceresult != null && raytraceresult.getType() != RayTraceResult.Type.MISS) {
                this.onImpact(raytraceresult);
            }
            f = 0.1F;
        }
        super.tick();
        boolean liquid = this.isInWater() || this.isInLava();
        prevSwimPitch = this.swimPitch;

        float f2 = (float) -((float) this.getDeltaMovement().y * (liquid ? 2.5F : f) * (double) (180F / (float) Math.PI));
        this.swimPitch = f2;
        if (this.onGround && !this.isInWater() && !this.isInLava()) {
            this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
            this.yRot = this.random.nextFloat() * 360.0F;
            this.onGround = false;
            this.hasImpulse = true;
        }
        this.setNoGravity(false);
        if (liquid) {
            this.setNoGravity(true);
        }
        if (isDespawnSoon()) {
            despawnTimer++;
            if (despawnTimer > 100) {
                despawnTimer = 0;
                this.spawnAnim();
                this.remove();
            }
        }
    }

    private void onImpact(RayTraceResult raytraceresult) {
        RayTraceResult.Type raytraceresult$type = raytraceresult.getType();
        if (raytraceresult$type == RayTraceResult.Type.ENTITY) {
            this.onEntityHit((EntityRayTraceResult) raytraceresult);
        } else if (raytraceresult$type == RayTraceResult.Type.BLOCK) {
            BlockRayTraceResult traceResult = (BlockRayTraceResult) raytraceresult;
            BlockState blockstate = this.level.getBlockState(traceResult.getBlockPos());
            if (!blockstate.getBlockSupportShape(this.level, traceResult.getBlockPos()).isEmpty()) {
                Direction face = traceResult.getDirection();
                Vector3d prevMotion = this.getDeltaMovement();
                double motionX = prevMotion.x();
                double motionY = prevMotion.y();
                double motionZ = prevMotion.z();
                switch(face){
                    case EAST:
                    case WEST:
                        motionX = -motionX;
                        break;
                    case SOUTH:
                    case NORTH:
                        motionZ = -motionZ;
                        break;
                    default:
                        motionY = -motionY;
                        break;
                }
                this.setDeltaMovement(motionX, motionY, motionZ);
                if (this.tickCount > 200 || ricochetCount > 20) {
                   this.entityData.set(LAUNCHED, false);
                } else {
                    ricochetCount++;
                }
            }
        }
    }

    public Entity getParent() {
        UUID id = getParentId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
    }

    private void onEntityHit(EntityRayTraceResult raytraceresult) {
        Entity entity = this.getParent();
        if (entity instanceof LivingEntity && !level.isClientSide && raytraceresult.getEntity() instanceof LivingEntity) {
            LivingEntity target = (LivingEntity)raytraceresult.getEntity();
            target.hurt(DamageSource.indirectMobAttack(this, (LivingEntity)entity).setProjectile(), 3.0F);
            target.knockback(0.7F, entity.getX() - this.getX(), entity.getZ() - this.getZ());
            this.entityData.set(LAUNCHED, false);
        }
    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        return !p_230298_1_.isSpectator() && !(p_230298_1_ instanceof EntityStraddler)&& !(p_230298_1_ instanceof EntityStradpole);
    }

    public boolean isOnFire() {
        return false;
    }

    public boolean canStandOnFluid(Fluid p_230285_1_) {
        return p_230285_1_.is(FluidTags.LAVA);
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && (this.isInWater() || this.isInLava())) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.05D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    protected void handleAirSupply(int p_209207_1_) {

    }

    public void shoot(double p_70186_1_, double p_70186_3_, double p_70186_5_, float p_70186_7_, float p_70186_8_) {
        Vector3d lvt_9_1_ = (new Vector3d(p_70186_1_, p_70186_3_, p_70186_5_)).normalize().add(this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_, this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_, this.random.nextGaussian() * 0.007499999832361937D * (double) p_70186_8_).scale(p_70186_7_);
        this.setDeltaMovement(lvt_9_1_);
        float lvt_10_1_ = MathHelper.sqrt(getHorizontalDistanceSqr(lvt_9_1_));
        this.yRot = (float) (MathHelper.atan2(lvt_9_1_.x, lvt_9_1_.z) * 57.2957763671875D);
        this.xRot = (float) (MathHelper.atan2(lvt_9_1_.y, lvt_10_1_) * 57.2957763671875D);
        this.xRotO = this.xRot;
        this.yBodyRot = yRot;
        this.yHeadRot = yRot;
        this.yHeadRotO = yRot;
        this.yRotO = yRot;
        this.setDespawnSoon(true);
        this.entityData.set(LAUNCHED, true);
    }

    class StradpoleAISwim extends RandomWalkingGoal {
        public StradpoleAISwim(EntityStradpole creature, double speed, int chance) {
            super(creature, speed, chance, false);
        }

        public boolean canUse() {
            if (!this.mob.isInLava() && !this.mob.isInWater() || this.mob.isPassenger() || mob.getTarget() != null || !this.mob.isInWater() && !this.mob.isInLava() && this.mob instanceof ISemiAquatic && !((ISemiAquatic) this.mob).shouldEnterWater()) {
                return false;
            } else {
                if (!this.forceTrigger) {
                    if (this.mob.getRandom().nextInt(this.interval) != 0) {
                        return false;
                    }
                }
                Vector3d vector3d = this.getPosition();
                if (vector3d == null) {
                    return false;
                } else {
                    this.wantedX = vector3d.x;
                    this.wantedY = vector3d.y;
                    this.wantedZ = vector3d.z;
                    this.forceTrigger = false;
                    return true;
                }
            }
        }

        @Nullable
        protected Vector3d getPosition() {
            if (this.mob.getRandom().nextFloat() < 0.3F) {
                Vector3d vector3d = findSurfaceTarget(this.mob, 15, 7);
                if (vector3d != null) {
                    return vector3d;
                }
            }
            Vector3d vector3d = RandomPositionGenerator.getPos(this.mob, 7, 3);

            for (int i = 0; vector3d != null && !this.mob.level.getFluidState(new BlockPos(vector3d)).is(FluidTags.LAVA) && !this.mob.level.getBlockState(new BlockPos(vector3d)).isPathfindable(this.mob.level, new BlockPos(vector3d), PathType.WATER) && i++ < 15; vector3d = RandomPositionGenerator.getPos(this.mob, 10, 7)) {
            }

            return vector3d;
        }

        private boolean canJumpTo(BlockPos pos, int dx, int dz, int scale) {
            BlockPos blockpos = pos.offset(dx * scale, 0, dz * scale);
            return this.mob.level.getFluidState(blockpos).is(FluidTags.LAVA) || this.mob.level.getFluidState(blockpos).is(FluidTags.WATER) && !this.mob.level.getBlockState(blockpos).getMaterial().blocksMotion();
        }

        private boolean isAirAbove(BlockPos pos, int dx, int dz, int scale) {
            return this.mob.level.getBlockState(pos.offset(dx * scale, 1, dz * scale)).isAir() && this.mob.level.getBlockState(pos.offset(dx * scale, 2, dz * scale)).isAir();
        }

        private Vector3d findSurfaceTarget(CreatureEntity creature, int i, int i1) {
            BlockPos upPos = creature.blockPosition();
            while (creature.level.getFluidState(upPos).is(FluidTags.WATER) || creature.level.getFluidState(upPos).is(FluidTags.LAVA)) {
                upPos = upPos.above();
            }
            if (isAirAbove(upPos.below(), 0, 0, 0) && canJumpTo(upPos.below(), 0, 0, 0)) {
                return new Vector3d(upPos.getX() + 0.5F, upPos.getY() - 1F, upPos.getZ() + 0.5F);
            }
            return null;
        }
    }

}
