package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.message.MessageCrowDismount;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.item.ItemFrameEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.CapabilityItemHandler;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;

import javax.annotation.Nullable;
import java.util.*;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityCrow extends TameableEntity implements ITargetsDroppedItems {

    private static final DataParameter<Boolean> FLYING = EntityDataManager.defineId(EntityCrow.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ATTACK_TICK = EntityDataManager.defineId(EntityCrow.class, DataSerializers.INT);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityCrow.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityCrow.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> PERCH_POS = EntityDataManager.defineId(EntityCrow.class, DataSerializers.OPTIONAL_BLOCK_POS);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevAttackProgress;
    public float attackProgress;
    public int fleePumpkinFlag = 0;
    public boolean aiItemFlag = false;
    public boolean aiItemFrameFlag = false;
    public float prevSitProgress;
    public float sitProgress;
    private boolean isLandNavigator;
    private int timeFlying = 0;
    @Nullable
    private UUID seedThrowerID;
    private int heldItemTime = 0;
    private int checkPerchCooldown = 0;
    private boolean gatheringClockwise = false;

    protected EntityCrow(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathNodeType.COCOA, -1.0F);
        this.setPathfindingMalus(PathNodeType.FENCE, -1.0F);
        switchNavigator(false);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new CrowAIMelee(this));
        this.goalSelector.addGoal(3, new CrowAIFollowOwner(this, 1.0D, 4.0F, 2.0F, true));
        this.goalSelector.addGoal(4, new AIDepositChests());
        this.goalSelector.addGoal(4, new AIScatter());
        this.goalSelector.addGoal(5, new AIAvoidPumpkins());
        this.goalSelector.addGoal(5, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new CrowAICircleCrops(this));
        this.goalSelector.addGoal(7, new AIWalkIdle());
        this.goalSelector.addGoal(8, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(9, new LookAtGoal(this, CreatureEntity.class, 6.0F));
        this.goalSelector.addGoal(10, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new AITargetItems(this, false, false, 40, 16));
        this.targetSelector.addGoal(2, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(3, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(4, (new HurtByTargetGoal(this, PlayerEntity.class)).setAlertOthers());

    }


    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.crowSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static <T extends MobEntity> boolean canCrowSpawn(EntityType<EntityCrow> crow, IWorld worldIn, SpawnReason reason, BlockPos p_223317_3_, Random random) {
        BlockState blockstate = worldIn.getBlockState(p_223317_3_.below());
        return (blockstate.is(BlockTags.LEAVES) || blockstate.is(Blocks.GRASS_BLOCK) || blockstate.is(BlockTags.LOGS) || blockstate.is(Blocks.AIR)) && worldIn.getRawBrightness(p_223317_3_, 0) > 8;
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

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigator(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new FlightMoveController(this, 0.7F, false);
            this.navigation = new DirectPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
        return false;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            this.setOrderedToSit(false);
            if (entity != null && this.isTame() && !(entity instanceof PlayerEntity) && !(entity instanceof AbstractArrowEntity)) {
                amount = (amount + 1.0F) / 4.0F;
            }
            boolean prev = super.hurt(source, amount);
            if (prev) {
                if (!this.getMainHandItem().isEmpty()) {
                    this.spawnAtLocation(this.getMainHandItem().copy());
                    this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                }
            }
            return prev;
        }
    }

    public void rideTick() {
        Entity entity = this.getVehicle();
        if (this.isPassenger() && !entity.isAlive()) {
            this.stopRiding();
        } else if (isTame() && entity instanceof LivingEntity && isOwnedBy((LivingEntity) entity)) {
            this.setDeltaMovement(0, 0, 0);
            this.tick();
            Entity riding = this.getVehicle();
            if (this.isPassenger()) {
                int i = riding.getPassengers().indexOf(this);
                float radius = 0.43F;
                float angle = (0.01745329251F * (((PlayerEntity) riding).yBodyRot + (i == 0 ? -90 : 90)));
                double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
                double extraZ = radius * MathHelper.cos(angle);
                double extraY = (riding.isShiftKeyDown() ? 1.25D : 1.45D);
                this.yHeadRot = ((PlayerEntity) riding).yHeadRot;
                this.yRotO = ((PlayerEntity) riding).yHeadRot;
                this.setPos(riding.getX() + extraX, riding.getY() + extraY, riding.getZ() + extraZ);
                if (!riding.isAlive() || boardingCooldown == 0 && riding.isShiftKeyDown() || ((PlayerEntity) riding).isFallFlying() || this.getTarget() != null && this.getTarget().isAlive()) {
                    this.removeVehicle();
                    if (!level.isClientSide) {
                        AlexsMobs.sendMSGToAll(new MessageCrowDismount(this.getId(), riding.getId()));
                    }
                }
            }
        }else{
            super.rideTick();
        }
    }


    public int getRidingCrows(LivingEntity player) {
        int crowCount = 0;
        for (Entity e : player.getPassengers()) {
            if (e instanceof EntityCrow) {
                crowCount++;
            }
        }
        return crowCount;
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.PUMPKIN_SEEDS && this.isTame();
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if (!this.getMainHandItem().isEmpty() && type != ActionResultType.SUCCESS) {
            this.spawnAtLocation(this.getMainHandItem().copy());
            this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            return ActionResultType.SUCCESS;
        } else {
            if (type == ActionResultType.PASS && isTame() && isOwnedBy(player) && !isFood(itemstack)) {
                if (isCrowEdible(itemstack) && this.getMainHandItem().isEmpty()) {
                    ItemStack cop = itemstack.copy();
                    cop.setCount(1);
                    this.setItemInHand(Hand.MAIN_HAND, cop);
                    itemstack.shrink(1);
                }
                this.setCommand(this.getCommand() + 1);
                if (this.getCommand() == 4) {
                    this.setCommand(0);
                }
                if(this.getCommand() == 3){
                    player.displayClientMessage(new TranslationTextComponent("entity.alexsmobs.crow.command_3", this.getName()), true);
                }else{
                    player.displayClientMessage(new TranslationTextComponent("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                }
                boolean sit = this.getCommand() == 2;
                if (sit) {
                    this.setOrderedToSit(true);
                    return ActionResultType.SUCCESS;
                } else {
                    this.setOrderedToSit(false);
                    return ActionResultType.SUCCESS;
                }
            }
            return super.mobInteract(player, hand);
        }
    }


    public void tick() {
        super.tick();
        this.prevAttackProgress = attackProgress;
        prevFlyProgress = flyProgress;
        this.prevSitProgress = this.sitProgress;
        if ((this.isSitting() || this.isPassenger()) && sitProgress < 5) {
            sitProgress += 1;
        }
        if (!(this.isSitting() || this.isPassenger()) && sitProgress > 0) {
            sitProgress -= 1;
        }
        if (isFlying() && flyProgress < 5F) {
            flyProgress++;
        }
        if (!isFlying() && flyProgress > 0F) {
            flyProgress--;
        }
        if (fleePumpkinFlag > 0) {
            fleePumpkinFlag--;
        }
        if (!level.isClientSide) {
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
            if (isFlying()) {
                timeFlying++;
                this.setNoGravity(true);
                if (this.isSitting() || this.isPassenger() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                timeFlying = 0;
                this.setNoGravity(false);
            }
        }
        if (!this.getMainHandItem().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 60 && isCrowEdible(this.getMainHandItem()) && (!this.isTame() || this.getHealth() < this.getMaxHealth())) {
                heldItemTime = 0;
                this.heal(4);
                this.playSound(SoundEvents.PARROT_EAT, this.getSoundVolume(), this.getVoicePitch());
                if (this.getMainHandItem().getItem() == Items.PUMPKIN_SEEDS && seedThrowerID != null && !this.isTame()) {
                    if (getRandom().nextFloat() < 0.3F) {
                        this.setTame(true);
                        this.setCommand(1);
                        this.setOwnerUUID(this.seedThrowerID);
                        PlayerEntity player = level.getPlayerByUUID(seedThrowerID);
                        if (player instanceof ServerPlayerEntity) {
                            CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
                        }
                        this.level.broadcastEntityEvent(this, (byte) 7);
                    } else {
                        this.level.broadcastEntityEvent(this, (byte) 6);
                    }
                }
                if (this.getMainHandItem().hasContainerItem()) {
                    this.spawnAtLocation(this.getMainHandItem().getContainerItem());
                }
                this.getMainHandItem().shrink(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (boardingCooldown > 0) {
            boardingCooldown--;
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
        if(checkPerchCooldown > 0){
            checkPerchCooldown--;
        }

        if(this.isTame() && checkPerchCooldown == 0){
            checkPerchCooldown = 50;
            BlockState below = this.getBlockStateOn();
            if(below.getBlock() == Blocks.HAY_BLOCK){
                this.heal(1);
                this.level.broadcastEntityEvent(this, (byte) 67);
                this.setPerchPos(this.getBlockPosBelowThatAffectsMyMovement());
            }
        }
        if(this.getCommand() == 3 && isTame() && getPerchPos() != null && checkPerchCooldown == 0){
            checkPerchCooldown = 120;
            BlockState below = this.level.getBlockState(getPerchPos());
            if(below.getBlock() != Blocks.HAY_BLOCK){
                this.level.broadcastEntityEvent(this, (byte) 68);
                this.setPerchPos(null);
                this.setCommand(2);
                this.setOrderedToSit(true);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 67) {
            for(int i = 0; i < 7; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
            }
        } else if (id == 68) {
            for(int i = 0; i < 7; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(ParticleTypes.ANGRY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
            }
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("MonkeySitting", this.isSitting());
        compound.putInt("Command", this.getCommand());
        if (this.getPerchPos() != null) {
            compound.putInt("PerchX", this.getPerchPos().getX());
            compound.putInt("PerchY", this.getPerchPos().getY());
            compound.putInt("PerchZ", this.getPerchPos().getZ());
        }
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

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setOrderedToSit(compound.getBoolean("MonkeySitting"));
        this.setCommand(compound.getInt("Command"));
        if (compound.contains("PerchX") && compound.contains("PerchY") && compound.contains("PerchZ")) {
            this.setPerchPos(new BlockPos(compound.getInt("PerchX"), compound.getInt("PerchY"), compound.getInt("PerchZ")));
        }
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        if(flying && isBaby()){
            return;
        }
        this.entityData.set(FLYING, flying);
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

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
        this.entityData.define(ATTACK_TICK, 0);
        this.entityData.define(COMMAND, Integer.valueOf(0));
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(PERCH_POS, Optional.empty());
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || source == DamageSource.CACTUS || super.isInvulnerableTo(source);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.CROW.create(serverWorld);
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());

        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    public int getAmbientSoundInterval() {
        return 60;
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CROW_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CROW_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CROW_HURT;
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
        BlockPos newPos = ground.above(distFromGround > 8 ? flightHeight : (int)this.getRandom().nextInt(6) + 1);
        if (!this.isTargetBlocked(Vector3d.atCenterOf(newPos)) && this.distanceToSqr(Vector3d.atCenterOf(newPos)) > 1) {
            return Vector3d.atCenterOf(newPos);
        }
        return null;
    }


    private BlockPos getCrowGround(BlockPos in){
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

    private boolean isOverWater() {
        BlockPos position = this.blockPosition();
        while (position.getY() > 2 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return !level.getFluidState(position).isEmpty();
    }

    public void peck() {
        this.entityData.set(ATTACK_TICK, 7);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return isCrowEdible(stack) || this.isTame();
    }

    private boolean isCrowEdible(ItemStack stack) {
        return stack.getItem().isEdible() || ItemTags.getAllTags().getTag(AMTagRegistry.CROW_FOODSTUFFS).contains(stack.getItem());
    }

    public double getMaxDistToItem() {
        return 1.0D;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        ItemStack duplicate = e.getItem().copy();
        duplicate.setCount(1);
        if (!this.getItemInHand(Hand.MAIN_HAND).isEmpty() && !this.level.isClientSide) {
            this.spawnAtLocation(this.getItemInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setItemInHand(Hand.MAIN_HAND, duplicate);
        if (e.getItem().getItem() == Items.PUMPKIN_SEEDS && !this.isTame()) {
            seedThrowerID = e.getThrower();
        } else {
            seedThrowerID = null;
        }
    }

    public BlockPos getPerchPos() {
        return this.entityData.get(PERCH_POS).orElse(null);
    }

    public void setPerchPos(BlockPos pos) {
        this.entityData.set(PERCH_POS, Optional.ofNullable(pos));
    }


    private class AIWalkIdle extends Goal {
        protected final EntityCrow crow;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;

        public AIWalkIdle() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE));
            this.crow = EntityCrow.this;
        }

        @Override
        public boolean canUse() {
            if (this.crow.isVehicle() || EntityCrow.this.getCommand() == 1 || EntityCrow.this.aiItemFlag || (crow.getTarget() != null && crow.getTarget().isAlive()) || this.crow.isPassenger() || this.crow.isSitting()) {
                return false;
            } else {
                if (this.crow.getRandom().nextInt(30) != 0 && !crow.isFlying()) {
                    return false;
                }
                if (this.crow.isOnGround()) {
                    this.flightTarget = random.nextBoolean();
                } else {
                    this.flightTarget = random.nextInt(5) > 0 && crow.timeFlying < 200;
                }
                if(crow.getCommand() == 3){
                    if(crow.aiItemFrameFlag){
                        return false;
                    }
                    this.flightTarget = true;
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
                crow.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                this.crow.getNavigation().moveTo(this.x, this.y, this.z, 1F);
            }
            if (!flightTarget && isFlying() && crow.onGround) {
                crow.setFlying(false);
            }
            if (isFlying() && crow.onGround && crow.timeFlying > 10) {
                crow.setFlying(false);
            }
        }

        @Nullable
        protected Vector3d getPosition() {
            Vector3d vector3d = crow.position();
            if (crow.getCommand() == 3 && crow.getPerchPos() != null) {
                return crow.getGatheringVec(vector3d, 4 + random.nextInt(2));
            }
            if(crow.isOverWater()){
                flightTarget = true;
            }
            if (flightTarget) {
                if (crow.timeFlying < 50 || crow.isOverWater()) {
                    return crow.getBlockInViewAway(vector3d, 0);
                } else {
                    return crow.getBlockGrounding(vector3d);
                }
            } else {

                return RandomPositionGenerator.getPos(this.crow, 10, 7);
            }
        }

        public boolean canContinueToUse() {
            if (crow.aiItemFlag || crow.isSitting() || EntityCrow.this.getCommand() == 1) {
                return false;
            }
            if (flightTarget) {
                return crow.isFlying() && crow.distanceToSqr(x, y, z) > 2F;
            } else {
                return (!this.crow.getNavigation().isDone()) && !this.crow.isVehicle();
            }
        }

        public void start() {
            if (flightTarget) {
                crow.setFlying(true);
                crow.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                this.crow.getNavigation().moveTo(this.x, this.y, this.z, 1F);
            }
        }

        public void stop() {
            this.crow.getNavigation().stop();
            super.stop();
        }
    }

    private Vector3d getGatheringVec(Vector3d vector3d, float gatheringCircleDist) {
        float angle = (0.01745329251F * 8 * (gatheringClockwise ? -tickCount : tickCount));
        double extraX = gatheringCircleDist * MathHelper.sin((angle));
        double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if(this.getPerchPos() != null){
            Vector3d pos = new Vector3d(getPerchPos().getX() + extraX, getPerchPos().getY() + 2, getPerchPos().getZ() + extraZ);
            if (this.level.isEmptyBlock(new BlockPos(pos))) {
                return pos;
            }
        }
        return null;
    }

    private class AIScatter extends Goal {
        protected final AIScatter.Sorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vector3d flightTarget = null;
        private int cooldown = 0;
        private ITag tag;

        AIScatter() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            tag = EntityTypeTags.getAllTags().getTag(AMTagRegistry.SCATTERS_CROWS);
            this.theNearestAttackableTargetSorter = new AIScatter.Sorter(EntityCrow.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e.isAlive() && e.getType().is(tag) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntityCrow.this.isPassenger() || EntityCrow.this.aiItemFlag || EntityCrow.this.isVehicle() || EntityCrow.this.isTame()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityCrow.this.level.getGameTime() % 10;
                if (EntityCrow.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityCrow.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityCrow.this.level.getEntitiesOfClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null && !EntityCrow.this.isTame();
        }

        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (flightTarget != null) {
                EntityCrow.this.setFlying(true);
                EntityCrow.this.getMoveControl().setWantedPosition(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                if(cooldown == 0 && EntityCrow.this.isTargetBlocked(flightTarget)){
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (EntityCrow.this.onGround || flightTarget == null || flightTarget != null && EntityCrow.this.distanceToSqr(flightTarget) < 3) {
                    Vector3d vec = EntityCrow.this.getBlockInViewAway(targetEntity.position(), 0);
                    if (vec != null && vec.y() > EntityCrow.this.getY()) {
                        flightTarget = vec;
                    }
                }
                if (EntityCrow.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntityCrow.this.getX(), EntityCrow.this.getY() + 0.5, EntityCrow.this.getZ());
            AxisAlignedBB aabb = new AxisAlignedBB(-2, -2, -2, 2, 2, 2);
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

    private class AIAvoidPumpkins extends Goal {
        private final int searchLength;
        private final int verticalSearchRange;
        protected BlockPos destinationBlock;
        protected int runDelay = 70;
        private Vector3d flightTarget;

        private AIAvoidPumpkins() {
            searchLength = 20;
            verticalSearchRange = 1;
        }

        public boolean canContinueToUse() {
            return destinationBlock != null && isPumpkin(EntityCrow.this.level, destinationBlock.mutable()) && isCloseToPumpkin(16);
        }

        public boolean isCloseToPumpkin(double dist) {
            return destinationBlock == null || EntityCrow.this.distanceToSqr(Vector3d.atCenterOf(destinationBlock)) < dist * dist;
        }

        @Override
        public boolean canUse() {
            if (EntityCrow.this.isTame()) {
                return false;
            }
            if (this.runDelay > 0) {
                --this.runDelay;
                return false;
            } else {
                this.runDelay = 70 + EntityCrow.this.random.nextInt(150);
                return this.searchForDestination();
            }
        }

        public void start() {
            EntityCrow.this.fleePumpkinFlag = 200;
            Vector3d vec = EntityCrow.this.getBlockInViewAway(Vector3d.atCenterOf(destinationBlock), 10);
            if (vec != null) {
                flightTarget = vec;
                EntityCrow.this.setFlying(true);
                EntityCrow.this.getMoveControl().setWantedPosition(vec.x, vec.y, vec.z, 1F);
            }
        }

        public void tick() {
            if (this.isCloseToPumpkin(16)) {
                EntityCrow.this.fleePumpkinFlag = 200;
                if (flightTarget == null || EntityCrow.this.distanceToSqr(flightTarget) < 2F) {
                    Vector3d vec = EntityCrow.this.getBlockInViewAway(Vector3d.atCenterOf(destinationBlock), 10);
                    if (vec != null) {
                        flightTarget = vec;
                        EntityCrow.this.setFlying(true);
                    }
                }
                if (flightTarget != null) {
                    EntityCrow.this.getMoveControl().setWantedPosition(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                }
            }
        }

        public void stop() {
            flightTarget = null;
        }

        protected boolean searchForDestination() {
            int lvt_1_1_ = this.searchLength;
            int lvt_2_1_ = this.verticalSearchRange;
            BlockPos lvt_3_1_ = EntityCrow.this.blockPosition();
            BlockPos.Mutable lvt_4_1_ = new BlockPos.Mutable();

            for (int lvt_5_1_ = -8; lvt_5_1_ <= 2; lvt_5_1_++) {
                for (int lvt_6_1_ = 0; lvt_6_1_ < lvt_1_1_; ++lvt_6_1_) {
                    for (int lvt_7_1_ = 0; lvt_7_1_ <= lvt_6_1_; lvt_7_1_ = lvt_7_1_ > 0 ? -lvt_7_1_ : 1 - lvt_7_1_) {
                        for (int lvt_8_1_ = lvt_7_1_ < lvt_6_1_ && lvt_7_1_ > -lvt_6_1_ ? lvt_6_1_ : 0; lvt_8_1_ <= lvt_6_1_; lvt_8_1_ = lvt_8_1_ > 0 ? -lvt_8_1_ : 1 - lvt_8_1_) {
                            lvt_4_1_.setWithOffset(lvt_3_1_, lvt_7_1_, lvt_5_1_ - 1, lvt_8_1_);
                            if (this.isPumpkin(EntityCrow.this.level, lvt_4_1_)) {
                                this.destinationBlock = lvt_4_1_;
                                return true;
                            }
                        }
                    }
                }
            }

            return false;
        }

        private boolean isPumpkin(World world, BlockPos.Mutable lvt_4_1_) {
            return BlockTags.getAllTags().getTag(AMTagRegistry.CROW_FEARS).contains(world.getBlockState(lvt_4_1_).getBlock());
        }

    }

    private class AITargetItems extends CreatureAITargetItems {

        public AITargetItems(CreatureEntity creature, boolean checkSight, boolean onlyNearby, int tickThreshold, int radius) {
            super(creature, checkSight, onlyNearby, tickThreshold, radius);
            this.executionChance = 1;
        }

        public void stop() {
            super.stop();
            ((EntityCrow) mob).aiItemFlag = false;
        }

        public boolean canUse() {
            return super.canUse()  &&  !((EntityCrow) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        public boolean canContinueToUse() {
            return super.canContinueToUse() && !((EntityCrow) mob).isSitting() &&  (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        protected void moveTo() {
            EntityCrow crow = (EntityCrow) mob;
            if (this.targetEntity != null) {
                crow.aiItemFlag = true;
                if (this.mob.distanceTo(targetEntity) < 2) {
                    crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1);
                    crow.peck();
                }
                if (this.mob.distanceTo(this.targetEntity) > 8 || crow.isFlying()) {
                    crow.setFlying(true);
                    float f = (float) (crow.getX() - targetEntity.getX());
                    float f1 = 1.8F;
                    float f2 = (float) (crow.getZ() - targetEntity.getZ());
                    float xzDist = MathHelper.sqrt(f * f + f2 * f2);

                    if(!crow.canSee(targetEntity)){
                        crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), 1 + crow.getY(), this.targetEntity.getZ(), 1);
                    }else{
                        if (xzDist < 5) {
                            f1 = 0;
                        }
                        crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1);
                    }
                } else {
                    this.mob.getNavigation().moveTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            moveTo();
        }
    }


    private class AIDepositChests extends Goal {
        protected final AIDepositChests.Sorter theNearestAttackableTargetSorter;
        protected final Predicate<ItemFrameEntity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private ItemFrameEntity targetEntity;
        private Vector3d flightTarget = null;
        private int cooldown = 0;
        private ITag tag;

        AIDepositChests() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            this.theNearestAttackableTargetSorter = new AIDepositChests.Sorter(EntityCrow.this);
            this.targetEntitySelector = new Predicate<ItemFrameEntity>() {
                @Override
                public boolean apply(@Nullable ItemFrameEntity e) {
                    BlockPos hangingPosition = e.getPos().relative(e.getDirection().getOpposite());
                    TileEntity entity = e.level.getBlockEntity(hangingPosition);
                    if(entity != null){
                        LazyOptional<IItemHandler> handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, e.getDirection().getOpposite());
                        if(handler != null && handler.isPresent()){
                            return e.getItem().sameItem(EntityCrow.this.getMainHandItem());
                        }
                    }
                    return false;
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntityCrow.this.isPassenger() || EntityCrow.this.aiItemFlag || EntityCrow.this.isVehicle() || EntityCrow.this.isSitting() || EntityCrow.this.getCommand() != 3) {
                return false;
            }
            if(EntityCrow.this.getMainHandItem().isEmpty()){
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityCrow.this.level.getGameTime() % 10;
                if (EntityCrow.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityCrow.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<ItemFrameEntity> list = EntityCrow.this.level.getEntitiesOfClass(ItemFrameEntity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
            if (list.isEmpty()) {
                return false;
            } else {
                Collections.sort(list, this.theNearestAttackableTargetSorter);
                this.targetEntity = list.get(0);
                this.mustUpdate = false;
                EntityCrow.this.aiItemFrameFlag = true;
                return true;
            }
        }

        @Override
        public boolean canContinueToUse() {
            return targetEntity != null && EntityCrow.this.getCommand() == 3 && !EntityCrow.this.getMainHandItem().isEmpty();
        }

        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
            EntityCrow.this.aiItemFrameFlag = false;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (flightTarget != null) {
                EntityCrow.this.setFlying(true);
                if(EntityCrow.this.horizontalCollision){
                    EntityCrow.this.getMoveControl().setWantedPosition(flightTarget.x, EntityCrow.this.getY() + 1F, flightTarget.z, 1F);

                }else{
                    EntityCrow.this.getMoveControl().setWantedPosition(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                }
            }
            if (targetEntity != null) {
                flightTarget = targetEntity.position();
                if (EntityCrow.this.distanceTo(targetEntity) < 2.0F) {
                    try{
                        BlockPos hangingPosition = targetEntity.getPos().relative(targetEntity.getDirection().getOpposite());
                        TileEntity entity = targetEntity.level.getBlockEntity(hangingPosition);
                        Direction deposit = targetEntity.getDirection();
                        LazyOptional<IItemHandler> handler = entity.getCapability(CapabilityItemHandler.ITEM_HANDLER_CAPABILITY, deposit);
                        if(handler.orElse(null) != null && cooldown == 0) {
                            ItemStack duplicate = EntityCrow.this.getItemInHand(Hand.MAIN_HAND).copy();
                            ItemStack insertSimulate = ItemHandlerHelper.insertItem(handler.orElse(null), duplicate, true);
                            if (!insertSimulate.equals(duplicate)) {
                                ItemStack shrunkenStack = ItemHandlerHelper.insertItem(handler.orElse(null), duplicate, false);
                                if(shrunkenStack.isEmpty()){
                                    EntityCrow.this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                                }else{
                                    EntityCrow.this.setItemInHand(Hand.MAIN_HAND, shrunkenStack);
                                }
                                EntityCrow.this.peck();
                            }else{
                                cooldown = 20;
                            }
                        }
                    }catch (Exception e){
                    }
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntityCrow.this.getX(), EntityCrow.this.getY(), EntityCrow.this.getZ());
            AxisAlignedBB aabb = new AxisAlignedBB(-16, -16, -16, 16, 16, 16);
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
