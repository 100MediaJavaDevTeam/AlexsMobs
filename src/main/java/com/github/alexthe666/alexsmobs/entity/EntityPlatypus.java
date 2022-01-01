package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.material.Material;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.Random;

public class EntityPlatypus extends AnimalEntity implements ISemiAquatic, ITargetsDroppedItems {

    private static final DataParameter<Boolean> SENSING = EntityDataManager.defineId(EntityPlatypus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SENSING_VISUAL = EntityDataManager.defineId(EntityPlatypus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> DIGGING = EntityDataManager.defineId(EntityPlatypus.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FEDORA = EntityDataManager.defineId(EntityPlatypus.class, DataSerializers.BOOLEAN);
    public float prevInWaterProgress;
    public float inWaterProgress;
    public float prevDigProgress;
    public float digProgress;
    public boolean superCharged = false;
    private boolean isLandNavigator;
    private int swimTimer = -1000;

    protected EntityPlatypus(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER, 0.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0.0F);
        switchNavigator(false);
    }

    public static boolean canPlatypusSpawn(EntityType type, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        ITag<Block> tag = BlockTags.getAllTags().getTag(AMTagRegistry.PLATYPUS_SPAWNS);
        boolean spawnBlock = tag != null && tag.contains(worldIn.getBlockState(pos.below()).getBlock());
        return (tag == null && worldIn.getBlockState(pos.below()).getBlock() == Blocks.DIRT || spawnBlock) && pos.getY() < worldIn.getSeaLevel() + 4;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.platypusSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 16.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == AMItemRegistry.LOBSTER_TAIL || item == AMItemRegistry.COOKED_LOBSTER_TAIL;
    }


    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.PLATYPUS_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.PLATYPUS_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.PLATYPUS_HURT;
    }
    
    protected ItemStack getFishBucket() {
        ItemStack stack = new ItemStack(AMItemRegistry.PLATYPUS_BUCKET);
        CompoundNBT platTag = new CompoundNBT();
        this.addAdditionalSaveData(platTag);
        stack.getOrCreateTag().put("PlatypusData", platTag);
        if (this.hasCustomName()) {
            stack.setHoverName(this.getCustomName());
        }
        return stack;
    }

    public ActionResultType mobInteract(PlayerEntity p_230254_1_, Hand p_230254_2_) {
        ItemStack itemstack = p_230254_1_.getItemInHand(p_230254_2_);
        boolean redstone = itemstack.getItem() == Items.REDSTONE || itemstack.getItem() == Items.REDSTONE_BLOCK;
        if(itemstack.getItem() == AMItemRegistry.FEDORA && !this.hasFedora()){
            if (!p_230254_1_.isCreative()) {
                itemstack.shrink(1);
            }
            this.setFedora(true);
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        if (redstone && !this.isSensing()) {
            superCharged = itemstack.getItem() == Items.REDSTONE_BLOCK;
            if (!p_230254_1_.isCreative()) {
                itemstack.shrink(1);
            }
            this.setSensing(true);
            return ActionResultType.sidedSuccess(this.level.isClientSide);
        }
        if (itemstack.getItem() == Items.WATER_BUCKET && this.isAlive()) {
            this.playSound(SoundEvents.BUCKET_FILL_FISH, 1.0F, 1.0F);
            itemstack.shrink(1);
            ItemStack itemstack1 = this.getFishBucket();
            if (!this.level.isClientSide) {
                CriteriaTriggers.FILLED_BUCKET.trigger((ServerPlayerEntity) p_230254_1_, itemstack1);
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

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new BreatheAirGoal(this));
        this.goalSelector.addGoal(1, new AnimalAIFindWater(this));
        this.goalSelector.addGoal(1, new AnimalAILeaveWater(this));
        this.goalSelector.addGoal(2, new BreedGoal(this, 0.8D));
        this.goalSelector.addGoal(3, new PanicGoal(this, 1.1D));
        this.goalSelector.addGoal(3, new TemptGoal(this, 1.0D, Ingredient.of(Items.REDSTONE, Items.REDSTONE_BLOCK), false){
            public void start() {
                super.start();
                EntityPlatypus.this.setSensingVisual(true);
            }

            public boolean canUse(){
                return super.canUse() && !EntityPlatypus.this.isSensing();
            }

            public void stop() {
                super.stop();
                EntityPlatypus.this.setSensingVisual(false);
            }
        });
        this.goalSelector.addGoal(5, new TemptGoal(this, 1.1D, Ingredient.of(ItemTags.getAllTags().getTag(AMTagRegistry.PLATYPUS_FOODSTUFFS)), false){
            public boolean canUse(){
                return super.canUse() && !EntityPlatypus.this.isSensing();
            }
        });
        this.goalSelector.addGoal(5, new PlatypusAIDigForItems(this));
        this.goalSelector.addGoal(6, new SemiAquaticAIRandomSwimming(this, 1.0D, 30));
        this.goalSelector.addGoal(7, new RandomWalkingGoal(this, 1.0D, 60));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(9, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false, false, 40, 15){
            public boolean canUse(){
                return super.canUse() && !EntityPlatypus.this.isSensing();
            }

            public boolean canContinueToUse(){
                return super.canContinueToUse() && !EntityPlatypus.this.isSensing();
            }
        });
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if(prev && source.getDirectEntity() instanceof LivingEntity){
            LivingEntity entity = (LivingEntity)source.getDirectEntity();
            entity.addEffect(new EffectInstance(Effects.POISON, 100));
        }
        return prev;
    }

    public boolean isPerry() {
        String s = TextFormatting.stripFormatting(this.getName().getString());
        return s != null && s.toLowerCase().contains("perry");
    }


    public int getMaxAirSupply() {
        return 4800;
    }

    protected int increaseAirSupply(int currentAir) {
        return this.getMaxAirSupply();
    }

    public void spawnGroundEffects() {
        float radius = 0.3F;
        for (int i1 = 0; i1 < 3; i1++) {
            double motionX = getRandom().nextGaussian() * 0.07D;
            double motionY = getRandom().nextGaussian() * 0.07D;
            double motionZ = getRandom().nextGaussian() * 0.07D;
            float angle = (0.01745329251F * this.yBodyRot) + i1;
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraY = 0.8F;
            double extraZ = radius * MathHelper.cos(angle);
            BlockPos ground = this.getBlockPosBelowThatAffectsMyMovement();
            BlockState BlockState = this.level.getBlockState(ground);
            if (BlockState.getMaterial() != Material.AIR && BlockState.getMaterial() != Material.WATER) {
                if (level.isClientSide) {
                    level.addParticle(new BlockParticleData(ParticleTypes.BLOCK, BlockState), true, this.getX() + extraX, ground.getY() + extraY, this.getZ() + extraZ, motionX, motionY, motionZ);
                }
            }
        }
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason
            reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setAirSupply(this.getMaxAirSupply());
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public boolean isPushedByFluid() {
        return false;
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

    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DIGGING, false);
        this.entityData.define(SENSING, Boolean.valueOf(false));
        this.entityData.define(SENSING_VISUAL, Boolean.valueOf(false));
        this.entityData.define(FEDORA, false);
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.hasFedora()) {
            this.spawnAtLocation(AMItemRegistry.FEDORA);
        }

    }

    public boolean isSensing() {
        return this.entityData.get(SENSING).booleanValue();
    }

    public void setSensing(boolean sensing) {
        this.entityData.set(SENSING, Boolean.valueOf(sensing));
    }

    public boolean isSensingVisual() {
        return this.entityData.get(SENSING_VISUAL).booleanValue();
    }

    public void setSensingVisual(boolean sensing) {
        this.entityData.set(SENSING_VISUAL, Boolean.valueOf(sensing));
    }

    public boolean hasFedora() {
        return this.entityData.get(FEDORA).booleanValue();
    }

    public void setFedora(boolean sensing) {
        this.entityData.set(FEDORA, Boolean.valueOf(sensing));
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Fedora", this.hasFedora());
        compound.putBoolean("Sensing", this.isSensing());
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setFedora(compound.getBoolean("Fedora"));
        this.setSensing(compound.getBoolean("Sensing"));
    }

        public void tick() {
        super.tick();
        prevInWaterProgress = inWaterProgress;
        prevDigProgress = digProgress;
        boolean dig = isDigging() && isInWaterOrBubble();
        if (dig && digProgress < 5F) {
            digProgress++;
        }
        if (!dig && digProgress > 0F) {
            digProgress--;
        }
        if (this.isInWaterOrBubble() && inWaterProgress < 5F) {
            inWaterProgress++;
        }
        if (!this.isInWaterOrBubble() && inWaterProgress > 0F) {
            inWaterProgress--;
        }
        if (this.isInWaterOrBubble() && this.isLandNavigator) {
            switchNavigator(false);
        }
        if (!this.isInWaterOrBubble() && !this.isLandNavigator) {
            switchNavigator(true);
        }
        if (this.onGround && isDigging()) {
            spawnGroundEffects();
        }
        if (inWaterProgress > 0) {
            this.maxUpStep = 1;
        } else {
            this.maxUpStep = 0.6F;
        }
        if (!level.isClientSide) {
            if (isInWater()) {
                swimTimer++;
            } else {
                swimTimer--;
            }
        }
        if (this.isAlive() && (this.isSensing() || this.isSensingVisual())) {
            for (int j = 0; j < 2; ++j) {
                float radius = this.getBbWidth() * 0.65F;
                float angle = (0.01745329251F * this.yBodyRot);
                double extraX = (radius * (1.5F + random.nextFloat() * 0.3F)) * MathHelper.sin((float) (Math.PI + angle)) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().x * 2F;
                double extraZ = (radius * (1.5F + random.nextFloat() * 0.3F)) * MathHelper.cos(angle) + (random.nextFloat() - 0.5F) + this.getDeltaMovement().z * 2F;
                double actualX = radius * MathHelper.sin((float) (Math.PI + angle));
                double actualZ = radius * MathHelper.cos(angle);
                double motX = actualX - extraX;
                double motZ = actualZ - extraZ;
                this.level.addParticle(AMParticleRegistry.PLATYPUS_SENSE, this.getX() + extraX, this.getBbHeight() * 0.3F + this.getY(), this.getZ() + extraZ, motX * 0.1F, 0, motZ * 0.1F);
            }
        }
    }

    public boolean isDigging() {
        return this.entityData.get(DIGGING);
    }

    public void setDigging(boolean digging) {
        this.entityData.set(DIGGING, digging);
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigatorWide(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new AnimalSwimMoveControllerSink(this, 1.2F, 1.6F);
            this.navigation = new SemiAquaticPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    public boolean shouldEnterWater() {
        return this.getLastHurtByMob() != null || swimTimer <= -1000 || this.isSensing();
    }

    @Override
    public boolean shouldLeaveWater() {
        return swimTimer > 600 && !this.isSensing();
    }

    @Override
    public boolean shouldStopMoving() {
        return this.isDigging();
    }

    @Override
    public int getWaterSearchRange() {
        return 10;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.PLATYPUS.create(serverWorld);
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return !this.isSensing() && ItemTags.getAllTags().getTag(AMTagRegistry.PLATYPUS_FOODSTUFFS).contains(stack.getItem());
    }

    @Override
    public void onGetItem(ItemEntity e) {
        this.playSound(SoundEvents.CAT_EAT, this.getSoundVolume(), this.getVoicePitch());
        if(e.getItem().getItem() == Items.REDSTONE || e.getItem().getItem() == Items.REDSTONE_BLOCK){
            superCharged = e.getItem().getItem().getItem() == Items.REDSTONE_BLOCK;
            this.setSensing(true);
        }else{
            this.heal(6);
        }
    }
}
