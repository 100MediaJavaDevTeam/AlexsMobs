package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.effect.AMEffectRegistry;
import com.github.alexthe666.alexsmobs.misc.AMPointOfInterestRegistry;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import com.google.common.base.Predicates;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.ai.controller.MovementController;
import net.minecraft.entity.ai.goal.Goal;
import net.minecraft.entity.ai.goal.LookAtGoal;
import net.minecraft.entity.ai.goal.LookRandomlyGoal;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.monster.PhantomEntity;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.passive.IFlyingAnimal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.particles.BasicParticleType;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tileentity.BeaconTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.village.PointOfInterestManager;
import net.minecraft.world.IWorld;
import net.minecraft.world.World;
import net.minecraft.world.gen.Heightmap;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.EnumSet;
import java.util.List;
import java.util.Random;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class EntitySunbird extends AnimalEntity implements IFlyingAnimal {

    public static final Predicate<? super Entity> SCORCH_PRED = new com.google.common.base.Predicate<Entity>() {
        @Override
        public boolean apply(@Nullable Entity e) {
            return e.isAlive() && e.getType().is(EntityTypeTags.getAllTags().getTag(AMTagRegistry.SUNBIRD_SCORCH_TARGETS));
        }
    };
    public float birdPitch = 0;
    public float prevBirdPitch = 0;
    private int beaconSearchCooldown = 50;
    private BlockPos beaconPos = null;
    private boolean orbitClockwise = false;

    protected EntitySunbird(EntityType type, World worldIn) {
        super(type, worldIn);
        this.moveControl = new MoveHelperController(this);
        orbitClockwise = new Random().nextBoolean();
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 20.0D).add(Attributes.FOLLOW_RANGE, 64.0D).add(Attributes.ATTACK_DAMAGE, 2.0D).add(Attributes.MOVEMENT_SPEED, 1F);
    }

    public static boolean canSunbirdSpawn(EntityType<? extends MobEntity> typeIn, IWorld worldIn, SpawnReason reason, BlockPos pos, Random randomIn) {
        BlockPos blockpos = pos.below();
        return reason == SpawnReason.SPAWNER || true;
    }

    public boolean checkSpawnRules(IWorld worldIn, SpawnReason spawnReasonIn) {
        return AMEntityRegistry.rollSpawn(AMConfig.sunbirdSpawnRolls, this.getRandom(), spawnReasonIn);
    }

    protected SoundEvent getAmbientSound() {
        return AMSoundRegistry.SUNBIRD_IDLE;
    }

    protected SoundEvent getHurtSound(DamageSource damageSourceIn) {
        return AMSoundRegistry.SUNBIRD_HURT;
    }

    protected SoundEvent getDeathSound() {
        return AMSoundRegistry.SUNBIRD_HURT;
    }

    protected void registerGoals() {
        this.goalSelector.addGoal(3, new RandomFlyGoal(this));
        this.goalSelector.addGoal(4, new LookAtGoal(this, PlayerEntity.class, 32F));
        this.goalSelector.addGoal(5, new LookRandomlyGoal(this));
    }

    public float getBrightness() {
        return 1.0F;
    }

    public boolean isNoGravity() {
        return true;
    }

    public boolean causeFallDamage(float distance, float damageMultiplier) {
        return false;
    }

    protected void checkFallDamage(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
    }

    public boolean hurt(DamageSource source, float amount) {
        boolean prev = super.hurt(source, amount);
        if (prev) {
            if (source.getEntity() != null) {
                if (source.getEntity() instanceof LivingEntity) {
                    LivingEntity hurter = (LivingEntity) source.getEntity();
                    if (hurter.hasEffect(AMEffectRegistry.SUNBIRD_BLESSING)) {
                        hurter.removeEffect(AMEffectRegistry.SUNBIRD_BLESSING);
                    }
                    hurter.addEffect(new EffectInstance(AMEffectRegistry.SUNBIRD_CURSE, 600, 0));
                }
            }
            return prev;
        }
        return prev;
    }

    public void travel(Vector3d travelVector) {
        if (this.isInWater()) {
            this.moveRelative(0.02F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.8F));
        } else if (this.isInLava()) {
            this.moveRelative(0.02F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.5D));
        } else {
            BlockPos ground = new BlockPos(this.getX(), this.getY() - 1.0D, this.getZ());
            float f = 0.91F;
            if (this.onGround) {
                f = this.level.getBlockState(ground).getSlipperiness(this.level, ground, this) * 0.91F;
            }

            float f1 = 0.16277137F / (f * f * f);
            f = 0.91F;
            if (this.onGround) {
                f = this.level.getBlockState(ground).getSlipperiness(this.level, ground, this) * 0.91F;
            }
            this.calculateEntityAnimation(this, true);

            this.moveRelative(0.2F, travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(f));
        }

        this.calculateEntityAnimation(this, false);
    }

    public void tick() {
        super.tick();
        prevBirdPitch = this.birdPitch;

        float f2 = (float) -((float) this.getDeltaMovement().y * (double) (180F / (float) Math.PI));
        this.birdPitch = f2;

        if (level.isClientSide) {
            float radius = 0.35F + random.nextFloat() * 1.85F;
            float angle = (0.01745329251F * ((random.nextBoolean() ? -85F : 85F) + this.yBodyRot));
            float angleMotion = (0.01745329251F * this.yBodyRot);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            double extraXMotion = -0.2F * MathHelper.sin((float) (Math.PI + angleMotion));
            double extraZMotion = -0.2F * MathHelper.cos(angleMotion);
            double yRandom = 0.2F + random.nextFloat() * 0.3F;
            BasicParticleType type = ParticleTypes.FIREWORK;
            this.level.addParticle(type, this.getX() + extraX, this.getY() + yRandom, this.getZ() + extraZ, extraXMotion, 0D, extraZMotion);
        } else {
            if (this.tickCount % 100 == 0) {
                List<Entity> list = this.level.getEntitiesOfClass(LivingEntity.class, this.getScorchArea(), SCORCH_PRED);
                for (Entity e : list) {
                    e.setSecondsOnFire(4);
                    if (e instanceof PhantomEntity) {
                        ((PhantomEntity) e).addEffect(new EffectInstance(AMEffectRegistry.SUNBIRD_CURSE, 200, 0));
                    }
                }
                List<PlayerEntity> playerList = this.level.getEntitiesOfClass(PlayerEntity.class, this.getScorchArea(), Predicates.alwaysTrue());
                for (PlayerEntity e : playerList) {
                    if (!e.hasEffect(AMEffectRegistry.SUNBIRD_BLESSING) && !e.hasEffect(AMEffectRegistry.SUNBIRD_CURSE)) {
                        e.addEffect(new EffectInstance(AMEffectRegistry.SUNBIRD_BLESSING, 600, 0));
                    }
                }
            }
            if (beaconSearchCooldown > 0) {
                beaconSearchCooldown--;
            }
            if (beaconSearchCooldown <= 0) {
                beaconSearchCooldown = 100 + random.nextInt(200);
                if (level instanceof ServerWorld) {
                    List<BlockPos> beacons = this.getNearbyBeacons(this.blockPosition(), (ServerWorld) level, 64);
                    BlockPos closest = null;
                    for (BlockPos pos : beacons) {
                        if (closest == null || this.distanceToSqr(closest.getX(), closest.getY(), closest.getZ()) > this.distanceToSqr(pos.getX(), pos.getY(), pos.getZ())) {
                            if (isValidBeacon(pos)) {
                                closest = pos;
                            }
                        }
                    }
                    if (closest != null && isValidBeacon(closest)) {
                        beaconPos = closest;
                    }
                }
                if (beaconPos != null) {

                    if (!isValidBeacon(beaconPos) && tickCount > 40) {
                        this.beaconPos = null;
                    }
                }
            }
        }
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.contains("BeaconPosX")) {
            int i = compound.getInt("BeaconPosX");
            int j = compound.getInt("BeaconPosY");
            int k = compound.getInt("BeaconPosZ");
            this.beaconPos = new BlockPos(i, j, k);
        } else {
            this.beaconPos = null;
        }
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        BlockPos blockpos = this.beaconPos;
        if (blockpos != null) {
            compound.putInt("BeaconPosX", blockpos.getX());
            compound.putInt("BeaconPosY", blockpos.getY());
            compound.putInt("BeaconPosZ", blockpos.getZ());
        }

    }


    private AxisAlignedBB getScorchArea() {
        return this.getBoundingBox().inflate(15, 32, 15);
    }

    @Nullable
    @Override
    public AgeableEntity getBreedOffspring(ServerWorld p_241840_1_, AgeableEntity p_241840_2_) {
        return null;
    }

    public boolean isTargetBlocked(Vector3d target) {
        Vector3d Vector3d = new Vector3d(this.getX(), this.getEyeY(), this.getZ());
        return this.level.clip(new RayTraceContext(Vector3d, target, RayTraceContext.BlockMode.COLLIDER, RayTraceContext.FluidMode.NONE, this)).getType() != RayTraceResult.Type.MISS;
    }

    private List<BlockPos> getNearbyBeacons(BlockPos blockpos, ServerWorld world, int range) {
        PointOfInterestManager pointofinterestmanager = world.getPoiManager();
        Stream<BlockPos> stream = pointofinterestmanager.findAll(AMPointOfInterestRegistry.BEACON.getPredicate(), Predicates.alwaysTrue(), blockpos, range, PointOfInterestManager.Status.ANY);
        return stream.collect(Collectors.toList());
    }

    private boolean isValidBeacon(BlockPos pos) {
        TileEntity te = level.getBlockEntity(pos);
        return te instanceof BeaconTileEntity && ((BeaconTileEntity) te).getLevels() > 0;
    }

    static class MoveHelperController extends MovementController {
        private final EntitySunbird parentEntity;

        public MoveHelperController(EntitySunbird sunbird) {
            super(sunbird);
            this.parentEntity = sunbird;
        }

        public void tick() {
            if (this.operation == MovementController.Action.MOVE_TO) {
                Vector3d vector3d = new Vector3d(this.wantedX - parentEntity.getX(), this.wantedY - parentEntity.getY(), this.wantedZ - parentEntity.getZ());
                double d0 = vector3d.length();
                if (d0 < parentEntity.getBoundingBox().getSize()) {
                    this.operation = MovementController.Action.WAIT;
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().scale(0.5D));
                } else {
                    parentEntity.setDeltaMovement(parentEntity.getDeltaMovement().add(vector3d.scale(this.speedModifier * 0.05D / d0)));
                    if (parentEntity.getTarget() == null) {
                        Vector3d vector3d1 = parentEntity.getDeltaMovement();
                        parentEntity.yRot = -((float) MathHelper.atan2(vector3d1.x, vector3d1.z)) * (180F / (float) Math.PI);
                        parentEntity.yBodyRot = parentEntity.yRot;
                    } else {
                        double d2 = parentEntity.getTarget().getX() - parentEntity.getX();
                        double d1 = parentEntity.getTarget().getZ() - parentEntity.getZ();
                        parentEntity.yRot = -((float) MathHelper.atan2(d2, d1)) * (180F / (float) Math.PI);
                        parentEntity.yBodyRot = parentEntity.yRot;
                    }
                }

            }
        }

        private boolean canReach(Vector3d p_220673_1_, int p_220673_2_) {
            AxisAlignedBB axisalignedbb = this.parentEntity.getBoundingBox();

            for (int i = 1; i < p_220673_2_; ++i) {
                axisalignedbb = axisalignedbb.move(p_220673_1_);
                if (!this.parentEntity.level.noCollision(this.parentEntity, axisalignedbb)) {
                    return false;
                }
            }

            return true;
        }
    }

    static class RandomFlyGoal extends Goal {
        private final EntitySunbird parentEntity;
        private BlockPos target = null;

        public RandomFlyGoal(EntitySunbird sunbird) {
            this.parentEntity = sunbird;
            this.setFlags(EnumSet.of(Goal.Flag.MOVE));
        }

        public boolean canUse() {
            MovementController movementcontroller = this.parentEntity.getMoveControl();
            if (!movementcontroller.hasWanted() || target == null) {
                if (parentEntity.beaconPos != null) {
                    target = getBlockInViewBeacon(parentEntity.beaconPos, 5 + parentEntity.random.nextInt(1));
                } else {
                    target = getBlockInViewSunbird();
                }
                if (target != null) {
                    this.parentEntity.getMoveControl().setWantedPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, parentEntity.beaconPos != null ? 0.8D : 1.0D);
                }
                return true;
            }
            return false;
        }

        public boolean canContinueToUse() {
            return target != null && parentEntity.distanceToSqr(Vector3d.atCenterOf(target)) > 2.4D && parentEntity.getMoveControl().hasWanted() && !parentEntity.horizontalCollision;
        }

        public void stop() {
            target = null;
        }

        public void tick() {
            if (target == null) {
                if (parentEntity.beaconPos != null) {
                    target = getBlockInViewBeacon(parentEntity.beaconPos, 5 + parentEntity.random.nextInt(1));
                } else {
                    target = getBlockInViewSunbird();
                }
            }
            if(parentEntity.beaconPos != null && parentEntity.random.nextInt(100) == 0){
                parentEntity.orbitClockwise = parentEntity.random.nextBoolean();
            }
            if (target != null) {
                this.parentEntity.getMoveControl().setWantedPosition(target.getX() + 0.5D, target.getY() + 0.5D, target.getZ() + 0.5D, parentEntity.beaconPos != null ? 0.8D : 1.0D);
                if (parentEntity.distanceToSqr(Vector3d.atCenterOf(target)) < 2.5F) {
                    target = null;
                }
            }
        }

        private BlockPos getBlockInViewBeacon(BlockPos orbitPos, float gatheringCircleDist) {
            float angle = (0.01745329251F * (float) 9 * (parentEntity.orbitClockwise ? -parentEntity.tickCount : parentEntity.tickCount));
            double extraX = gatheringCircleDist * MathHelper.sin((angle));
            double extraZ = gatheringCircleDist * MathHelper.cos(angle);
            if (orbitPos != null) {
                BlockPos pos = new BlockPos(orbitPos.getX() + extraX, orbitPos.getY() + parentEntity.random.nextInt(2) + 2, orbitPos.getZ() + extraZ);
                if (parentEntity.level.isEmptyBlock(new BlockPos(pos))) {
                    return pos;
                }
            }
            return null;
        }

        public BlockPos getBlockInViewSunbird() {
            float radius = 0.75F * (0.7F * 6) * -3 - parentEntity.getRandom().nextInt(24);
            float neg = parentEntity.getRandom().nextBoolean() ? 1 : -1;
            float renderYawOffset = parentEntity.yBodyRot;
            float angle = (0.01745329251F * renderYawOffset) + 3.15F + (parentEntity.getRandom().nextFloat() * neg);
            double extraX = radius * MathHelper.sin((float) (Math.PI + angle));
            double extraZ = radius * MathHelper.cos(angle);
            BlockPos radialPos = new BlockPos(parentEntity.getX() + extraX, 0, parentEntity.getZ() + extraZ);
            BlockPos ground = parentEntity.level.getHeightmapPos(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, radialPos);
            int distFromGround = (int) parentEntity.getY() - ground.getY();
            int flightHeight = Math.max(ground.getY(), 180 + parentEntity.getRandom().nextInt(40)) - ground.getY();
            BlockPos newPos = radialPos.above(distFromGround > 16 ? flightHeight : (int) parentEntity.getY() + parentEntity.getRandom().nextInt(16) + 1);
            if (!parentEntity.isTargetBlocked(Vector3d.atCenterOf(newPos)) && parentEntity.distanceToSqr(Vector3d.atCenterOf(newPos)) > 6) {
                return newPos;
            }
            return null;
        }

    }
}
