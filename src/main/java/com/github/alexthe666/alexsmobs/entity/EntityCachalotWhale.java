package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.DolphinLookController;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.pathfinding.PathType;
import net.minecraft.pathfinding.SwimmerPathNavigator;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityCachalotWhale extends AnimalEntity {

    private static final EntityPredicate REWARD_PLAYER_PREDICATE = (new EntityPredicate()).range(50.0D).allowInvulnerable().allowSameTeam().allowNonAttackable();
    private static final DataParameter<Boolean> CHARGING = EntityDataManager.defineId(EntityCachalotWhale.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SLEEPING = EntityDataManager.defineId(EntityCachalotWhale.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> BEACHED = EntityDataManager.defineId(EntityCachalotWhale.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> ALBINO = EntityDataManager.defineId(EntityCachalotWhale.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DESPAWN_BEACH = EntityDataManager.defineId(EntityCachalotWhale.class, DataSerializers.BOOLEAN);
    public final double[][] ringBuffer = new double[64][3];
    public final EntityCachalotPart headPart;
    public final EntityCachalotPart bodyFrontPart;
    public final EntityCachalotPart bodyPart;
    public final EntityCachalotPart tail1Part;
    public final EntityCachalotPart tail2Part;
    public final EntityCachalotPart tail3Part;
    public final EntityCachalotPart[] whaleParts;
    public int ringBufferIndex = -1;
    public float prevChargingProgress;
    public float chargeProgress;
    public float prevSleepProgress;
    public float sleepProgress;
    public float prevBeachedProgress;
    public float beachedProgress;
    private boolean receivedEcho = false;
    private boolean waitForEchoFlag = true;
    private int echoTimer = 0;
    private boolean prevEyesInWater = false;
    private int spoutTimer = 0;
    private int chargeCooldown = 0;
    private float whaleSpeedMod = 1F;
    private int rewardTime = 0;
    private PlayerEntity rewardPlayer;
    private int blockBreakCounter;
    private int despawnDelay = 47999;
    private int ambergrisDrops = 0;
    private boolean hasAlbinoAttribute = false;
    private int echoSoundCooldown = 0;

    public EntityCachalotWhale(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.moveControl = new AnimalSwimMoveControllerSink(this, 1, 1, 3);
        this.lookControl = new DolphinLookController(this, 4);
        this.headPart = new EntityCachalotPart(this, 3.0F, 3.5F);
        this.bodyFrontPart = new EntityCachalotPart(this, 4.0F, 4.0F);
        this.bodyPart = new EntityCachalotPart(this, 5.0F, 4.0F);
        this.tail1Part = new EntityCachalotPart(this, 4.0F, 3.0F);
        this.tail2Part = new EntityCachalotPart(this, 3.0F, 2.0F);
        this.tail3Part = new EntityCachalotPart(this, 3.0F, 0.7F);
        this.whaleParts = new EntityCachalotPart[]{this.headPart, this.bodyFrontPart, this.bodyPart, this.tail1Part, this.tail2Part, this.tail3Part};
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 160.0D).add(Attributes.KNOCKBACK_RESISTANCE, 1.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.MOVEMENT_SPEED, 1.2F).add(Attributes.ATTACK_DAMAGE, 30F);
    }

    public static <T extends MobEntity> boolean canCachalotWhaleSpawn(EntityType<T> entityType, IServerWorld iServerWorld, SpawnReason reason, BlockPos pos, Random random) {
        return iServerWorld.getFluidState(pos).is(FluidTags.WATER);
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isSleeping() && !this.isCharging() && !this.isDespawnBeach() && !isAlbino();
    }

    private boolean canDespawn() {
        return isDespawnBeach();
    }

    private void tryDespawn() {
        if (this.canDespawn()) {
            this.despawnDelay = this.despawnDelay - 1;
            if (this.despawnDelay <= 0) {
                this.dropLeash(true, false);
                this.remove();
            }
        }
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.CACHALOT_WHALE_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.CACHALOT_WHALE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.CACHALOT_WHALE_HURT;
    }

    public void scaleParts() {
        for (EntityCachalotPart parts : whaleParts) {
            float prev = parts.scale;
            parts.scale = this.isBaby() ? 0.5F : 1F;
            if (prev != parts.scale) {
                parts.refreshDimensions();
            }
        }
    }

    public boolean isPickable() {
        return false;
    }

    public void pushEntities() {
    }

    public ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        return super.mobInteract(p_230254_1_, p_230254_2_);
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Albino", this.isAlbino());
        compound.putBoolean("Beached", this.isBeached());
        compound.putBoolean("BeachedDespawnFlag", this.isDespawnBeach());
        compound.putInt("DespawnDelay", this.despawnDelay);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setAlbino(compound.getBoolean("Albino"));
        this.setBeached(compound.getBoolean("Beached"));
        this.setDespawnBeach(compound.getBoolean("BeachedDespawnFlag"));
        if (compound.contains("DespawnDelay", 99)) {
            this.despawnDelay = compound.getInt("DespawnDelay");
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CHARGING, Boolean.valueOf(false));
        this.entityData.define(SLEEPING, Boolean.valueOf(false));
        this.entityData.define(BEACHED, Boolean.valueOf(false));
        this.entityData.define(ALBINO, Boolean.valueOf(false));
        this.entityData.define(DESPAWN_BEACH, Boolean.valueOf(false));
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new AIBreathe());
        this.goalSelector.addGoal(1, new FindWaterGoal(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(3, new AnimalAIFollowParentRanged(this, 1.1F, 32, 10));
        this.goalSelector.addGoal(4, new AnimalAIRandomSwimming(this, 0.6D, 10, 24, true) {
            public boolean canUse() {
                return !EntityCachalotWhale.this.isSleeping() && !EntityCachalotWhale.this.isBeached() && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 20.0F));
        this.goalSelector.addGoal(7, new FollowBoatGoal(this));
        this.targetSelector.addGoal(1, (new AnimalAIHurtByTargetNotBaby(this).setAlertOthers()));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, LivingEntity.class, 30, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.CACHALOT_WHALE_TARGETS))) {
            public boolean canUse() {
                return !EntityCachalotWhale.this.isSleeping() && !EntityCachalotWhale.this.isBeached() && super.canUse();
            }
        });
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new SwimmerPathNavigator(this, worldIn);
    }

    public void customServerAiStep() {
        super.customServerAiStep();
        breakBlock();
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0) {
            --this.blockBreakCounter;
            return;
        }
        boolean flag = false;
        ResourceLocation breakables = this.isCharging() && this.getTarget() != null ? AMTagRegistry.CACHALOT_WHALE_BREAKABLES : AMTagRegistry.ORCA_BREAKABLES;
        if (!level.isClientSide && this.blockBreakCounter == 0 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, this)) {
            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
                        BlockPos pos = new BlockPos(a, b, c);
                        BlockState state = level.getBlockState(pos);
                        FluidState fluidState = level.getFluidState(pos);
                        Block block = state.getBlock();
                        if (!state.isAir() && !state.getShape(level, pos).isEmpty() && BlockTags.getAllTags().getTag(breakables).contains(state.getBlock()) && fluidState.isEmpty()) {
                            if (block != Blocks.AIR) {
                                this.setDeltaMovement(this.getDeltaMovement().multiply(0.6F, 1, 0.6F));
                                flag = true;
                                level.destroyBlock(pos, true);
                                if (state.getBlock().is(BlockTags.ICE)) {
                                    level.setBlockAndUpdate(pos, Blocks.WATER.defaultBlockState());
                                }
                            }
                        }
                    }
                }
            }
        }
        if (flag) {
            blockBreakCounter = this.isCharging() && this.getTarget() != null ? 2 : 20;
        }
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));

        } else {
            super.travel(travelVector);
        }
    }

    private void spawnSpoutParticles() {
        if (this.isAlive()) {
            for (int j = 0; j < 5 + random.nextInt(4); ++j) {
                float radius = this.headPart.getBbWidth() * 0.5F;
                float angle = (0.01745329251F * this.yBodyRot);
                double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin((float) (Math.PI + angle)) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().x * 2F;
                double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().z * 2F;
                double motX = this.random.nextGaussian();
                double motZ = this.random.nextGaussian();
                this.level.addParticle(AMParticleRegistry.WHALE_SPLASH, this.headPart.getX() + extraX, this.headPart.getY() + this.headPart.getBbHeight(), this.headPart.getZ() + extraZ, motX * 0.1F + this.getDeltaMovement().x, 2F, motZ * 0.1F + this.getDeltaMovement().z);
            }
        }
    }

    public boolean isCharging() {
        return this.entityData.get(CHARGING).booleanValue();
    }

    public void setCharging(boolean charging) {
        this.entityData.set(CHARGING, Boolean.valueOf(charging));
    }

    public boolean isSleeping() {
        return this.entityData.get(SLEEPING).booleanValue();
    }

    public void setSleeping(boolean charging) {
        this.entityData.set(SLEEPING, Boolean.valueOf(charging));
    }

    public boolean isBeached() {
        return this.entityData.get(BEACHED).booleanValue();
    }

    public void setBeached(boolean charging) {
        this.entityData.set(BEACHED, Boolean.valueOf(charging));
    }

    public boolean isAlbino() {
        return this.entityData.get(ALBINO).booleanValue();
    }

    public void setAlbino(boolean albino) {
        boolean prev = isAlbino();
        if (!prev && albino) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(230.0D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(45.0D);
            this.setHealth(160.0F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(160.0D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(30.0D);
        }
        this.entityData.set(ALBINO, Boolean.valueOf(albino));
    }

    public boolean isDespawnBeach() {
        return this.entityData.get(DESPAWN_BEACH).booleanValue();
    }

    public void setDespawnBeach(boolean despawn) {
        this.entityData.set(DESPAWN_BEACH, Boolean.valueOf(despawn));
    }

    protected float getSoundVolume() {
        return this.isSilent() ? 0 : (float) AMConfig.cachalotVolume;
    }

    public void aiStep() {
        super.aiStep();
        scaleParts();
        if (echoSoundCooldown > 0) {
            echoSoundCooldown--;
        }
        if (this.isSleeping()) {
            this.getNavigation().stop();
            this.xRot = -90;
            this.whaleSpeedMod = 0;
            if (this.isEyeInFluid(FluidTags.WATER) && this.getAirSupply() < 200) {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.06, 0));
            } else {
                BlockPos waterPos = this.blockPosition();
                while (level.getFluidState(waterPos).is(FluidTags.WATER) && waterPos.getY() < 255) {
                    waterPos = waterPos.above();
                }
                if (waterPos.getY() - this.getY() < (isBaby() ? 7 : 12)) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, -0.06, 0));
                }
                if (random.nextInt(100) == 0) {
                    this.setDeltaMovement(this.getDeltaMovement().add(0, random.nextGaussian() * 0.06, 0));
                }
            }
        } else {
            if (this.whaleSpeedMod == 0) {
                this.whaleSpeedMod = 1;
            }
        }
        float rPitch = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
        this.xRot = MathHelper.clamp(rPitch, -90, 90);
        if (this.isOnGround() && !this.isInWaterOrBubble()) {
            this.setBeached(true);
            this.xRot = 0;
            this.setSleeping(false);
        }
        if (this.isBeached()) {
            this.whaleSpeedMod = 0;
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.5, 0.5, 0.5));
            if (this.isEyeInFluid(FluidTags.WATER)) {
                PlayerEntity entity = this.level.getNearestPlayer(REWARD_PLAYER_PREDICATE, this);
                if (this.getLastHurtByMob() != entity) {
                    rewardTime = 15;
                    rewardPlayer = entity;
                }
                this.despawnDelay = 47999;
                this.setBeached(false);
            }
        }
        if (!this.isBeached() && rewardTime > 0) {
            float dif = 0;
            if (rewardPlayer != null) {
                double d0 = rewardPlayer.getX() - this.getX();
                double d1 = rewardPlayer.getEyeY() - this.getEyeY();
                double d2 = rewardPlayer.getZ() - this.getZ();
                double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                float targetYaw = (float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                float targetPitch = (float) (-(MathHelper.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
                this.yRot = (this.yRot + MathHelper.clamp(targetYaw - this.yRot, -2, 2));
                this.xRot = (this.xRot + MathHelper.clamp(targetPitch - this.xRot, -2, 2));
                this.yBodyRot = yRot;
                dif = Math.abs(MathHelper.wrapDegrees(targetYaw) - MathHelper.wrapDegrees(this.yRot));
            }
            if (dif < 5) {
                if (rewardTime % 5 == 0 && ambergrisDrops < 2 + random.nextInt(1) && this.isDespawnBeach()) {
                    ambergrisDrops++;
                    if (!level.isClientSide) {
                        Vector3d vec = this.getMouthVec();
                        ItemEntity itementity = new ItemEntity(this.level, vec.x, vec.y, vec.z, new ItemStack(AMItemRegistry.AMBERGRIS));
                        itementity.setDefaultPickUpDelay();
                        level.addFreshEntity(itementity);
                    }
                }
                this.rewardTime--;
            }
            if (rewardTime <= 2) {
                this.setDespawnBeach(false);
                this.setCharging(false);
                this.whaleSpeedMod = 1F;
            } else {
                this.setCharging(true);
                this.whaleSpeedMod = 0.2F;
            }
        }

        prevChargingProgress = chargeProgress;
        prevSleepProgress = sleepProgress;
        prevBeachedProgress = beachedProgress;
        if (isCharging() && this.chargeProgress < 10F) {
            this.chargeProgress++;
        }
        if (!isCharging() && this.chargeProgress > 0F) {
            this.chargeProgress--;
        }
        if (isSleeping() && this.sleepProgress < 10F) {
            this.sleepProgress++;
        }
        if (!isSleeping() && this.sleepProgress > 0F) {
            this.sleepProgress--;
        }
        if (isBeached() && this.beachedProgress < 10F) {
            this.beachedProgress++;
        }
        if (!isBeached() && this.beachedProgress > 0F) {
            this.beachedProgress--;
        }
        this.yHeadRot = this.yRot;
        this.yBodyRot = this.yRot;

        if (!this.isNoAi()) {
            if (this.ringBufferIndex < 0) {
                for (int i = 0; i < this.ringBuffer.length; ++i) {
                    this.ringBuffer[i][0] = this.yRot;
                    this.ringBuffer[i][1] = this.getY();
                }
            }
            this.ringBufferIndex++;
            if (this.ringBufferIndex == this.ringBuffer.length) {
                this.ringBufferIndex = 0;
            }
            this.ringBuffer[this.ringBufferIndex][0] = this.yRot;
            this.ringBuffer[ringBufferIndex][1] = this.getY();
            Vector3d[] avector3d = new Vector3d[this.whaleParts.length];

            for (int j = 0; j < this.whaleParts.length; ++j) {
                this.whaleParts[j].collideWithNearbyEntities();
                avector3d[j] = new Vector3d(this.whaleParts[j].getX(), this.whaleParts[j].getY(), this.whaleParts[j].getZ());
            }
            float f4 = MathHelper.sin(this.yRot * ((float) Math.PI / 180F) - 0 * 0.01F);
            float f19 = MathHelper.cos(this.yRot * ((float) Math.PI / 180F) - 0 * 0.01F);
            float f15 = (float) (this.getMovementOffsets(5, 1.0F)[1] - this.getMovementOffsets(10, 1.0F)[1]) * 10.0F * ((float) Math.PI / 180F);
            float f16 = MathHelper.cos(f15);
            float f2 = MathHelper.sin(f15);
            float f17 = this.yRot * ((float) Math.PI / 180F);
            float pitch = this.xRot * ((float) Math.PI / 180F);
            float f3 = MathHelper.sin(f17) * (1 - Math.abs(this.xRot / 90F));
            float f18 = MathHelper.cos(f17) * (1 - Math.abs(this.xRot / 90F));

            this.setPartPosition(this.bodyPart, f3 * 0.5F, -pitch * 0.5F, -f18 * 0.5F);
            this.setPartPosition(this.bodyFrontPart, (f3) * -3.5F, -pitch * 3F, (f18) * 3.5F);
            this.setPartPosition(this.headPart, f3 * -7F, -pitch * 5F, -f18 * -7F);
            double[] adouble = this.getMovementOffsets(5, 1.0F);

            for (int k = 0; k < 3; ++k) {
                EntityCachalotPart enderdragonpartentity = null;
                if (k == 0) {
                    enderdragonpartentity = this.tail1Part;
                }
                if (k == 1) {
                    enderdragonpartentity = this.tail2Part;
                }
                if (k == 2) {
                    enderdragonpartentity = this.tail3Part;
                }

                double[] adouble1 = this.getMovementOffsets(15 + k * 5, 1.0F);
                float f7 = this.yRot * ((float) Math.PI / 180F) + (float) MathHelper.wrapDegrees(adouble1[0] - adouble[0]) * ((float) Math.PI / 180F);
                float f20 = MathHelper.sin(f7) * (1 - Math.abs(this.xRot / 90F));
                float f21 = MathHelper.cos(f7) * (1 - Math.abs(this.xRot / 90F));
                float f22 = k == 2 ? -3.6F : -3.6F;
                float f23 = (float) (k + 1) * f22 - 2F;
                this.setPartPosition(enderdragonpartentity, -(f3 * 0.5F + f20 * f23) * f16, pitch * 1.5F * (k + 1), (f18 * 0.5F + f21 * f23) * f16);
            }

            for (int l = 0; l < this.whaleParts.length; ++l) {
                this.whaleParts[l].xo = avector3d[l].x;
                this.whaleParts[l].yo = avector3d[l].y;
                this.whaleParts[l].zo = avector3d[l].z;
                this.whaleParts[l].xOld = avector3d[l].x;
                this.whaleParts[l].yOld = avector3d[l].y;
                this.whaleParts[l].zOld = avector3d[l].z;
            }
        }
        if (!level.isClientSide) {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) {
                whaleSpeedMod = this.isSleeping() ? 0 : 1;
                this.setCharging(false);
            } else if (!isBeached() && !isSleeping()) {
                this.lookAt(target, 360, 360);
                waitForEchoFlag = this.getLastHurtByMob() == null || !this.getLastHurtByMob().is(target);
                if (target instanceof PlayerEntity || !target.isInWaterOrBubble()) {
                    waitForEchoFlag = false;
                }
                if (waitForEchoFlag && !receivedEcho) {
                    this.setCharging(false);
                    whaleSpeedMod = 0.25F;
                    if (echoTimer % 10 == 0) {
                        if (echoTimer % 40 == 0) {
                            this.playSound(AMSoundRegistry.CACHALOT_WHALE_CLICK, this.getSoundVolume(), this.getVoicePitch());
                        }
                        EntityCachalotEcho echo = new EntityCachalotEcho(this.level, this);
                        float radius = this.headPart.getBbWidth() * 0.5F;
                        float angle = (0.01745329251F * this.yBodyRot);
                        double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin((float) (Math.PI + angle)) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().x * 2F;
                        double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().z * 2F;
                        double x = this.headPart.getX() + extraX;
                        double y = this.headPart.getY() + this.headPart.getBbHeight() * 0.5D;
                        double z = this.headPart.getZ() + extraZ;
                        double d0 = target.getX() - x;
                        double d1 = target.getY(0.1D) - y;
                        double d2 = target.getZ() - z;
                        echo.setPos(x, y, z);
                        echo.shoot(d0, d1, d2, 1F, 0.0F);
                        this.level.addFreshEntity(echo);
                    }
                    echoTimer++;
                }
                if (!waitForEchoFlag || receivedEcho) {
                    double d0 = target.getX() - this.getX();
                    double d1 = target.getEyeY() - this.getEyeY();
                    double d2 = target.getZ() - this.getZ();
                    double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
                    float targetYaw = (float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                    float targetPitch = (float) (-(MathHelper.atan2(d1, d3) * (double) (180F / (float) Math.PI)));
                    this.xRot = (this.xRot + MathHelper.clamp(targetPitch - this.xRot, -2, 2));
                    if (d0 * d0 + d2 * d2 >= 4) {
                        this.yRot = (this.yRot + MathHelper.clamp(targetYaw - this.yRot, -2, 2));
                        this.yBodyRot = yRot;
                    }
                    float dif = Math.abs(MathHelper.wrapDegrees(targetYaw) - MathHelper.wrapDegrees(this.yRot));
                    if (chargeCooldown <= 0 && dif < 4) {
                        this.setCharging(true);
                        whaleSpeedMod = 1.5F;
                        if (d0 * d0 + d2 * d2 < 4) {
                            this.yRot = yRotO;
                            this.yBodyRot = yRotO;
                            this.setDeltaMovement(this.getDeltaMovement().multiply(0.8, 1, 0.8));
                        } else {
                            this.getMoveControl().setWantedPosition(target.getX(), target.getY(), target.getZ(), 1.0D);
                        }
                        if (this.isCharging()) {
                            if (this.distanceTo(target) < this.getBbWidth() && chargeProgress > 4) {
                                target.hurt(DamageSource.mobAttack(this), (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                                this.setCharging(false);
                                if (target.getVehicle() instanceof BoatEntity) {
                                    BoatEntity boat = (BoatEntity) target.getVehicle();
                                    for (int i = 0; i < 3; ++i) {
                                        this.spawnAtLocation(boat.getBoatType().getPlanks());
                                    }
                                    for (int j = 0; j < 2; ++j) {
                                        this.spawnAtLocation(Items.STICK);
                                    }
                                    target.removeVehicle();
                                    boat.hurt(DamageSource.mobAttack(this), 1000);
                                    boat.remove();
                                }
                                chargeCooldown = target instanceof PlayerEntity ? 30 : 100;
                                if (random.nextInt(10) == 0) {
                                    if (!level.isClientSide) {
                                        Vector3d vec = this.getMouthVec();
                                        ItemEntity itementity = new ItemEntity(this.level, vec.x, vec.y, vec.z, new ItemStack(AMItemRegistry.CACHALOT_WHALE_TOOTH));
                                        itementity.setDefaultPickUpDelay();
                                        level.addFreshEntity(itementity);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (chargeCooldown > 0) {
                chargeCooldown--;
            }
            if (spoutTimer > 0) {
                level.broadcastEntityEvent(this, (byte) 67);
                spoutTimer--;
                this.xRot = 0;
                this.setDeltaMovement(this.getDeltaMovement().multiply(0, 0, 0));
            }
            if (isSleepTime() && !this.isSleeping() && this.isInWaterOrBubble() && this.getTarget() == null) {
                this.setSleeping(true);
            }
            if (this.isSleeping() && (!isSleepTime() || this.getTarget() != null)) {
                this.setSleeping(false);
            }
        }

        if (this.isAlive() && isCharging()) {
            for (Entity entity : this.level.getEntitiesOfClass(LivingEntity.class, this.headPart.getBoundingBox().inflate(1.0D), null)) {
                if (!isAlliedTo(entity) && !(entity instanceof EntityCachalotPart) && entity != this) {
                    launch(entity, true);
                }
            }
        }
        if (this.isInWater() && !this.isEyeInFluid(FluidTags.WATER) && this.getAirSupply() > 140) {
            this.setDeltaMovement(this.getDeltaMovement().add(0, -0.06, 0));
        }
        if (!this.level.isClientSide) {
            this.tryDespawn();
        }
        prevEyesInWater = this.isEyeInFluid(FluidTags.WATER);
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround() || e.isInWater()) {
            double d0 = e.getX() - this.getX();
            double d1 = e.getZ() - this.getZ();
            double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            float f = huge ? 2F : 0.5F;
            e.push(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    private boolean isSleepTime() {
        long time = level.getDayTime();
        return time > 18000 && time < 22812 && this.isInWaterOrBubble();
    }

    public Vector3d getReturnEchoVector() {
        float radius = this.headPart.getBbWidth() * 0.5F;
        float angle = (0.01745329251F * this.yBodyRot);
        double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin((float) (Math.PI + angle)) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().x * 2F;
        double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().z * 2F;
        double x = this.headPart.getX() + extraX;
        double y = this.headPart.getY() + this.headPart.getBbHeight() * 0.5D;
        double z = this.headPart.getZ() + extraZ;
        return new Vector3d(x, y, z);
    }

    public Vector3d getMouthVec() {
        float radius = this.headPart.getBbWidth() * 0.5F;
        float angle = (0.01745329251F * this.yBodyRot);
        double extraX = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.sin((float) (Math.PI + angle)) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().x * 2F;
        double extraZ = (radius * (1F + random.nextFloat() * 0.13F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().z * 2F;
        double x = this.headPart.getX() + extraX;
        double y = this.headPart.getY() + 0.25D;
        double z = this.headPart.getZ() + extraZ;
        return new Vector3d(x, y, z);
    }

    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        LivingEntity prev = this.getTarget();
        if (prev != entitylivingbaseIn && entitylivingbaseIn != null) {
            receivedEcho = false;
        }
        super.setTarget(entitylivingbaseIn);
    }

    public double[] getMovementOffsets(int p_70974_1_, float partialTicks) {
        if (this.isDeadOrDying()) {
            partialTicks = 0.0F;
        }

        partialTicks = 1.0F - partialTicks;
        int i = this.ringBufferIndex - p_70974_1_ & 63;
        int j = this.ringBufferIndex - p_70974_1_ - 1 & 63;
        double[] adouble = new double[3];
        double d0 = this.ringBuffer[i][0];
        double d1 = this.ringBuffer[j][0] - d0;
        adouble[0] = d0 + d1 * (double) partialTicks;
        d0 = this.ringBuffer[i][1];
        d1 = this.ringBuffer[j][1] - d0;
        adouble[1] = d0 + d1 * (double) partialTicks;
        adouble[2] = MathHelper.lerp(partialTicks, this.ringBuffer[i][2], this.ringBuffer[j][2]);
        return adouble;
    }

    public void push(Entity entityIn) {

    }

    private void setPartPosition(EntityCachalotPart part, double offsetX, double offsetY, double offsetZ) {
        part.setPos(this.getX() + offsetX * part.scale, this.getY() + offsetY * part.scale, this.getZ() + offsetZ * part.scale);
    }

    @Override
    public boolean isMultipartEntity() {
        return true;
    }

    @Override
    public net.minecraftforge.entity.PartEntity<?>[] getParts() {
        return this.whaleParts;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        EntityCachalotWhale whale = AMEntityRegistry.CACHALOT_WHALE.create(serverWorld);
        whale.setAlbino(this.isAlbino());
        return whale;
    }

    public boolean attackEntityPartFrom(EntityCachalotPart entityCachalotPart, DamageSource source, float amount) {
        return this.hurt(source, amount);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setAirSupply(this.getMaxAirSupply());
        this.xRot = 0.0F;
        if (spawnDataIn == null) {
            spawnDataIn = new AgeableEntity.AgeableData(0.75F);
        }
        this.setAlbino(random.nextInt(100) == 0);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public boolean canBreatheUnderwater() {
        return false;
    }

    public void baseTick() {
        int i = this.getAirSupply();
        super.baseTick();
        this.updateAir(i);
    }

    public boolean isPushedByFluid() {
        return false;
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.WATER;
    }

    public boolean checkSpawnObstruction(IWorldReader worldIn) {
        return worldIn.isUnobstructed(this);
    }

    protected void updateAir(int p_209207_1_) {
    }

    public int getMaxAirSupply() {
        return 4000;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 67) {
            spawnSpoutParticles();
        } else {
            super.handleEntityEvent(id);
        }
    }

    protected int increaseAirSupply(int currentAir) {
        if (!this.isEyeInFluid(FluidTags.WATER) && prevEyesInWater && !level.isClientSide && spoutTimer <= 0 && currentAir < this.getMaxAirSupply() / 2) {
            spoutTimer = 20 + random.nextInt(10);
        }
        return this.getMaxAirSupply();
    }

    public int getMaxHeadXRot() {
        return 1;
    }

    public int getMaxHeadYRot() {
        return 3;
    }

    public void recieveEcho() {
        this.receivedEcho = true;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.cachalotWhaleSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    class AIBreathe extends Goal {

        public AIBreathe() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        public boolean canUse() {
            return EntityCachalotWhale.this.getAirSupply() < 140;
        }

        public boolean canContinueToUse() {
            return this.canUse();
        }

        public boolean isInterruptable() {
            return false;
        }

        public void start() {
            this.navigate();
        }

        private void navigate() {
            Iterable<BlockPos> lvt_1_1_ = BlockPos.betweenClosed(MathHelper.floor(EntityCachalotWhale.this.getX() - 1.0D), MathHelper.floor(EntityCachalotWhale.this.getY()), MathHelper.floor(EntityCachalotWhale.this.getZ() - 1.0D), MathHelper.floor(EntityCachalotWhale.this.getX() + 1.0D), MathHelper.floor(EntityCachalotWhale.this.getY() + 8.0D), MathHelper.floor(EntityCachalotWhale.this.getZ() + 1.0D));
            BlockPos lvt_2_1_ = null;
            Iterator var3 = lvt_1_1_.iterator();

            while (var3.hasNext()) {
                BlockPos lvt_4_1_ = (BlockPos) var3.next();
                if (this.canBreatheAt(EntityCachalotWhale.this.level, lvt_4_1_)) {
                    lvt_2_1_ = lvt_4_1_.below((int) (EntityCachalotWhale.this.getBbHeight() * 0.25d));
                    break;
                }
            }

            if (lvt_2_1_ == null) {
                lvt_2_1_ = new BlockPos(EntityCachalotWhale.this.getX(), EntityCachalotWhale.this.getY() + 4.0D, EntityCachalotWhale.this.getZ());
            }
            if (EntityCachalotWhale.this.isEyeInFluid(FluidTags.WATER)) {
                EntityCachalotWhale.this.setDeltaMovement(EntityCachalotWhale.this.getDeltaMovement().add(0, 0.05F, 0));
            }

            EntityCachalotWhale.this.getNavigation().moveTo(lvt_2_1_.getX(), lvt_2_1_.getY(), lvt_2_1_.getZ(), 0.7D);
        }

        public void tick() {
            this.navigate();
        }

        private boolean canBreatheAt(IWorldReader p_205140_1_, BlockPos p_205140_2_) {
            BlockState lvt_3_1_ = p_205140_1_.getBlockState(p_205140_2_);
            return (p_205140_1_.getFluidState(p_205140_2_).isEmpty() || lvt_3_1_.is(Blocks.BUBBLE_COLUMN)) && lvt_3_1_.isPathfindable(p_205140_1_, p_205140_2_, PathType.LAND);
        }
    }
}
