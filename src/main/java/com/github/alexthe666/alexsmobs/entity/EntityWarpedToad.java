package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.JumpController;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.*;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityWarpedToad extends TameableEntity implements ITargetsDroppedItems, IFollower, ISemiAquatic {

    private static final DataParameter<Float> TONGUE_LENGTH = EntityDataManager.defineId(EntityWarpedToad.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> TONGUE_OUT = EntityDataManager.defineId(EntityWarpedToad.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityWarpedToad.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityWarpedToad.class, DataSerializers.INT);
    public float blinkProgress;
    public float prevBlinkProgress;
    public float attackProgress;
    public float prevAttackProgress;
    public float sitProgress;
    public float prevSitProgress;
    public float swimProgress;
    public float prevSwimProgress;
    private boolean isLandNavigator;
    private int jumpTicks;
    private int jumpDuration;
    private boolean wasOnGround;
    private int currentMoveTypeDuration;
    private int swimTimer = -100;

    protected EntityWarpedToad(EntityType entityType, World world) {
        super(entityType, world);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.LAVA, 0.0F);
        switchNavigator(false);
    }

    public boolean isBased() {
        String s = TextFormatting.stripFormatting(this.getName().getString());
        return s != null && s.toLowerCase().contains("pepe");
    }

    public static boolean canWarpedToadSpawn(EntityType<? extends MobEntity> typeIn, IServerWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        BlockPos blockpos = pos.below();
        boolean spawnBlock = worldIn.getFluidState(blockpos).is(FluidTags.LAVA) || worldIn.getBlockState(blockpos).canOcclude();
        return reason == SpawnReason.SPAWNER || spawnBlock;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.25F).add(Attributes.MOVEMENT_SPEED, 0.35F);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.WARPED_TOAD_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.WARPED_TOAD_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.WARPED_TOAD_HURT;
    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.warpedToadSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public int getMaxSpawnClusterSize() {
        return 5;
    }

    public boolean isMaxGroupSizeReached(int sizeIn) {
        return false;
    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && this.isTame() && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amount = (amount + 1.0F) / 3.0F;
            }
            return super.hurt(source, amount);
        }
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("MonkeySitting", this.isSitting());
        compound.putInt("Command", this.getCommand());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setOrderedToSit(compound.getBoolean("MonkeySitting"));
        this.setCommand(compound.getInt("Command"));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SitGoal(this));
        this.goalSelector.addGoal(1, new EntityWarpedToad.TongueAttack(this));
        this.goalSelector.addGoal(2, new FollowOwner(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(3, new AnimalAILeaveWater(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS)), false));
        this.goalSelector.addGoal(5, new WarpedToadAIRandomSwimming(this, 1.0D, 7));
        this.goalSelector.addGoal(6, new AnimalAIWanderRanged(this, 60, 1.0D, 5, 4));
        this.goalSelector.addGoal(10, new LookAtGoal(this, PlayerEntity.class, 10.0F));
        this.goalSelector.addGoal(11, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, new EntityAINearestTarget3D(this, LivingEntity.class, 50, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.WARPED_TOAD_TARGETS))));
        this.targetSelector.addGoal(5, new HurtByTargetGoal(this));
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && (this.isInWater() || this.isInLava())) {
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

    protected float getJumpPower() {
        return 0.5F;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }


    protected void jumpFromGround() {
        super.jumpFromGround();
        double d0 = this.moveControl.getSpeedModifier();
        if (d0 > 0.0D) {
            double d1 = getHorizontalDistanceSqr(this.getDeltaMovement());
            if (d1 < 0.01D) {
            }
        }

        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte) 1);
        }

    }

    public void setMovementSpeed(double newSpeed) {
        this.getNavigation().setSpeedModifier(newSpeed);
        this.moveControl.setWantedPosition(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ(), newSpeed);
    }

    public void setJumping(boolean jumping) {
        super.setJumping(jumping);
        if (jumping) {
            //    this.playSound(this.getJumpSound(), this.getSoundVolume(), ((this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F) * 0.8F);
        }
    }

    public void startJumping() {
        this.setJumping(true);
        this.jumpDuration = 10;
        this.jumpTicks = 0;
    }

    public void customServerAiStep() {
        super.customServerAiStep();

        if (this.currentMoveTypeDuration > 0) {
            --this.currentMoveTypeDuration;
        }

        if (this.onGround && !this.isSitting()) {
            if (!this.wasOnGround) {
                this.setJumping(false);
                this.checkLandingDelay();
            }

            if (this.currentMoveTypeDuration == 0) {
                LivingEntity livingentity = this.getTarget();
                if (livingentity != null && this.distanceToSqr(livingentity) < 16.0D) {
                    this.calculateRotationYaw(livingentity.getX(), livingentity.getZ());
                    this.moveControl.setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeedModifier());
                    this.startJumping();
                    this.wasOnGround = true;
                }
            }
            if (this.jumpControl instanceof EntityWarpedToad.JumpHelperController) {
                EntityWarpedToad.JumpHelperController rabbitController = (EntityWarpedToad.JumpHelperController) this.jumpControl;
                if (!rabbitController.getIsJumping()) {
                    if (this.moveControl.hasWanted() && this.currentMoveTypeDuration == 0) {
                        Path path = this.navigation.getPath();
                        Vector3d vector3d = new Vector3d(this.moveControl.getWantedX(), this.moveControl.getWantedY(), this.moveControl.getWantedZ());
                        if (path != null && !path.isDone()) {
                            vector3d = path.getNextEntityPos(this);
                        }

                        this.calculateRotationYaw(vector3d.x, vector3d.z);
                        this.startJumping();
                    }
                } else if (!rabbitController.canJump()) {
                    this.enableJumpControl();
                }
            }
        } else if (this.isSitting()) {
            this.setJumping(false);
            this.checkLandingDelay();
        }

        this.wasOnGround = this.onGround;
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == AMItemRegistry.MOSQUITO_LARVA && isTame();
    }


    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if (!isTame() && item == AMItemRegistry.MOSQUITO_LARVA) {
            this.usePlayerItem(player, itemstack);
            this.playSound(SoundEvents.STRIDER_EAT, this.getSoundVolume(), this.getVoicePitch());
            if (getRandom().nextInt(3) == 0) {
                this.tame(player);
                this.level.broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level.broadcastEntityEvent(this, (byte) 6);
            }
            return ActionResultType.SUCCESS;
        }
        if (isTame() && (ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS).contains(itemstack.getItem()))) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.usePlayerItem(player, itemstack);
                this.playSound(SoundEvents.STRIDER_EAT, this.getSoundVolume(), this.getVoicePitch());
                this.heal(5);
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.PASS;

        }
        if (type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player) && !isFood(itemstack)) {
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
        return type;
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

    public boolean canSpawnSprintParticle() {
        return false;
    }

    private void calculateRotationYaw(double x, double z) {
        this.yRot = (float) (MathHelper.atan2(z - this.getZ(), x - this.getX()) * (double) (180F / (float) Math.PI)) - 90.0F;
    }

    private void enableJumpControl() {
        if (jumpControl instanceof EntityWarpedToad.JumpHelperController) {
            ((EntityWarpedToad.JumpHelperController) this.jumpControl).setCanJump(true);
        }
    }

    private void disableJumpControl() {
        if (jumpControl instanceof EntityWarpedToad.JumpHelperController) {
            ((EntityWarpedToad.JumpHelperController) this.jumpControl).setCanJump(false);
        }
    }

    private void updateMoveTypeDuration() {
        if (this.moveControl.getSpeedModifier() < 2.2D) {
            this.currentMoveTypeDuration = 10;
        } else {
            this.currentMoveTypeDuration = 1;
        }

    }

    private void checkLandingDelay() {
        this.updateMoveTypeDuration();
        this.disableJumpControl();
    }

    public void aiStep() {
        super.aiStep();
        if(this.isBaby() && this.getEyeHeight() > this.getBbHeight()){
            this.refreshDimensions();
        }
        if (this.jumpTicks != this.jumpDuration) {
            ++this.jumpTicks;
        } else if (this.jumpDuration != 0) {
            this.jumpTicks = 0;
            this.jumpDuration = 0;
            this.setJumping(false);
        }
        if (!level.isClientSide) {
            if (isInWater() || isInLava()) {
                if (swimTimer < 0) {
                    swimTimer = 0;
                }
                swimTimer++;
            } else {
                if (swimTimer > 0) {
                    swimTimer = 0;
                }
                swimTimer--;
            }
        }
    }


    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.jumpControl = new EntityWarpedToad.JumpHelperController(this);
            this.moveControl = new EntityWarpedToad.MoveHelperController(this);
            this.navigation = createNavigation(level);
            this.isLandNavigator = true;
        } else {
            this.jumpControl = new JumpController(this);
            this.moveControl = new AquaticMoveController(this, 1.2F);
            this.navigation = new BoneSerpentPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(TONGUE_LENGTH, 1F);
        this.entityData.define(TONGUE_OUT, false);
        this.entityData.define(COMMAND, Integer.valueOf(0));
        this.entityData.define(SITTING, Boolean.valueOf(false));
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

    public void tick() {
        super.tick();
        prevBlinkProgress = blinkProgress;
        prevAttackProgress = attackProgress;
        prevSitProgress = sitProgress;
        prevSwimProgress = swimProgress;
        this.maxUpStep = 1;

        boolean isTechnicalBlinking = this.tickCount % 50 > 42;
        if (isTechnicalBlinking && blinkProgress < 5F) {
            blinkProgress++;
        }
        if (!isTechnicalBlinking && blinkProgress > 0F) {
            blinkProgress--;
        }
        if (isTongueOut() && attackProgress < 5F) {
            attackProgress++;
        }
        LivingEntity entityIn = this.getTarget();
        if (entityIn != null && attackProgress > 0) {
            if (isTongueOut()) {
                double d0 = entityIn.getX() - this.getX();
                double d2 = entityIn.getZ() - this.getZ();
                double d1 = entityIn.getEyeY() - this.getEyeY();
                double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                float f = (float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                float f1 = (float) (-(MathHelper.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
                this.xRot = f1;
                this.yRot = f;
                this.yBodyRot = this.yRot;
                this.yHeadRot = this.yRot;
            } else {
                if (entityIn instanceof EntityCrimsonMosquito) {
                    ((EntityCrimsonMosquito) entityIn).setShrink(true);
                }
                this.xRot = 0;
                float radius = attackProgress * 0.2F * 1.2F * (getTongueLength() - getTongueLength() * 0.4F);
                float angle = (0.01745329251F * this.yBodyRot);
                double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
                double extraZ = radius * MathHelper.cos(angle);
                double yHelp = entityIn.getBbHeight();
                Vector3d minus = new Vector3d(this.getX() + extraX - this.getTarget().getX(), this.getEyeHeight() - yHelp - this.getTarget().getY(), this.getZ() + extraZ - this.getTarget().getZ());
                this.getTarget().setDeltaMovement(minus);
                if (attackProgress == 0.5F) {
                    float damage = (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getValue();
                    if (entityIn instanceof EntityCrimsonMosquito) {
                        damage = Float.MAX_VALUE;
                    }
                    entityIn.hurt(DamageSource.mobAttack(this), damage);
                }
            }

            if (attackProgress == 5 && (entityIn.getBbHeight() < 0.89D || entityIn instanceof EntityCrimsonMosquito) && !entityIn.hasPassenger(this)) {
            }
        }
        if (!level.isClientSide && isTongueOut() && attackProgress == 5F) {
            setTongueOut(false);
            attackProgress = 4F;
        }
        if (!isTongueOut() && attackProgress > 0F) {
            attackProgress -= 0.5F;
        }
        if (isSitting() && sitProgress < 5F) {
            sitProgress++;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress--;
        }
        if (shouldSwim() && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (!shouldSwim() && !this.isLandNavigator) {
            switchNavigator(true);
        }
        if (shouldSwim() && swimProgress < 5F) {
            swimProgress++;
        }
        if (!shouldSwim() && swimProgress > 0F) {
            swimProgress--;
        }
    }

    public boolean shouldSwim() {
        return isInWater() || isInLava();
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return ItemTags.getAllTags().getTag(AMTagRegistry.INSECT_ITEMS).contains(stack.getItem());
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(5);
    }

    public boolean isBlinking() {
        return blinkProgress > 1 || blinkProgress < -1 || attackProgress > 1;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.WARPED_TOAD.create(serverWorld);
    }

    public float getTongueLength() {
        return entityData.get(TONGUE_LENGTH);
    }

    public void setTongueLength(float length) {
        entityData.set(TONGUE_LENGTH, length);
    }

    public float getJumpCompletion(float partialTicks) {
        return this.jumpDuration == 0 ? 0.0F : ((float) this.jumpTicks + partialTicks) / (float) this.jumpDuration;
    }

    public boolean isPushedByFluid() {
        return false;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 1) {
            this.spawnSprintParticle();
            this.jumpDuration = 10;
            this.jumpTicks = 0;
        } else {
            super.handleEntityEvent(id);
        }

    }

    public boolean hasJumper() {
        return jumpControl instanceof EntityWarpedToad.JumpHelperController;
    }

    @Override
    public boolean shouldEnterWater() {
        return swimTimer < -200 && !isSitting() && this.getCommand() != 1;
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 600 && !isSitting() && this.getCommand() != 1;
    }

    @Override
    public boolean shouldStopMoving() {
        return isSitting();
    }

    private boolean isTongueOut() {
        return this.entityData.get(TONGUE_OUT);
    }

    private void setTongueOut(boolean out) {
        this.entityData.set(TONGUE_OUT, out);
    }

    @Override
    public int getWaterSearchRange() {
        return 8;
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
    }

    static class MoveHelperController extends MovementController {
        private final EntityWarpedToad warpedToad;
        private double nextJumpSpeed;

        public MoveHelperController(EntityWarpedToad warpedToad) {
            super(warpedToad);
            this.warpedToad = warpedToad;
        }

        public void tick() {
            if (this.warpedToad.hasJumper() && this.warpedToad.onGround && !this.warpedToad.jumping && !((EntityWarpedToad.JumpHelperController) this.warpedToad.jumpControl).getIsJumping()) {
                this.warpedToad.setMovementSpeed(0.0D);
            } else if (this.hasWanted()) {
                this.warpedToad.setMovementSpeed(this.nextJumpSpeed);
            }

            super.tick();
        }

        /**
         * Sets the speed and location to move to
         */
        public void setWantedPosition(double x, double y, double z, double speedIn) {
            if (this.warpedToad.isInWater()) {
                speedIn = 1.5D;
            }

            super.setWantedPosition(x, y, z, speedIn);
            if (speedIn > 0.0D) {
                this.nextJumpSpeed = speedIn;
            }

        }
    }

    public class JumpHelperController extends JumpController {
        private final EntityWarpedToad toad;
        private boolean canJump;

        public JumpHelperController(EntityWarpedToad toad) {
            super(toad);
            this.toad = toad;
        }

        public boolean getIsJumping() {
            return this.jump;
        }

        public boolean canJump() {
            return this.canJump;
        }

        public void setCanJump(boolean canJumpIn) {
            this.canJump = canJumpIn;
        }

        public void tick() {
            if (this.jump) {
                this.toad.startJumping();
                this.jump = false;
            }

        }
    }

    public class TongueAttack extends Goal {
        private final EntityWarpedToad parentEntity;
        private int spitCooldown = 0;
        private BlockPos shootPos = null;

        public TongueAttack(EntityWarpedToad toad) {
            this.parentEntity = toad;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {

            return parentEntity.getTarget() != null && parentEntity.getPassengers().isEmpty();
        }

        public boolean canContinueToUse() {
            return parentEntity.getTarget() != null && parentEntity.getPassengers().isEmpty();
        }

        public void stop() {
            spitCooldown = 20;
            parentEntity.getNavigation().stop();
        }

        public void tick() {
            if (spitCooldown > 0) {
                spitCooldown--;
            }
            Entity entityIn = parentEntity.getTarget();
            if (entityIn != null) {
                double dist = parentEntity.distanceTo(entityIn);
                if (dist < 8 && this.parentEntity.canSee(entityIn)) {
                    if (!parentEntity.isTongueOut() && parentEntity.attackProgress == 0 && spitCooldown == 0) {
                        this.parentEntity.setTongueLength((float) Math.max(1F, dist + 2F));
                        spitCooldown = 10;
                        this.parentEntity.setTongueOut(true);
                    }
                }
                this.parentEntity.getNavigation().moveTo(entityIn, 1.4F);


            }
        }
    }

    public class FollowOwner extends Goal {
        private final EntityWarpedToad tameable;
        private final IWorldReader world;
        private final double followSpeed;
        private final float maxDist;
        private final float minDist;
        private final boolean teleportToLeaves;
        private LivingEntity owner;
        private int timeToRecalcPath;
        private float oldWaterCost;

        public FollowOwner(EntityWarpedToad p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
            this.tameable = p_i225711_1_;
            this.world = p_i225711_1_.level;
            this.followSpeed = p_i225711_2_;
            this.minDist = p_i225711_4_;
            this.maxDist = p_i225711_5_;
            this.teleportToLeaves = p_i225711_6_;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
            if (!(p_i225711_1_.getNavigation() instanceof GroundPathNavigator) && !(p_i225711_1_.getNavigation() instanceof FlyingPathNavigator)) {
                throw new IllegalArgumentException("Unsupported mob type for FollowOwnerGoal");
            }
        }

        public boolean canUse() {
            LivingEntity lvt_1_1_ = this.tameable.getOwner();
            if (lvt_1_1_ == null) {
                return false;
            } else if (lvt_1_1_.isSpectator()) {
                return false;
            } else if (this.tameable.isSitting() || tameable.getCommand() != 1) {
                return false;
            } else if (this.tameable.distanceToSqr(lvt_1_1_) < (double) (this.minDist * this.minDist)) {
                return false;
            } else {
                this.owner = lvt_1_1_;
                return true;
            }
        }

        public boolean canContinueToUse() {
            if (this.tameable.getNavigation().isDone()) {
                return false;
            } else if (this.tameable.isSitting() || tameable.getCommand() != 1) {
                return false;
            } else {
                return this.tameable.distanceToSqr(this.owner) > (double) (this.maxDist * this.maxDist);
            }
        }

        public void start() {
            this.timeToRecalcPath = 0;
            this.oldWaterCost = this.tameable.getPathfindingMalus(PathNodeType.WATER);
            this.tameable.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        }

        public void stop() {
            this.owner = null;
            this.tameable.getNavigation().stop();
            this.tameable.setPathfindingMalus(PathNodeType.WATER, this.oldWaterCost);
        }

        public void tick() {
            this.tameable.getLookControl().setLookAt(this.owner, 10.0F, (float) this.tameable.getMaxHeadXRot());
            if (--this.timeToRecalcPath <= 0) {
                this.timeToRecalcPath = 10;
                if (!this.tameable.isLeashed() && !this.tameable.isPassenger()) {
                    if (this.tameable.distanceToSqr(this.owner) >= 144.0D) {
                        this.tryToTeleportNearEntity();
                    } else {
                        this.tameable.getNavigation().moveTo(this.owner, this.followSpeed);
                    }

                }
            }
        }

        private void tryToTeleportNearEntity() {
            BlockPos lvt_1_1_ = this.owner.blockPosition();

            for (int lvt_2_1_ = 0; lvt_2_1_ < 10; ++lvt_2_1_) {
                int lvt_3_1_ = this.getRandomNumber(-3, 3);
                int lvt_4_1_ = this.getRandomNumber(-1, 1);
                int lvt_5_1_ = this.getRandomNumber(-3, 3);
                boolean lvt_6_1_ = this.tryToTeleportToLocation(lvt_1_1_.getX() + lvt_3_1_, lvt_1_1_.getY() + lvt_4_1_, lvt_1_1_.getZ() + lvt_5_1_);
                if (lvt_6_1_) {
                    return;
                }
            }

        }

        private boolean tryToTeleportToLocation(int p_226328_1_, int p_226328_2_, int p_226328_3_) {
            if (Math.abs((double) p_226328_1_ - this.owner.getX()) < 2.0D && Math.abs((double) p_226328_3_ - this.owner.getZ()) < 2.0D) {
                return false;
            } else if (!this.isTeleportFriendlyBlock(new BlockPos(p_226328_1_, p_226328_2_, p_226328_3_))) {
                return false;
            } else {
                this.tameable.moveTo((double) p_226328_1_ + 0.5D, p_226328_2_, (double) p_226328_3_ + 0.5D, this.tameable.yRot, this.tameable.xRot);
                this.tameable.getNavigation().stop();
                return true;
            }
        }

        private boolean isTeleportFriendlyBlock(BlockPos p_226329_1_) {
            PathNodeType lvt_2_1_ = WalkNodeProcessor.getBlockPathTypeStatic(this.world, p_226329_1_.mutable());
            if (lvt_2_1_ != PathNodeType.WALKABLE) {
                return false;
            } else {
                BlockState lvt_3_1_ = this.world.getBlockState(p_226329_1_.below());
                if (!this.teleportToLeaves && lvt_3_1_.getBlock() instanceof LeavesBlock) {
                    return false;
                } else {
                    BlockPos lvt_4_1_ = p_226329_1_.subtract(this.tameable.blockPosition());
                    return this.world.noCollision(this.tameable, this.tameable.getBoundingBox().move(lvt_4_1_));
                }
            }
        }

        private int getRandomNumber(int p_226327_1_, int p_226327_2_) {
            return this.tameable.getRandom().nextInt(p_226327_2_ - p_226327_1_ + 1) + p_226327_1_;
        }
    }

}
