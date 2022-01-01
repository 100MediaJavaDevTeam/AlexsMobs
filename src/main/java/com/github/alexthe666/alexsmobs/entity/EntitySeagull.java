package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.CreatureAITargetItems;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.SeagullAIRevealTreasure;
import com.github.alexthe666.alexsmobs.entity.ai.SeagullAIStealFromPlayers;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.google.common.base.Predicate;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.RandomPositionGenerator;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.ListNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.BlockParticleData;
import net.minecraft.particles.IParticleData;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.GroundPathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.Biomes;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.MapDecoration;

import javax.annotation.Nullable;
import java.util.*;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntitySeagull extends AnimalEntity implements ITargetsDroppedItems {

    private static final DataParameter<Boolean> FLYING = EntityDataManager.defineId(EntitySeagull.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> FLIGHT_LOOK_YAW = EntityDataManager.defineId(EntitySeagull.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> ATTACK_TICK = EntityDataManager.defineId(EntitySeagull.class, DataSerializers.INT);
    private static final DataParameter<Boolean> SITTING = EntityDataManager.defineId(EntitySeagull.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Optional<BlockPos>> TREASURE_POS = EntityDataManager.defineId(EntitySeagull.class, DataSerializers.OPTIONAL_BLOCK_POS);
    public float prevFlyProgress;
    public float flyProgress;
    public float prevFlapAmount;
    public float flapAmount;
    public boolean aiItemFlag = false;
    public float attackProgress;
    public float prevAttackProgress;
    public float sitProgress;
    public float prevSitProgress;
    public int stealCooldown = random.nextInt(2500);
    private boolean isLandNavigator;
    private int timeFlying;
    private BlockPos orbitPos = null;
    private double orbitDist = 5D;
    private boolean orbitClockwise = false;
    private boolean fallFlag = false;
    private int flightLookCooldown = 0;
    private float targetFlightLookYaw;
    private int heldItemTime = 0;
    public int treasureSitTime;
    public UUID feederUUID = null;

    protected EntitySeagull(EntityType type, World worldIn) {
        super(type, worldIn);
        this.setPathfindingMalus(PathNodeType.DANGER_FIRE, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER, -1.0F);
        this.setPathfindingMalus(PathNodeType.WATER_BORDER, 16.0F);
        this.setPathfindingMalus(PathNodeType.COCOA, -1.0F);
        this.setPathfindingMalus(PathNodeType.FENCE, -1.0F);
        switchNavigator(false);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SEAGULL_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SEAGULL_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SEAGULL_HURT;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Flying", this.isFlying());
        compound.putBoolean("Sitting", this.isSitting());
        compound.putInt("StealCooldown", this.stealCooldown);
        compound.putInt("TreasureSitTime", this.treasureSitTime);
        if(feederUUID != null){
            compound.putUUID("FeederUUID", feederUUID);
        }
        if(this.getTreasurePos() != null){
            compound.putInt("TresX", this.getTreasurePos().getX());
            compound.putInt("TresY", this.getTreasurePos().getY());
            compound.putInt("TresZ", this.getTreasurePos().getZ());
        }
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.setFlying(compound.getBoolean("Flying"));
        this.setSitting(compound.getBoolean("Sitting"));
        this.stealCooldown = compound.getInt("StealCooldown");
        this.treasureSitTime = compound.getInt("TreasureSitTime");
        if(compound.hasUUID("FeederUUID")){
            this.feederUUID = compound.getUUID("FeederUUID");
        }
        if(compound.contains("TresX") && compound.contains("TresY") && compound.contains("TresZ")){
            this.setTreasurePos(new BlockPos(compound.getInt("TresX"), compound.getInt("TresY"), compound.getInt("TresZ")));
        }
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 8.0D).add(Attributes.ATTACK_DAMAGE, 1.0D).add(Attributes.MOVEMENT_SPEED, 0.2F);
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.targetSelector.addGoal(1, new SeagullAIRevealTreasure(this));
        this.targetSelector.addGoal(2, new SeagullAIStealFromPlayers(this));
        this.goalSelector.addGoal(3, new BreedGoal(this, 1.0D));
        this.goalSelector.addGoal(4, new TemptGoal(this, 1.0D, Ingredient.of(Items.COD, AMItemRegistry.LOBSTER_TAIL, AMItemRegistry.COOKED_LOBSTER_TAIL), false){
            public boolean canUse(){
                return !EntitySeagull.this.aiItemFlag && super.canUse();
            }
        });
        this.goalSelector.addGoal(5, new AIWanderIdle());
        this.goalSelector.addGoal(6, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(7, new LookAtGoal(this, CreatureEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.goalSelector.addGoal(9, new AIScatter());
        this.targetSelector.addGoal(1, new AITargetItems(this, false, false, 15, 16));
    }

    public boolean isFood(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.COD;
    }

    public static boolean canSeagullSpawn(EntityType<? extends AnimalEntity> animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return worldIn.getRawBrightness(pos, 0) > 8 && worldIn.getFluidState(pos.below()).isEmpty();
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.seagullSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    private void switchNavigator(boolean onLand) {
        if (onLand) {
            this.moveControl = new MovementController(this);
            this.navigation = new GroundPathNavigator(this, level);
            this.isLandNavigator = true;
        } else {
            this.moveControl = new MoveHelper(this);
            this.navigation = new DirectPathNavigator(this, level);
            this.isLandNavigator = false;
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(FLYING, false);
        this.entityData.define(SITTING, false);
        this.entityData.define(ATTACK_TICK, 0);
        this.entityData.define(TREASURE_POS, Optional.empty());
        this.entityData.define(FLIGHT_LOOK_YAW, 0F);
    }

    public boolean isFlying() {
        return this.entityData.get(FLYING);
    }

    public void setFlying(boolean flying) {
        if (flying && this.isBaby()) {
            flying = false;
        }
        this.entityData.set(FLYING, flying);
    }

    public boolean isSitting() {
        return this.entityData.get(SITTING);
    }

    public void setSitting(boolean sitting) {
        this.entityData.set(SITTING, sitting);
    }

    public float getFlightLookYaw() {
        return entityData.get(FLIGHT_LOOK_YAW);
    }

    public void setFlightLookYaw(float yaw) {
        entityData.set(FLIGHT_LOOK_YAW, yaw);
    }

    public BlockPos getTreasurePos() {
        return this.entityData.get(TREASURE_POS).orElse(null);
    }

    public void setTreasurePos(BlockPos pos) {
        this.entityData.set(TREASURE_POS, Optional.ofNullable(pos));
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else {
            Entity entity = source.getEntity();
            boolean prev = super.hurt(source, amount);
            if (prev) {
                this.setSitting(false);
                if (!this.getMainHandItem().isEmpty()) {
                    this.spawnAtLocation(this.getMainHandItem());
                    this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
                    stealCooldown = 1500 + random.nextInt(1500);
                }
                this.feederUUID = null;
                this.treasureSitTime = 0;
            }
            return prev;
        }
    }

    public void tick() {
        super.tick();
        this.prevFlyProgress = flyProgress;
        this.prevFlapAmount = flapAmount;
        this.prevAttackProgress = attackProgress;
        this.prevSitProgress = sitProgress;
        float yMot = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
        float absYaw = Math.abs(this.yRot - this.yRotO);
        if (isFlying() && flyProgress < 5F) {
            flyProgress++;
        }
        if (!isFlying() && flyProgress > 0F) {
            flyProgress--;
        }
        if (isSitting() && sitProgress < 5F) {
            sitProgress++;
        }
        if (!isSitting() && sitProgress > 0F) {
            sitProgress--;
        }
        if (absYaw > 8) {
            flapAmount = Math.min(1F, flapAmount + 0.1F);
        } else if (yMot < 0.0F) {
            flapAmount = Math.min(-yMot * 0.2F, 1F);
        } else {
            if (flapAmount > 0.0F) {
                flapAmount -= Math.min(flapAmount, 0.05F);
            } else {
                flapAmount = 0;
            }
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
        if (!level.isClientSide) {
            if (isFlying()) {
                float lookYawDist = Math.abs(this.getFlightLookYaw() - targetFlightLookYaw);
                if (flightLookCooldown > 0) {
                    flightLookCooldown--;
                }
                if (flightLookCooldown == 0 && this.random.nextInt(4) == 0 && lookYawDist < 0.5F) {
                    targetFlightLookYaw = MathHelper.clamp(random.nextFloat() * 120F - 60, -60, 60);
                    flightLookCooldown = 3 + random.nextInt(15);
                }
                if (this.getFlightLookYaw() < this.targetFlightLookYaw && lookYawDist > 0.5F) {
                    this.setFlightLookYaw(this.getFlightLookYaw() + Math.min(lookYawDist, 4F));
                }
                if (this.getFlightLookYaw() > this.targetFlightLookYaw && lookYawDist > 0.5F) {
                    this.setFlightLookYaw(this.getFlightLookYaw() - Math.min(lookYawDist, 4F));
                }
                if (this.onGround && !this.isInWaterOrBubble() && this.timeFlying > 30) {
                    this.setFlying(false);
                }
                timeFlying++;
                this.setNoGravity(true);
                if (this.isPassenger() || this.isInLove()) {
                    this.setFlying(false);
                }
            } else {
                fallFlag = false;
                timeFlying = 0;
                this.setNoGravity(false);
            }
            if (isFlying() && this.isLandNavigator) {
                switchNavigator(false);
            }
            if (!isFlying() && !this.isLandNavigator) {
                switchNavigator(true);
            }
        }
        if (!this.getMainHandItem().isEmpty()) {
            heldItemTime++;
            if (heldItemTime > 200 && canTargetItem(this.getMainHandItem())) {
                heldItemTime = 0;
                this.heal(4);
                this.playSound(SoundEvents.GENERIC_EAT, this.getSoundVolume(), this.getVoicePitch());
                if (this.getMainHandItem().hasContainerItem()) {
                    this.spawnAtLocation(this.getMainHandItem().getContainerItem());
                }
                eatItemEffect(this.getMainHandItem());
                this.getMainHandItem().shrink(1);
            }
        } else {
            heldItemTime = 0;
        }
        if (stealCooldown > 0) {
            stealCooldown--;
        }
        if(treasureSitTime > 0){
            treasureSitTime--;
        }
        if(this.isSitting() && this.isInWaterOrBubble()){
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.02F, 0));
        }
    }

    public void eatItem(){
        heldItemTime = 200;
    }
    @Override
    public boolean canTargetItem(ItemStack stack) {
        return stack.getItem().isEdible() && !this.isSitting();
    }

    private void eatItemEffect(ItemStack heldItemMainhand) {
        for (int i = 0; i < 2 + random.nextInt(2); i++) {
            double d2 = this.random.nextGaussian() * 0.02D;
            double d0 = this.random.nextGaussian() * 0.02D;
            double d1 = this.random.nextGaussian() * 0.02D;
            float radius = this.getBbWidth() * 0.65F;
            float angle = (0.01745329251F * this.yBodyRot);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            IParticleData data = new ItemParticleData(ParticleTypes.ITEM, heldItemMainhand);
            if (heldItemMainhand.getItem() instanceof BlockItem) {
                data = new BlockParticleData(ParticleTypes.BLOCK, ((BlockItem) heldItemMainhand.getItem()).getBlock().defaultBlockState());
            }
            this.level.addParticle(data, this.getX() + extraX, this.getY() + this.getBbHeight() * 0.6F, this.getZ() + extraZ, d0, d1, d2);
        }
    }

    public void setDataFromTreasureMap(PlayerEntity player){
        boolean flag = false;
        for(ItemStack map : player.getHandSlots()){
            if(map.getItem() == Items.FILLED_MAP || map.getItem() == Items.MAP){
                if (map.hasTag() && map.getTag().contains("Decorations", 9)) {
                    ListNBT listnbt = map.getTag().getList("Decorations", 10);
                    for(int i = 0; i < listnbt.size(); i++){
                        CompoundNBT nbt = listnbt.getCompound(i);
                        byte type = nbt.getByte("type");
                        if(type == MapDecoration.Type.RED_X.getIcon() || type == MapDecoration.Type.TARGET_X.getIcon()){
                            int x = nbt.getInt("x");
                            int z = nbt.getInt("z");
                            if(this.distanceToSqr(x, this.getY(), z) <= 400){
                                flag = true;
                                this.setTreasurePos(new BlockPos(x, 0, z));
                            }
                        }
                    }
                }
            }
        }
        if(flag){
            this.feederUUID = player.getUUID();
            this.treasureSitTime = 300;
            this.stealCooldown = 1500 + random.nextInt(1500);
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

    public boolean isWingull() {
        String s = TextFormatting.stripFormatting(this.getName().getString());
        return s != null && s.toLowerCase().equals("wingull");
    }

    @Override
    public void onGetItem(ItemEntity e) {
        ItemStack duplicate = e.getItem().copy();
        duplicate.setCount(1);
        if (!this.getItemInHand(Hand.MAIN_HAND).isEmpty() && !this.level.isClientSide) {
            this.spawnAtLocation(this.getItemInHand(Hand.MAIN_HAND), 0.0F);
        }
        stealCooldown += 600 + random.nextInt(1200);
        if(e.getThrower() != null && (e.getItem().getItem() == AMItemRegistry.LOBSTER_TAIL || e.getItem().getItem() == AMItemRegistry.COOKED_LOBSTER_TAIL)){
            PlayerEntity player = level.getPlayerByUUID(e.getThrower());
            if(player != null){
                setDataFromTreasureMap(player);
                feederUUID = e.getThrower();
            }
        }
        this.setFlying(true);
        this.setItemInHand(Hand.MAIN_HAND, duplicate);
    }

    public Vector3d getBlockInViewAway(Vector3d fleePos, float radiusAdd) {
        float radius = 5 + radiusAdd + this.getRandom().nextInt(5);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getSeagullGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 8 + this.getRandom().nextInt(4);
        BlockPos newPos = ground.above(distFromGround > 3 ? flightHeight : this.getRandom().nextInt(4) + 8);
        if (!this.isTargetBlocked(Vector3d.atCenterOf(newPos)) && this.distanceToSqr(Vector3d.atCenterOf(newPos)) > 1) {
            return Vector3d.atCenterOf(newPos);
        }
        return null;
    }

    public BlockPos getSeagullGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), this.getY(), in.getZ());
        while (position.getY() < 256 && !level.getFluidState(position).isEmpty()) {
            position = position.above();
        }
        while (position.getY() > 2 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return position;
    }

    public Vector3d getBlockGrounding(Vector3d fleePos) {
        float radius = 10 + this.getRandom().nextInt(15);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, getY(), fleePos.z() + extraZ);
        BlockPos ground = this.getSeagullGround(radialPos);
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

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());

        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    private Vector3d getOrbitVec(Vector3d vector3d, float gatheringCircleDist) {
        float angle = (0.01745329251F * (float) this.orbitDist * (orbitClockwise ? -tickCount : tickCount));
        double extraX = gatheringCircleDist * MathHelper.sin((angle));
        double extraZ = gatheringCircleDist * MathHelper.cos(angle);
        if (this.orbitPos != null) {
            Vector3d pos = new Vector3d(orbitPos.getX() + extraX, orbitPos.getY() + random.nextInt(2), orbitPos.getZ() + extraZ);
            if (this.level.isEmptyBlock(new BlockPos(pos))) {
                return pos;
            }
        }
        return null;
    }

    private boolean isOverWaterOrVoid() {
        BlockPos position = this.blockPosition();
        while (position.getY() > 0 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        return !level.getFluidState(position).isEmpty() || position.getY() <= 0;
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if (!this.getMainHandItem().isEmpty() && type != ActionResultType.SUCCESS) {
            this.spawnAtLocation(this.getMainHandItem().copy());
            this.setItemInHand(Hand.MAIN_HAND, ItemStack.EMPTY);
            stealCooldown = 1500 + random.nextInt(1500);
            return ActionResultType.SUCCESS;
        } else {
            return type;
        }
    }


    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return AMEntityRegistry.SEAGULL.create(serverWorld);
    }

    public void peck() {
        this.entityData.set(ATTACK_TICK, 7);
    }

    private class AIScatter extends Goal {
        protected final EntitySeagull.AIScatter.Sorter theNearestAttackableTargetSorter;
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
            this.theNearestAttackableTargetSorter = new EntitySeagull.AIScatter.Sorter(EntitySeagull.this);
            this.targetEntitySelector = new Predicate<Entity>() {
                @Override
                public boolean apply(@Nullable Entity e) {
                    return e.isAlive() && e.getType().is(tag) || e instanceof PlayerEntity && !((PlayerEntity) e).isCreative();
                }
            };
        }

        @Override
        public boolean canUse() {
            if (EntitySeagull.this.isPassenger() || EntitySeagull.this.isSitting() || EntitySeagull.this.aiItemFlag || EntitySeagull.this.isVehicle()) {
                return false;
            }
            if (!this.mustUpdate) {
                long worldTime = EntitySeagull.this.level.getGameTime() % 10;
                if (EntitySeagull.this.getNoActionTime() >= 100 && worldTime != 0) {
                    return false;
                }
                if (EntitySeagull.this.getRandom().nextInt(this.executionChance) != 0 && worldTime != 0) {
                    return false;
                }
            }
            List<Entity> list = EntitySeagull.this.level.getEntitiesOfClass(Entity.class, this.getTargetableArea(this.getTargetDistance()), this.targetEntitySelector);
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
            return targetEntity != null;
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
                EntitySeagull.this.setFlying(true);
                EntitySeagull.this.getMoveControl().setWantedPosition(flightTarget.x, flightTarget.y, flightTarget.z, 1F);
                if (cooldown == 0 && EntitySeagull.this.isTargetBlocked(flightTarget)) {
                    cooldown = 30;
                    flightTarget = null;
                }
            }

            if (targetEntity != null) {
                if (EntitySeagull.this.onGround || flightTarget == null || flightTarget != null && EntitySeagull.this.distanceToSqr(flightTarget) < 3) {
                    Vector3d vec = EntitySeagull.this.getBlockInViewAway(targetEntity.position(), 0);
                    if (vec != null && vec.y() > EntitySeagull.this.getY()) {
                        flightTarget = vec;
                    }
                }
                if (EntitySeagull.this.distanceTo(targetEntity) > 20.0F) {
                    this.stop();
                }
            }
        }

        protected double getTargetDistance() {
            return 4D;
        }

        protected AxisAlignedBB getTargetableArea(double targetDistance) {
            Vector3d renderCenter = new Vector3d(EntitySeagull.this.getX(), EntitySeagull.this.getY() + 0.5, EntitySeagull.this.getZ());
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

    private class AIWanderIdle extends Goal {
        protected final EntitySeagull eagle;
        protected double x;
        protected double y;
        protected double z;
        private boolean flightTarget = false;
        private int orbitResetCooldown = 0;
        private int maxOrbitTime = 360;
        private int orbitTime = 0;

        public AIWanderIdle() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE));
            this.eagle = EntitySeagull.this;
        }

        @Override
        public boolean canUse() {
            if (orbitResetCooldown < 0) {
                orbitResetCooldown++;
            }
            if ((eagle.getTarget() != null && eagle.getTarget().isAlive() && !this.eagle.isVehicle()) || eagle.isSitting() || this.eagle.isPassenger()) {
                return false;
            } else {
                if (this.eagle.getRandom().nextInt(20) != 0 && !eagle.isFlying() || eagle.aiItemFlag) {
                    return false;
                }
                if (this.eagle.isBaby()) {
                    this.flightTarget = false;
                } else if (this.eagle.isInWaterOrBubble()) {
                    this.flightTarget = true;
                } else if (this.eagle.isOnGround()) {
                    this.flightTarget = random.nextInt(10) == 0;
                } else {
                    if (orbitResetCooldown == 0 && random.nextInt(6) == 0) {
                        orbitResetCooldown = 100 + random.nextInt(300);
                        eagle.orbitPos = eagle.blockPosition();
                        eagle.orbitDist = 4 + random.nextInt(5);
                        eagle.orbitClockwise = random.nextBoolean();
                        orbitTime = 0;
                        maxOrbitTime = (int) (180 + 360 * random.nextFloat());
                    }
                    this.flightTarget = random.nextInt(5) != 0 && eagle.timeFlying < 400;
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
            if (orbitResetCooldown > 0) {
                orbitResetCooldown--;
            }
            if (orbitResetCooldown < 0) {
                orbitResetCooldown++;
            }
            if (orbitResetCooldown > 0 && eagle.orbitPos != null) {
                if (orbitTime < maxOrbitTime && !eagle.isInWaterOrBubble()) {
                    orbitTime++;
                } else {
                    orbitTime = 0;
                    eagle.orbitPos = null;
                    orbitResetCooldown = -400 - random.nextInt(400);
                }
            }
            if (eagle.horizontalCollision && !eagle.onGround) {
                stop();
            }
            if (flightTarget) {
                eagle.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                if (eagle.isFlying() && !eagle.onGround) {
                    if (!eagle.isInWaterOrBubble()) {
                        //  eagle.setMotion(eagle.getMotion().mul(1F, 0.6F, 1F));
                    }
                } else {
                    this.eagle.getNavigation().moveTo(this.x, this.y, this.z, 1F);
                }
            }
            if (!flightTarget && isFlying()) {
                eagle.fallFlag = true;
                if (eagle.onGround) {
                    eagle.setFlying(false);
                    orbitTime = 0;
                    eagle.orbitPos = null;
                    orbitResetCooldown = -400 - random.nextInt(400);
                }
            }
            if (isFlying() && (!level.isEmptyBlock(eagle.getBlockPosBelowThatAffectsMyMovement()) || eagle.onGround) && !eagle.isInWaterOrBubble() && eagle.timeFlying > 30) {
                eagle.setFlying(false);
                orbitTime = 0;
                eagle.orbitPos = null;
                orbitResetCooldown = -400 - random.nextInt(400);
            }
        }

        @Nullable
        protected Vector3d getPosition() {
            Vector3d vector3d = eagle.position();
            if (orbitResetCooldown > 0 && eagle.orbitPos != null) {
                return eagle.getOrbitVec(vector3d, 4 + random.nextInt(4));
            }
            if (eagle.isVehicle() || eagle.isOverWaterOrVoid()) {
                flightTarget = true;
            }
            if (flightTarget) {
                if (eagle.timeFlying < 340 || eagle.isVehicle() || eagle.isOverWaterOrVoid()) {
                    return eagle.getBlockInViewAway(vector3d, 0);
                } else {
                    return eagle.getBlockGrounding(vector3d);
                }
            } else {
                return RandomPositionGenerator.getPos(this.eagle, 10, 7);
            }
        }

        public boolean canContinueToUse() {
            if (flightTarget) {
                return eagle.isFlying() && eagle.distanceToSqr(x, y, z) > 4F;
            } else {
                return (!this.eagle.getNavigation().isDone()) && !this.eagle.isVehicle();
            }
        }

        public void start() {
            if (flightTarget) {
                eagle.setFlying(true);
                eagle.getMoveControl().setWantedPosition(x, y, z, 1F);
            } else {
                this.eagle.getNavigation().moveTo(this.x, this.y, this.z, 1F);
            }
        }

        public void stop() {
            this.eagle.getNavigation().stop();
            super.stop();
        }
    }

    class MoveHelper extends MovementController {
        private final EntitySeagull parentEntity;

        public MoveHelper(EntitySeagull bird) {
            super(bird);
            this.parentEntity = bird;
        }

        public void tick() {
            if (this.operation == MovementController.Action.MOVE_TO) {
                Vector3d vector3d = new Vector3d(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d5 = vector3d.length();
                if (d5 < 0.3) {
                    this.operation = MovementController.Action.WAIT;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
                } else {
                    double d1 = this.wantedY - this.parentEntity.getY();
                    float yScale = d1 > 0 || fallFlag ? 1F : 0.7F;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(vector3d.scale(speedModifier * 0.03D / d5)));
                    Vector3d vector3d1 = parentEntity.getDeltaMovement();
                    parentEntity.yRot = -((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * (180F / (float) Math.PI);
                    parentEntity.yBodyRot = parentEntity.yRot;

                }

            }
        }
    }

    private class AITargetItems extends CreatureAITargetItems {

        public AITargetItems(CreatureEntity creature, boolean checkSight, boolean onlyNearby, int tickThreshold, int radius) {
            super(creature, checkSight, onlyNearby, tickThreshold, radius);
            this.executionChance = 1;
        }

        public void stop() {
            super.stop();
            ((EntitySeagull) mob).aiItemFlag = false;
        }

        public boolean canUse() {
            return super.canUse() && !((EntitySeagull) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        public boolean canContinueToUse() {
            return super.canContinueToUse() && !((EntitySeagull) mob).isSitting() && (mob.getTarget() == null || !mob.getTarget().isAlive());
        }

        @Override
        protected void moveTo() {
            EntitySeagull crow = (EntitySeagull) mob;
            if (this.targetEntity != null) {
                crow.aiItemFlag = true;
                if (this.mob.distanceTo(targetEntity) < 2) {
                    crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                    crow.peck();
                }
                if (this.mob.distanceTo(this.targetEntity) > 8 || crow.isFlying()) {
                    crow.setFlying(true);
                    float f = (float) (crow.getX() - targetEntity.getX());
                    float f1 = 1.8F;
                    float f2 = (float) (crow.getZ() - targetEntity.getZ());
                    float xzDist = MathHelper.sqrt(f * f + f2 * f2);

                    if (!crow.canSee(targetEntity)) {
                        crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), 1 + crow.getY(), this.targetEntity.getZ(), 1.5F);
                    } else {
                        if (xzDist < 5) {
                            f1 = 0;
                        }
                        crow.getMoveControl().setWantedPosition(this.targetEntity.getX(), f1 + this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                    }
                } else {
                    this.mob.getNavigation().moveTo(this.targetEntity.getX(), this.targetEntity.getY(), this.targetEntity.getZ(), 1.5F);
                }
            }
        }

        @Override
        public void tick() {
            super.tick();
            moveTo();
        }
    }
}
