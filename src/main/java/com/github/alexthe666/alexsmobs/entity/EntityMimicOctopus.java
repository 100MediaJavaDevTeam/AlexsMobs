package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LeavesBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.CreeperEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.passive.fish.PufferfishEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.WalkNodeProcessor;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.*;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityMimicOctopus extends TameableEntity implements ISemiAquatic, IFollower {

    private static final DataParameter<Boolean> STOP_CHANGE = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FROM_BUCKET = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> UPGRADED = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> MIMIC_ORDINAL = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    private static final DataParameter<Integer> PREV_MIMIC_ORDINAL = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    private static final DataParameter<Integer> MOISTNESS = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockState>> MIMICKED_BLOCK = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BLOCK_STATE);
    private static final DataParameter<Optional<BlockState>> PREV_MIMICKED_BLOCK = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BLOCK_STATE);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> LAST_SCARED_MOB_ID = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    private static final DataParameter<Integer> UPGRADED_LASER_ENTITY_ID = EntityDataManager.defineId(EntityMimicOctopus.class, DataSerializers.INT);
    public MimicState localMimicState = MimicState.OVERLAY;
    public float transProgress = 0F;
    public float prevTransProgress = 0F;
    public float colorShiftProgress = 0F;
    public float prevColorShiftProgress = 0F;
    public float groundProgress = 5F;
    public float prevGroundProgress = 0F;
    public float sitProgress = 0F;
    public float prevSitProgress = 0F;
    private boolean isLandNavigator;
    private int moistureAttackTime = 0;
    private int camoCooldown = 120 + random.nextInt(1200);
    private int mimicCooldown = 0;
    private int stopMimicCooldown = -1;
    private int fishFeedings;
    private int mimicreamFeedings;
    private int exclaimTime = 0;
    private BlockState localMimic;
    private LivingEntity laserTargetEntity;
    private int guardianLaserTime;

    protected EntityMimicOctopus(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 16D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    public static boolean canMimicOctopusSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        BlockPos downPos = pos;
        while (downPos.getY() > 1 && !worldIn.getFluidState(downPos).isEmpty()) {
            downPos = downPos.below();
        }
        boolean spawnBlock = BlockTags.getAllTags().getTag(AMTagRegistry.MIMIC_OCTOPUS_SPAWNS).contains(worldIn.getBlockState(downPos).getBlock());
        return spawnBlock && downPos.getY() < worldIn.getSeaLevel() + 1;
    }

    public static MimicState getStateForItem(ItemStack stack) {
        if (ItemTags.getAllTags().getTag(AMTagRegistry.MIMIC_OCTOPUS_CREEPER_ITEMS).contains(stack.getItem())) {
            return MimicState.CREEPER;
        }
        if (ItemTags.getAllTags().getTag(AMTagRegistry.MIMIC_OCTOPUS_GUARDIAN_ITEMS).contains(stack.getItem())) {
            return MimicState.GUARDIAN;
        }
        if (ItemTags.getAllTags().getTag(AMTagRegistry.MIMIC_OCTOPUS_PUFFERFISH_ITEMS).contains(stack.getItem())) {
            return MimicState.PUFFERFISH;
        }
        return null;
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.MIMIC_OCTOPUS_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MIMIC_OCTOPUS_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MIMIC_OCTOPUS_HURT;
    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mimicOctopusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.entityData.set(PREV_MIMIC_ORDINAL, 0);
        this.setMimickedBlock(null);
        this.setMimicState(MimicState.OVERLAY);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(MIMIC_ORDINAL, compound.getInt("MimicState"));
        this.setUpgraded(compound.getBoolean("Upgraded"));
        this.setOrderedToSit(compound.getBoolean("Sitting"));
        this.setStopChange(compound.getBoolean("StopChange"));
        this.setCommand(compound.getInt("OctoCommand"));
        this.setMoistness(compound.getInt("Moistness"));
        this.setFromBucket(compound.getBoolean("FromBucket"));
        BlockState blockstate = null;
        if (compound.contains("MimickedBlockState", 10)) {
            blockstate = NBTUtil.readBlockState(compound.getCompound("MimickedBlockState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        this.setMimickedBlock(blockstate);
        this.camoCooldown = compound.getInt("CamoCooldown");
        this.mimicCooldown = compound.getInt("MimicCooldown");
        this.stopMimicCooldown = compound.getInt("StopMimicCooldown");
        this.fishFeedings = compound.getInt("FishFeedings");
        this.mimicreamFeedings = compound.getInt("MimicreamFeedings");
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("MimicState", this.getMimicState().ordinal());
        compound.putBoolean("Upgraded", this.isUpgraded());
        compound.putBoolean("Sitting", this.isSitting());
        compound.putInt("OctoCommand", this.getCommand());
        compound.putInt("Moistness", this.getMoistness());
        compound.putBoolean("FromBucket", this.isFromBucket());
        compound.putBoolean("StopChange", this.isStopChange());
        BlockState blockstate = this.getMimickedBlock();
        if (blockstate != null) {
            compound.put("MimickedBlockState", NBTUtil.writeBlockState(blockstate));
        }
        compound.putInt("CamoCooldown", this.camoCooldown);
        compound.putInt("MimicCooldown", this.mimicCooldown);
        compound.putInt("StopMimicCooldown", this.stopMimicCooldown);
        compound.putInt("FishFeedings", this.fishFeedings);
        compound.putInt("MimicreamFeedings", this.mimicreamFeedings);
    }

    protected ItemStack getFishBucket() {
        ItemStack stack = new ItemStack(AMItemRegistry.MIMIC_OCTOPUS_BUCKET);
        CompoundNBT platTag = new CompoundNBT();
        this.addAdditionalSaveData(platTag);
        stack.getOrCreateTag().put("MimicOctopusData", platTag);
        if (this.hasCustomName()) {
            stack.setHoverName(this.getCustomName());
        }
        return stack;
    }

    protected float getJumpPower() {
        return super.getJumpPower() * (this.isInWaterOrBubble() ? 1.3F : 1F);
    }

    @Override
    public boolean shouldFollow() {
        return this.getCommand() == 1;
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

    public boolean isPushedByFluid() {
        return false;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new AIAttack());
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new FollowOwner(this, 1.3D, 4.0F, 2.0F, false));
        this.goalSelector.addGoal(3, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(3, new AnimalAILeaveWater(this));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, Ingredient.of(AMItemRegistry.LOBSTER_TAIL, AMItemRegistry.COOKED_LOBSTER_TAIL, Items.TROPICAL_FISH), false) {
            @Override
            public void tick() {
                EntityMimicOctopus.this.setMimickedBlock(null);
                super.tick();
                EntityMimicOctopus.this.camoCooldown = 40;
                EntityMimicOctopus.this.stopMimicCooldown = 40;
            }
        });
        this.goalSelector.addGoal(5, new AIFlee());
        this.goalSelector.addGoal(7, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(8, new AIMimicNearbyMobs());
        this.goalSelector.addGoal(9, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(10, new AISwim());
        this.goalSelector.addGoal(11, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(11, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return EntityMimicOctopus.this.isTame() && super.canUse();
            }
        });
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return isTame() && (item == Items.TROPICAL_FISH);
    }

    public boolean isActiveCamo() {
        return this.getMimicState() == MimicState.OVERLAY && this.getMimickedBlock() != null;
    }

    public double getVisibilityPercent(@Nullable Entity lookingEntity) {
        if (isActiveCamo()) {
            return super.getVisibilityPercent(lookingEntity) * 0.1F;
        } else {
            return super.getVisibilityPercent(lookingEntity);
        }
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        MimicState readState = getStateForItem(itemstack);
        ActionResultType type = super.mobInteract(player, hand);
        if (readState != null && this.isTame()) {
            if (mimicCooldown == 0) {
                this.setMimicState(readState);
                mimicCooldown = 20;
                stopMimicCooldown = isUpgraded() ? 120 : 1200;
                camoCooldown = stopMimicCooldown;
                this.setMimickedBlock(null);
            }
            return ActionResultType.SUCCESS;
        }
        if (isTame() && (item == Items.INK_SAC)) {
            this.setStopChange(!this.isStopChange());
            if (this.isStopChange()) {
                this.makeEatingParticles(itemstack);
            } else {
                this.level.broadcastEntityEvent(this, (byte) 6);
                this.mimicEnvironment();
            }
            return ActionResultType.SUCCESS;
        }
        if (!isTame() && (item == AMItemRegistry.LOBSTER_TAIL || item == AMItemRegistry.COOKED_LOBSTER_TAIL)) {
            this.usePlayerItem(player, itemstack);
            this.playSound(SoundEvents.DOLPHIN_EAT, this.getSoundVolume(), this.getVoicePitch());
            fishFeedings++;
            if (this.getMimicState() == MimicState.OVERLAY && this.getMimickedBlock() == null) {
                if (fishFeedings > 5 && getRandom().nextInt(2) == 0 || fishFeedings > 8) {
                    this.tame(player);
                    this.level.broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.level.broadcastEntityEvent(this, (byte) 6);
                }
            }
            return ActionResultType.SUCCESS;
        }
        if (isTame() && (item == AMItemRegistry.LOBSTER_TAIL || item == AMItemRegistry.COOKED_LOBSTER_TAIL)) {
            if (this.getHealth() < this.getMaxHealth()) {
                this.usePlayerItem(player, itemstack);
                this.playSound(SoundEvents.DOLPHIN_EAT, this.getSoundVolume(), this.getVoicePitch());
                this.heal(5);
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.PASS;
        }
        if (this.isTame() && itemstack.getItem() == Items.WATER_BUCKET && this.isAlive()) {
            this.playSound(SoundEvents.BUCKET_FILL_FISH, 1.0F, 1.0F);
            itemstack.shrink(1);
            ItemStack itemstack1 = this.getFishBucket();
            if (!this.level.isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity) player, itemstack1);
            }

            if (itemstack.isEmpty()) {
                player.setItemInHand(hand, itemstack1);
            } else if (!player.inventory.add(itemstack1)) {
                player.drop(itemstack1, false);
            }

            this.remove();
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        if (this.isTame() && item == Items.SLIME_BALL && this.getMoistness() < 24000) {
            this.setMoistness(48000);
            this.makeEatingParticles(itemstack);
            this.usePlayerItem(player, itemstack);
            return ActionResultType.SUCCESS;
        }
        if (this.isTame() && !this.isUpgraded() && item == AMItemRegistry.MIMICREAM) {
            mimicreamFeedings++;
            if (mimicreamFeedings > 5 || mimicreamFeedings > 2 && random.nextInt(2) == 0) {
                this.level.broadcastEntityEvent(this, (byte) 46);
                this.setUpgraded(true);
                this.setMimicState(MimicState.MIMICUBE);
                this.setStopChange(false);
                this.setMimickedBlock(null);
                this.stopMimicCooldown = 40;
            }
            this.makeEatingParticles(itemstack);
            this.usePlayerItem(player, itemstack);
            return ActionResultType.SUCCESS;
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

    public int getCommand() {
        return this.entityData.get(COMMAND).intValue();
    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, Integer.valueOf(command));
    }

    private void makeEatingParticles(ItemStack item) {
        for (int i = 0; i < 6 + random.nextInt(3); i++) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, item), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
        }
    }

    @Override
    public void calculateEntityAnimation(LivingEntity p_233629_1_, boolean p_233629_2_) {
        p_233629_1_.animationSpeedOld = p_233629_1_.animationSpeed;
        double d0 = p_233629_1_.getX() - p_233629_1_.xo;
        double d1 = p_233629_1_.getY() - p_233629_1_.yo;
        double d2 = p_233629_1_.getZ() - p_233629_1_.zo;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * (groundProgress < 2.5F ? 4.0F : 8.0F);
        if (f > 1.0F) {
            f = 1.0F;
        }

        p_233629_1_.animationSpeed += (f - p_233629_1_.animationSpeed) * 0.4F;
        p_233629_1_.animationPosition += p_233629_1_.animationSpeed;
    }

    public boolean canBreatheUnderwater() {
        return true;
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigatorWide(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.3F, 1);
            this.navigation = new SemiAquaticPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    public void tick() {
        super.tick();
        if (localMimic != this.getPrevMimickedBlock()) {
            localMimic = this.getPrevMimickedBlock();
            colorShiftProgress = 0.0F;
        }
        if (localMimicState != this.getPrevMimicState()) {
            localMimicState = this.getPrevMimicState();
            transProgress = 0.0F;
        }
        if (this.isInWater() && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (!this.isInWater() && !this.isLandNavigator) {
            switchNavigator(true);
        }
        BlockPos pos = new BlockPos(this.getX(), this.getEyeY() - 1F, this.getZ());
        boolean ground = level.getBlockState(pos).isFaceSturdy(level, pos, Direction.UP) && this.getMimicState() != MimicState.GUARDIAN || !this.isInWaterOrBubble() || this.isSitting();
        this.prevTransProgress = transProgress;
        this.prevColorShiftProgress = colorShiftProgress;
        this.prevGroundProgress = groundProgress;
        this.prevSitProgress = sitProgress;
        if (this.getPrevMimicState() != this.getMimicState() && transProgress < 5.0F) {
            transProgress += 0.25F;
        }
        if (this.getPrevMimicState() == this.getMimicState() && transProgress > 0F) {
            transProgress -= 0.25F;
        }
        if (getPrevMimickedBlock() != this.getMimickedBlock() && colorShiftProgress < 5.0F) {
            colorShiftProgress += 0.25F;
        }
        if (getPrevMimickedBlock() == this.getMimickedBlock() && colorShiftProgress > 0F) {
            colorShiftProgress -= 0.25F;
        }
        if (ground && groundProgress < 5F) {
            groundProgress += 0.5F;
        }
        if (!ground && groundProgress > 0F) {
            groundProgress -= 0.5F;
        }
        if (isSitting() && sitProgress < 5F) {
            sitProgress += 0.5F;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress -= 0.5F;
        }
        if (this.isInWaterOrBubble()) {
            float f2 = (float) -((float) this.getDeltaMovement().y * 3 * (double) (180F / (float) Math.PI));
            this.xRot = f2;
        }
        if (camoCooldown > 0) {
            camoCooldown--;
        }
        if (mimicCooldown > 0) {
            mimicCooldown--;
        }
        if (stopMimicCooldown > 0) {
            stopMimicCooldown--;
        }
        if (this.isNoAi()) {
            this.setAirSupply(this.getMaxAirSupply());
        } else {
            if (this.isInWaterRainOrBubble() || this.getMainHandItem().getItem() == Items.WATER_BUCKET) {
                this.setMoistness(60000);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0 && moistureAttackTime-- <= 0) {
                    this.setOrderedToSit(false);
                    this.hurt(DamageSource.DRY_OUT, random.nextInt(2) == 0 ? 1.0F : 0F);
                    moistureAttackTime = 20;
                }
            }
        }
        if (camoCooldown <= 0 && random.nextInt(300) == 0) {
            mimicEnvironment();
            camoCooldown = this.getRandom().nextInt(2200) + 200;
        }
        if ((this.getMimicState() != MimicState.OVERLAY || this.getMimickedBlock() != null) && stopMimicCooldown == 0 && !this.isStopChange()) {
            this.setMimicState(MimicState.OVERLAY);
            this.setMimickedBlock(null);
            stopMimicCooldown = -1;
        }
        if (level.isClientSide && exclaimTime > 0) {
            exclaimTime--;
            if (exclaimTime == 0) {
                Entity e = level.getEntity(this.entityData.get(LAST_SCARED_MOB_ID));
                if (e != null && transProgress >= 5.0F) {
                    double d2 = this.random.nextGaussian() * 0.1D;
                    double d0 = this.random.nextGaussian() * 0.1D;
                    double d1 = this.random.nextGaussian() * 0.1D;
                    this.level.addParticle(AMParticleRegistry.SHOCKED, e.getX(), e.getEyeY() + e.getBbHeight() * 0.15F + (double) (this.random.nextFloat() * e.getBbHeight() * 0.15F), e.getZ(), d0, d1, d2);
                }
            }
        }

        if (this.hasGuardianLaser()) {
            if (this.guardianLaserTime < 30) {
                ++this.guardianLaserTime;
            }
            LivingEntity livingentity = this.getGuardianLaser();
            if (livingentity != null && this.isInWaterOrBubble()) {
                this.getLookControl().setLookAt(livingentity, 90.0F, 90.0F);
                this.getLookControl().tick();
                double d5 = this.getLaserAttackAnimationScale(0.0F);
                double d0 = livingentity.getX() - this.getX();
                double d1 = livingentity.getY(0.5D) - this.getEyeY();
                double d2 = livingentity.getZ() - this.getZ();
                double d3 = Math.sqrt(d0 * d0 + d1 * d1 + d2 * d2);
                d0 = d0 / d3;
                d1 = d1 / d3;
                d2 = d2 / d3;
                double d4 = this.random.nextDouble();
                while (d4 < d3) {
                    d4 += 1.8D - d5 + this.random.nextDouble() * (1.7D - d5);
                    this.level.addParticle(ParticleTypes.BUBBLE, this.getX() + d0 * d4, this.getEyeY() + d1 * d4, this.getZ() + d2 * d4, 0.0D, 0.0D, 0.0D);
                }
                if (guardianLaserTime == 30) {
                    livingentity.hurt(DamageSource.mobAttack(this), 5);
                    guardianLaserTime = 0;
                    this.entityData.set(UPGRADED_LASER_ENTITY_ID, -1);
                }
            }
        }
        if (!level.isClientSide && tickCount % 40 == 0) {
            this.heal(2);
        }
    /*if(!world.isRemote){
            if(ticksExisted % 80 == 0){
                mimicEnvironment();
            }else if(ticksExisted % 40 == 0){
                this.setMimicState(MimicState.OVERLAY);
                this.setMimickedBlock(null);
            }
        }*/
    }

    public float getLaserAttackAnimationScale(float p_175477_1_) {
        return ((float) this.guardianLaserTime + p_175477_1_) / 30F;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 68) {
            if (exclaimTime == 0) {
                exclaimTime = 20;
            }
        } else if (id == 69) {
            this.creeperExplode();
        } else {
            super.handleEntityEvent(id);
        }
    }

    public void mimicEnvironment() {
        if (!this.isStopChange()) {
            BlockPos down = getPositionDown();
            if (!level.isEmptyBlock(down)) {
                this.setMimicState(MimicState.OVERLAY);
                this.setMimickedBlock(level.getBlockState(down));
            }
            stopMimicCooldown = this.getRandom().nextInt(2200);
        }
    }

    public int getMoistness() {
        return this.entityData.get(MOISTNESS);
    }

    public void setMoistness(int p_211137_1_) {
        this.entityData.set(MOISTNESS, p_211137_1_);
    }

    private BlockPos getPositionDown() {
        BlockPos pos = new BlockPos(this.getX(), this.getEyeY(), this.getZ());
        while (pos.getY() > 1 && (level.isEmptyBlock(pos) || level.getBlockState(pos).getMaterial() == Material.WATER)) {
            pos = pos.below();
        }
        return pos;
    }

    public void travel(Vector3d travelVector) {
        if (this.isSitting()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            travelVector = Vector3d.ZERO;
            super.travel(travelVector);
            return;
        }
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
        } else {
            super.travel(travelVector);
        }
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean isFromBucket() {
        return this.entityData.get(FROM_BUCKET).booleanValue();
    }

    public void setFromBucket(boolean sit) {
        this.entityData.set(FROM_BUCKET, Boolean.valueOf(sit));
    }

    public boolean isUpgraded() {
        return this.entityData.get(FROM_BUCKET).booleanValue();
    }

    public void setUpgraded(boolean sit) {
        this.entityData.set(FROM_BUCKET, Boolean.valueOf(sit));
    }

    public boolean isStopChange() {
        return this.entityData.get(STOP_CHANGE).booleanValue();
    }

    public void setStopChange(boolean sit) {
        this.entityData.set(STOP_CHANGE, Boolean.valueOf(sit));
    }

    public boolean hasGuardianLaser() {
        return this.entityData.get(UPGRADED_LASER_ENTITY_ID) != -1 && this.isUpgraded() && this.isInWaterOrBubble();
    }

    @Nullable
    public LivingEntity getGuardianLaser() {
        if (!this.hasGuardianLaser()) {
            return null;
        } else if (this.level.isClientSide) {
            if (this.laserTargetEntity != null) {
                return this.laserTargetEntity;
            } else {
                Entity lvt_1_1_ = this.level.getEntity(this.entityData.get(UPGRADED_LASER_ENTITY_ID));
                if (lvt_1_1_ instanceof LivingEntity) {
                    this.laserTargetEntity = (LivingEntity) lvt_1_1_;
                    return this.laserTargetEntity;
                } else {
                    return null;
                }
            }
        } else {
            return this.getTarget();
        }
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.MIMIC_OCTOPUS.create(serverWorld);
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && !this.isFromBucket();
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MIMIC_ORDINAL, 0);
        this.entityData.define(PREV_MIMIC_ORDINAL, -1);
        this.entityData.define(MOISTNESS, 60000);
        this.entityData.define(MIMICKED_BLOCK, Optional.empty());
        this.entityData.define(PREV_MIMICKED_BLOCK, Optional.empty());
        this.entityData.define(SITTING, false);
        this.entityData.define(COMMAND, 0);
        this.entityData.define(LAST_SCARED_MOB_ID, -1);
        this.entityData.define(FROM_BUCKET, false);
        this.entityData.define(UPGRADED, false);
        this.entityData.define(STOP_CHANGE, false);
        this.entityData.define(UPGRADED_LASER_ENTITY_ID, -1);
    }

    public MimicState getMimicState() {
        return MimicState.values()[MathHelper.clamp(entityData.get(MIMIC_ORDINAL), 0, 4)];
    }

    public void setMimicState(MimicState state) {
        if (getMimicState() != state) {
            this.entityData.set(PREV_MIMIC_ORDINAL, this.entityData.get(MIMIC_ORDINAL));
        }
        this.entityData.set(MIMIC_ORDINAL, state.ordinal());
    }

    public MimicState getPrevMimicState() {
        if (entityData.get(PREV_MIMIC_ORDINAL) == -1) {
            return null;
        }
        return MimicState.values()[MathHelper.clamp(entityData.get(PREV_MIMIC_ORDINAL), 0, 4)];
    }

    @Nullable
    public BlockState getMimickedBlock() {
        return this.entityData.get(MIMICKED_BLOCK).orElse(null);
    }

    public void setMimickedBlock(@Nullable BlockState state) {
        if (getMimickedBlock() != state) {
            this.entityData.set(PREV_MIMICKED_BLOCK, Optional.ofNullable(getMimickedBlock()));
        }
        this.entityData.set(MIMICKED_BLOCK, Optional.ofNullable(state));
    }

    @Nullable
    public BlockState getPrevMimickedBlock() {
        return this.entityData.get(PREV_MIMICKED_BLOCK).orElse(null);
    }

    protected void updateAir(int p_209207_1_) {
        if (this.isAlive() && !this.isInWaterOrBubble()) {
            this.setAirSupply(p_209207_1_ - 1);
            if (this.getAirSupply() == -20) {
                this.setAirSupply(0);
                this.hurt(DamageSource.DROWN, 2.0F);
            }
        } else {
            this.setAirSupply(1200);
        }
    }

    @Override
    public boolean shouldEnterWater() {
        return !this.isSitting() && (this.getTarget() == null || this.getTarget().isInWaterOrBubble());
    }

    @Override
    public boolean shouldLeaveWater() {
        return this.getTarget() != null && !this.getTarget().isInWaterOrBubble();
    }

    @Override
    public boolean shouldStopMoving() {
        return isSitting();
    }

    @Override
    public int getWaterSearchRange() {
        return 16;
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());

        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    public Vector3d getBlockInViewAway(Vector3d fleePos, float radiusAdd) {
        float radius = 0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24) - radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getOctopusGround(radialPos);

        return ground != null ? Vector3d.atCenterOf(ground) : null;
    }

    private BlockPos getOctopusGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), this.getY(), in.getZ());
        while (position.getY() > 2 && level.getFluidState(position).is(FluidTags.WATER)) {
            position = position.below();
        }
        return position;
    }

    public void onSyncedDataUpdated(DataParameter<?> key) {
        super.onSyncedDataUpdated(key);
        if (UPGRADED_LASER_ENTITY_ID.equals(key)) {
            this.guardianLaserTime = 0;
            this.laserTargetEntity = null;
        }

    }

    private void creeperExplode() {
        Explosion explosion = new Explosion(level, this,  DamageSource.mobAttack(this), (ExplosionContext)null, this.getX(), this.getY(), this.getZ(), 1 + random.nextFloat(), false, Explosion.Mode.NONE);
        explosion.explode();
        explosion.finalizeExplosion(true);
    }

    public enum MimicState {
        OVERLAY,
        CREEPER,
        GUARDIAN,
        PUFFERFISH,
        MIMICUBE
    }

    private class AISwim extends SemiAquaticAIRandomSwimming {

        public AISwim() {
            super(EntityMimicOctopus.this, 1, 35);
        }

        protected Vector3d findSurfaceTarget(CreatureEntity creature, int i, int i1) {
            if (creature.getRandom().nextInt(5) == 0) {
                return super.findSurfaceTarget(creature, i, i1);
            } else {
                BlockPos downPos = creature.blockPosition();
                while (creature.level.getFluidState(downPos).is(FluidTags.WATER) || creature.level.getFluidState(downPos).is(FluidTags.LAVA)) {
                    downPos = downPos.below();
                }
                if (level.getBlockState(downPos).canOcclude() && level.getBlockState(downPos).getBlock() != Blocks.MAGMA_BLOCK) {
                    return new Vector3d(downPos.getX() + 0.5F, downPos.getY(), downPos.getZ() + 0.5F);
                }
            }
            return null;
        }

    }

    private class AIFlee extends Goal {
        protected final EntitySorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 8;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vector3d flightTarget = null;
        private int cooldown = 0;
        private ITag tag;

        AIFlee() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            tag = EntityTypeTags.getAllTags().getTag(AMTagRegistry.MIMIC_OCTOPUS_FEARS);
            this.theNearestAttackableTargetSorter = new EntitySorter(EntityMimicOctopus.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e.isAlive() && e.getType().is(tag) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntityMimicOctopus.this.isPassenger() || EntityMimicOctopus.this.isVehicle() || EntityMimicOctopus.this.isTame()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityMimicOctopus.this.level.getGameTime() % 10;
                if (EntityMimicOctopus.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityMimicOctopus.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityMimicOctopus.this.level.getEntitiesOfClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null && !EntityMimicOctopus.this.isTame() && EntityMimicOctopus.this.distanceTo(targetEntity) < 20;
        }

        public void stop() {
            flightTarget = null;
            this.targetEntity = null;
            EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
            EntityMimicOctopus.this.setMimickedBlock(null);
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (!EntityMimicOctopus.this.isActiveCamo()) {
                EntityMimicOctopus.this.mimicEnvironment();
            }
            if (flightTarget != null) {
                EntityMimicOctopus.this.getNavigation().moveTo(flightTarget.x, flightTarget.y, flightTarget.z, 1.2F);
                if (cooldown == 0 && EntityMimicOctopus.this.isTargetBlocked(flightTarget)) {
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (flightTarget == null || flightTarget != null && EntityMimicOctopus.this.distanceToSqr(flightTarget) < 6) {
                    Vector3d vec;
                    vec = RandomPositionGenerator.getPosAvoid(EntityMimicOctopus.this, 16, 7, targetEntity.position());
                    if (vec != null) {
                        flightTarget = vec;
                    }
                }
                if (EntityMimicOctopus.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 10;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntityMimicOctopus.this.getX(), EntityMimicOctopus.this.getY() + 0.5, EntityMimicOctopus.this.getZ());
            AxisAlignedBB aabb = new AxisAlignedBB(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
            return aabb.move(renderCenter);
        }
    }

    public class EntitySorter implements Comparator<Entity> {
        private final Entity theEntity;

        public EntitySorter(Entity theEntityIn) {
            this.theEntity = theEntityIn;
        }

        public int compare(Entity p_compare_1_, Entity p_compare_2_) {
            double d0 = this.theEntity.distanceToSqr(p_compare_1_);
            double d1 = this.theEntity.distanceToSqr(p_compare_2_);
            return d0 < d1 ? -1 : (d0 > d1 ? 1 : 0);
        }
    }

    public class FollowOwner extends Goal {
        private final EntityMimicOctopus tameable;
        private final IWorldReader world;
        private final double followSpeed;
        private final float maxDist;
        private final float minDist;
        private final boolean teleportToLeaves;
        private LivingEntity owner;
        private int timeToRecalcPath;
        private float oldWaterCost;

        public FollowOwner(EntityMimicOctopus p_i225711_1_, double p_i225711_2_, float p_i225711_4_, float p_i225711_5_, boolean p_i225711_6_) {
            this.tameable = p_i225711_1_;
            this.world = p_i225711_1_.level;
            this.followSpeed = p_i225711_2_;
            this.minDist = p_i225711_4_;
            this.maxDist = p_i225711_5_;
            this.teleportToLeaves = p_i225711_6_;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
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
            } else if (this.tameable.getTarget() != null && this.tameable.getTarget().isAlive()) {
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
            } else if (this.tameable.getTarget() != null && this.tameable.getTarget().isAlive()) {
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
            if (world.getFluidState(p_226329_1_).is(FluidTags.WATER) || !world.getFluidState(p_226329_1_).is(FluidTags.WATER) && world.getFluidState(p_226329_1_.below()).is(FluidTags.WATER)) {
                return true;
            }
            if (lvt_2_1_ != PathNodeType.WALKABLE || tameable.getMoistness() < 2000) {
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

    private class AIMimicNearbyMobs extends Goal {
        protected final EntitySorter theNearestAttackableTargetSorter;
        protected final Predicate<? super Entity> targetEntitySelector;
        protected int executionChance = 30;
        protected boolean mustUpdate;
        private Entity targetEntity;
        private Vector3d flightTarget = null;
        private int cooldown = 0;

        AIMimicNearbyMobs() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
            this.theNearestAttackableTargetSorter = new EntitySorter(EntityMimicOctopus.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e.isAlive() && (e instanceof CreeperEntity || e instanceof GuardianEntity || e instanceof PufferfishEntity);
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntityMimicOctopus.this.isPassenger() || EntityMimicOctopus.this.isVehicle() || EntityMimicOctopus.this.getMimicState() != MimicState.OVERLAY || mimicCooldown > 0) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntityMimicOctopus.this.level.getGameTime() % 10;
                if (EntityMimicOctopus.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntityMimicOctopus.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntityMimicOctopus.this.level.getEntitiesOfClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null && EntityMimicOctopus.this.distanceTo(targetEntity) < 10 && EntityMimicOctopus.this.getMimicState() == MimicState.OVERLAY;
        }

        public void stop() {
            EntityMimicOctopus.this.getNavigation().stop();
            flightTarget = null;
            this.targetEntity = null;
        }

        @Override
        public void tick() {
            if (cooldown > 0) {
                cooldown--;
            }
            if (targetEntity != null) {
                EntityMimicOctopus.this.getNavigation().moveTo(targetEntity, 1.2F);
                if (EntityMimicOctopus.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                    EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
                    EntityMimicOctopus.this.setMimickedBlock(null);
                } else if (EntityMimicOctopus.this.distanceTo(targetEntity) < 5.0F && EntityMimicOctopus.this.canSee(targetEntity)) {
                    int i = 1200;
                    EntityMimicOctopus.this.stopMimicCooldown = i;
                    EntityMimicOctopus.this.camoCooldown = i + 40;
                    EntityMimicOctopus.this.mimicCooldown = 40;
                    if (targetEntity instanceof CreeperEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.CREEPER);
                    } else if (targetEntity instanceof GuardianEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.GUARDIAN);
                    } else if (targetEntity instanceof PufferfishEntity) {
                        EntityMimicOctopus.this.setMimicState(MimicState.PUFFERFISH);
                    } else {
                        EntityMimicOctopus.this.setMimicState(MimicState.OVERLAY);
                        EntityMimicOctopus.this.setMimickedBlock(null);
                    }
                    stop();
                }

            }
        }

        protected double getTargetDistance() {
            return 10;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntityMimicOctopus.this.getX(), EntityMimicOctopus.this.getY() + 0.5, EntityMimicOctopus.this.getZ());
            AxisAlignedBB aabb = new AxisAlignedBB(-targetDistance, -targetDistance, -targetDistance, targetDistance, targetDistance, targetDistance);
            return aabb.move(renderCenter);
        }
    }

    private class AIAttack extends Goal {
        private int executionCooldown = 0;
        private int scareMobTime = 0;
        private Vector3d fleePosition = null;

        public AIAttack() {
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if (executionCooldown > 0) {
                EntityMimicOctopus.this.entityData.set(UPGRADED_LASER_ENTITY_ID, -1);
                executionCooldown--;
            }
            if (EntityMimicOctopus.this.isStopChange() && EntityMimicOctopus.this.getMimicState() == MimicState.OVERLAY) {
                return false;
            }
            return executionCooldown == 0 && EntityMimicOctopus.this.isTame() && EntityMimicOctopus.this.getTarget() != null && EntityMimicOctopus.this.getTarget().isAlive();
        }

        public void stop() {
            fleePosition = null;
            scareMobTime = 0;
            executionCooldown = 100 + random.nextInt(200);
            if (EntityMimicOctopus.this.isUpgraded()) {
                executionCooldown = 30;
            } else {
                EntityMimicOctopus.this.setLastHurtByMob(null);
                EntityMimicOctopus.this.setTarget(null);
            }
            if (EntityMimicOctopus.this.stopMimicCooldown <= 0) {
                EntityMimicOctopus.this.mimicEnvironment();
            }
            EntityMimicOctopus.this.entityData.set(UPGRADED_LASER_ENTITY_ID, -1);
        }

        public Vector3d generateFleePosition(LivingEntity fleer) {
            for (int i = 0; i < 15; i++) {
                BlockPos pos = fleer.blockPosition().offset(random.nextInt(32) - 16, random.nextInt(16), random.nextInt(32) - 16);
                while (fleer.level.isEmptyBlock(pos) && pos.getY() > 1) {
                    pos = pos.below();
                }
                if (fleer instanceof CreatureEntity) {
                    if (((CreatureEntity) fleer).getWalkTargetValue(pos) >= 0.0F) {
                        return Vector3d.atCenterOf(pos);
                    }
                } else {
                    return Vector3d.atCenterOf(pos);
                }
            }
            return null;
        }

        public void tick() {
            LivingEntity target = EntityMimicOctopus.this.getTarget();
            if (target != null) {
                if (scareMobTime > 0) {
                    if (fleePosition == null || target.distanceToSqr(fleePosition) < target.getBbWidth() * target.getBbWidth() * 2) {
                        fleePosition = generateFleePosition(target);
                    }
                    if (target instanceof MobEntity) {
                        if (fleePosition != null) {
                            ((MobEntity) target).getNavigation().moveTo(fleePosition.x, fleePosition.y, fleePosition.z, 1.5F);
                            ((MobEntity) target).getMoveControl().setWantedPosition(fleePosition.x, fleePosition.y, fleePosition.z, 1.5F);
                            ((MobEntity) target).setTarget(null);
                        }
                    }
                    camoCooldown = Math.max(camoCooldown, 20);
                    stopMimicCooldown = Math.max(stopMimicCooldown, 20);
                    scareMobTime--;
                    if (scareMobTime == 0) {
                        stop();
                        return;
                    }
                }
                double dist = EntityMimicOctopus.this.distanceTo(target);
                boolean move = true;
                if (dist < 7F && EntityMimicOctopus.this.canSee(target) && EntityMimicOctopus.this.getMimicState() == MimicState.GUARDIAN && EntityMimicOctopus.this.isUpgraded()) {
                    EntityMimicOctopus.this.entityData.set(UPGRADED_LASER_ENTITY_ID, target.getId());
                    move = false;
                }
                if (dist < 3) {
                    EntityMimicOctopus.this.entityData.set(LAST_SCARED_MOB_ID, target.getId());
                    if (move) {
                        move = EntityMimicOctopus.this.isUpgraded() && dist > 2;
                    }
                    EntityMimicOctopus.this.getNavigation().stop();
                    if (!EntityMimicOctopus.this.isStopChange()) {
                        EntityMimicOctopus.this.setMimickedBlock(null);
                        MimicState prev = EntityMimicOctopus.this.getMimicState();
                        if (EntityMimicOctopus.this.isInWaterOrBubble()) {
                            if (prev != MimicState.GUARDIAN && prev != MimicState.PUFFERFISH) {
                                if (random.nextBoolean()) {
                                    EntityMimicOctopus.this.setMimicState(MimicState.GUARDIAN);
                                } else {
                                    EntityMimicOctopus.this.setMimicState(MimicState.PUFFERFISH);
                                }
                            }
                        } else {
                            EntityMimicOctopus.this.setMimicState(MimicState.CREEPER);
                        }
                    }
                    if (EntityMimicOctopus.this.getMimicState() != MimicState.OVERLAY) {
                        EntityMimicOctopus.this.mimicCooldown = 40;
                        EntityMimicOctopus.this.stopMimicCooldown = Math.max(EntityMimicOctopus.this.stopMimicCooldown, 60);
                    }
                    if (EntityMimicOctopus.this.isUpgraded() && EntityMimicOctopus.this.transProgress >= 5.0F) {
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.PUFFERFISH) {
                            if (EntityMimicOctopus.this.getBoundingBox().expandTowards(2, 1.3, 2).intersects(target.getBoundingBox())) {
                                target.hurt(DamageSource.mobAttack(EntityMimicOctopus.this), 4);
                                target.addEffect(new EffectInstance(Effects.POISON, 400, 2));
                            }
                        }
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.GUARDIAN) {
                            if (EntityMimicOctopus.this.getBoundingBox().expandTowards(1, 1, 1).intersects(target.getBoundingBox())) {
                                target.hurt(DamageSource.mobAttack(EntityMimicOctopus.this), 1);
                            }
                            EntityMimicOctopus.this.entityData.set(UPGRADED_LASER_ENTITY_ID, target.getId());
                        }
                        if (EntityMimicOctopus.this.getMimicState() == MimicState.CREEPER) {
                            EntityMimicOctopus.this.creeperExplode();
                            EntityMimicOctopus.this.level.broadcastEntityEvent(EntityMimicOctopus.this, (byte) 69);
                            executionCooldown = 300;
                        }
                    }
                    if (scareMobTime == 0) {
                        EntityMimicOctopus.this.level.broadcastEntityEvent(EntityMimicOctopus.this, (byte) 68);
                        scareMobTime = 60 + random.nextInt(60);
                    }
                }
                if (move) {
                    EntityMimicOctopus.this.lookAt(target, 30, 30);
                    EntityMimicOctopus.this.getNavigation().moveTo(target, 1.2F);
                }
            }
        }
    }
}
