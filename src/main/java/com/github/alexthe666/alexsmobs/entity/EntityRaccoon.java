package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.merchant.villager.AbstractVillagerEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.IronGolemEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.entity.projectile.AbstractArrowEntity;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.*;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityRaccoon extends TameableEntity implements IAnimatedEntity, IFollower, ITargetsDroppedItems, ILootsChests {

    private static final DataParameter<Boolean> STANDING = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> BEGGING = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> WASHING = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<BlockPos>> WASH_POS = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Integer> COMMAND = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.INT);
    private static final DataParameter<Integer> CARPET_COLOR = EntityDataManager.defineId(EntityRaccoon.class, DataSerializers.INT);
    public float prevStandProgress;
    public float standProgress;
    public float prevBegProgress;
    public float begProgress;
    public float prevWashProgress;
    public float washProgress;
    public float prevSitProgress;
    public float sitProgress;
    public int maxStandTime = 75;
    private int standingTime = 0;
    private int stealCooldown = 0;
    public int lookForWaterBeforeEatingTimer = 0;
    private int animationTick;
    private Animation currentAnimation;
    private int pickupItemCooldown = 0;
    @Nullable
    private UUID eggThrowerUUID = null;
    public boolean forcedSit = false;
    public static final Animation ANIMATION_ATTACK = Animation.create(12);
    private static final EntityPredicate VILLAGER_STEAL_PREDICATE = (new EntityPredicate()).range(20.0D).allowInvulnerable().allowSameTeam();
    private static final EntityPredicate IRON_GOLEM_PREDICATE = (new EntityPredicate()).range(20.0D).allowUnseeable().allowInvulnerable().allowSameTeam();

    protected EntityRaccoon(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 0.0F);
    }

    protected float getWaterSlowDown() {
        return 0.98F;
    }


    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.RACCOON_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.RACCOON_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.RACCOON_HURT;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.raccoonSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SitGoal(this));
        this.goalSelector.addGoal(1, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(2, new RaccoonAIWash(this));
        this.goalSelector.addGoal(3, new TameableAIFollowOwner(this, 1.3D, 10.0F, 2.0F, false));
        this.goalSelector.addGoal(4, new SwimGoal(this));
        this.goalSelector.addGoal(5, new LeapAtTargetGoal(this, 0.4F));
        this.goalSelector.addGoal(6, new MeleeAttackGoal(this, 1.1D, true));
        this.goalSelector.addGoal(7, new AnimalAILootChests(this, 16));
        this.goalSelector.addGoal(8, new FollowParentGoal(this, 1.1D));
        this.goalSelector.addGoal(9, new RaccoonAIBeg(this, 0.65D));
        this.goalSelector.addGoal(10, new AnimalAIPanicBaby(this, 1.25D));
        this.goalSelector.addGoal(11, new AIStealFromVillagers(this));
        this.goalSelector.addGoal(12, new StrollGoal(200));
        this.goalSelector.addGoal(13, new TameableAIDestroyTurtleEggs(this, 1.0D, 3));
        this.goalSelector.addGoal(14, new AnimalAIWanderRanged(this, 120, 1.0D, 14, 7));
        this.goalSelector.addGoal(15, new LookAtGoal(this, PlayerEntity.class, 15.0F));
        this.goalSelector.addGoal(15, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, (new AnimalAIHurtByTargetNotBaby(this)));
        this.targetSelector.addGoal(1, new CreatureAITargetItems(this, false));
        this.targetSelector.addGoal(3, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(4, new OwnerHurtTargetGoal(this));
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
        if(this.getAnimation() == NO_ANIMATION){
            this.setAnimation(ANIMATION_ATTACK);
        }
        return true;
    }

    protected void dropEquipment() {
        super.dropEquipment();
        if (this.getColor() != null) {
            if (!this.level.isClientSide) {
                this.spawnAtLocation(this.getCarpetItemBeingWorn());
            }
            this.setColor(null);
        }

    }

    @Nullable
    public DyeColor getColor() {
        int lvt_1_1_ = this.entityData.get(CARPET_COLOR);
        return lvt_1_1_ == -1 ? null : DyeColor.byId(lvt_1_1_);
    }

    public void setColor(@Nullable DyeColor color) {
        this.entityData.set(CARPET_COLOR, color == null ? -1 : color.getId());
    }

    public Item getCarpetItemBeingWorn() {
        if (this.getColor() != null) {
            return EntityElephant.DYE_COLOR_ITEM_MAP.get(this.getColor());
        }
        return Items.AIR;
    }


    public boolean isFood(ItemStack stack) {
        return stack.getItem() == Items.BREAD;
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        boolean owner = this.isTame() && isOwnedBy(player);
        if (owner && ItemTags.CARPETS.contains(item)) {
            DyeColor color = EntityElephant.getCarpetColor(itemstack);
            if (color != this.getColor()) {
                if (this.getColor() != null) {
                    this.spawnAtLocation(this.getCarpetItemBeingWorn());
                }
                this.playSound(SoundEvents.LLAMA_SWAG, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
                itemstack.shrink(1);
                this.setColor(color);
                return ActionResultType.SUCCESS;
            }
            return ActionResultType.PASS;
        } else if (owner && this.getColor() != null && itemstack.getItem() == Items.SHEARS) {
            this.playSound(SoundEvents.SHEEP_SHEAR, 1.0F, (this.random.nextFloat() - this.random.nextFloat()) * 0.2F + 1.0F);
            if (this.getColor() != null) {
                this.spawnAtLocation(this.getCarpetItemBeingWorn());
            }
            this.setColor(null);
            return ActionResultType.SUCCESS;
        }else if(isTame() && isRaccoonFood(itemstack) && !isFood(itemstack) && this.getHealth() < this.getMaxHealth()){
            if(this.getMainHandItem().isEmpty()){
                ItemStack copy = itemstack.copy();
                copy.setCount(1);
                this.setItemInHand(Hand.MAIN_HAND, copy);
                this.onEatItem();
                if(itemstack.hasContainerItem()){
                    this.spawnAtLocation(itemstack.getContainerItem());
                }
                if(!player.isCreative()){
                    itemstack.shrink(1);
                }
                this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            }else{
                this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                this.heal(5);
            }
            this.usePlayerItem(player, itemstack);
            return ActionResultType.SUCCESS;
        }
        if(owner && !this.getMainHandItem().isEmpty()){
            if(!this.level.isClientSide){
                this.spawnAtLocation(this.getMainHandItem().copy());
            }
            this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            pickupItemCooldown = 60;
            return ActionResultType.SUCCESS;
        }
        if(type != ActionResultType.SUCCESS && isTame() && isOwnedBy(player) && !isFood(itemstack)){
            if(!player.isShiftKeyDown()){
                this.setCommand(this.getCommand() + 1);
                if(this.getCommand() == 3){
                    this.setCommand(0);
                }
                player.displayClientMessage(new TranslationTextComponent("entity.alexsmobs.all.command_" + this.getCommand(), this.getName()), true);
                boolean sit = this.getCommand() == 2;
                if(sit){
                    this.forcedSit = true;
                    this.setOrderedToSit(true);
                    return ActionResultType.SUCCESS;
                }else{
                    this.forcedSit = false;
                    this.setOrderedToSit(false);
                    return ActionResultType.SUCCESS;
                }
            }
        }
        return type;
    }


    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("RacSitting", this.isSitting());
        compound.putBoolean("ForcedToSit", this.forcedSit);
        compound.putInt("RacCommand", this.getCommand());
        compound.putInt("Carpet", this.entityData.get(CARPET_COLOR));
        compound.putInt("StealCooldown", stealCooldown);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setOrderedToSit(compound.getBoolean("RacSitting"));
        this.forcedSit = compound.getBoolean("ForcedToSit");
        this.setCommand(compound.getInt("RacCommand"));
        this.entityData.set(CARPET_COLOR, compound.getInt("Carpet"));
        this.stealCooldown = compound.getInt("StealCooldown");

    }

    public void setCommand(int command) {
        this.entityData.set(COMMAND, Integer.valueOf(command));
    }

    public int getCommand() {
        return this.entityData.get(COMMAND).intValue();
    }

    public void setOrderedToSit(boolean sit) {
        this.entityData.set(SITTING, Boolean.valueOf(sit));
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING).booleanValue();
    }

    public static boolean isRaccoonFood(ItemStack stack){
        return stack.isEdible() || ItemTags.getAllTags().getTag(AMTagRegistry.RACCOON_FOODSTUFFS).contains(stack.getItem());
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 9D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 0.25F);
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
            return super.hurt(source, amount);
        }
    }

    public void tick() {
        super.tick();
        this.prevStandProgress = this.standProgress;
        this.prevBegProgress = this.begProgress;
        this.prevWashProgress = this.washProgress;
        this.prevSitProgress = this.sitProgress;
        if (this.isStanding() && standProgress < 5) {
            standProgress += 1;
        }
        if (!this.isStanding() && standProgress > 0) {
            standProgress -= 1;
        }
        if (this.isBegging() && begProgress < 5) {
            begProgress += 1;
        }
        if (!this.isBegging() && begProgress > 0) {
            begProgress -= 1;
        }
        if (this.isWashing() && washProgress < 5) {
            washProgress += 1;
        }
        if (!this.isWashing() && washProgress > 0) {
            washProgress -= 1;
        }
        if (this.isSitting() && sitProgress < 5) {
            sitProgress += 1;
        }
        if (!this.isSitting() && sitProgress > 0) {
            sitProgress -= 1;
        }
        if (isStanding() && ++standingTime > maxStandTime) {
            this.setStanding(false);
            standingTime = 0;
            maxStandTime = 75 + random.nextInt(50);
        }
        if(!level.isClientSide){
            if(lookForWaterBeforeEatingTimer > 0){
                lookForWaterBeforeEatingTimer--;
            }else if(!isWashing() && canTargetItem(this.getMainHandItem())) {
                onEatItem();
                if(this.getMainHandItem().hasContainerItem()){
                    this.spawnAtLocation(this.getMainHandItem().getContainerItem());
                }
                this.getMainHandItem().shrink(1);
            }
        }
        if(isWashing()){
            if(getWashPos() != null){
                BlockPos washingPos = getWashPos();
                if(this.distanceToSqr(washingPos.getX() + 0.5D, washingPos.getY() + 0.5D, washingPos.getZ() + 0.5D) < 3){
                    for(int j = 0; (float)j < 4; ++j) {
                        double d2 = (this.random.nextDouble()) ;
                        double d3 = (this.random.nextDouble()) ;
                        Vector3d vector3d = this.getDeltaMovement();

                        this.level.addParticle(ParticleTypes.SPLASH, washingPos.getX() + d2, (double)(washingPos.getY() + 0.8F), washingPos.getZ() + d3, vector3d.x, vector3d.y, vector3d.z);
                    }
                }else{
                    setWashing(false);
                }
            }
        }
        if(!level.isClientSide && this.getTarget() != null && this.canSee(this.getTarget()) && this.distanceTo(this.getTarget()) < 4 && this.getAnimation() == ANIMATION_ATTACK && this.getAnimationTick() == 5) {
            float f1 = this.yRot * ((float)Math.PI / 180F);
            this.setDeltaMovement(this.getDeltaMovement().add((double)(-MathHelper.sin(f1) * -0.06F), 0.0D, (double)(MathHelper.cos(f1) * -0.06F)));
            this.getTarget().knockback(0.35F, getTarget().getX() - this.getX(), getTarget().getZ() - this.getZ());
            this.getTarget().hurt(DamageSource.mobAttack(this), (float) this.getAttribute(Attributes.ATTACK_DAMAGE).getBaseValue());
        }
        if(stealCooldown > 0){
            stealCooldown--;
        }
        if(pickupItemCooldown > 0){
            pickupItemCooldown--;
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    public void onEatItem(){
        this.heal(10);
        this.level.broadcastEntityEvent(this, (byte)92);
        this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
    }

    public void postWashItem(ItemStack stack){
        if(stack.getItem() == Items.EGG && eggThrowerUUID != null && !this.isTame()){
            if(getRandom().nextFloat() < 0.3F){
                this.setTame(true);
                this.setOwnerUUID(eggThrowerUUID);
                PlayerEntity player = level.getPlayerByUUID(eggThrowerUUID);
                if (player instanceof ServerPlayerEntity) {
                    CriteriaTriggers.TAME_ANIMAL.trigger((ServerPlayerEntity)player, this);
                }
                this.level.broadcastEntityEvent(this, (byte)7);
            }else{
                this.level.broadcastEntityEvent(this, (byte)6);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if(id == 92){
            for (int i = 0; i < 6 + random.nextInt(3); i++) {
                double d2 = this.random.nextGaussian() * 0.02D;
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItemInHand(Hand.MAIN_HAND)), this.getX() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, this.getY() + this.getBbHeight() * 0.5F + (double) (this.random.nextFloat() * this.getBbHeight() * 0.5F), this.getZ() + (double) (this.random.nextFloat() * this.getBbWidth()) - (double) this.getBbWidth() * 0.5F, d0, d1, d2);
            }
        }else{
            super.handleEntityEvent(id);
        }
    }
        public boolean isStanding() {
        return this.entityData.get(STANDING).booleanValue();
    }

    public void setStanding(boolean standing) {
        this.entityData.set(STANDING, Boolean.valueOf(standing));
    }

    public boolean isBegging() {
        return this.entityData.get(BEGGING).booleanValue();
    }

    public void setBegging(boolean begging) {
        this.entityData.set(BEGGING, Boolean.valueOf(begging));
    }

    public boolean isWashing() {
        return this.entityData.get(WASHING).booleanValue();
    }

    public void setWashing(boolean washing) {
        this.entityData.set(WASHING, Boolean.valueOf(washing));
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STANDING, Boolean.valueOf(false));
        this.entityData.define(SITTING, Boolean.valueOf(false));
        this.entityData.define(BEGGING, Boolean.valueOf(false));
        this.entityData.define(WASHING, Boolean.valueOf(false));
        this.entityData.define(CARPET_COLOR, -1);
        this.entityData.define(COMMAND, 0);
        this.entityData.define(WASH_POS, Optional.empty());
    }


    public BlockPos getWashPos() {
        return this.entityData.get(WASH_POS).orElse(null);
    }

    public void setWashPos(BlockPos washingPos) {
        this.entityData.set(WASH_POS, Optional.ofNullable(washingPos));
    }

    @Override
    public int getAnimationTick() {
        return animationTick;
    }

    @Override
    public void setAnimationTick(int i) {
        animationTick = i;
    }

    @Override
    public Animation getAnimation() {
        return currentAnimation;
    }

    @Override
    public void setAnimation(Animation animation) {
        currentAnimation = animation;
        if(animation == ANIMATION_ATTACK){
            maxStandTime = 15;
            this.setStanding(true);
        }
    }

    @Override
    public Animation[] getAnimations() {
        return new Animation[]{ANIMATION_ATTACK};
    }


    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.RACCOON.create(serverWorld);
    }

    public void travel(Vector3d vec3d) {
        if (this.isSitting() || this.isWashing()) {
            if (this.getNavigation().getPath() != null) {
                this.getNavigation().stop();
            }
            vec3d = Vector3d.ZERO;
        }
        super.travel(vec3d);
    }

    @Override
    public boolean shouldFollow() {
        return getCommand() == 1;
    }

    @Override
    public boolean canTargetItem(ItemStack stack) {
        return isRaccoonFood(stack) && pickupItemCooldown == 0;
    }

    @Override
    public void onGetItem(ItemEntity e) {
        lookForWaterBeforeEatingTimer = 100;
        ItemStack duplicate = e.getItem().copy();
        duplicate.setCount(1);
        if (!this.getItemInHand(Hand.MAIN_HAND).isEmpty() && !this.level.isClientSide) {
            this.spawnAtLocation(this.getItemInHand(Hand.MAIN_HAND), 0.0F);
        }
        this.setItemInHand(Hand.MAIN_HAND, duplicate);
        if(e.getItem().getItem() == Items.EGG){
            eggThrowerUUID = e.getThrower();
        }else{
            eggThrowerUUID = null;
        }
    }

    @Override
    public boolean isLootable(IInventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (shouldLootItem(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean shouldLootItem(ItemStack stack) {
        return isRaccoonFood(stack);
    }

    class StrollGoal extends MoveThroughVillageAtNightGoal {
        public StrollGoal(int p_i50726_3_) {
            super(EntityRaccoon.this, p_i50726_3_);
        }

        /**
         * Execute a one shot task or start executing a continuous task
         */
        public void start() {
            super.start();
        }

        /**
         * Returns whether execution should begin. You can also read and cache any state necessary for execution in this
         * method as well.
         */
        public boolean canUse() {
            return super.canUse() && this.canFoxMove();
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        public boolean canContinueToUse() {
            return super.canContinueToUse() && this.canFoxMove();
        }

        private boolean canFoxMove() {
            return !EntityRaccoon.this.isWashing() && !EntityRaccoon.this.isSitting() && EntityRaccoon.this.getTarget() == null;
        }
    }

    public BlockPos getLightPosition() {
        BlockPos pos = new BlockPos(this.position());
        if (!level.getBlockState(pos).canOcclude()) {
            return pos.above();
        }
        return pos;
    }

    private class AIStealFromVillagers extends Goal {
        EntityRaccoon raccoon;
        AbstractVillagerEntity target;
        int golemCheckTime = 0;
        int cooldown = 0;
        int fleeTime = 0;

        private AIStealFromVillagers(EntityRaccoon raccoon){
            this.raccoon = raccoon;
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            if(cooldown > 0){
                cooldown--;
                return false;
            }else if(raccoon != null && raccoon.stealCooldown == 0 && raccoon.getMainHandItem() != null && raccoon.getMainHandItem().isEmpty()){
                AbstractVillagerEntity villager = getNearbyVillagers();
                if(!isGolemNearby() && villager != null){
                    target = villager;
                }
                cooldown = 150;
                return target != null;
            }
            return false;
        }

        @Override
        public boolean canContinueToUse() {
            return target != null && raccoon != null;
        }

        public void stop(){
            target = null;
            cooldown = 200 + random.nextInt(200);
            golemCheckTime = 0;
            fleeTime = 0;
        }

        public void tick(){
            if(target != null){
                golemCheckTime++;
                if(fleeTime > 0){
                    fleeTime--;
                    if(raccoon.getNavigation().isDone()){
                        Vector3d fleevec = RandomPositionGenerator.getPosAvoid(raccoon, 16, 7, raccoon.position());
                        if(fleevec != null){
                            raccoon.getNavigation().moveTo(fleevec.x, fleevec.y, fleevec.z, 1.3F);
                        }
                    }
                    if(fleeTime == 0){
                        stop();
                    }
                }else{
                    raccoon.getNavigation().moveTo(target, 1.0D);
                    if(raccoon.distanceTo(target) < 1.7F){
                        raccoon.setStanding(true);
                        raccoon.maxStandTime = 15;
                        MerchantOffers offers = target.getOffers();
                        if(offers == null || offers.isEmpty() || offers.size() < 1){
                            stop();
                        }else{
                            MerchantOffer offer = offers.get(offers.size() <= 1 ? 0 : raccoon.getRandom().nextInt(offers.size() - 1));
                            if(offer != null){
                                ItemStack stealStack = offer.getResult().getItem() == Items.EMERALD ? offer.getBaseCostA() : offer.getResult();
                                if(stealStack.isEmpty()){
                                    stop();
                                }else{
                                    offer.increaseUses();
                                    ItemStack copy = stealStack.copy();
                                    copy.setCount(1);
                                    raccoon.setItemInHand(Hand.MAIN_HAND, copy);
                                    fleeTime = 60 + random.nextInt(60);
                                    raccoon.getNavigation().stop();
                                    lookForWaterBeforeEatingTimer = 120 + random.nextInt(60);
                                    target.hurt(DamageSource.mobAttack(raccoon), target.getHealth() <= 2 ? 0 : 1);
                                    raccoon.stealCooldown = 24000 + random.nextInt(48000);
                                }
                            }
                        }
                    }
                    if(golemCheckTime % 30 == 0 && random.nextBoolean() && isGolemNearby()){
                        stop();
                    }
                }
            }
        }

        @Nullable
        private boolean isGolemNearby() {
            List<IronGolemEntity> lvt_1_1_ = raccoon.level.getNearbyEntities(IronGolemEntity.class, IRON_GOLEM_PREDICATE, raccoon, raccoon.getBoundingBox().inflate(25.0D));
            return !lvt_1_1_.isEmpty();
        }

        @Nullable
        private AbstractVillagerEntity getNearbyVillagers() {
            List<AbstractVillagerEntity> lvt_1_1_ = raccoon.level.getNearbyEntities(AbstractVillagerEntity.class, VILLAGER_STEAL_PREDICATE, raccoon, raccoon.getBoundingBox().inflate(20.0D));
            double lvt_2_1_ = 10000;
            AbstractVillagerEntity lvt_4_1_ = null;
            Iterator var5 = lvt_1_1_.iterator();

            while(var5.hasNext()) {
                AbstractVillagerEntity lvt_6_1_ = (AbstractVillagerEntity)var5.next();
                if (lvt_6_1_.getHealth() > 2.0F && !lvt_6_1_.getOffers().isEmpty() && raccoon.distanceToSqr(lvt_6_1_) < lvt_2_1_) {
                    lvt_4_1_ = lvt_6_1_;
                    lvt_2_1_ = raccoon.distanceToSqr(lvt_6_1_);
                }
            }

            return lvt_4_1_;
        }

    }
}
