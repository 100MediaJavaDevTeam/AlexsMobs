package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.enchantment.AMEnchantmentRegistry;
import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import com.github.alexthe666.alexsmobs.message.MessageSyncEntityPos;
import net.minecraft.block.BlockState;
import net.minecraft.block.LilyPadBlock;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.*;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.fluid.Fluids;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.shapes.IBooleanFunction;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.util.math.shapes.VoxelShapes;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class EntityStraddleboard extends Entity implements IJumpingMount {
    private static final DataParameter<ItemStack> ITEMSTACK = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.ITEM_STACK);
    private static final DataParameter<Integer> TIME_SINCE_HIT = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.INT);
    private static final DataParameter<Float> DAMAGE_TAKEN = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.FLOAT);
    private static final DataParameter<Integer> ROCKING_TICKS = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.INT);
    private static final DataParameter<Integer> COLOR = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.INT);
    private static final DataParameter<Boolean> DEFAULT_COLOR = EntityDataManager.defineId(EntityStraddleboard.class, DataSerializers.BOOLEAN);
    public float boardRot = 0;
    public float prevBoardRot = 0;
    private double lastYd;
    private boolean rocking;
    private boolean downwards;
    private float rockingIntensity;
    private float rockingAngle;
    private float prevRockingAngle;
    private boolean jumpOutOfLava = false;
    private float outOfControlTicks;
    private BoatEntity.Status status;
    private BoatEntity.Status previousStatus;
    private float momentum;
    private double waterLevel;
    private float boatGlide;
    private int extinguishTimer = 0;

    public EntityStraddleboard(EntityType<?> p_i48580_1_, World p_i48580_2_) {
        super(p_i48580_1_, p_i48580_2_);
        this.blocksBuilding = true;
    }

    public EntityStraddleboard(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.STRADDLEBOARD, world);
    }

    public EntityStraddleboard(World worldIn, double x, double y, double z) {
        this(AMEntityRegistry.STRADDLEBOARD, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(Vector3d.ZERO);
        this.xo = x;
        this.yo = y;
        this.zo = z;
    }

    public static boolean canVehicleCollide(Entity p_242378_0_, Entity entity) {
        return (entity.canBeCollidedWith() || entity.isPushable()) && !p_242378_0_.isPassengerOfSameVehicle(entity);
    }

    protected float getEyeHeight(Pose poseIn, EntitySize sizeIn) {
        return sizeIn.height;
    }

    protected boolean isMovementNoisy() {
        return false;
    }

    protected void defineSynchedData() {
        this.entityData.define(TIME_SINCE_HIT, 0);
        this.entityData.define(ITEMSTACK, new ItemStack(AMItemRegistry.STRADDLEBOARD));
        this.entityData.define(ROCKING_TICKS, 0);
        this.entityData.define(DEFAULT_COLOR, true);
        this.entityData.define(COLOR, 0);
        this.entityData.define(DAMAGE_TAKEN, 0.0F);
    }

    public boolean shouldRiderSit() {
        return false;
    }

    public boolean canCollideWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }

    public boolean canBeCollidedWith() {
        return true;
    }

    public boolean isPushable() {
        return true;
    }

    protected Vector3d getRelativePortalPosition(Direction.Axis axis, TeleportationRepositioner.Result result) {
        return LivingEntity.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(axis, result));
    }

    public double getPassengersRidingOffset() {
        return 0.9D;
    }

    public boolean hurt(DamageSource source, float amount) {
        if (this.isInvulnerableTo(source)) {
            return false;
        } else if (!this.level.isClientSide && !this.removed) {
            this.setTimeSinceHit(10);
            this.setDamageTaken(this.getDamageTaken() + amount * 10.0F);
            this.markHurt();
            this.setRockingTicks(25);
            boolean flag = source.getEntity() instanceof PlayerEntity && ((PlayerEntity) source.getEntity()).abilities.instabuild;
            if (flag || this.getDamageTaken() > 40.0F) {
                if (!flag) {
                    PlayerEntity p = null;
                    if(source.getEntity() instanceof PlayerEntity){
                        p = (PlayerEntity)source.getEntity();
                    }
                    if(this.getControllingPassenger() != null && this.getControllingPassenger() instanceof PlayerEntity){
                        p = (PlayerEntity)this.getControllingPassenger();
                    }
                    boolean dropItem = true;
                    if(p != null && this.getEnchant(AMEnchantmentRegistry.STRADDLE_BOARDRETURN) > 0){
                        if(p.addItem(this.getItemBoard())){
                            dropItem = false;
                        }
                    }
                    if(dropItem){
                        if(this.level.getGameRules().getBoolean(GameRules.RULE_DOENTITYDROPS)){
                            this.spawnAtLocation(this.getItemBoard());
                        }
                    }

                }

                this.remove();
            }

            return true;
        } else {
            return true;
        }
    }

    private ItemStack getItemBoard() {
        return this.getItemStack();
    }


    public void push(Entity entityIn) {
        if (entityIn instanceof EntityStraddleboard) {
            if (entityIn.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(entityIn);
            }
        } else if (entityIn.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(entityIn);
        }

    }

    @OnlyIn(Dist.CLIENT)
    public void animateHurt() {
        this.setTimeSinceHit(10);
        this.setDamageTaken(this.getDamageTaken() * 11.0F);
    }

    public boolean isPickable() {
        return !this.removed;
    }

    private BoatEntity.Status getBoatStatus() {
        BoatEntity.Status boatentity$status = this.getUnderwaterStatus();
        if (boatentity$status != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return boatentity$status;
        } else if (this.checkInWater()) {
            return BoatEntity.Status.IN_WATER;
        } else {
            float f = this.getBoatGlide();
            if (f > 0.0F) {
                this.boatGlide = f;
                return BoatEntity.Status.ON_LAND;
            } else {
                return BoatEntity.Status.IN_AIR;
            }
        }
    }

    public float getBoatGlide() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY - 0.001D, axisalignedbb.minZ, axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ);
        int i = MathHelper.floor(axisalignedbb1.minX) - 1;
        int j = MathHelper.ceil(axisalignedbb1.maxX) + 1;
        int k = MathHelper.floor(axisalignedbb1.minY) - 1;
        int l = MathHelper.ceil(axisalignedbb1.maxY) + 1;
        int i1 = MathHelper.floor(axisalignedbb1.minZ) - 1;
        int j1 = MathHelper.ceil(axisalignedbb1.maxZ) + 1;
        VoxelShape voxelshape = VoxelShapes.create(axisalignedbb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(int l1 = i; l1 < j; ++l1) {
            for(int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);
                if (j2 != 2) {
                    for(int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            blockpos$mutable.set(l1, k2, i2);
                            BlockState blockstate = this.level.getBlockState(blockpos$mutable);
                            if (!(blockstate.getBlock() instanceof LilyPadBlock) && VoxelShapes.joinIsNotEmpty(blockstate.getBlockSupportShape(this.level, blockpos$mutable).move((double)l1, (double)k2, (double)i2), voxelshape, IBooleanFunction.AND)) {
                                f += blockstate.getSlipperiness(this.level, blockpos$mutable, this);
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float)k1;
    }

    private boolean checkInWater() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.minY);
        int l = MathHelper.ceil(axisalignedbb.minY - 0.001D);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        this.waterLevel = Double.MIN_VALUE;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for(int k1 = i; k1 < j; ++k1) {
            for(int l1 = k; l1 < l; ++l1) {
                for(int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if (fluidstate.is(FluidTags.WATER) || fluidstate.is(FluidTags.LAVA)) {
                        float f = (float)l1 + fluidstate.getHeight(this.level, blockpos$mutable);
                        this.waterLevel = Math.max((double)f, this.waterLevel);
                        flag |= axisalignedbb.minY < (double)f;
                    }
                }
            }
        }

        return flag;
    }

    private void updateMotion() {
        double d0 = -0.04F;
        double d1 = this.isNoGravity() ? 0.0D : (double) -0.04F;
        double d2 = 0.0D;
        this.momentum = 0.05F;
        if (this.previousStatus == BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.IN_AIR && this.status != BoatEntity.Status.ON_LAND) {
            this.waterLevel = this.getY(1.0D);
            this.setPos(this.getX(), (double) (this.getWaterLevelAbove() - this.getBbHeight()) + 0.25, this.getZ());
            this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 1D, 1.0D));
            this.lastYd = 0.0D;
            this.status = BoatEntity.Status.IN_WATER;
        } else {
            if (this.status == BoatEntity.Status.IN_WATER) {
                d2 = (this.waterLevel - this.getY()) / (double) this.getBbHeight();
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_FLOWING_WATER) {
                d1 = -7.0E-4D;
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.UNDER_WATER) {
                d2 = 0.01F;
                this.momentum = 0.45F;
            } else if (this.status == BoatEntity.Status.IN_AIR) {
                this.momentum = 0.9F;
            } else if (this.status == BoatEntity.Status.ON_LAND) {
                this.momentum = this.boatGlide;
                if (this.getControllingPassenger() instanceof PlayerEntity) {
                    this.boatGlide /= 2.0F;
                }
            }

            Vector3d vector3d = this.getDeltaMovement();
            this.setDeltaMovement(vector3d.x * (double) this.momentum, vector3d.y + d1, vector3d.z * (double) this.momentum);
            if (d2 > 0.0D) {
                Vector3d vector3d1 = this.getDeltaMovement();
                this.setDeltaMovement(vector3d1.x, (vector3d1.y + d2 * 0.06153846016296973D) * 0.75D, vector3d1.z);
            }
        }

    }

    public boolean isDefaultColor() {
        return this.entityData.get(DEFAULT_COLOR).booleanValue();
    }

    public void setDefaultColor(boolean bar) {
        this.entityData.set(DEFAULT_COLOR, Boolean.valueOf(bar));
    }

    public int getColor() {
        if(isDefaultColor()){
            return 0XADC3D7;
        }
        return this.entityData.get(COLOR);
    }

    public void setColor(int index) {
        this.entityData.set(COLOR, index);
    }


    @Nullable
    private BoatEntity.Status getUnderwaterStatus() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        double d0 = axisalignedbb.maxY + 0.001D;
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(d0);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(k1, l1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if ((fluidstate.is(FluidTags.WATER) || fluidstate.is(FluidTags.LAVA) && d0 < (double) ((float) blockpos$mutable.getY() + fluidstate.getHeight(this.level, blockpos$mutable))))
                    {
                        if (!fluidstate.isSource()) {
                            return BoatEntity.Status.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? BoatEntity.Status.UNDER_WATER : null;
    }


    public void tick() {
        prevBoardRot = this.boardRot;
        super.tick();
        this.previousStatus = this.status;
        this.status = this.getBoatStatus();
        this.floatStrider();
        this.checkInsideBlocks();
        if (this.isInWall()) {
            this.moveTowardsClosestSpace(this.getX(), (this.getBoundingBox().minY + this.getBoundingBox().maxY) / 2.0D, this.getZ());
        }
        if (this.getTimeSinceHit() > 0) {
            this.setTimeSinceHit(this.getTimeSinceHit() - 1);
        }
        if (this.getDamageTaken() > 0.0F) {
            this.setDamageTaken(this.getDamageTaken() - 1.0F);
        }
        if (isInLava()) {
            this.setNoGravity(true);
            if(this.fluidHeight.getDouble(FluidTags.LAVA) >= this.getBbHeight()){
                this.setDeltaMovement(0, 0.1, 0);
            }
        }else{
            this.setNoGravity(false);
        }
        float f2 = (float) -((float) this.getDeltaMovement().y * 0.5F * (double) (180F / (float) Math.PI));
        this.xRot = f2;

        if(extinguishTimer > 0){
            extinguishTimer--;
        }
        this.updateRocking();
        Entity controller = getControllingPassenger();
        if (controller instanceof PlayerEntity) {
            PlayerEntity player = (PlayerEntity) controller;
            if(this.tickCount % 50 == 0){
                if(getEnchant(AMEnchantmentRegistry.STRADDLE_LAVAWAX) > 0){
                    player.addEffect(new EffectInstance(Effects.FIRE_RESISTANCE, 100, 0, true, false));
                }
            }
            if(player.getRemainingFireTicks() > 0 && extinguishTimer == 0){
                player.clearFire();
            }
            this.yRot = player.yRotO;
            Vector3d vector3d = this.getDeltaMovement();
            if (vector3d.y > -0.5D) {
                this.fallDistance = 1.0F;
            }

            Vector3d vector3d1 = player.getLookAngle();
            float f = player.xRot * ((float) Math.PI / 180F);
            double d1 = Math.sqrt(vector3d1.x * vector3d1.x + vector3d1.z * vector3d1.z);
            double d3 = Math.sqrt(getHorizontalDistanceSqr(vector3d));
            double d4 = vector3d1.length();
            float f1 = MathHelper.cos(f);
            f1 = (float) ((double) f1 * (double) f1 * Math.min(1.0D, d4 / 0.4D));
            double d5 = vector3d.y * -0.1D * (double) f1;
            float slow = player.zza < 0 ? 0 : player.zza * 0.115F;
            float threshold = 0.05F;
            if (this.yRotO - this.yRot > threshold) {
                boardRot = boardRot + 2;
                slow *= 0;
            } else if (this.yRotO - this.yRot < -threshold) {
                boardRot = boardRot - 2;
                slow *= 0;
            } else if (boardRot > 0) {
                boardRot = (Math.max(boardRot - 10, 0));
            } else if (boardRot < 0) {
                boardRot = (Math.min(boardRot + 10, 0));
            }
            boardRot = (MathHelper.clamp(boardRot, -25, 25));

            vector3d = vector3d.add(vector3d1.x * slow / d1, 0.0D, vector3d1.z * slow / d1);


            if (d1 > 0.0D) {
                vector3d = vector3d.add((vector3d1.x / d1 * d3 - vector3d.x) * 0.1D, 0.0D, (vector3d1.z / d1 * d3 - vector3d.z) * 0.1D);
            }

            this.setDeltaMovement(vector3d.multiply(0.99F, 1F, 0.99F));

            if (player.isInWall()) {
                player.removeVehicle();
                this.hurt(DamageSource.GENERIC, 100);
            }
        }
        this.updateMotion();
        this.move(MoverType.SELF, this.getDeltaMovement());
    }

    public double getEyeY() {
        return this.getY() + 0.3F;
    }


    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if(!level.isClientSide){
            EntityStraddleboard copy = AMEntityRegistry.STRADDLEBOARD.create(level);
            CompoundNBT tag = new CompoundNBT();
            this.addAdditionalSaveData(tag);
            copy.readAdditionalSaveData(tag);
            copy.copyPosition(passenger);
            level.addFreshEntity(copy);
        }
        this.remove();
    }

    private void floatStrider() {

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

    private void updateRocking() {
        if (this.level.isClientSide) {
            int i = this.getRockingTicks();
            if (i > 0) {
                this.rockingIntensity += 1F;
            } else {
                this.rockingIntensity -= 0.1F;
            }

            this.rockingIntensity = MathHelper.clamp(this.rockingIntensity, 0.0F, 1.0F);
            this.prevRockingAngle = this.rockingAngle;
            this.rockingAngle = 10.0F * (float) Math.sin(0.5F * (float) this.level.getGameTime()) * this.rockingIntensity;
        } else {
            if (!this.rocking) {
                this.setRockingTicks(0);
            }

            int k = this.getRockingTicks();
            if (k > 0) {
                --k;
                this.setRockingTicks(k);
                int j = 60 - k - 1;
                if (j > 0 && k == 0) {
                    this.setRockingTicks(0);
                    Vector3d vector3d = this.getDeltaMovement();
                    if (this.downwards) {
                        this.setDeltaMovement(vector3d.add(0.0D, -0.7D, 0.0D));
                        this.ejectPassengers();
                    } else {
                        this.setDeltaMovement(vector3d.x, this.hasPassenger(PlayerEntity.class) ? 2.7D : 0.6D, vector3d.z);
                    }
                }

                this.rocking = false;
            }
        }

    }

    public float getWaterLevelAbove() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(axisalignedbb.maxY - this.lastYd);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        BlockPos.Mutable blockpos$mutable = new BlockPos.Mutable();

        label39:
        for (int k1 = k; k1 < l; ++k1) {
            float f = 0.0F;

            for (int l1 = i; l1 < j; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockpos$mutable.set(l1, k1, i2);
                    FluidState fluidstate = this.level.getFluidState(blockpos$mutable);
                    if (fluidstate.is(FluidTags.WATER) || fluidstate.is(FluidTags.LAVA)) {
                        f = Math.max(f, fluidstate.getHeight(this.level, blockpos$mutable));
                    }

                    if (f >= 1.0F) {
                        continue label39;
                    }
                }
            }

            if (f < 1.0F) {
                return (float) blockpos$mutable.getY() + f;
            }
        }

        return (float) (l + 1);
    }

    @Override
    public ActionResultType interact(PlayerEntity player, Hand hand) {
        if (player.isSecondaryUseActive()) {
            return ActionResultType.PASS;
        } else {
            if (!this.level.isClientSide) {
                return player.startRiding(this) ? ActionResultType.CONSUME : ActionResultType.PASS;
            } else {
                return ActionResultType.SUCCESS;
            }
        }
    }

    /**
     * Gets the damage taken from the last hit.
     */
    public float getDamageTaken() {
        return this.entityData.get(DAMAGE_TAKEN);
    }

    public void setDamageTaken(float damageTaken) {
        this.entityData.set(DAMAGE_TAKEN, damageTaken);
    }

    /**
     * Gets the time since the last hit.
     */
    public int getTimeSinceHit() {
        return this.entityData.get(TIME_SINCE_HIT);
    }

    /**
     * Sets the time to count down from since the last time entity was hit.
     */
    public void setTimeSinceHit(int timeSinceHit) {
        this.entityData.set(TIME_SINCE_HIT, timeSinceHit);
    }

    private int getRockingTicks() {
        return this.entityData.get(ROCKING_TICKS);
    }

    private void setRockingTicks(int ticks) {
        this.entityData.set(ROCKING_TICKS, ticks);
    }

    @OnlyIn(Dist.CLIENT)
    public float getRockingAngle(float partialTicks) {
        return MathHelper.lerp(partialTicks, this.prevRockingAngle, this.rockingAngle);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }


    @Override
    protected void readAdditionalSaveData(CompoundNBT compound) {
        this.setDefaultColor(compound.getBoolean("IsDefColor"));
        if(compound.contains("BoardStack")){
            this.setItemStack(ItemStack.of(compound.getCompound("BoardStack")));
        }
        this.setColor(compound.getInt("Color"));
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT compound) {
        compound.putBoolean("IsDefColor", this.isDefaultColor());
        compound.putInt("Color", this.getColor());
        if(!this.getItemStack().isEmpty()){
            CompoundNBT stackTag = new CompoundNBT();
            this.getItemStack().save(stackTag);
            compound.put("BoardStack", stackTag);
        }

    }

    @Override
    public void onPlayerJump(int i) {

    }

    @Override
    public boolean canJump() {
        return this.level.getBlockState(this.getBlockPosBelowThatAffectsMyMovement()).getFluidState().is(FluidTags.LAVA);
    }

    @Override
    public void handleStartJump(int i) {
        jumpOutOfLava = true;
        this.hasImpulse=true;
        float scaled = i * 0.01F + 0.1F * getEnchant(AMEnchantmentRegistry.STRADDLE_JUMP);
        this.setDeltaMovement(this.getDeltaMovement().add(0, scaled * 1.5F, 0));
    }

    private int getEnchant(Enchantment enchantment){
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, this.getItemBoard());
    }

    public boolean shouldSerpentFriend(){
        return getEnchant(AMEnchantmentRegistry.STRADDLE_SERPENTFRIEND) > 0;
    }

    @Override
    public void handleStopJump() {

    }

    public void setItemStack(ItemStack item){
        this.entityData.set(ITEMSTACK, item);
    }

    public ItemStack getItemStack(){
       return this.entityData.get(ITEMSTACK);
    }

    public enum Status {
        IN_WATER,
        UNDER_WATER,
        UNDER_FLOWING_WATER,
        ON_LAND,
        IN_AIR
    }
}
