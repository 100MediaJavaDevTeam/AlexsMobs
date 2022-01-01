package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;
import java.util.UUID;
import java.util.function.Predicate;

public class EntityKomodoDragon extends TameableEntity implements ITargetsDroppedItems {

    private static final Ingredient TEMPTATION_ITEMS = Ingredient.of(Items.ROTTEN_FLESH);
    public int slaughterCooldown = 0;
    public int timeUntilSpit = this.random.nextInt(12000) + 24000;
    private int riderAttackCooldown = 0;
    public static final Predicate<EntityKomodoDragon> HURT_OR_BABY = (p_213616_0_) -> {
        return p_213616_0_.isBaby() || p_213616_0_.getHealth() <= 0.7F * p_213616_0_.getMaxHealth();
    };

    public static <T extends MobEntity> boolean canKomodoDragonSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        boolean spawnBlock = BlockTags.getAllTags().getTag(AMTagRegistry.KOMODO_DRAGON_SPAWNS).contains(worldIn.getBlockState(pos.below()).getBlock());
        return spawnBlock && worldIn.getRawBrightness(pos, 0) > 8;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.komodoDragonSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new SitGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 2D, false));
        this.goalSelector.addGoal(2, new TameableAIRide(this, 2D));
        this.goalSelector.addGoal(4, new TameableAITempt(this, 1.1D, TEMPTATION_ITEMS, false));
        this.goalSelector.addGoal(4, new AnimalAIFleeAdult(this, 1.25D, 32));
        this.goalSelector.addGoal(5, new KomodoDragonAIBreed(this, 1.0D));
        this.goalSelector.addGoal(6, new RandomWalkingGoal(this, 1D, 50));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
        this.targetSelector.addGoal(3, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new CreatureAITargetItems(this, false));
        this.targetSelector.addGoal(6, new NearestAttackableTargetGoal(this, EntityKomodoDragon.class, 50, true, false, HURT_OR_BABY));
        this.targetSelector.addGoal(7, new NearestAttackableTargetGoal(this, PlayerEntity.class, 150, true, true, null));
        this.targetSelector.addGoal(8, new EntityAINearestTarget3D(this, LivingEntity.class, 180, false, true, AMEntityRegistry.buildPredicateFromTag(EntityTypeTags.getAllTags().getTag(AMTagRegistry.KOMODO_DRAGON_TARGETS))));
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

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.KOMODO_DRAGON_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.KOMODO_DRAGON_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.KOMODO_DRAGON_HURT;
    }



    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("SpitTime")) {
            this.timeUntilSpit = compound.getInt("SpitTime");
        }

    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("SpitTime", this.timeUntilSpit);
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return isTame() && item == Items.ROTTEN_FLESH;
    }

    public void tick() {
        super.tick();
        if(slaughterCooldown > 0){
            slaughterCooldown--;
        }
        if (!this.level.isClientSide && this.isAlive() && !this.isBaby() && --this.timeUntilSpit <= 0) {
            this.spawnAtLocation(AMItemRegistry.KOMODO_SPIT);
            this.timeUntilSpit = this.random.nextInt(12000) + 24000;
        }
        if(riderAttackCooldown > 0){
            riderAttackCooldown--;
        }
        if(this.getControllingPassenger() != null && this.getControllingPassenger() instanceof PlayerEntity){
            PlayerEntity rider = (PlayerEntity)this.getControllingPassenger();
            if(rider.getLastHurtMob() != null && this.distanceTo(rider.getLastHurtMob()) < this.getBbWidth() + 3F && !this.isAlliedTo(rider.getLastHurtMob())){
                UUID preyUUID = rider.getLastHurtMob().getUUID();
                if (!this.getUUID().equals(preyUUID) && riderAttackCooldown == 0) {
                    doHurtTarget(rider.getLastHurtMob());
                    riderAttackCooldown = 20;
                }
            }
        }
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

    public boolean doHurtTarget(Entity entityIn) {
        if (super.doHurtTarget(entityIn)) {
            if (entityIn instanceof LivingEntity) {
                int i = 5;
                if (this.level.getDifficulty() == Difficulty.NORMAL) {
                    i = 10;
                } else if (this.level.getDifficulty() == Difficulty.HARD) {
                    i = 20;
                }
                ((LivingEntity)entityIn).addEffect(new EffectInstance(Effects.POISON, i * 20, 0));
            }

            return true;
        } else {
            return false;
        }
    }

    public boolean canBeAffected(EffectInstance potioneffectIn) {
        if (potioneffectIn.getEffect() == Effects.POISON) {
            return false;
        }
        return super.canBeAffected(potioneffectIn);
    }

    @Nullable
    public Entity getControllingPassenger() {
        for (Entity passenger : this.getPassengers()) {
            if (passenger instanceof PlayerEntity) {
                PlayerEntity player = (PlayerEntity) passenger;
                return player;
            }
        }
        return null;
    }

    public void positionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            float radius = 0;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            passenger.setPos(this.getX() + extraX, this.getY() + this.getPassengersRidingOffset() + passenger.getMyRidingOffset(), this.getZ() + extraZ);
        }
    }

    public double getPassengersRidingOffset() {
        float f = Math.min(0.25F, this.animationSpeed);
        float f1 = this.animationPosition;
        return (double)this.getBbHeight() - 0.2D + (double)(0.12F * MathHelper.cos(f1 * 0.7F) * 0.7F * f);
    }



    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);

        if(item == Items.ROTTEN_FLESH && !isTame()){
            int size = itemstack.getCount();
            int tameAmount = 58 + random.nextInt(16);
            if(size > tameAmount){
                this.tame(player);
            }
            itemstack.shrink(size);
            return ActionResultType.SUCCESS;
        }
        if(type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player)){
            if(isFood(itemstack)){
                this.setInLoveTime(600);
                this.usePlayerItem(player, itemstack);
                return ActionResultType.SUCCESS;
            }
            if(!player.isShiftKeyDown() && !isFood(itemstack) && !this.isBaby()){
                player.startRiding(this);
                return ActionResultType.SUCCESS;
            }
        }
        return type;
    }

    protected EntityKomodoDragon(EntityType type, World worldIn) {
        super(type, worldIn);
    }

    protected float getWaterSlowDown() {
        return 0.98F;
    }

    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if(!this.isBaby() || slaughterCooldown > 0){
            super.setTarget(entitylivingbaseIn);
        }
    }
    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30D).add(Attributes.ARMOR, 0.0D).add(Attributes.ATTACK_DAMAGE, 4.0D).add(Attributes.MOVEMENT_SPEED, 0.23F);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return AMEntityRegistry.KOMODO_DRAGON.create(p_241840_1_);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem() == Items.ROTTEN_FLESH || stack.getItem().getFoodProperties() != null && stack.getItem().getFoodProperties().isMeat();
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.heal(10);
    }
}
