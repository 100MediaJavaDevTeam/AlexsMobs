package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.entity.ai.DirectPathNavigator;
import com.github.alexthe666.alexsmobs.entity.ai.EntityAINearestTarget3D;
import com.github.alexthe666.alexsmobs.entity.ai.FlightMoveController;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.pathfinding.PathNavigator;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.*;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.world.*;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerBossInfo;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nullable;
import java.util.*;

import net.minecraft.entity.ai.goal.Goal.Flag;

public class EntityVoidWorm extends MonsterEntity {

    public static final ResourceLocation SPLITTER_LOOT = new ResourceLocation("alexsmobs", "entities/void_worm_splitter");
    private static final DataParameter<Optional<UUID>> CHILD_UUID = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Optional<UUID>> SPLIT_FROM_UUID = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Integer> SEGMENT_COUNT = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.INT);
    private static final DataParameter<Integer> JAW_TICKS = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.INT);
    private static final DataParameter<Float> WORM_ANGLE = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> SPEEDMOD = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.FLOAT);
    private static final DataParameter<Boolean> SPLITTER = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> PORTAL_TICKS = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.INT);
    private final ServerBossInfo bossInfo = (ServerBossInfo) (new ServerBossInfo(this.getDisplayName(), BossInfo.Color.BLUE, BossInfo.Overlay.PROGRESS)).setDarkenScreen(true);
    public float prevWormAngle;
    public float prevJawProgress;
    public float jawProgress;
    public Vector3d teleportPos = null;
    public EntityVoidPortal portalTarget = null;
    public boolean fullyThrough = true;
    public boolean updatePostSummon = false;
    private int makePortalCooldown = 0;
    private int stillTicks = 0;
    private int blockBreakCounter;
    private int makeIdlePortalCooldown = 200 + random.nextInt(800);

    protected EntityVoidWorm(EntityType<? extends MonsterEntity> type, World worldIn) {
        super(type, worldIn);
        this.xpReward = 10;
        this.moveControl = new FlightMoveController(this, 1F, false, true);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.VOID_WORM_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.VOID_WORM_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.VOID_WORM_HURT;
    }

    protected float getSoundVolume() {
        return isSilent() ? 0 : 5;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.voidWormSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    public static boolean canVoidWormSpawn(EntityType animal, IWorld worldIn, SpawnReason reason, BlockPos pos, Random random) {
        return true;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, AMConfig.voidWormMaxHealth).add(Attributes.ARMOR, 4.0D).add(Attributes.FOLLOW_RANGE, 256.0D).add(Attributes.MOVEMENT_SPEED, 0.3F).add(Attributes.ATTACK_DAMAGE, 5);
    }

    @Nullable
    protected ResourceLocation getDefaultLootTable() {
        return this.isSplitter() ? SPLITTER_LOOT : super.getDefaultLootTable();
    }

    public void kill() {
        this.remove();
    }

    public void die(DamageSource cause) {
       super.die(cause);
       if(!level.isClientSide && !this.isSplitter()){
           if(cause != null && cause.getEntity() instanceof ServerPlayerEntity) {
               AMAdvancementTriggerRegistry.VOID_WORM_SLAY_HEAD.trigger((ServerPlayerEntity) cause.getEntity());
           }
       }
    }

    @Override
    public ItemEntity spawnAtLocation(ItemStack stack) {
        ItemEntity itementity = this.spawnAtLocation(stack, 0.0F);
        if (itementity != null) {
            itementity.setNoGravity(true);
            itementity.setGlowing(true);
            itementity.setExtendedLifetime();
        }
        return itementity;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.FALL || source == DamageSource.DROWN || source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || source == DamageSource.LAVA || source == DamageSource.OUT_OF_WORLD || source.isFire() || super.isInvulnerableTo(source);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new EntityVoidWorm.AIEnterPortal());
        this.goalSelector.addGoal(2, new EntityVoidWorm.AIAttack());
        this.goalSelector.addGoal(3, new EntityVoidWorm.AIFlyIdle());
        this.targetSelector.addGoal(1, new EntityAINearestTarget3D(this, PlayerEntity.class, 10, false, true, null));
        this.targetSelector.addGoal(2, new EntityAINearestTarget3D(this, EnderDragonEntity.class, 10, false, true, null));
    }

    protected PathNavigator createNavigation(World worldIn) {
        return new DirectPathNavigator(this, level);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("ChildUUID")) {
            this.setChildId(compound.getUUID("ChildUUID"));
        }
        this.setWormSpeed(compound.getFloat("WormSpeed"));
        this.setSplitter(compound.getBoolean("Splitter"));
        this.setPortalTicks(compound.getInt("PortalTicks"));
        this.makeIdlePortalCooldown = compound.getInt("MakePortalTime");
        this.makePortalCooldown = compound.getInt("MakePortalCooldown");
        if (this.hasCustomName()) {
            this.bossInfo.setName(this.getDisplayName());
        }

    }

    public void setCustomName(@Nullable ITextComponent name) {
        super.setCustomName(name);
        this.bossInfo.setName(this.getDisplayName());
    }

    public boolean isNoGravity() {
        return true;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        if (this.getChildId() != null) {
            compound.putUUID("ChildUUID", this.getChildId());
        }
        compound.putInt("PortalTicks", getPortalTicks());
        compound.putInt("MakePortalTime", makeIdlePortalCooldown);
        compound.putInt("MakePortalCooldown", makePortalCooldown);
        compound.putFloat("WormSpeed", getWormSpeed());
        compound.putBoolean("Splitter", isSplitter());
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
    }

    public boolean canBeLeashed(PlayerEntity player) {
        return true;
    }


    public void tick() {
        super.tick();
        prevWormAngle = this.getWormAngle();
        prevJawProgress = this.jawProgress;
        float threshold = 0.05F;
        if (this.isSplitter()) {
            this.xpReward = 10;
        } else {
            this.xpReward = 70;
        }
        if (this.yRotO - this.yRot > threshold) {
            this.setWormAngle(this.getWormAngle() + 15);
        } else if (this.yRotO - this.yRot < -threshold) {
            this.setWormAngle(this.getWormAngle() - 15);
        } else if (this.getWormAngle() > 0) {
            this.setWormAngle(Math.max(this.getWormAngle() - 20, 0));
        } else if (this.getWormAngle() < 0) {
            this.setWormAngle(Math.min(this.getWormAngle() + 20, 0));
        }
        if (!level.isClientSide) {
            if (!fullyThrough) {
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.9F, 0.9F, 0.9F).add(0, -0.01, 0));
            } else {
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.01, 0));
            }
        }
        if (Math.abs(xo - this.getX()) < 0.01F && Math.abs(yo - this.getY()) < 0.01F && Math.abs(zo - this.getZ()) < 0.01F) {
            stillTicks++;
        } else {
            stillTicks = 0;
        }
        if (stillTicks > 40 && makePortalCooldown == 0) {
            createStuckPortal();
        }
        if (makePortalCooldown > 0) {
            makePortalCooldown--;
        }
        if (makeIdlePortalCooldown > 0) {
            makeIdlePortalCooldown--;
        }
        if (makeIdlePortalCooldown == 0 && random.nextInt(100) == 0) {
            this.createPortalRandomDestination();
            makeIdlePortalCooldown = 200 + random.nextInt(1000);
        }
        if (this.entityData.get(JAW_TICKS) > 0) {
            if (this.jawProgress < 5) {
                jawProgress++;
            }
            this.entityData.set(JAW_TICKS, this.entityData.get(JAW_TICKS) - 1);
        } else {
            if (this.jawProgress > 0) {
                jawProgress--;
            }
        }
        if (this.isAlive()) {
            for (Entity entity : this.level.getEntitiesOfClass(LivingEntity.class, this.getBoundingBox().inflate(2.0D), null)) {
                if (!entity.is(this) && !(entity instanceof EntityVoidWormPart) && !entity.isAlliedTo(this) && entity != this) {
                    launch(entity, false);
                }
            }
            maxUpStep = 2;
        }
        yBodyRot = yRot;
        float f2 = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
        this.xRot = f2;
        this.maxUpStep = 2;
        if (!level.isClientSide) {
            Entity child = getChild();
            if (child == null) {
                LivingEntity partParent = this;
                int tailstart = Math.min(3 + random.nextInt(2), getSegmentCount());
                int segments = getSegmentCount();
                for (int i = 0; i < segments; i++) {
                    float scale = 1F + (i / (float) segments) * 0.5F;
                    boolean tail = false;
                    if (i >= segments - tailstart) {
                        tail = true;
                        scale = scale * 0.85F;
                    }
                    EntityVoidWormPart part = new EntityVoidWormPart(AMEntityRegistry.VOID_WORM_PART, partParent,  1.0F + (scale * (tail ? 0.65F : 0.3F)) + (i == 0 ? 0.8F : 0), 180, i == 0 ? -0.0F : i == segments - tailstart ? -0.3F : 0);
                    part.setParent(partParent);
                    if (updatePostSummon) {
                        part.setPortalTicks(i * 2);
                    }
                    part.setBodyIndex(i);
                    part.setTail(tail);
                    part.setWormScale(scale);
                    if (partParent == this) {
                        this.setChildId(part.getUUID());
                    } else if (partParent instanceof EntityVoidWormPart) {
                        ((EntityVoidWormPart) partParent).setChildId(part.getUUID());
                    }
                    part.setInitialPartPos(this);
                    partParent = part;
                    level.addFreshEntity(part);
                }
            }
        }
        if (this.getPortalTicks() > 0) {
            this.setPortalTicks(this.getPortalTicks() - 1);
            if (this.getPortalTicks() == 2 && teleportPos != null) {
                this.setPos(teleportPos.x, teleportPos.y, teleportPos.z);
                teleportPos = null;
            }
        }
        if (this.portalTarget != null && this.portalTarget.getLifespan() < 5) {
            this.portalTarget = null;
        }
        this.bossInfo.setPercent(this.getHealth() / this.getMaxHealth());
        breakBlock();
        if (updatePostSummon) {
            updatePostSummon = false;
        }
        if (!this.isSilent() && !level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte) 67);
        }
    }

    public void setMaxHealth(double maxHealth, boolean heal){
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(maxHealth);
        if(heal){
            this.heal((float)maxHealth);
        }
    }

    public void startSeenByPlayer(ServerPlayerEntity player) {
        super.startSeenByPlayer(player);
        this.bossInfo.addPlayer(player);
    }

    public void stopSeenByPlayer(ServerPlayerEntity player) {
        super.stopSeenByPlayer(player);
        this.bossInfo.removePlayer(player);
    }

    public void teleportTo(Vector3d vec) {
        this.setPortalTicks(10);
        teleportPos = vec;
        fullyThrough = false;
        if (this.getChild() instanceof EntityVoidWormPart) {
            ((EntityVoidWormPart) this.getChild()).teleportTo(this.position(), teleportPos);
        }
    }

    private void launch(Entity e, boolean huge) {
        if (e.isOnGround()) {
            double d0 = e.getX() - this.getX();
            double d1 = e.getZ() - this.getZ();
            double d2 = Math.max(d0 * d0 + d1 * d1, 0.001D);
            float f = huge ? 2F : 0.5F;
            e.push(d0 / d2 * f, huge ? 0.5D : 0.2F, d1 / d2 * f);
        }
    }

    public void resetWormScales() {
        if (!level.isClientSide) {
            Entity child = getChild();
            if (child == null) {
                LivingEntity nextPart = this;
                int tailstart = Math.min(3 + random.nextInt(2), getSegmentCount());
                int segments = getSegmentCount();
                int i = 0;
                while (nextPart instanceof EntityVoidWormPart) {
                    EntityVoidWormPart part = ((EntityVoidWormPart) ((EntityVoidWormPart) nextPart).getChild());
                    i++;
                    float scale = 1F + (i / (float) segments) * 0.5F;
                    boolean tail = i >= segments - tailstart;
                    part.setTail(tail);
                    part.setWormScale(scale);
                    part.radius = 1.0F + (scale * (tail ? 0.65F : 0.3F)) + (i == 0 ? 0.8F : 0F);
                    part.offsetY = i == 0 ? -0.0F : i == segments - tailstart ? -0.3F : 0;
                    nextPart = part;
                }
            }
        }
    }

    @Nullable
    public ILivingEntityData finalizeSpawn(IServerWorld worldIn, DifficultyInstance difficultyIn, SpawnReason
            reason, @Nullable ILivingEntityData spawnDataIn, @Nullable CompoundNBT dataTag) {
        this.setSegmentCount(25 + random.nextInt(15));
        this.xRot = 0.0F;
        this.setMaxHealth(AMConfig.voidWormMaxHealth, true);
        return super.finalizeSpawn(worldIn, difficultyIn, reason, spawnDataIn, dataTag);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(SPLIT_FROM_UUID, Optional.empty());
        this.entityData.define(CHILD_UUID, Optional.empty());
        this.entityData.define(SEGMENT_COUNT, 10);
        this.entityData.define(JAW_TICKS, 0);
        this.entityData.define(WORM_ANGLE, 0F);
        this.entityData.define(SPEEDMOD, 1F);
        this.entityData.define(SPLITTER, false);
        this.entityData.define(PORTAL_TICKS, 0);
    }


    public float getWormAngle() {
        return this.entityData.get(WORM_ANGLE);
    }

    public void setWormAngle(float progress) {
        this.entityData.set(WORM_ANGLE, progress);
    }

    public float getWormSpeed() {
        return this.entityData.get(SPEEDMOD);
    }

    public void setWormSpeed(float progress) {
        if (getWormSpeed() != progress) {
            moveControl = new FlightMoveController(this, progress, false, true);
        }
        this.entityData.set(SPEEDMOD, progress);
    }

    public boolean isSplitter() {
        return this.entityData.get(SPLITTER);
    }

    public void setSplitter(boolean splitter) {
        this.entityData.set(SPLITTER, splitter);
    }

    public void openMouth(int time) {
        this.entityData.set(JAW_TICKS, time);
    }

    public boolean isMouthOpen() {
        return entityData.get(JAW_TICKS) >= 5F;
    }

    @Nullable
    public UUID getChildId() {
        return this.entityData.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.entityData.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public UUID getSplitFromUUID() {
        return this.entityData.get(SPLIT_FROM_UUID).orElse(null);
    }

    public void setSplitFromUuid(@Nullable UUID uniqueId) {
        this.entityData.set(SPLIT_FROM_UUID, Optional.ofNullable(uniqueId));
    }

    public int getPortalTicks() {
        return this.entityData.get(PORTAL_TICKS).intValue();
    }

    public void setPortalTicks(int ticks) {
        this.entityData.set(PORTAL_TICKS, Integer.valueOf(ticks));
    }

    public int getSegmentCount() {
        return this.entityData.get(SEGMENT_COUNT).intValue();
    }

    public void setSegmentCount(int command) {
        this.entityData.set(SEGMENT_COUNT, Integer.valueOf(command));
    }

    public void pushEntities() {
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        entities.stream().filter(entity -> !(entity instanceof EntityVoidWormPart) && entity.isPushable()).forEach(entity -> entity.push(this));
    }

    @Override
    public void push(Entity entityIn) {

    }

    public void createStuckPortal() {
        if (this.getTarget() != null) {
            createPortal(this.getTarget().position().add(random.nextInt(8) - 4, 2 + random.nextInt(3), random.nextInt(8) - 4));
        } else {
            Vector3d vec = Vector3d.atCenterOf(level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, this.blockPosition().above(random.nextInt(10) + 10)));
            createPortal(vec);
        }
    }

    public void createPortal(Vector3d to) {
        createPortal(this.position().add(this.getLookAngle().scale(20)), to, null);
    }


    public void createPortalRandomDestination() {
        Vector3d vec = null;
        for (int i = 0; i < 15; i++) {
            BlockPos pos = new BlockPos(this.getX() + random.nextInt(60) - 30, 0, this.getZ() + random.nextInt(60) - 30);
            BlockPos height = level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING, pos);
            if(height.getY() < 10){
                height = height.above(50 + random.nextInt(50));
            }else{
                height = height.above(random.nextInt(30));
            }
            if (level.isEmptyBlock(height)) {
                vec = Vector3d.atBottomCenterOf(height);
            }
        }
        if (vec != null) {
            createPortal(this.position().add(this.getLookAngle().scale(20)), vec, null);
        }
    }

    public void createPortal(Vector3d from, Vector3d to, @Nullable Direction outDir) {
        if (!level.isClientSide && portalTarget == null) {
            Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
            RayTraceResult result = this.level.clip(new RayTraceContext(Vector3d, from, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this));
            Vector3d vec = result.getLocation() != null ? result.getLocation() : this.position();
            if (result instanceof BlockRayTraceResult) {
                BlockRayTraceResult result1 = (BlockRayTraceResult) result;
                vec = vec.add(net.minecraft.util.math.vector.Vector3d.atLowerCornerOf(result1.getDirection().getNormal()));
            }
            EntityVoidPortal portal = AMEntityRegistry.VOID_PORTAL.create(level);
            portal.setPos(vec.x, vec.y, vec.z);
            Vector3d dirVec = vec.subtract(this.position());
            Direction dir = Direction.getNearest(dirVec.x, dirVec.y, dirVec.z);
            portal.setAttachmentFacing(dir);
            portal.setLifespan(10000);
            if (!level.isClientSide) {
                level.addFreshEntity(portal);
            }
            portalTarget = portal;
            portal.setDestination(new BlockPos(to.x, to.y, to.z), outDir);
            makePortalCooldown = 300;
        }
    }

    public void resetPortalLogic() {
        portalTarget = null;
    }

    public boolean isPushable() {
        return false;
    }

    public void breakBlock() {
        if (this.blockBreakCounter > 0) {
            --this.blockBreakCounter;
            return;
        }
        boolean flag = false;
        if (!level.isClientSide && this.blockBreakCounter == 0 && net.minecraftforge.event.ForgeEventFactory.getMobGriefingEvent(level, this)) {
            for (int a = (int) Math.round(this.getBoundingBox().minX); a <= (int) Math.round(this.getBoundingBox().maxX); a++) {
                for (int b = (int) Math.round(this.getBoundingBox().minY) - 1; (b <= (int) Math.round(this.getBoundingBox().maxY) + 1) && (b <= 127); b++) {
                    for (int c = (int) Math.round(this.getBoundingBox().minZ); c <= (int) Math.round(this.getBoundingBox().maxZ); c++) {
                        BlockPos pos = new BlockPos(a, b, c);
                        BlockState state = level.getBlockState(pos);
                        FluidState fluidState = level.getFluidState(pos);
                        Block block = state.getBlock();
                        if (!state.isAir() && !state.getShape(level, pos).isEmpty() && BlockTags.getAllTags().getTag(AMTagRegistry.VOID_WORM_BREAKABLES).contains(state.getBlock()) && fluidState.isEmpty()) {
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
            blockBreakCounter = 10;
        }
    }


    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());

        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    public Vector3d getBlockInViewAway(Vector3d fleePos, float radiusAdd) {
        float radius = (0.75F * (0.7F * 6) * -3 - this.getRandom().nextInt(24)) * radiusAdd;
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getGround(radialPos);
        int distFromGround = (int) this.getY() - ground.getY();
        int flightHeight = 10 + this.getRandom().nextInt(20);
        BlockPos newPos = ground.above(distFromGround > 8 ? flightHeight : this.getRandom().nextInt(10) + 15);
        if (!this.isTargetBlocked(Vector3d.atCenterOf(newPos)) && this.distanceToSqr(Vector3d.atCenterOf(newPos)) > 1) {
            return Vector3d.atCenterOf(newPos);
        }
        return null;
    }

    public Vector3d getBlockInViewAwaySlam(Vector3d fleePos, int slamHeight) {
        float radius = 3 + random.nextInt(3);
        float neg = this.getRandom().nextBoolean() ? 1 : -1;
        float renderYawOffset = this.yBodyRot;
        float angle = (0.01745329251F * renderYawOffset) + 3.15F + (this.getRandom().nextFloat() * neg);
        double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
        double extraZ = radius * MathHelper.cos(angle);
        BlockPos radialPos = new BlockPos(fleePos.x() + extraX, 0, fleePos.z() + extraZ);
        BlockPos ground = getHeighestAirAbove(radialPos, slamHeight);
        if (!this.isTargetBlocked(Vector3d.atCenterOf(ground)) && this.distanceToSqr(Vector3d.atCenterOf(ground)) > 1) {
            return Vector3d.atCenterOf(ground);
        }
        return null;
    }

    private BlockPos getHeighestAirAbove(BlockPos radialPos, int limit) {
        BlockPos position = new BlockPos(radialPos.getX(), this.getY(), radialPos.getZ());
        while (position.getY() < 256 && position.getY() < this.getY() + limit && level.isEmptyBlock(position)) {
            position = position.above();
        }
        return position;
    }

    private BlockPos getGround(BlockPos in) {
        BlockPos position = new BlockPos(in.getX(), this.getY(), in.getZ());
        while (position.getY() > 1 && level.isEmptyBlock(position)) {
            position = position.below();
        }
        if (position.getY() < 2) {
            return position.above(60 + random.nextInt(5));
        }

        return position;
    }

    public boolean isAlliedTo(Entity entityIn) {
        return super.isAlliedTo(entityIn) || this.getSplitFromUUID() != null && this.getSplitFromUUID().equals(entityIn.getUUID()) ||
                entityIn instanceof EntityVoidWorm && ((EntityVoidWorm) entityIn).getSplitFromUUID() != null && ((EntityVoidWorm) entityIn).getSplitFromUUID().equals(entityIn.getUUID());
    }

    private void spit(Vector3d shotAt, boolean portal) {
        shotAt = shotAt.yRot(-this.yRot * ((float) Math.PI / 180F));
        EntityVoidWormShot shot = new EntityVoidWormShot(this.level, this);
        double d0 = shotAt.x;
        double d1 = shotAt.y;
        double d2 = shotAt.z;
        float f = MathHelper.sqrt(d0 * d0 + d2 * d2) * 0.35F;
        shot.shoot(d0, d1 + (double) f, d2, 0.5F, 3.0F);
        if (!this.isSilent()) {
            this.level.playSound(null, this.getX(), this.getY(), this.getZ(), SoundEvents.DROWNED_SHOOT, this.getSoundSource(), 1.0F, 1.0F + (this.random.nextFloat() - this.random.nextFloat()) * 0.2F);
        }
        shot.setPortalType(portal);
        this.openMouth(5);
        this.level.addFreshEntity(shot);
    }

    private boolean wormAttack(Entity entity, DamageSource source, float dmg) {
        dmg *= AMConfig.voidWormDamageModifier;
        return entity instanceof EnderDragonEntity ? ((EnderDragonEntity) entity).reallyHurt(source, dmg * 0.5F) : entity.hurt(source, dmg);
    }

    public void playHurtSoundWorm(DamageSource source) {
        this.playHurtSound(source);
    }


    private enum AttackMode {
        CIRCLE,
        SLAM_RISE,
        SLAM_FALL,
        PORTAL
    }

    private class AIFlyIdle extends Goal {
        protected final EntityVoidWorm voidWorm;
        protected double x;
        protected double y;
        protected double z;

        public AIFlyIdle() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE));
            this.voidWorm = EntityVoidWorm.this;
        }

        @Override
        public boolean canUse() {
            if (this.voidWorm.isVehicle() || this.voidWorm.portalTarget != null || (voidWorm.getTarget() != null && voidWorm.getTarget().isAlive()) || this.voidWorm.isPassenger()) {
                return false;
            } else {
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
            voidWorm.getMoveControl().setWantedPosition(x, y, z, 1F);
        }

        @Nullable
        protected Vector3d getPosition() {
            Vector3d vector3d = voidWorm.position();
            return voidWorm.getBlockInViewAway(vector3d, 1);
        }

        public boolean canContinueToUse() {
            return voidWorm.distanceToSqr(x, y, z) > 20F && this.voidWorm.portalTarget == null && !voidWorm.horizontalCollision && (voidWorm.getTarget() == null || !voidWorm.getTarget().isAlive());
        }

        public void start() {
            voidWorm.getMoveControl().setWantedPosition(x, y, z, 1F);
        }

        public void stop() {
            this.voidWorm.getNavigation().stop();
            super.stop();
        }
    }

    public class AIAttack extends Goal {

        private AttackMode mode = AttackMode.CIRCLE;
        private int modeTicks = 0;
        private int maxCircleTime = 500;
        private Vector3d moveTo = null;

        public AIAttack() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return EntityVoidWorm.this.getTarget() != null && EntityVoidWorm.this.getTarget().isAlive();
        }

        public void stop() {
            mode = AttackMode.CIRCLE;
            modeTicks = 0;
        }

        public void start() {
            mode = AttackMode.CIRCLE;
            maxCircleTime = 60 + random.nextInt(200);
        }

        public void tick() {
            LivingEntity target = EntityVoidWorm.this.getTarget();
            boolean flag = false;
            float speed = 1;
            for (Entity entity : EntityVoidWorm.this.level.getEntitiesOfClass(LivingEntity.class, EntityVoidWorm.this.getBoundingBox().inflate(2.0D), null)) {
                if (!entity.is(EntityVoidWorm.this) && !(entity instanceof EntityVoidWormPart) && !entity.isAlliedTo(EntityVoidWorm.this) && entity != EntityVoidWorm.this) {
                    if (EntityVoidWorm.this.isMouthOpen()) {
                        launch(entity, true);
                        flag = true;
                        wormAttack(entity, DamageSource.mobAttack(EntityVoidWorm.this), 8.0F + random.nextFloat() * 8.0F);
                    } else {
                        EntityVoidWorm.this.openMouth(15);
                    }
                }
            }
            if (target != null) {
                if (mode == AttackMode.CIRCLE) {
                    if (moveTo == null || EntityVoidWorm.this.distanceToSqr(moveTo) < 16 || EntityVoidWorm.this.horizontalCollision) {
                        moveTo = EntityVoidWorm.this.getBlockInViewAway(target.position(), 0.4F + random.nextFloat() * 0.2F);
                    }
                    modeTicks++;
                    if (modeTicks % 50 == 0) {
                        EntityVoidWorm.this.spit(new Vector3d(3, 3, 0), false);
                        EntityVoidWorm.this.spit(new Vector3d(-3, 3, 0), false);
                        EntityVoidWorm.this.spit(new Vector3d(3, -3, 0), false);
                        EntityVoidWorm.this.spit(new Vector3d(-3, -3, 0), false);
                    }
                    if (modeTicks > maxCircleTime) {
                        maxCircleTime = 60 + random.nextInt(200);
                        mode = AttackMode.SLAM_RISE;
                        modeTicks = 0;
                        moveTo = null;
                    }
                } else if (mode == AttackMode.SLAM_RISE) {
                    if (moveTo == null) {
                        moveTo = EntityVoidWorm.this.getBlockInViewAwaySlam(target.position(), 20 + random.nextInt(20));
                    }
                    if (moveTo != null) {
                        if (EntityVoidWorm.this.getY() > target.getY() + 15) {
                            moveTo = null;
                            modeTicks = 0;
                            mode = AttackMode.SLAM_FALL;
                        }
                    }
                } else if (mode == AttackMode.SLAM_FALL) {
                    speed = 2;
                    EntityVoidWorm.this.lookAt(target, 360, 360);
                    moveTo = target.position();
                    if (EntityVoidWorm.this.horizontalCollision) {
                        moveTo = new Vector3d(target.getX(), EntityVoidWorm.this.getY() + 3, target.getZ());
                    }
                    EntityVoidWorm.this.openMouth(20);
                    if (EntityVoidWorm.this.distanceToSqr(moveTo) < 4 || flag) {
                        mode = AttackMode.CIRCLE;
                        moveTo = null;
                        modeTicks = 0;
                    }
                }
            }
            if (!EntityVoidWorm.this.canSee(target) && random.nextInt(100) == 0) {
                if (EntityVoidWorm.this.makePortalCooldown == 0) {
                    Vector3d to = new Vector3d(target.getX(), target.getBoundingBox().maxY + 0.1, target.getZ());
                    EntityVoidWorm.this.createPortal(EntityVoidWorm.this.position().add(EntityVoidWorm.this.getLookAngle().scale(20)), to, Direction.UP);
                    EntityVoidWorm.this.makePortalCooldown = 50;
                    mode = AttackMode.SLAM_FALL;
                }
            }
            if (moveTo != null && EntityVoidWorm.this.portalTarget == null) {
                EntityVoidWorm.this.getMoveControl().setWantedPosition(moveTo.x, moveTo.y, moveTo.z, speed);
            }
        }
    }

    public class AIEnterPortal extends Goal {

        public AIEnterPortal() {
            super();
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return EntityVoidWorm.this.portalTarget != null;
        }

        public void tick() {
            if (EntityVoidWorm.this.portalTarget != null) {
                noPhysics = true;
                AxisAlignedBB bb = EntityVoidWorm.this.portalTarget.getBoundingBox();
                double centerX = bb.minX + ((bb.maxX - bb.minX) / 2F);
                double centerY = bb.minY + ((bb.maxY - bb.minY) / 2F);
                double centerZ = bb.minZ + ((bb.maxZ - bb.minZ) / 2F);
                float sped = 0.08F;
                EntityVoidWorm.this.setDeltaMovement(EntityVoidWorm.this.getDeltaMovement().add(Math.signum(centerX - EntityVoidWorm.this.getX()) * sped, Math.signum(centerY - EntityVoidWorm.this.getY()) * sped, Math.signum(centerZ - EntityVoidWorm.this.getZ()) * sped));
                EntityVoidWorm.this.getMoveControl().setWantedPosition(centerX, centerY, centerZ, 1);
            }
        }

        public void stop() {
            noPhysics = false;
        }
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 67) {
            AlexsMobs.PROXY.onEntityStatus(this, id);
        } else {
            super.handleEntityEvent(id);
        }
    }
}
