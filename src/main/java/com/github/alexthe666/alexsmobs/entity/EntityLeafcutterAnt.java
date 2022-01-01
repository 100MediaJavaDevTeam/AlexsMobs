package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.entity.ai.*;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.misc.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.github.alexthe666.alexsmobs.tileentity.TileEntityLeafcutterAnthill;
import com.github.alexthe666.citadel.animation.Animation;
import com.github.alexthe666.citadel.animation.AnimationHandler;
import com.github.alexthe666.citadel.animation.IAnimatedEntity;
import com.google.common.base.Predicates;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.VineBlock;
import net.minecraft.block.material.Material;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.monster.DrownedEntity;
import net.minecraft.entity.monster.GuardianEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTUtil;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.pathfinding.ClimberPathNavigator;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.pathfinding.PathNodeType;
import net.minecraft.tags.ItemTags;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntityLeafcutterAnt extends AnimalEntity implements IAngerable, IAnimatedEntity {

    public static final Animation ANIMATION_BITE = Animation.create(13);
    protected static final EntitySize QUEEN_SIZE = EntitySize.fixed(1.25F, 0.98F);
    public static final ResourceLocation QUEEN_LOOT = new ResourceLocation("alexsmobs", "entities/leafcutter_ant_queen");
    private static final DataParameter<Optional<BlockPos>> LEAF_HARVESTED_POS = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<BlockState>> LEAF_HARVESTED_STATE = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.BLOCK_STATE);
    private static final DataParameter<Boolean> HAS_LEAF = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Float> ANT_SCALE = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.FLOAT);
    private static final DataParameter<Direction> ATTACHED_FACE = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.DIRECTION);
    private static final DataParameter<Byte> CLIMBING = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.BYTE);
    private static final DataParameter<Boolean> QUEEN = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> ANGER_TIME = EntityDataManager.defineId(EntityLeafcutterAnt.class, DataSerializers.INT);
    private static final Direction[] HORIZONTALS = new Direction[]{Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
    private static final RangedInteger ANGRY_TIMER = TickRangeConverter.rangeOfSeconds(10, 20);
    public float attachChangeProgress = 0F;
    public float prevAttachChangeProgress = 0F;
    private Direction prevAttachDir = Direction.DOWN;
    @Nullable
    private EntityLeafcutterAnt caravanHead;
    @Nullable
    private EntityLeafcutterAnt caravanTail;
    private UUID lastHurtBy;
    @Nullable
    private BlockPos hivePos = null;
    private int stayOutOfHiveCountdown;
    private int animationTick;
    private Animation currentAnimation;
    private boolean isUpsideDownNavigator;
    private static final Ingredient TEMPTATION_ITEMS = Ingredient.of(AMItemRegistry.GONGYLIDIA);
    private int haveBabyCooldown = 0;
    public EntityLeafcutterAnt(EntityType type, World world) {
        super(type, world);
        this.setPathfindingMalus(PathNodeType.WATER, -1.0F);
        switchNavigator(true);

    }

    public void setTarget(@Nullable LivingEntity entitylivingbaseIn) {
        if(entitylivingbaseIn instanceof PlayerEntity && ((PlayerEntity) entitylivingbaseIn).isCreative()){
            return;
        }
        super.setTarget(entitylivingbaseIn);
    }

    @Nullable
    protected ResourceLocation getDefaultLootTable() {
        return this.isQueen() ? QUEEN_LOOT : super.getDefaultLootTable();
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.ARTHROPOD;
    }

    private void switchNavigator(boolean rightsideUp) {
        if (rightsideUp) {
            this.moveControl = new MovementController(this);
            this.navigation = new ClimberPathNavigator(this, level);
            this.isUpsideDownNavigator = false;
        } else {
            this.moveControl = new FlightMoveController(this, 0.6F, false);
            this.navigation = new DirectPathNavigator(this, level);
            this.isUpsideDownNavigator = true;
        }
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 6.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.MOVEMENT_SPEED, 0.25F).add(Attributes.ATTACK_DAMAGE, 2F);
    }

    private static boolean isSideSolid(IBlockReader reader, BlockPos pos, Entity entityIn, Direction direction) {
        return Block.isFaceFull(reader.getBlockState(pos).getCollisionShape(reader, pos, ISelectionContext.of(entityIn)), direction);
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(0, new SwimGoal(this));
        this.goalSelector.addGoal(1, new ReturnToHiveGoal());
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, false));
        this.goalSelector.addGoal(3, new TameableAITempt(this, 1.1D, TEMPTATION_ITEMS, false));
        this.goalSelector.addGoal(4, new LeafcutterAntAIFollowCaravan(this, 1D));
        this.goalSelector.addGoal(5, new LeafcutterAntAIForageLeaves(this));
        this.goalSelector.addGoal(6, new AnimalAIWanderRanged(this, 30, 1.0D, 25, 7));
        this.goalSelector.addGoal(7, new LookAtGoal(this, PlayerEntity.class, 6.0F));
        this.goalSelector.addGoal(8, new LookRandomlyGoal(this));
        this.targetSelector.addGoal(1, (new AngerGoal(this)).setAlertOthers(new Class[0]));
        this.targetSelector.addGoal(2, new ResetAngerGoal<>(this, true));
    }

    public EntitySize getDimensions(Pose poseIn) {
        return isQueen() && !isBaby() ? QUEEN_SIZE : super.getDimensions(poseIn);
    }

    public boolean canTrample(BlockState state, BlockPos pos, float fallDistance) {
        return false;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public Direction getAttachmentFacing() {
        return this.entityData.get(ATTACHED_FACE);
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new ClimberPathNavigator(this, worldIn);
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return isQueen() ? AMSoundRegistry.LEAFCUTTER_ANT_QUEEN_HURT : AMSoundRegistry.LEAFCUTTER_ANT_HURT;
    }

    protected SoundEvent getDeathSound() {
        return isQueen() ? AMSoundRegistry.LEAFCUTTER_ANT_QUEEN_HURT : AMSoundRegistry.LEAFCUTTER_ANT_HURT;
    }

    protected void playStepSound(BlockPos pos, BlockState state) {

    }

    private void pacifyAllNearby(){
        stopBeingAngry();
        List<EntityLeafcutterAnt> list = level.getEntitiesOfClass(EntityLeafcutterAnt.class, this.getBoundingBox().inflate(20D, 6.0D, 20D));
        for(EntityLeafcutterAnt ant : list){
            ant.stopBeingAngry();
        }
    }

    public ActionResultType mobInteract(PlayerEntity player, Hand hand) {
        ItemStack itemstack = player.getItemInHand(hand);
        Item item = itemstack.getItem();
        ActionResultType type = super.mobInteract(player, hand);
        if(type != ActionResultType.SUCCESS && item == AMItemRegistry.GONGYLIDIA){
            if(isQueen() && haveBabyCooldown == 0){
                int babies = 1 + random.nextInt(1);
                pacifyAllNearby();
                for(int i = 0; i < babies; i++){
                    EntityLeafcutterAnt leafcutterAnt = AMEntityRegistry.LEAFCUTTER_ANT.create(level);
                    leafcutterAnt.copyPosition(this);
                    leafcutterAnt.setAge(-24000);
                    if(!level.isClientSide){
                        level.broadcastEntityEvent(this, (byte)18);
                        level.addFreshEntity(leafcutterAnt);
                    }
                }
                if(!player.isCreative()){
                    itemstack.shrink(1);
                }
                haveBabyCooldown = 24000;
                this.setBaby(false);
            }else{
                pacifyAllNearby();
                if(!player.isCreative()){
                    itemstack.shrink(1);
                }
                level.broadcastEntityEvent(this, (byte)48);
                this.heal(3);
            }

            return ActionResultType.SUCCESS;

        }
        return type;
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 48) {
            for(int i = 0; i < 3; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(ParticleTypes.HAPPY_VILLAGER, this.getRandomX(1.0D), this.getRandomY() + 0.5D, this.getRandomZ(1.0D), d0, d1, d2);
            }
        } else {
            super.handleEntityEvent(id);
        }

    }

    public void tick() {
        this.prevAttachChangeProgress = this.attachChangeProgress;
        super.tick();
        if (this.isQueen() && this.getBbWidth() < QUEEN_SIZE.width) {
            this.refreshDimensions();
        }
        if (attachChangeProgress > 0F) {
            attachChangeProgress -= 0.25F;
        }
        this.maxUpStep = isQueen() ? 1F : 0.5F;
        Vector3d vector3d = this.getDeltaMovement();
        if (!this.level.isClientSide && !this.isQueen()) {
            this.setBesideClimbableBlock(this.horizontalCollision || this.verticalCollision && !this.isOnGround());
            if (this.isOnGround() || this.isInWaterOrBubble() || this.isInLava()) {
                this.entityData.set(ATTACHED_FACE, Direction.DOWN);
            } else  if (this.verticalCollision) {
                this.entityData.set(ATTACHED_FACE, Direction.UP);
            }else {
                boolean flag = false;
                Direction closestDirection = Direction.DOWN;
                double closestDistance = 100;
                for (Direction dir : HORIZONTALS) {
                    BlockPos antPos = new BlockPos(MathHelper.floor(this.getX()), MathHelper.floor(this.getY()), MathHelper.floor(this.getZ()));
                    BlockPos offsetPos = antPos.relative(dir);
                    Vector3d offset = Vector3d.atCenterOf(offsetPos);
                    if (closestDistance > this.position().distanceTo(offset) && level.loadedAndEntityCanStandOnFace(offsetPos, this, dir.getOpposite())) {
                        closestDistance = this.position().distanceTo(offset);
                        closestDirection = dir;
                    }
                }
                this.entityData.set(ATTACHED_FACE, closestDirection);
            }
        }
        boolean flag = false;
        if (this.getAttachmentFacing() != Direction.DOWN) {
            if(this.getAttachmentFacing() == Direction.UP){
                this.setDeltaMovement(this.getDeltaMovement().add(0, 1, 0));
            }else{
                if (!this.horizontalCollision && this.getAttachmentFacing() != Direction.UP) {
                    Vector3d vec = Vector3d.atLowerCornerOf(this.getAttachmentFacing().getNormal());
                    this.setDeltaMovement(this.getDeltaMovement().add(vec.normalize().multiply(0.1F, 0.1F, 0.1F)));
                }
                if (!this.onGround && vector3d.y < 0.0D) {
                    this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.5D, 1.0D));
                    flag = true;
                }
            }
        }
        if(this.getAttachmentFacing() == Direction.UP) {
            this.setNoGravity(true);
            this.setDeltaMovement(vector3d.multiply(0.7D, 1D, 0.7D));
        }else{
            this.setNoGravity(false);
        }
        if (!flag) {
            if (this.onClimbable()) {
                this.setDeltaMovement(vector3d.multiply(1.0D, 0.4D, 1.0D));
            }
        }
        if (prevAttachDir != this.getAttachmentFacing()) {
            attachChangeProgress = 1F;
        }
        this.prevAttachDir = this.getAttachmentFacing();
        if (!this.level.isClientSide) {
            if (this.getAttachmentFacing() == Direction.UP && !this.isUpsideDownNavigator) {
                switchNavigator(false);
            }
            if (this.getAttachmentFacing() != Direction.UP && this.isUpsideDownNavigator) {
                switchNavigator(true);
            }
            if (this.stayOutOfHiveCountdown > 0) {
                --this.stayOutOfHiveCountdown;
            }

            if (this.tickCount % 20 == 0 && !this.isHiveValid()) {
                this.hivePos = null;
            }
            LivingEntity attackTarget = this.getTarget();
            if (attackTarget != null && distanceTo(attackTarget) < attackTarget.getBbWidth() + this.getBbWidth() + 1 && this.canSee(attackTarget)) {
                if (this.getAnimation() == ANIMATION_BITE && this.getAnimationTick() == 6) {
                    float damage = (float) ((int) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
                    attackTarget.hurt(DamageSource.mobAttack(this), damage);
                }
            }
        }
        AnimationHandler.INSTANCE.updateAnimations(this);
    }

    private boolean isClimeableFromSide(BlockPos offsetPos, Direction opposite) {
        return false;
    }

    private boolean isHiveValid() {
        if (!this.hasHive()) {
            return false;
        } else {
            TileEntity tileentity = this.level.getBlockEntity(this.hivePos);
            return tileentity instanceof TileEntityLeafcutterAnthill;
        }
    }

    protected void onInsideBlock(BlockState state) {

    }

    public boolean onClimbable() {
        return this.isBesideClimbableBlock();
    }

    public boolean isBesideClimbableBlock() {
        return (this.entityData.get(CLIMBING) & 1) != 0;
    }

    public void setBesideClimbableBlock(boolean climbing) {
        byte b0 = this.entityData.get(CLIMBING);
        if (climbing) {
            b0 = (byte) (b0 | 1);
        } else {
            b0 = (byte) (b0 & -2);
        }

        this.entityData.set(CLIMBING, b0);
    }

    public int getRemainingPersistentAngerTime() {
        return this.entityData.get(ANGER_TIME);
    }

    public void setRemainingPersistentAngerTime(int time) {
        this.entityData.set(ANGER_TIME, time);
    }

    public UUID getPersistentAngerTarget() {
        return this.lastHurtBy;
    }

    public void setPersistentAngerTarget(@Nullable UUID target) {
        this.lastHurtBy = target;
    }

    public void startPersistentAngerTimer() {
        this.setRemainingPersistentAngerTime(ANGRY_TIMER.randomValue(this.random));
    }

    protected void customServerAiStep() {
        if (!this.level.isClientSide) {
            this.updatePersistentAnger((ServerWorld)this.level, false);
        }
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CLIMBING, (byte) 0);
        this.entityData.define(LEAF_HARVESTED_POS, Optional.empty());
        this.entityData.define(LEAF_HARVESTED_STATE, Optional.empty());
        this.entityData.define(HAS_LEAF, false);
        this.entityData.define(QUEEN, false);
        this.entityData.define(ATTACHED_FACE, Direction.DOWN);
        this.entityData.define(ANT_SCALE, 1.0F);
        this.entityData.define(ANGER_TIME, 0);
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setAntScale(0.75F + random.nextFloat() * 0.3F);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    public float getAntScale() {
        return this.entityData.get(ANT_SCALE);
    }

    public void setAntScale(float scale) {
        this.entityData.set(ANT_SCALE, scale);
    }


    public BlockPos getHarvestedPos() {
        return this.entityData.get(LEAF_HARVESTED_POS).orElse(null);
    }

    public void setLeafHarvestedPos(BlockPos harvestedPos) {
        this.entityData.set(LEAF_HARVESTED_POS, Optional.ofNullable(harvestedPos));
    }

    public BlockState getHarvestedState() {
        return this.entityData.get(LEAF_HARVESTED_STATE).orElse(null);
    }

    public void setLeafHarvestedState(BlockState state) {
        this.entityData.set(LEAF_HARVESTED_STATE, Optional.ofNullable(state));
    }

    public boolean hasLeaf() {
        return this.entityData.get(HAS_LEAF).booleanValue();
    }

    public void setLeaf(boolean leaf) {
        this.entityData.set(HAS_LEAF, Boolean.valueOf(leaf));
    }

    public boolean isQueen() {
        return this.entityData.get(QUEEN).booleanValue();
    }

    public void setQueen(boolean queen) {
        boolean prev = isQueen();
        if (!prev && queen) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(36.0D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(6.0D);
            this.setHealth(36F);
        } else {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(6.0D);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0D);
        }
        this.entityData.set(QUEEN, Boolean.valueOf(queen));
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(ATTACHED_FACE, Direction.from3DDataValue(compound.getByte("AttachFace")));
        this.setLeaf(compound.getBoolean("Leaf"));
        this.setQueen(compound.getBoolean("Queen"));
        this.setAntScale(compound.getFloat("AntScale"));
        BlockState blockstate = null;
        if (compound.contains("HarvestedLeafState", 10)) {
            blockstate = NBTUtil.readBlockState(compound.getCompound("HarvestedLeafState"));
            if (blockstate.isAir()) {
                blockstate = null;
            }
        }
        this.stayOutOfHiveCountdown = compound.getInt("CannotEnterHiveTicks");
        this.haveBabyCooldown = compound.getInt("BabyCooldown");
        this.hivePos = null;
        if (compound.contains("HivePos")) {
            this.hivePos = NBTUtil.readBlockPos(compound.getCompound("HivePos"));
        }
        this.setLeafHarvestedState(blockstate);
        if (compound.contains("HLPX")) {
            int i = compound.getInt("HLPX");
            int j = compound.getInt("HLPY");
            int k = compound.getInt("HLPZ");
            this.entityData.set(LEAF_HARVESTED_POS, Optional.of(new BlockPos(i, j, k)));
        } else {
            this.entityData.set(LEAF_HARVESTED_POS, Optional.empty());
        }
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        compound.putByte("AttachFace", (byte) this.entityData.get(ATTACHED_FACE).get3DDataValue());
        compound.putBoolean("Leaf", this.hasLeaf());
        compound.putBoolean("Queen", this.isQueen());
        compound.putFloat("AntScale", this.getAntScale());
        BlockState blockstate = this.getHarvestedState();
        if (blockstate != null) {
            compound.put("HarvestedLeafState", NBTUtil.writeBlockState(blockstate));
        }
        if (this.hasHive()) {
            compound.put("HivePos", NBTUtil.writeBlockPos(this.getHivePos()));
        }
        compound.putInt("CannotEnterHiveTicks", this.stayOutOfHiveCountdown);
        compound.putInt("BabyCooldown", this.haveBabyCooldown);
        BlockPos blockpos = this.getHarvestedPos();
        if (blockpos != null) {
            compound.putInt("HLPX", blockpos.getX());
            compound.putInt("HLPY", blockpos.getY());
            compound.putInt("HLPZ", blockpos.getZ());
        }

    }

    public void setStayOutOfHiveCountdown(int p_226450_1_) {
        this.stayOutOfHiveCountdown = p_226450_1_;
    }

    private boolean isHiveNearFire() {
        if (this.hivePos == null) {
            return false;
        } else {
            TileEntity tileentity = this.level.getBlockEntity(this.hivePos);
            return tileentity instanceof TileEntityLeafcutterAnthill && ((TileEntityLeafcutterAnthill) tileentity).isNearFire();
        }
    }

    private boolean doesHiveHaveSpace(BlockPos pos) {
        TileEntity tileentity = this.level.getBlockEntity(pos);
        if (tileentity instanceof TileEntityLeafcutterAnthill) {
            return !((TileEntityLeafcutterAnthill) tileentity).isFullOfAnts();
        } else {
            return false;
        }
    }

    public boolean hasHive() {
        return this.hivePos != null;
    }

    @Nullable
    public BlockPos getHivePos() {
        return this.hivePos;
    }


    public void leaveCaravan() {
        if (this.caravanHead != null) {
            this.caravanHead.caravanTail = null;
        }

        this.caravanHead = null;
    }

    public void joinCaravan(EntityLeafcutterAnt caravanHeadIn) {
        this.caravanHead = caravanHeadIn;
        this.caravanHead.caravanTail = this;
    }

    public boolean hasCaravanTrail() {
        return this.caravanTail != null;
    }

    public boolean inCaravan() {
        return this.caravanHead != null;
    }

    @Nullable
    public EntityLeafcutterAnt getCaravanHead() {
        return this.caravanHead;
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld serverWorld, AgeableEntity ageableEntity) {
        return null;
    }

    public boolean shouldLeadCaravan() {
        return !this.hasLeaf();
    }

    @Override
    public void calculateEntityAnimation(LivingEntity p_233629_1_, boolean p_233629_2_) {
        p_233629_1_.animationSpeedOld = p_233629_1_.animationSpeed;
        double d0 = p_233629_1_.getX() - p_233629_1_.xo;
        double d1 = (p_233629_1_.getY() - p_233629_1_.yo) * 2.0F;
        double d2 = p_233629_1_.getZ() - p_233629_1_.zo;
        float f = MathHelper.sqrt(d0 * d0 + d1 * d1 + d2 * d2) * 4.0F;
        if (f > 1.0F) {
            f = 1.0F;
        }

        p_233629_1_.animationSpeed += (f - p_233629_1_.animationSpeed) * 0.4F;
        p_233629_1_.animationPosition += p_233629_1_.animationSpeed;
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
        return new Animation[]{ANIMATION_BITE};
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
        this.setAnimation(ANIMATION_BITE);
        return true;
    }

    private class ReturnToHiveGoal extends Goal {

        private int searchCooldown = 1;
        private BlockPos hivePos;
        private int approachTime = 0;

        public ReturnToHiveGoal() {
        }

        @Override
        public boolean canUse() {
            if(EntityLeafcutterAnt.this.stayOutOfHiveCountdown > 0){
                return false;
            }
            if (EntityLeafcutterAnt.this.hasLeaf() || EntityLeafcutterAnt.this.isQueen()) {
                searchCooldown--;
                BlockPos hive = EntityLeafcutterAnt.this.hivePos;
                if (hive != null && EntityLeafcutterAnt.this.level.getBlockEntity(hive) instanceof TileEntityLeafcutterAnthill) {
                    hivePos = hive;
                    return true;
                }
                if (searchCooldown <= 0) {
                    searchCooldown = 400;
                    PointOfInterestManager pointofinterestmanager = ((ServerWorld) level).getPoiManager();
                    Stream<BlockPos> stream = pointofinterestmanager.findAll(AMPointOfInterestRegistry.LEAFCUTTER_ANT_HILL.getPredicate(), Predicates.alwaysTrue(), EntityLeafcutterAnt.this.blockPosition(), 100, PointOfInterestManager.Status.ANY);
                    List<BlockPos> listOfHives = stream.collect(Collectors.toList());
                    BlockPos ret = null;
                    for (BlockPos pos : listOfHives) {
                        if (ret == null || pos.distSqr(EntityLeafcutterAnt.this.blockPosition()) < ret.distSqr(EntityLeafcutterAnt.this.blockPosition())) {
                            ret = pos;
                        }
                    }
                    hivePos = ret;
                    EntityLeafcutterAnt.this.hivePos = ret;
                    return hivePos != null;
                }
            }
            return false;
        }

        public boolean canContinueToUse() {
            return hivePos != null && EntityLeafcutterAnt.this.distanceToSqr(Vector3d.upFromBottomCenterOf(hivePos, 1)) > 1F;
        }

        public void stop() {
            this.hivePos = null;
            this.searchCooldown = 20;
            this.approachTime = 0;
        }

        public void tick() {
            double dist = EntityLeafcutterAnt.this.distanceToSqr(Vector3d.upFromBottomCenterOf(hivePos, 1));
            if (dist < 1.2F && EntityLeafcutterAnt.this.getBlockPosBelowThatAffectsMyMovement().equals(hivePos)) {
                TileEntity tileentity = EntityLeafcutterAnt.this.level.getBlockEntity(hivePos);
                if (tileentity instanceof TileEntityLeafcutterAnthill) {
                    TileEntityLeafcutterAnthill beehivetileentity = (TileEntityLeafcutterAnthill) tileentity;
                    beehivetileentity.tryEnterHive(EntityLeafcutterAnt.this, EntityLeafcutterAnt.this.hasLeaf());
                }
            }
            if (dist < 16) {
                approachTime++;
                if(dist < (approachTime < 200 ? 2 : 10) && EntityLeafcutterAnt.this.getY() >= hivePos.getY()){
                    if(EntityLeafcutterAnt.this.getAttachmentFacing() != Direction.DOWN){
                        EntityLeafcutterAnt.this.setDeltaMovement(EntityLeafcutterAnt.this.getDeltaMovement().add(0, 0.1, 0));
                    }
                   EntityLeafcutterAnt.this.getMoveControl().setWantedPosition((double) hivePos.getX() + 0.5F, (double) hivePos.getY() + 1.5F, (double) hivePos.getZ() + 0.5F, 1.0D);
                }
                EntityLeafcutterAnt.this.navigation.resetMaxVisitedNodesMultiplier();
                EntityLeafcutterAnt.this.navigation.moveTo((double) hivePos.getX() + 0.5F, (double) hivePos.getY() + 1.6F, (double) hivePos.getZ() + 0.5F, 1.0D);
            } else {
                startMovingToFar(this.hivePos);
            }
        }

        private boolean startMovingToFar(BlockPos pos) {
            EntityLeafcutterAnt.this.navigation.setMaxVisitedNodesMultiplier(10.0F);
            EntityLeafcutterAnt.this.navigation.moveTo(pos.getX(), pos.getY(), pos.getZ(), 1.0D);
            return EntityLeafcutterAnt.this.navigation.getPath() != null && EntityLeafcutterAnt.this.navigation.getPath().canReach();
        }

    }

    class AngerGoal extends HurtByTargetGoal {
        AngerGoal(EntityLeafcutterAnt beeIn) {
            super(beeIn);
            this.setAlertOthers(EntityLeafcutterAnt.class);
        }

        /**
         * Returns whether an in-progress EntityAIBase should continue executing
         */
        public boolean canContinueToUse() {
            return EntityLeafcutterAnt.this.isAngry() && super.canContinueToUse();
        }

        protected void alertOther(MobEntity mobIn, LivingEntity targetIn) {
            if (mobIn instanceof EntityLeafcutterAnt && this.mob.canSee(targetIn)) {
                mobIn.setTarget(targetIn);
            }

        }
    }
}
