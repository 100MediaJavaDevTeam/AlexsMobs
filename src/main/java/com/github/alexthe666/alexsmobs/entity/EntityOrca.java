package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.DolphinLookController;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.DamageSource;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.*;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

public class EntityOrca extends TameableEntity implements IAnimatedEntity {

    public static final Animation ANIMATION_BITE = Animation.create(8);
    public static final Animation ANIMATION_TAILSWING = Animation.create(20);
    private static final DataParameter<Integer> MOISTNESS = EntityDataManager.defineId(EntityOrca.class, DataSerializers.INT);
    private static final EntityPredicate PLAYER_PREDICATE = (new EntityPredicate()).range(24.0D).allowSameTeam().allowInvulnerable().allowUnseeable();
    public int jumpCooldown;
    private int animationTick;
    private Animation currentAnimation;
    private int blockBreakCounter;
    public static final Predicate<LivingEntity> TARGET_BABY  = (animal) -> {
        return animal.isBaby();
    };

    protected EntityOrca(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.moveControl = new MoveHelperController(this);
        this.lookControl = new DolphinLookController(this, 10);
    }

    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame();
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.orcaSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 60.0D).add(Attributes.FOLLOW_RANGE, 64.0D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 10.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.7F).add(Attributes.MOVEMENT_SPEED, 1.35F);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new SwimmerJumpPathNavigator(this, worldIn);
    }

    public int getMoistness() {
        return this.entityData.get(MOISTNESS);
    }

    public void setMoistness(int p_211137_1_) {
        this.entityData.set(MOISTNESS, p_211137_1_);
    }

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(MOISTNESS, 2400);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.ORCA_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.ORCA_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.ORCA_DIE;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreatheAirGoal(this));
        this.goalSelector.addGoal(1, new FindWaterGoal(this));
        this.goalSelector.addGoal(2, new EntityOrca.SwimWithPlayerGoal(this, 4.0D));
        this.goalSelector.addGoal(4, new RandomSwimmingGoal(this, 1.0D, 10));
        this.goalSelector.addGoal(4, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(5, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(5, new OrcaAIJump(this, 10));
        this.goalSelector.addGoal(6, new OrcaAIMeleeJump(this));
        this.goalSelector.addGoal(6, new OrcaAIMelee(this, 1.2F, true));
        this.goalSelector.addGoal(8, new FollowBoatGoal(this));
        this.targetSelector.addGoal(1, (new HurtByTargetGoal(this)).setAlertOthers());
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, EntityCachalotWhale.class, 5, false, false, TARGET_BABY));
        this.targetSelector.addGoal(3, new EntityAINearestTarget3D(this, LivingEntity.class, 200, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.ORCA_TARGETS))));
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
        return new Animation[]{ANIMATION_BITE, ANIMATION_TAILSWING};
    }

    public void travel(Vector3d travelVector) {
        if (this.isEffectiveAi() && this.isInWater()) {
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
        if (!level.isClientSide && this.blockBreakCounter == 0) {
            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
                        BlockPos pos = new BlockPos(a, b, c);
                        BlockState state = level.getBlockState(pos);
                        FluidState fluidState = level.getFluidState(pos);
                        Block block = state.getBlock();
                        if (!state.isAir() && !state.getShape(level, pos).isEmpty() && BlockTags.getAllTags().getTag(AMTagRegistry.ORCA_BREAKABLES).contains(state.getBlock()) && fluidState.isEmpty()) {
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
            blockBreakCounter = 20;
        }
    }

    public void tick() {
        super.tick();
        if (jumpCooldown > 0) {
            jumpCooldown--;
            float f2 = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
            this.xRot = f2;
        }
        if (this.isNoAi()) {
            this.setAirSupply(this.getMaxAirSupply());
        } else {

            if (this.isInWaterRainOrBubble()) {
                this.setMoistness(2400);
            } else {
                this.setMoistness(this.getMoistness() - 1);
                if (this.getMoistness() <= 0) {
                    this.hurt(DamageSource.DRY_OUT, 1.0F);
                }

                if (this.onGround) {
                    this.setDeltaMovement(this.getDeltaMovement().add((this.random.nextFloat() * 2.0F - 1.0F) * 0.2F, 0.5D, (this.random.nextFloat() * 2.0F - 1.0F) * 0.2F));
                    this.yRot = this.random.nextFloat() * 360.0F;
                    this.onGround = false;
                    this.hasImpulse = true;
                }
            }

            if (this.level.isClientSide && this.isInWater() && this.getDeltaMovement().lengthSqr() > 0.03D) {
                Vector3d vector3d = this.getViewVector(0.0F);
                float f = MathHelper.cos(this.yRot * ((float) Math.PI / 180F)) * 0.9F;
                float f1 = MathHelper.sin(this.yRot * ((float) Math.PI / 180F)) * 0.9F;
                float f2 = 1.2F - this.random.nextFloat() * 0.7F;

                for (int i = 0; i < 2; ++i) {
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double) f2 + (double) f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double) f2 + (double) f1, 0.0D, 0.0D, 0.0D);
                    this.level.addParticle(ParticleTypes.DOLPHIN, this.getX() - vector3d.x * (double) f2 - (double) f, this.getY() - vector3d.y, this.getZ() - vector3d.z * (double) f2 - (double) f1, 0.0D, 0.0D, 0.0D);
                }
            }

        }
        LivingEntity attackTarget = this.getTarget();
        if (attackTarget != null && distanceTo(attackTarget) < attackTarget.getBbWidth() + this.getBbWidth() + 2) {
            if (this.getAnimation() == ANIMATION_BITE && this.getAnimationTick() == 4) {
                float damage =(float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                if(attackTarget instanceof DrownedEntity || attackTarget instanceof GuardianEntity){
                    damage *= 2F;
                }
                boolean flag = attackTarget.hurt(DamageSource.mobAttack(this), damage);
                if (flag) {
                    this.doEnchantDamageEffects(this, attackTarget);
                    this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
                }
            }
            if (this.getAnimation() == ANIMATION_TAILSWING && this.getAnimationTick() == 6) {
                float damage =(float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                if(attackTarget instanceof DrownedEntity || attackTarget instanceof GuardianEntity){
                    damage *= 2F;
                }
                boolean flag = attackTarget.hurt(DamageSource.mobAttack(this), damage);
                if (flag) {
                    this.doEnchantDamageEffects(this, attackTarget);
                    this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
                }
                attackTarget.knockback(1F, MathHelper.sin(yRot * ((float) Math.PI / 180F)), -MathHelper.cos(yRot * ((float) Math.PI / 180F)));
                float knockbackResist = (float) MathHelper.clamp((1.0D - this.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE)), 0, 1);
                this.getTarget().setDeltaMovement(this.getTarget().getDeltaMovement().add(0, knockbackResist * 0.4F, 0));

            }
        }
        if (attackTarget != null && attackTarget instanceof PlayerEntity && attackTarget.hasEffect(AMEffectRegistry.ORCAS_MIGHT)) {
            attackTarget.removeEffect(AMEffectRegistry.ORCAS_MIGHT);
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int tick) {
        animationTick = tick;
    }

    public boolean doHurtTarget(Entity entityIn) {
        if(this.isInWaterOrBubble() && random.nextBoolean()){
            this.setAnimation(ANIMATION_TAILSWING);
        }else{
            this.setAnimation(ANIMATION_BITE);
        }
        return true;
    }

    public int getMaxAirSupply() {
        return 4800;
    }

    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }

    protected float getStandingEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return 1.0F;
    }

    public int getMaxHeadXRot() {
        return 1;
    }

    public int getMaxHeadYRot() {
        return 1;
    }

    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.SALMON;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.ORCA.create(serverWorld);
    }

    public boolean shouldUseJumpAttack(LivingEntity attackTarget) {
        if (attackTarget.isInWater()) {
            BlockPos up = attackTarget.blockPosition().above();
            return level.getFluidState(up.above()).isEmpty() && level.getFluidState(up.above(2)).isEmpty() && this.jumpCooldown == 0;
        } else {
            return this.jumpCooldown == 0;
        }
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason
            reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setAirSupply(this.getMaxAirSupply());
        this.xRot = 0.0F;
        this.setMoistness(2400);
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

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Moistness", this.getMoistness());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setMoistness(compound.getInt("Moistness"));
    }

    public void onJumpHit(LivingEntity entityIn) {
        boolean flag = entityIn.hurt(DamageSource.mobAttack(this), (float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        if (flag) {
            this.doEnchantDamageEffects(this, entityIn);
            this.playSound(SoundEvents.DOLPHIN_ATTACK, 1.0F, 1.0F);
        }
    }

    public static boolean canOrcaSpawn(EntityType<EntityOrca> p_223364_0_, IWorld p_223364_1_, SpawnReason reason, BlockPos p_223364_3_, Random p_223364_4_) {
        if (p_223364_3_.getY() > 45 && p_223364_3_.getY() < p_223364_1_.getSeaLevel()) {
            Optional<RegistryKey<Biome>> optional = p_223364_1_.getBiomeName(p_223364_3_);
            return (!Objects.equals(optional, Optional.of(Biomes.OCEAN)) || !Objects.equals(optional, Optional.of(Biomes.DEEP_OCEAN))) && p_223364_1_.getFluidState(p_223364_3_).is(FluidTags.WATER);
        } else {
            return false;
        }
    }

    static class SwimWithPlayerGoal extends Goal {
        private final EntityOrca dolphin;
        private final double speed;
        private PlayerEntity targetPlayer;

        SwimWithPlayerGoal(EntityOrca dolphinIn, double speedIn) {
            this.dolphin = dolphinIn;
            this.speed = speedIn;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
        }

        public boolean canUse() {
            this.targetPlayer = this.dolphin.level.getNearestPlayer(EntityOrca.PLAYER_PREDICATE, this.dolphin);
            if (this.targetPlayer == null) {
                return false;
            } else {
                return this.targetPlayer.isSwimming() && this.dolphin.getTarget() != this.targetPlayer;
            }
        }

        public boolean canContinueToUse() {
            return this.targetPlayer != null  && this.dolphin.getTarget() != this.targetPlayer && this.targetPlayer.isSwimming() && this.dolphin.distanceToSqr(this.targetPlayer) < 256.0D;
        }

        public void start() {
        }

        public void stop() {
            this.targetPlayer = null;
            this.dolphin.getNavigation().stop();
        }

        public void tick() {
            this.dolphin.getLookControl().setLookAt(this.targetPlayer, (float) (this.dolphin.getMaxHeadYRot() + 20), (float) this.dolphin.getMaxHeadXRot());
            if (this.dolphin.distanceToSqr(this.targetPlayer) < 10D) {
                this.dolphin.getNavigation().stop();
            } else {
                this.dolphin.getNavigation().moveTo(this.targetPlayer, this.speed);
            }

            if (this.targetPlayer.isSwimming() && this.targetPlayer.level.random.nextInt(6) == 0) {
                this.targetPlayer.addEffect(new EffectInstance(AMEffectRegistry.ORCAS_MIGHT, 1000));
            }
        }
    }

    static class MoveHelperController extends MovementController {
        private final EntityOrca dolphin;

        public MoveHelperController(EntityOrca dolphinIn) {
            super(dolphinIn);
            this.dolphin = dolphinIn;
        }

        public void tick() {
            if (this.dolphin.isInWater()) {
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
                    if (this.dolphin.isInWater()) {
                        this.dolphin.setSpeed(f1 * 0.02F);
                        float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt(d0 * d0 + d2 * d2)) * (double) (180F / (float) Math.PI)));
                        f2 = MathHelper.clamp(MathHelper.wrapDegrees(f2), -85.0F, 85.0F);
                        this.dolphin.xRot = this.rotlerp(this.dolphin.xRot, f2, 5.0F);
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
