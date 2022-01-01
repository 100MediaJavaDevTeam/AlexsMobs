package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.BreatheAirGoal;
import net.minecraft.entity.ai.goal.HurtByTargetGoal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.WitherSkeletonEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Direction;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityBoneSerpent extends MonsterEntity {

    private static final DataParameter<Optional<UUID>> CHILD_UUID = EntityDataManager.defineId(EntityBoneSerpent.class, DataSerializers.OPTIONAL_UUID);
    private static final Predicate<LivingEntity> NOT_RIDING_STRADDLEBOARD_FRIENDLY = (entity) -> {
        return entity.isAlive() && (entity.getVehicle() == null || !(entity.getVehicle() instanceof EntityStraddleboard) || !((EntityStraddleboard)entity.getVehicle()).shouldSerpentFriend());
    };;
    private static final Predicate<EntityStraddleboard> STRADDLEBOARD_FRIENDLY = (entity) -> {
        return entity.isVehicle() && entity.shouldSerpentFriend();
    };;

    public int jumpCooldown = 0;
    private boolean isLandNavigator;
    private int boardCheckCooldown = 0;
    private EntityStraddleboard boardToBoast = null;

    protected EntityBoneSerpent(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.LAVA, 0.0F);
        switchNavigator(false);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.boneSeprentSpawnRolls, this.getRandom(), spawnReasonIn) && super.checkSpawnRules(worldIn, spawnReasonIn);
    }

    public int getMaxSpawnClusterSize() {
        return 1;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }


    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.BONE_SERPENT_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.BONE_SERPENT_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.BONE_SERPENT_HURT;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 25.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ATTACK_DAMAGE, 5.0D).add(Attributes.MOVEMENT_SPEED, 1.45F);
    }

    public int getMaxFallDistance() {
        return 256;
    }

    public boolean canBeAffected(EffectInstance potioneffectIn) {
        if (potioneffectIn.getEffect() == Effects.WITHER) {
            return false;
        }
        return super.canBeAffected(potioneffectIn);
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.UNDEAD;
    }

    public float getWalkTargetValue(BlockPos pos, IWorldReader worldIn) {
        if (worldIn.getBlockState(pos).getFluidState().is(FluidTags.WATER) || worldIn.getBlockState(pos).getFluidState().is(FluidTags.LAVA)) {
            return 10.0F;
        } else {
            return this.isInLava() ? Float.NEGATIVE_INFINITY : 0.0F;
        }
    }

    public boolean isPushedByFluid() {
        return false;
    }

    public boolean canBeLeashed(PlayerEntity player) {
        return true;
    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    public static boolean canBoneSerpentSpawn(EntityType<EntityBoneSerpent> p_234314_0_, IWorld p_234314_1_, SpawnReason p_234314_2_, BlockPos p_234314_3_, Random p_234314_4_) {
        BlockPos.Mutable blockpos$mutable = p_234314_3_.mutable();

        do {
            blockpos$mutable.move(Direction.UP);
        } while(p_234314_1_.getFluidState(blockpos$mutable).is(FluidTags.LAVA));

        return p_234314_1_.getBlockState(blockpos$mutable).isAir();
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreatheAirGoal(this));
        this.goalSelector.addGoal(0, new BoneSerpentAIFindLava(this));
        this.goalSelector.addGoal(1, new BoneSerpentAIMeleeJump(this));
        this.goalSelector.addGoal(2, new BoneSerpentAIJump(this, 10));
        this.goalSelector.addGoal(3, new BoneSerpentAIRandomSwimming(this, 1.0D, 8));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        if(!AMConfig.neutralBoneSerpents){
            this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, PlayerEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
            this.targetSelector.addGoal(3, new EntityAINearestTarget3D(this, AbstractVillagerEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
        }
        this.targetSelector.addGoal(4, new EntityAINearestTarget3D(this, WitherSkeletonEntity.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
        this.targetSelector.addGoal(5, new EntityAINearestTarget3D(this, EntitySoulVulture.class, 10, true, false, NOT_RIDING_STRADDLEBOARD_FRIENDLY));
    }

    public void travel(Vector3d travelVector) {
        boolean liquid = this.isInLava() || this.isInWater();
        if (this.isEffectiveAi() && liquid) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            if (this.getTarget() == null) {
                this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.005D, 0.0D));
            }
        } else {
            super.travel(travelVector);
        }

    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = this.createNavigation(level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new BoneSerpentMoveController(this);
            this.navigation = new BoneSerpentPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        if (this.getChildId() != null) {
            compound.putUUID("ChildUUID", this.getChildId());
        }
    }


    public void pushEntities() {
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        entities.stream().filter(entity -> !(entity instanceof EntityBoneSerpentPart) && entity.isPushable()).forEach(entity -> entity.push(this));
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.FALL || source == DamageSource.DROWN || source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || source == DamageSource.LAVA || source.isFire() || super.isInvulnerableTo(source);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("ChildUUID")) {
            this.setChildId(compound.getUUID("ChildUUID"));
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHILD_UUID, Optional.empty());
    }

    @Nullable
    public UUID getChildId() {
        return this.entityData.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.entityData.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
    }

    public void tick() {
        super.tick();
        isInsidePortal = false;
        boolean ground = !this.isInLava() && !this.isInWater() && this.isOnGround();
        if (jumpCooldown > 0) {
            jumpCooldown--;
            float f2 = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
            this.xRot = f2;

        }
        if (!ground && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (ground && !this.isLandNavigator) {
            switchNavigator(true);
        }
        if (!level.isClientSide) {
            Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                int segments = 7 + getRandom().nextInt(8);
                for (int i = 0; i < segments; i++) {
                    EntityBoneSerpentPart part = new EntityBoneSerpentPart(AMEntityRegistry.BONE_SERPENT_PART, partParent, 0.9F, 180, 0);
                    part.setParent(partParent);
                    part.setBodyIndex(i);
                    if (partParent == this) {
                        this.setChildId(part.getUUID());
                    }
                    part.setInitialPartPos(this);
                    partParent = part;
                    if (i == segments - 1) {
                        part.setTail(true);
                    }
                    level.addFreshEntity(part);
                }
            }
        }
        if(!level.isClientSide){
            if(boardCheckCooldown <= 0){
                boardCheckCooldown = 100 + random.nextInt(150);
                List<EntityStraddleboard> list = this.level.getEntitiesOfClass(EntityStraddleboard.class, this.getBoundingBox().inflate(100, 15, 100), STRADDLEBOARD_FRIENDLY);
                EntityStraddleboard closestBoard = null;
                for(EntityStraddleboard board : list){
                    if(closestBoard == null || this.distanceTo(closestBoard) > this.distanceTo(board)){
                        closestBoard = board;
                    }
                }
                boardToBoast = closestBoard;
            }else{
                boardCheckCooldown--;
            }
            if(boardToBoast != null){
                if(this.distanceTo(boardToBoast) > 200){
                    boardToBoast = null;
                }else{
                    if((this.isInLava() || this.isInWater()) && this.distanceTo(boardToBoast) < 15 && jumpCooldown == 0){
                        float up = 0.7F + this.getRandom().nextFloat() * 0.8F;
                        Vector3d vector3d1 = this.getLookAngle();
                        this.setDeltaMovement(this.getDeltaMovement().add((double) vector3d1.x() * 0.6D, up, (double) vector3d1.y() * 0.6D));
                        this.getNavigation().stop();
                        this.jumpCooldown = this.getRandom().nextInt(300) + 100;
                    }
                    if(this.distanceTo(boardToBoast) > 5){
                        this.getNavigation().moveTo(boardToBoast, 1.5F);

                    }else{
                        this.getNavigation().stop();
                    }
                }
            }
        }
    }

    public boolean canBreatheUnderwater() {
        return true;
    }


    static class BoneSerpentMoveController extends MovementController {
        private final EntityBoneSerpent dolphin;

        public BoneSerpentMoveController(EntityBoneSerpent dolphinIn) {
            super(dolphinIn);
            this.dolphin = dolphinIn;
        }

        public void tick() {
            if (this.dolphin.isInWater() || this.dolphin.isInLava()) {
                this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add(0.0D, 0.005D, 0.0D));
            }

            if (this.operation == MovementController.Action.MOVE_TO && !this.dolphin.getNavigation().isDone()) {
                double d0 = this.wantedX - this.dolphin.getX();
                double d1 = this.wantedY - this.dolphin.getY();
                double d2 = this.wantedZ - this.dolphin.getZ();
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                if (d3 < (double) 2.5000003E-7F) {
                    this.mob.setZza(0.0F);
                } else {
                    float f = (float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                    this.dolphin.yRot = this.rotlerp(this.dolphin.yRot, f, 10.0F);
                    this.dolphin.yBodyRot = this.dolphin.yRot;
                    this.dolphin.yHeadRot = this.dolphin.yRot;
                    float f1 = (float) (this.speedModifier * this.dolphin.getAttributeValue(Attributes.MOVEMENT_SPEED));
                    if (this.dolphin.isInWater() || this.dolphin.isInLava()) {
                        this.dolphin.setSpeed(f1 * 0.02F);
                        float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt(d0 * d0 + d2 * d2)) * (double) (180F / (float) Math.PI)));
                        f2 = MathHelper.clamp(MathHelper.wrapDegrees(f2), -85.0F, 85.0F);
                        this.dolphin.setDeltaMovement(this.dolphin.getDeltaMovement().add(0.0D, (double) this.dolphin.getSpeed() * d1 * 0.6D, 0.0D));
                        this.dolphin.xRot = this.rotlerp(this.dolphin.xRot, f2, 1.0F);
                        float f3 = MathHelper.cos(this.dolphin.xRot * ((float) Math.PI / 180F));
                        float f4 = MathHelper.sin(this.dolphin.xRot * ((float) Math.PI / 180F));
                        this.dolphin.zza = f3 * f1;
                        this.dolphin.yya = -f4 * f1;
                    } else {
                        this.dolphin.setSpeed(f1 * 0.1F);
                    }

                }
            } else {
                this.dolphin.setSpeed(0.0F);
                this.dolphin.setXxa(0.0F);
                this.dolphin.setYya(0.0F);
                this.dolphin.setZza(0.0F);
            }
        }
    }


}
