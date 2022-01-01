package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EndergradeAIBreakFlowers;
import com.github.alexthe666.alexsmobs.entity.ai.EndergradeAITargetItems;
import com.github.alexthe666.alexsmobs.entity.ai.TameableAIRide;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

import net.minecraft.entity.ai.controller.MovementController.Action;

public class EntityEndergrade extends AnimalEntity implements IFlyingAnimal {

    private static final DataParameter<Integer> BITE_TICK = EntityDataManager.defineId(EntityEndergrade.class, DataSerializers.INT);
    private static final DataParameter<Boolean> SADDLED = EntityDataManager.defineId(EntityEndergrade.class, DataSerializers.BOOLEAN);
    public float tartigradePitch = 0;
    public float prevTartigradePitch = 0;
    public float biteProgress = 0;
    public float prevBiteProgress = 0;
    public boolean stopWandering = false;
    public boolean hasItemTarget = false;

    protected EntityEndergrade(EntityType type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new EntityEndergrade.MoveHelperController(this);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new DirectPathNavigator(this, worldIn);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.15F);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Saddled", this.isSaddled());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setSaddled(compound.getBoolean("Saddled"));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(BITE_TICK, 0);
        this.entityData.define(SADDLED, Boolean.valueOf(false));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new TameableAIRide(this, 1.2D));
        this.goalSelector.addGoal(1, new EndergradeAIBreakFlowers(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.2D){
            public void start(){
                super.start();
                EntityEndergrade.this.stopWandering = true;
            }

            public void stop(){
                super.stop();
                EntityEndergrade.this.stopWandering = false;
            }
        });
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.1D, Ingredient.of(Items.CHORUS_FRUIT), false){
            public void start(){
                super.start();
                EntityEndergrade.this.stopWandering = true;
            }

            public void stop(){
                super.stop();
                EntityEndergrade.this.stopWandering = false;
            }
        });
        this.goalSelector.addGoal(4, new RandomFlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 10));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new EndergradeAITargetItems(this, true));
    }

    @Nullable
    public Entity getControllingPassenger() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) passenger;
                if (player.getMainHandItem().getItem() == AMItemRegistry.CHORUS_ON_A_STICK || player.getOffhandItem().getItem() == AMItemRegistry.CHORUS_ON_A_STICK) {
                    return player;
                }
            }
        }
        return null;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ENDERGRADE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ENDERGRADE_HURT;
    }


    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        if (item == Items.SADDLE && !this.isSaddled()) {
            if (!player.isCreative()) {
                itemstack.shrink(1);
            }
            this.setSaddled(true);
            return ActionResultType.SUCCESS;
        }
        if (item == Items.CHORUS_FRUIT && this.hasEffect(AMEffectRegistry.ENDER_FLU)) {
            if (!player.isCreative()) {
                itemstack.shrink(1);
            }
            this.heal(8);
            this.removeEffect(AMEffectRegistry.ENDER_FLU);
            return ActionResultType.SUCCESS;
        }
        ActionResultType type = super.mobInteract(player, hand);
        if (type != ActionResultType.SUCCESS && !isFood(itemstack)) {
            if (!player.isShiftKeyDown() && this.isSaddled()) {
                player.startRiding(this);
                return ActionResultType.SUCCESS;
            }
        }
        return type;
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.CHORUS_FRUIT;
    }

    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            float radius = -0.25F;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + extraZ);
        }
    }

    public double getPassengersRidingOffset() {
        float f = Math.min(0.25F, this.animationSpeed);
        float f1 = this.animationPosition;
        return (double) this.getBbHeight() - 0.1D + (double) (0.12F * MathHelper.cos(f1 * 0.7F) * 0.7F * f);
    }

    public boolean isNoGravity() {
        return true;
    }

    public boolean isSaddled() {
        return this.entityData.get(SADDLED).booleanValue();
    }

    public void setSaddled(boolean saddled) {
        this.entityData.set(SADDLED, Boolean.valueOf(saddled));
    }

    public void tick() {
        super.tick();
        prevTartigradePitch = this.tartigradePitch;
        prevBiteProgress = this.biteProgress;
        float f2 = (float) -((float) this.getDeltaMovement().y * 3 * (double) (180F / (float) Math.PI));
        this.tartigradePitch = f2;
        if (this.getDeltaMovement().lengthSqr() > 0.005F) {
            float angleMotion = (0.01745329251F * this.yBodyRot);
            double extraXMotion = -0.2F * MathHelper.sin((float) (Math.PI + angleMotion));
            double extraZMotion = -0.2F * MathHelper.cos(angleMotion);
            this.level.addParticle(ParticleTypes.END_ROD, this.getRandomX(0.5D), this.getY() + 0.3, this.getRandomZ(0.5D), extraXMotion, 0D, extraZMotion);
        }
        int tick = this.entityData.get(BITE_TICK);
        if (tick > 0) {
            this.entityData.set(BITE_TICK, tick - 1);
            this.biteProgress++;
        } else if (biteProgress > 0) {
            biteProgress--;
        }
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.ARTHROPOD;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    private BlockPos getGroundPosition(BlockPos radialPos) {
        while (radialPos.getY() > 1 && level.isEmptyBlock(radialPos)) {
            radialPos = radialPos.below();
        }
        if (radialPos.getY() <= 1) {
            return new BlockPos(radialPos.getX(), level.getSeaLevel(), radialPos.getZ());
        }
        return radialPos;
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem() == Items.CHORUS_FRUIT || stack.getItem() == Items.CHORUS_FLOWER;
    }

    public void onGetItem(ItemEntity targetEntity) {
        this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
        this.heal(5);
    }

    public void bite() {
        this.entityData.set(BITE_TICK, 5);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.endergradeSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.ENDERGRADE.create(p_241840_1_);
    }

    public static boolean canEndergradeSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return !worldIn.getBlockState(pos.below()).isAir();
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.isSaddled()) {
            if (!this.level.isClientSide) {
                this.spawnAtLocation(Items.SADDLE);
            }
        }
    }

    static class RandomFlyGoal extends Goal {
        private final EntityEndergrade parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntityEndergrade mosquito) {
            this.parentEntity = mosquito;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canUse() {
            MovementController movementcontroller = this.parentEntity.getMoveControl();
            if (parentEntity.stopWandering || parentEntity.hasItemTarget) {
                return false;
            }
            if (!movementcontroller.hasWanted() || target == null) {
                target = getBlockInViewEndergrade();
                if (target != null) {
                    this.parentEntity.getMoveControl().setWantedPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                }
                return true;
            }
            return false;
        }

        public boolean canContinueToUse() {
            return target != null && !parentEntity.stopWandering && !parentEntity.hasItemTarget && parentEntity.distanceToSqr(Vector3d.atCenterOf(target)) > 2.4D && parentEntity.getMoveControl().hasWanted() && !parentEntity.horizontalCollision;
        }

        public void stop() {
            target = null;
        }

        public void tick() {
            if (target == null) {
                target = getBlockInViewEndergrade();
            }
            if (target != null) {
                this.parentEntity.getMoveControl().setWantedPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, 1.0D);
                if (parentEntity.distanceToSqr(Vector3d.atCenterOf(target)) < 2.5F) {
                    target = null;
                }
            }
        }

        public BlockPos getBlockInViewEndergrade() {
            float radius = 1 + parentEntity.getRandom().nextInt(5);
            float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            float renderYawOffset = parentEntity.yBodyRot;
            float angle = (0.01745329251F * renderYawOffset) + 3.15F + (parentEntity.getRandom().nextFloat() * neg);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            BlockPos radialPos = new BlockPos(parentEntity.getX() + extraX, parentEntity.getY() + 2, parentEntity.getZ() + extraZ);
            BlockPos ground = parentEntity.getGroundPosition(radialPos);
            BlockPos newPos = ground.above(1 + parentEntity.getRandom().nextInt(6));
            if (!parentEntity.isTargetBlocked(Vector3d.atCenterOf(newPos)) && parentEntity.distanceToSqr(Vector3d.atCenterOf(newPos)) > 6) {
                return newPos;
            }
            return null;
        }
    }

    static class MoveHelperController extends MovementController {
        private final EntityEndergrade parentEntity;

        public MoveHelperController(EntityEndergrade sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        public void tick() {
            if (this.operation == Action.STRAFE) {
                Vector3d vector3d = new Vector3d(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d0 = vector3d.length();
                parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(0, vector3d.scale(this.speedModifier * 0.05D / d0).y(), 0));
                float f = (float) this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
                float f1 = (float) this.speedModifier * f;
                float f2 = this.strafeForwards;
                float f3 = this.strafeRight;
                float f4 = MathHelper.sqrt(f2 * f2 + f3 * f3);
                if (f4 < 1.0F) {
                    f4 = 1.0F;
                }

                f4 = f1 / f4;
                f2 = f2 * f4;
                f3 = f3 * f4;
                float f5 = MathHelper.sin(this.mob.yRot * ((float) Math.PI / 180F));
                float f6 = MathHelper.cos(this.mob.yRot * ((float) Math.PI / 180F));
                float f7 = f2 * f6 - f3 * f5;
                float f8 = f3 * f6 + f2 * f5;
                this.strafeForwards = 1.0F;
                this.strafeRight = 0.0F;

                this.mob.setSpeed(f1);
                this.mob.setZza(this.strafeForwards);
                this.mob.setXxa(this.strafeRight);
                this.operation = MovementController.Action.WAIT;
            } else if (this.operation == MovementController.Action.MOVE_TO) {
                Vector3d vector3d = new Vector3d(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d0 = vector3d.length();
                if (d0 < parentEntity.getBoundingBox().getSize()) {
                    this.operation = MovementController.Action.WAIT;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
                } else {
                    double localSpeed = this.speedModifier;
                    if (parentEntity.isVehicle()) {
                        localSpeed *= 1.5D;
                    }
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(vector3d.scale(localSpeed * 0.005D / d0)));
                    if (parentEntity.getTarget() == null) {
                        Vector3d vector3d1 = parentEntity.getDeltaMovement();
                        parentEntity.yRot = -((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * (180F / (float) Math.PI);
                        parentEntity.yBodyRot = parentEntity.yRot;
                    } else {
                        double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                        double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                        parentEntity.yRot = -((float) MathHelper.atan2(d2, d1)) * (180F / (float) Math.PI);
                        parentEntity.yBodyRot = parentEntity.yRot;
                    }
                }

            }
        }

        private boolean canReach(Vector3d p_220673_1_, int p_220673_2_) {
            AxisAlignedBB axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.move(p_220673_1_);
                if (!this.parentEntity.level.noCollision(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }
}
