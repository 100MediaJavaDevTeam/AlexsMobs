package com.github.alexthe666.alexsmobs.entity;

import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.BlockRayTraceResult;
import net.minecraft.util.math.EntityRayTraceResult;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.UUID;

public class EntityCachalotEcho extends Entity {
    private static final DataParameter<Boolean> RETURNING = EntityDataManager.defineId(EntityCachalotEcho.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Boolean> FASTER_ANIM = EntityDataManager.defineId(EntityCachalotEcho.class, DataSerializers.BOOLEAN);
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;
    private boolean playerLaunched = false;

    public EntityCachalotEcho(EntityType p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityCachalotEcho(World worldIn, EntityCachalotWhale p_i47273_2_) {
        this(AMEntityRegistry.CACHALOT_ECHO, worldIn);
        this.setShooter(p_i47273_2_);
    }

    public EntityCachalotEcho(World worldIn, LivingEntity p_i47273_2_, boolean right) {
        this(AMEntityRegistry.CACHALOT_ECHO, worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.yHeadRot + (right ? 90 : -90);
        playerLaunched = true;
        this.setFasterAnimation(true);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getBbWidth()) * 0.5D * (double) MathHelper.sin(rot * ((float) Math.PI / 180F)), p_i47273_2_.getY() + 1D, p_i47273_2_.getZ() + (double) (p_i47273_2_.getBbWidth()) * 0.5D * (double) MathHelper.cos(rot * ((float) Math.PI / 180F)));
    }

    @OnlyIn(Dist.CLIENT)
    public EntityCachalotEcho(World worldIn, double x, double y, double z, double p_i47274_8_, double p_i47274_10_, double p_i47274_12_) {
        this(AMEntityRegistry.CACHALOT_ECHO, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(p_i47274_8_, p_i47274_10_, p_i47274_12_);
    }

    public EntityCachalotEcho(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.CACHALOT_ECHO, world);
    }

    protected static float lerpRotation(float p_234614_0_, float p_234614_1_) {
        while (p_234614_1_ - p_234614_0_ < -180.0F) {
            p_234614_0_ -= 360.0F;
        }

        while (p_234614_1_ - p_234614_0_ >= 180.0F) {
            p_234614_0_ += 360.0F;
        }

        return MathHelper.lerp(0.2F, p_234614_0_, p_234614_1_);
    }

    public boolean isReturning() {
        return this.entityData.get(RETURNING).booleanValue();
    }

    public void setReturning(boolean returning) {
        this.entityData.set(RETURNING, returning);
    }

    public boolean isFasterAnimation() {
        return this.entityData.get(FASTER_ANIM).booleanValue();
    }

    public void setFasterAnimation(boolean anim) {
        this.entityData.set(FASTER_ANIM, anim);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void tick() {
        double yMot = MathHelper.sqrt(this.getDeltaMovement().x * this.getDeltaMovement().x + this.getDeltaMovement().z * this.getDeltaMovement().z);
        this.xRot = (float) (MathHelper.atan2(this.getDeltaMovement().y, yMot) * (double) (180F / (float) Math.PI));
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        super.tick();
        Vector3d vector3d = this.getDeltaMovement();
        RayTraceResult raytraceresult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != RayTraceResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
            this.onImpact(raytraceresult);
        }
        Entity shooter = this.getOwner();
        if(this.isReturning() && shooter instanceof EntityCachalotWhale){
            EntityCachalotWhale whale = (EntityCachalotWhale)shooter;
            if(whale.headPart.distanceTo(this) < whale.headPart.getBbWidth()){
                this.remove();
                whale.recieveEcho();
            }

        }
        if(!playerLaunched && !level.isClientSide && !this.isInWaterOrBubble()){
            remove();
        }
        if (this.tickCount > 100) {
            remove();
        }

        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;

        this.updateRotation();
        float f = 0.99F;
        float f1 = 0.06F;
        if(playerLaunched){
            this.noPhysics = true;
        }
        this.setDeltaMovement(vector3d.scale(0.99F));
        this.setNoGravity(true);
        this.setPos(d0, d1, d2);
        this.yRot = (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) (180F / (float) Math.PI)) - 90;

    }

    protected void onEntityHit(EntityRayTraceResult result) {
        Entity entity = this.getOwner();
        if (isReturning()) {
            EntityCachalotWhale whale = null;
            if (entity instanceof EntityCachalotWhale) {
                whale = (EntityCachalotWhale) entity;
                if (result.getEntity() instanceof EntityCachalotWhale || result.getEntity() instanceof EntityCachalotPart) {
                    whale.recieveEcho();
                    this.remove();
                }
            }
        } else if (result.getEntity() != entity && !result.getEntity().is(entity)) {
            this.setReturning(true);
            if (entity instanceof EntityCachalotWhale) {
                Vector3d vec = ((EntityCachalotWhale) entity).getReturnEchoVector();
                double d0 = vec.x() - this.getX();
                double d1 = vec.y() - this.getY();
                double d2 = vec.z() - this.getZ();
                this.setDeltaMovement(Vector3d.ZERO);
                EntityCachalotEcho echo = new EntityCachalotEcho(this.level, ((EntityCachalotWhale) entity));
                echo.copyPosition(this);
                this.remove();
                echo.setReturning(true);
                echo.shoot(d0, d1, d2, 1, 0);
                if (!level.isClientSide) {
                    level.addFreshEntity(echo);
                }
            }
        }
    }

    protected void onHitBlock(BlockRayTraceResult p_230299_1_) {
        BlockState blockstate = this.level.getBlockState(p_230299_1_.getBlockPos());
        if (!this.level.isClientSide && !playerLaunched) {
            this.remove();
        }
    }

    protected void defineSynchedData() {
        this.entityData.define(RETURNING, false);
        this.entityData.define(FASTER_ANIM, false);
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUUID();
            this.ownerNetworkId = entityIn.getId();
        }

    }

    @Nullable
    public Entity getOwner() {
        if (this.ownerUUID != null && this.level instanceof ServerWorld) {
            return ((ServerWorld) this.level).getEntity(this.ownerUUID);
        } else {
            return this.ownerNetworkId != 0 ? this.level.getEntity(this.ownerNetworkId) : null;
        }
    }

    protected void addAdditionalSaveData(CompoundNBT compound) {
        if (this.ownerUUID != null) {
            compound.putUUID("Owner", this.ownerUUID);
        }

        if (this.leftOwner) {
            compound.putBoolean("LeftOwner", true);
        }

    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readAdditionalSaveData(CompoundNBT compound) {
        if (compound.hasUUID("Owner")) {
            this.ownerUUID = compound.getUUID("Owner");
        }

        this.leftOwner = compound.getBoolean("LeftOwner");
    }

    private boolean checkLeftOwner() {
        Entity entity = this.getOwner();
        if (entity != null) {
            for (Entity entity1 : this.level.getEntities(this, this.getBoundingBox().expandTowards(this.getDeltaMovement()).inflate(1.0D), (p_234613_0_) -> {
                return !p_234613_0_.isSpectator() && p_234613_0_.isPickable();
            })) {
                if (entity1.getRootVehicle() == entity.getRootVehicle()) {
                    return false;
                }
            }
        }

        return true;
    }

    public void shoot(double x, double y, double z, float velocity, float inaccuracy) {
        Vector3d vector3d = (new Vector3d(x, y, z)).normalize().add(this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy, this.random.nextGaussian() * (double) 0.0075F * (double) inaccuracy).scale(velocity);
        this.setDeltaMovement(vector3d);
        float f = MathHelper.sqrt(getHorizontalDistanceSqr(vector3d));
        this.yRot = (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) (180F / (float) Math.PI));
        this.xRot = (float) (MathHelper.atan2(vector3d.y, f) * (double) (180F / (float) Math.PI));
        this.yRotO = this.yRot;
        this.xRotO = this.xRot;
    }

    public void shootFromRotation(Entity p_234612_1_, float p_234612_2_, float p_234612_3_, float p_234612_4_, float p_234612_5_, float p_234612_6_) {
        float f = -MathHelper.sin(p_234612_3_ * ((float) Math.PI / 180F)) * MathHelper.cos(p_234612_2_ * ((float) Math.PI / 180F));
        float f1 = -MathHelper.sin((p_234612_2_ + p_234612_4_) * ((float) Math.PI / 180F));
        float f2 = MathHelper.cos(p_234612_3_ * ((float) Math.PI / 180F)) * MathHelper.cos(p_234612_2_ * ((float) Math.PI / 180F));
        this.shoot(f, f1, f2, p_234612_5_, p_234612_6_);
        Vector3d vector3d = p_234612_1_.getDeltaMovement();
        this.setDeltaMovement(this.getDeltaMovement().add(vector3d.x, p_234612_1_.isOnGround() ? 0.0D : vector3d.y, vector3d.z));
    }

    /**
     * Called when this EntityFireball hits a block or entity.
     */
    protected void onImpact(RayTraceResult result) {
        RayTraceResult.Type raytraceresult$type = result.getType();
        if(playerLaunched){
            return;
        }
        if (raytraceresult$type == RayTraceResult.Type.ENTITY) {
            this.onEntityHit((EntityRayTraceResult) result);
        } else if (raytraceresult$type == RayTraceResult.Type.BLOCK) {
            this.onHitBlock((BlockRayTraceResult) result);
        }

    }

    @OnlyIn(Dist.CLIENT)
    public void lerpMotion(double x, double y, double z) {
        this.setDeltaMovement(x, y, z);
        if (this.xRotO == 0.0F && this.yRotO == 0.0F) {
            float f = MathHelper.sqrt(x * x + z * z);
            this.xRot = (float) (MathHelper.atan2(y, f) * (double) (180F / (float) Math.PI));
            this.yRot = (float) (MathHelper.atan2(x, z) * (double) (180F / (float) Math.PI));
            this.xRotO = this.xRot;
            this.yRotO = this.yRot;
            this.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, this.xRot);
        }

    }

    protected boolean canHitEntity(Entity p_230298_1_) {
        if(playerLaunched){
            return false;
        }
        if (this.isReturning()) {
            return p_230298_1_ instanceof EntityCachalotPart || p_230298_1_ instanceof EntityCachalotWhale;
        } else if (p_230298_1_ instanceof EntityCachalotPart) {
            return false;
        }
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.isPickable()) {
            Entity entity = this.getOwner();
            return (entity == null || this.leftOwner || !entity.isPassengerOfSameVehicle(p_230298_1_));
        } else {
            return false;
        }
    }

    protected void updateRotation() {
        Vector3d vector3d = this.getDeltaMovement();
        float f = MathHelper.sqrt(getHorizontalDistanceSqr(vector3d));
        this.xRot = lerpRotation(this.xRotO, (float) (MathHelper.atan2(vector3d.y, f) * (double) (180F / (float) Math.PI)));
        this.yRot = lerpRotation(this.yRotO, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) (180F / (float) Math.PI)));
    }
}