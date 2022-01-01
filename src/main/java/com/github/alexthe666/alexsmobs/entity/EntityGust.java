package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.math.*;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

import java.util.List;

public class EntityGust extends Entity {
    protected static final DataParameter<Boolean> VERTICAL = EntityDataManager.defineId(EntityGust.class, DataSerializers.BOOLEAN);
    protected static final DataParameter<Float> X_DIR = EntityDataManager.defineId(EntityGust.class, DataSerializers.FLOAT);
    protected static final DataParameter<Float> Y_DIR = EntityDataManager.defineId(EntityGust.class, DataSerializers.FLOAT);
    protected static final DataParameter<Float> Z_DIR = EntityDataManager.defineId(EntityGust.class, DataSerializers.FLOAT);
    private Entity pushedEntity = null;

    public EntityGust(EntityType p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityGust(World worldIn) {
        this(AMEntityRegistry.GUST, worldIn);
    }

    public EntityGust(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.GUST, world);
    }

    public void push(Entity entityIn) {

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

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void tick() {
        super.tick();
        if(this.tickCount > 300){
            this.remove();
        }
        for (int i = 0; i < 1 + random.nextInt(1); ++i) {
            level.addParticle(AMParticleRegistry.GUSTER_SAND_SPIN, this.getX() + 0.5F * (random.nextFloat() - 0.5F), this.getY() + 0.5F * (random.nextFloat() - 0.5F), this.getZ() + 0.5F * (random.nextFloat() - 0.5F), this.getX(), this.getY() + 0.5F, this.getZ());
        }
        Vector3d vector3d = new Vector3d(this.entityData.get(X_DIR), this.entityData.get(Y_DIR), this.entityData.get(Z_DIR));
        RayTraceResult raytraceresult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != RayTraceResult.Type.MISS && tickCount > 4) {
            this.onImpact(raytraceresult);
        }
        List<Entity> list = this.level.getEntitiesOfClass(Entity.class, this.getBoundingBox().inflate(0.1));

        if(pushedEntity != null && this.distanceTo(pushedEntity) > 2){
            pushedEntity = null;
        }
        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        if(this.getY() > this.level.getMaxBuildHeight()){
            this.remove();
        }
        this.updateRotation();
        float f = 0.99F;
        float f1 = 0.06F;
         if (this.isInWaterOrBubble()) {
            this.remove();
        } else {
            this.setDeltaMovement(vector3d);
            this.setDeltaMovement(this.getDeltaMovement().add(0.0D, -0.06F, 0.0D));
            this.setPos(d0, d1, d2);
            if(pushedEntity != null){
                pushedEntity.setDeltaMovement(this.getDeltaMovement().add(0, 0.063, 0));
            }
            for(Entity e : list){
                e.setDeltaMovement(this.getDeltaMovement().add(0, 0.068, 0));
                if(e.getDeltaMovement().y < 0){
                    e.setDeltaMovement(e.getDeltaMovement().multiply(1, 0, 1));
                }
                e.fallDistance = 0;
            }
        }
    }

    public void setGustDir(float x, float y, float z){
        this.entityData.set(X_DIR, x);
        this.entityData.set(Y_DIR, y);
        this.entityData.set(Z_DIR, z);
    }

    public float getGustDir(int xyz){
       return this.entityData.get(xyz == 2 ? Z_DIR : xyz == 1 ? Y_DIR : X_DIR);
    }

    protected void onEntityHit(EntityRayTraceResult result) {
        Entity entity = result.getEntity();
        if(entity instanceof EntityGust){
            EntityGust other = (EntityGust)entity;
            double avgX = (other.getX() + this.getX()) / 2F;
            double avgY = (other.getY() + this.getY()) / 2F;
            double avgZ = (other.getZ() + this.getZ()) / 2F;
            other.setPos(avgX, avgY, avgZ);
            other.setGustDir(other.getGustDir(0) + this.getGustDir(0), other.getGustDir(1) + this.getGustDir(1), other.getGustDir(2) + this.getGustDir(2));
            if(this.isAlive() && other.isAlive()){
                this.remove();
            }
        }else if(entity != null){
            pushedEntity = entity;
        }
    }


    protected boolean canHitEntity(Entity p_230298_1_) {
        return !p_230298_1_.isSpectator();
    }

    protected void onHitBlock(BlockRayTraceResult p_230299_1_) {
        if( p_230299_1_.getBlockPos() != null){
            BlockPos pos = p_230299_1_.getBlockPos();
            if(level.getBlockState(pos).getMaterial().isSolid()){
                if (!this.level.isClientSide) {
                    this.remove();

                }
            }
        }

    }

    protected void defineSynchedData() {
        this.entityData.define(VERTICAL, false);
        this.entityData.define(X_DIR, 0f);
        this.entityData.define(Y_DIR, 0F);
        this.entityData.define(Z_DIR, 0F);
    }

    protected void addAdditionalSaveData(CompoundNBT compound) {
        compound.putBoolean("VerticalTornado", getVertical());
        compound.putFloat("GustDirX", this.entityData.get(X_DIR));
        compound.putFloat("GustDirY", this.entityData.get(Y_DIR));
        compound.putFloat("GustDirZ", this.entityData.get(Z_DIR));
    }

    /**
     * (abstract) Protected helper method to read subclass entity data from NBT.
     */
    protected void readAdditionalSaveData(CompoundNBT compound) {
        this.entityData.set(X_DIR, compound.getFloat("GustDirX"));
        this.entityData.set(Y_DIR, compound.getFloat("GustDirX"));
        this.entityData.set(Z_DIR, compound.getFloat("GustDirX"));
        this.setVertical((compound.getBoolean("VerticalTornado")));
    }

    public void setVertical(boolean vertical){
        this.entityData.set(VERTICAL, vertical);
    }

    public boolean getVertical(){
        return this.entityData.get(VERTICAL);
    }

    protected void onImpact(RayTraceResult result) {
        RayTraceResult.Type raytraceresult$type = result.getType();
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

    protected void updateRotation() {
        Vector3d vector3d = this.getDeltaMovement();
        float f = MathHelper.sqrt(getHorizontalDistanceSqr(vector3d));
        this.xRot = lerpRotation(this.xRotO, (float) (MathHelper.atan2(vector3d.y, f) * (double) (180F / (float) Math.PI)));
        this.yRot = lerpRotation(this.yRotO, (float) (MathHelper.atan2(vector3d.x, vector3d.z) * (double) (180F / (float) Math.PI)));
    }
}
