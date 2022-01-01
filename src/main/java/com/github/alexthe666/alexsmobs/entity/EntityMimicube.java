package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.AnimalAIWanderRanged;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.MimiCubeAIRangedAttack;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.*;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.SoundEvents;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import net.minecraft.entity.ai.controller.MovementController.Action;

public class EntityMimicube extends MonsterEntity implements IRangedAttackMob {

    private static final DataParameter<Integer> ATTACK_TICK = EntityDataManager.defineId(EntityMimicube.class, DataSerializers.INT);
    private final MimiCubeAIRangedAttack aiArrowAttack = new MimiCubeAIRangedAttack(this, 1.0D, 10, 15.0F);
    private final MeleeAttackGoal aiAttackOnCollide = new MeleeAttackGoal(this, 1.2D, false);
    public float squishAmount;
    public float squishFactor;
    public float prevSquishFactor;
    public float leftSwapProgress = 0;
    public float prevLeftSwapProgress = 0;
    public float rightSwapProgress = 0;
    public float prevRightSwapProgress = 0;
    public float helmetSwapProgress = 0;
    public float prevHelmetSwapProgress = 0;
    public float prevAttackProgress;
    public float attackProgress;
    private boolean wasOnGround;
    private int eatingTicks;

    protected EntityMimicube(EntityType type, World world) {
        super(type, world);
        this.moveControl = new MimicubeMoveHelper(this);
        this.navigation = new DirectPathNavigator(this, world);
        this.setCombatTask();
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.45F);
    }


    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.mimicubeSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ATTACK_TICK, 0);

    }

    public boolean doHurtTarget(Entity entityIn) {
        this.entityData.set(ATTACK_TICK, 5);
        return true;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(1, new AnimalAIWanderRanged(this, 60, 1.0D, 10, 7));
        this.goalSelector.addGoal(2, new LookAtGoal(this, PlayerEntity.class, 8.0F));
        this.goalSelector.addGoal(2, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, PlayerEntity.class, true));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, AbstractVillagerEntity.class, true));
    }

    public void setCombatTask() {
        if (this.level != null && !this.level.isClientSide) {
            this.goalSelector.removeGoal(this.aiAttackOnCollide);
            this.goalSelector.removeGoal(this.aiArrowAttack);
            ItemStack itemstack = this.getMainHandItem();
            if (itemstack.getItem() instanceof ShootableItem || itemstack.getItem() instanceof TridentItem) {
                int i = 10;
                if (this.level.getDifficulty() != Difficulty.HARD) {
                    i = 30;
                }

                this.aiArrowAttack.setAttackCooldown(i);
                this.goalSelector.addGoal(4, this.aiArrowAttack);
            } else {
                this.goalSelector.addGoal(4, this.aiAttackOnCollide);
            }

        }
    }

    public void attackEntityWithRangedAttackTrident(LivingEntity target, float distanceFactor) {
        TridentEntity tridententity = new TridentEntity(this.level, this, new ItemStack(Items.TRIDENT));
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333D) - tridententity.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
        tridententity.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEvents.DROWNED_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addFreshEntity(tridententity);
    }

    public void performRangedAttack(LivingEntity target, float distanceFactor) {
        if (this.getMainHandItem().getItem() instanceof TridentItem) {
            attackEntityWithRangedAttackTrident(target, distanceFactor);
            return;
        }
        ItemStack itemstack = this.getProjectile(this.getMainHandItem());
        AbstractArrowEntity abstractarrowentity = this.fireArrow(itemstack, distanceFactor);
        if (this.getMainHandItem().getItem() instanceof net.minecraft.item.BowItem)
            abstractarrowentity = ((net.minecraft.item.BowItem) this.getMainHandItem().getItem()).customArrow(abstractarrowentity);
        double d0 = target.getX() - this.getX();
        double d1 = target.getY(0.3333333333333333D) - abstractarrowentity.getY();
        double d2 = target.getZ() - this.getZ();
        double d3 = MathHelper.sqrt(d0 * d0 + d2 * d2);
        abstractarrowentity.shoot(d0, d1 + d3 * (double) 0.2F, d2, 1.6F, (float) (14 - this.level.getDifficulty().getId() * 4));
        this.playSound(SoundEvents.SKELETON_SHOOT, 1.0F, 1.0F / (this.getRandom().nextFloat() * 0.4F + 0.8F));
        this.level.addFreshEntity(abstractarrowentity);
    }

    protected AbstractArrowEntity fireArrow(ItemStack arrowStack, float distanceFactor) {
        return ProjectileHelper.getMobArrow(this, arrowStack, distanceFactor);
    }

    public boolean canFireProjectileWeapon(ShootableItem p_230280_1_) {
        return p_230280_1_ == Items.BOW;
    }

    public void setItemSlot(EquipmentSlotType slotIn, ItemStack stack) {
        if (slotIn == EquipmentSlotType.HEAD && !stack.sameItem(this.getItemBySlot(EquipmentSlotType.HEAD))) {
            helmetSwapProgress = 5;
            this.level.broadcastEntityEvent(this, (byte) 45);
        }
        if (slotIn == EquipmentSlotType.MAINHAND && !stack.sameItem(this.getItemBySlot(EquipmentSlotType.MAINHAND))) {
            rightSwapProgress = 5;
            this.level.broadcastEntityEvent(this, (byte) 46);
        }
        if (slotIn == EquipmentSlotType.OFFHAND && !stack.sameItem(this.getItemBySlot(EquipmentSlotType.OFFHAND))) {
            leftSwapProgress = 5;
            this.level.broadcastEntityEvent(this, (byte) 47);
        }
        super.setItemSlot(slotIn, stack);
        if (!this.level.isClientSide) {
            this.setCombatTask();
        }

    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        super.handleEntityEvent(id);
        if (id == 45) {
            helmetSwapProgress = 5;
        }
        if (id == 46) {
            rightSwapProgress = 5;
        }
        if (id == 47) {
            leftSwapProgress = 5;

        }
    }

    public boolean isBlocking() {
        return this.getMainHandItem().isShield(this) || this.getOffhandItem().isShield(this);
    }

    public boolean hurt(DamageSource source, float amount) {
        Entity trueSource = source.getEntity();
        if (trueSource != null && trueSource instanceof LivingEntity) {
            LivingEntity attacker = (LivingEntity) trueSource;
            if (!attacker.getItemBySlot(EquipmentSlotType.HEAD).isEmpty()) {
                this.setItemSlot(EquipmentSlotType.HEAD, mimicStack(attacker.getItemBySlot(EquipmentSlotType.HEAD)));
            }
            if (!attacker.getItemBySlot(EquipmentSlotType.OFFHAND).isEmpty()) {
                this.setItemSlot(EquipmentSlotType.OFFHAND, mimicStack(attacker.getItemBySlot(EquipmentSlotType.OFFHAND)));
            }
            if (!attacker.getItemBySlot(EquipmentSlotType.MAINHAND).isEmpty()) {
                this.setItemSlot(EquipmentSlotType.MAINHAND, mimicStack(attacker.getItemBySlot(EquipmentSlotType.MAINHAND)));
            }
        }
        return super.hurt(source, amount);
    }

    private ItemStack mimicStack(ItemStack stack){
        ItemStack copy = stack.copy();
        if(copy.isDamageableItem()){
            copy.setDamageValue(copy.getMaxDamage());
        }
        return copy;
    }

    public void tick() {
        super.tick();
        this.squishFactor += (this.squishAmount - this.squishFactor) * 0.5F;
        this.prevSquishFactor = this.squishFactor;
        this.prevHelmetSwapProgress = this.helmetSwapProgress;
        this.prevRightSwapProgress = this.rightSwapProgress;
        this.prevLeftSwapProgress = this.leftSwapProgress;
        this.prevAttackProgress = attackProgress;
        if (rightSwapProgress > 0F) {
            rightSwapProgress -= 0.5F;
        }
        if (leftSwapProgress > 0F) {
            leftSwapProgress -= 0.5F;
        }
        if (helmetSwapProgress > 0F) {
            helmetSwapProgress -= 0.5F;
        }
        if (this.onGround && !this.wasOnGround) {

            for (int j = 0; j < 8; ++j) {
                float f = this.random.nextFloat() * ((float) Math.PI * 2F);
                float f1 = this.random.nextFloat() * 0.5F + 0.5F;
                float f2 = MathHelper.sin(f) * 0.5F * f1;
                float f3 = MathHelper.cos(f) * 0.5F * f1;
                this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, new ItemStack(AMItemRegistry.MIMICREAM)), this.getX() + (double)f2, this.getY(), this.getZ() + (double)f3, 0.0D, 0.0D, 0.0D);
            }

            this.playSound(this.getSquishSound(), this.getSoundVolume(), ((this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F) / 0.8F);
            this.squishAmount = -0.35F;
        } else if (!this.onGround && this.wasOnGround) {
            this.squishAmount = 2F;
        }
        if(this.isInWater()){
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.05D, 0));
        }
        if (this.getOffhandItem().getItem().isEdible() && this.getHealth() < this.getMaxHealth()) {
            if (eatingTicks < 100) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItemInHand(Hand.OFF_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
                }
                if (eatingTicks % 6 == 0) {
                    this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                }
                eatingTicks++;
            }
            if (eatingTicks == 100) {
                this.playSound(SoundEvents.PLAYER_BURP, this.getSoundVolume(), this.getVoicePitch());
                this.getOffhandItem().shrink(1);
                this.heal(5);
                eatingTicks = 0;
            }
        } else if (this.getMainHandItem().getItem().isEdible() && this.getHealth() < this.getMaxHealth()) {
            if (eatingTicks < 100) {
                for (int i = 0; i < 3; i++) {
                    double d2 = this.random.nextGaussian() * 0.02D;
                    double d0 = this.random.nextGaussian() * 0.02D;
                    double d1 = this.random.nextGaussian() * 0.02D;
                    this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItemInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
                }
                this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                if (eatingTicks % 6 == 0) {
                    this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                }
                eatingTicks++;
            }
            if (eatingTicks == 100) {
                this.playSound(SoundEvents.PLAYER_BURP, this.getSoundVolume(), this.getVoicePitch());
                this.getMainHandItem().shrink(1);
                this.heal(5);
            }
        } else {
            eatingTicks = 0;
        }
        this.wasOnGround = this.onGround;
        this.alterSquishAmount();
        LivingEntity livingentity = this.getTarget();
        if (livingentity != null && this.distanceToSqr(livingentity) < 144D) {
            this.moveControl.setWantedPosition(livingentity.getX(), livingentity.getY(), livingentity.getZ(), this.moveControl.getSpeedModifier());
            this.wasOnGround = true;
        }
        if (this.entityData.get(ATTACK_TICK) > 0) {
            if (this.entityData.get(ATTACK_TICK) == 2 && this.getTarget() != null && this.distanceTo(this.getTarget()) < 2.3D) {
                super.doHurtTarget(this.getTarget());
            }
            this.entityData.set(ATTACK_TICK, this.entityData.get(ATTACK_TICK) - 1);
            if (attackProgress < 3F) {
                attackProgress++;
            }
        } else {
            if (attackProgress > 0F) {
                attackProgress--;
            }
        }

    }

    protected float getEquipmentDropChance(EquipmentSlotType slotIn) {
        return 0;
    }

    private SoundEvent getSquishSound() {
        return AMSoundRegistry.MIMICUBE_JUMP;
    }

    private SoundEvent getJumpSound() {
        return AMSoundRegistry.MIMICUBE_JUMP;
    }

    protected void jumpFromGround() {
        Vector3d vector3d = this.getDeltaMovement();
        this.setDeltaMovement(vector3d.x, this.getJumpPower(), vector3d.z);
        this.hasImpulse = true;
    }

    protected int getJumpDelay() {
        return this.random.nextInt(20) + 10;
    }

    protected void alterSquishAmount() {
        this.squishAmount *= 0.6F;
    }

    public boolean shouldShoot() {
        return this.getMainHandItem().getItem() instanceof ShootableItem || this.getMainHandItem().getItem() instanceof TridentItem;
    }

    private class MimicubeMoveHelper extends MovementController {
        private final EntityMimicube slime;
        private float yRot;
        private int jumpDelay;
        private boolean isAggressive;

        public MimicubeMoveHelper(EntityMimicube slimeIn) {
            super(slimeIn);
            this.slime = slimeIn;
            this.yRot = 180.0F * slimeIn.yRot / (float) Math.PI;
        }

        public void setDirection(float yRotIn, boolean aggressive) {
            this.yRot = yRotIn;
            this.isAggressive = aggressive;
        }

        public void setSpeed(double speedIn) {
            this.speedModifier = speedIn;
            this.operation = MovementController.Action.MOVE_TO;
        }

        public void tick() {
            if (this.mob.isOnGround()) {
                this.mob.setSpeed((float) (this.speedModifier * this.mob.getAttributeValue(Attributes.MOVEMENT_SPEED)));
                if (this.jumpDelay-- <= 0 && this.operation != Action.WAIT) {
                    this.jumpDelay = this.slime.getJumpDelay();
                    if (this.mob.getTarget() != null) {
                        this.jumpDelay /= 3;
                    }

                    this.slime.getJumpControl().jump();
                    this.slime.playSound(this.slime.getJumpSound(), this.slime.getSoundVolume(), this.slime.getVoicePitch());
                } else {
                    this.slime.xxa = 0.0F;
                    this.slime.zza = 0.0F;
                    this.mob.setSpeed(0.0F);
                }
            }
            super.tick();
        }
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.MIMICUBE_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.MIMICUBE_HURT;
    }

}

