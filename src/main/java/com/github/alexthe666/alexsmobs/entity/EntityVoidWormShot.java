package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.monster.VexEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileHelper;
import net.minecraft.item.Items;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.potion.EffectInstance;
import net.minecraft.potion.Effects;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.SoundEvents;
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
import java.util.List;
import java.util.UUID;

public class EntityVoidWormShot extends Entity {
    private UUID ownerUUID;
    private int ownerNetworkId;
    private boolean leftOwner;
    private static final DataParameter<Boolean> PORTALLING = EntityDataManager.defineId(EntityVoidWormShot.class, DataSerializers.BOOLEAN);

    public EntityVoidWormShot(EntityType p_i50162_1_, World p_i50162_2_) {
        super(p_i50162_1_, p_i50162_2_);
    }

    public EntityVoidWormShot(World worldIn, EntityVoidWorm p_i47273_2_) {
        this(AMEntityRegistry.VOID_WORM_SHOT, worldIn);
        this.setShooter(p_i47273_2_);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getBbWidth() + 1.0F) * 0.35D * (double) MathHelper.sin(p_i47273_2_.yBodyRot * ((float) Math.PI / 180F)), p_i47273_2_.getY() + (double) 1F, p_i47273_2_.getZ() + (double) (p_i47273_2_.getBbWidth() + 1.0F) * 0.35D * (double) MathHelper.cos(p_i47273_2_.yBodyRot * ((float) Math.PI / 180F)));
    }

    public EntityVoidWormShot(World worldIn, LivingEntity p_i47273_2_, boolean right) {
        this(AMEntityRegistry.VOID_WORM_SHOT, worldIn);
        this.setShooter(p_i47273_2_);
        float rot = p_i47273_2_.yHeadRot + (right ? 60 : -60);
        this.setPos(p_i47273_2_.getX() - (double) (p_i47273_2_.getBbWidth()) * 0.9F * (double) MathHelper.sin(rot * ((float) Math.PI / 180F)), p_i47273_2_.getY() + (double) 1F, p_i47273_2_.getZ() + (double) (p_i47273_2_.getBbWidth()) * 0.9D * (double) MathHelper.cos(rot * ((float) Math.PI / 180F)));
    }

    @OnlyIn(Dist.CLIENT)
    public EntityVoidWormShot(World worldIn, double x, double y, double z, double p_i47274_8_, double p_i47274_10_, double p_i47274_12_) {
        this(AMEntityRegistry.VOID_WORM_SHOT, worldIn);
        this.setPos(x, y, z);
        this.setDeltaMovement(p_i47274_8_, p_i47274_10_, p_i47274_12_);
    }

    public EntityVoidWormShot(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.VOID_WORM_SHOT, world);
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
        if (!this.leftOwner) {
            this.leftOwner = this.checkLeftOwner();
        }
        if (this.tickCount > 40) {
            Entity entity = this.getShooter();
            if(isPortalType()){
                this.setDeltaMovement(Vector3d.ZERO);
                if (this.tickCount > 60) {
                    this.remove();
                }
            }else{
                if (entity instanceof MobEntity && ((MobEntity) entity).getTarget() != null) {
                    LivingEntity target = ((MobEntity) entity).getTarget();
                    if(target == null){
                        this.kill();
                    }
                    double d0 = target.getX() - this.getX();
                    double d1 = target.getY() + target.getBbHeight() * 0.5F - this.getY();
                    double d2 = target.getZ() - this.getZ();
                    Vector3d vector3d = new Vector3d(d0, d1, d2);
                    float speed = 0.05F;
                    shoot(d0, d1, d2, 1, 0);
                    this.yRot = -((float) MathHelper.atan2(d0, d2)) * (180F / (float) Math.PI);
                }
            }
        }
        super.tick();
        Vector3d vector3d = this.getDeltaMovement();
        RayTraceResult raytraceresult = ProjectileHelper.getHitResult(this, this::canHitEntity);
        if (raytraceresult != null && raytraceresult.getType() != RayTraceResult.Type.MISS && !net.minecraftforge.event.ForgeEventFactory.onProjectileImpact(this, raytraceresult)) {
            this.onImpact(raytraceresult);
        }
        double d0 = this.getX() + vector3d.x;
        double d1 = this.getY() + vector3d.y;
        double d2 = this.getZ() + vector3d.z;
        this.setNoGravity(true);
        this.updateRotation();
        float f = 0.99F;
        float f1 = 0.06F;
        if (this.level.getBlockStates(this.getBoundingBox()).noneMatch(AbstractBlock.AbstractBlockState::isAir)) {
            this.remove();
        } else if (this.isInWaterOrBubble()) {
            this.remove();
        } else {
            this.setDeltaMovement(vector3d.scale(0.99F));
            this.setPos(d0, d1, d2);
        }
    }

    protected void onEntityHit(EntityRayTraceResult p_213868_1_) {
        Entity entity = this.getShooter();
        if (entity instanceof LivingEntity && !(p_213868_1_.getEntity() instanceof EntityVoidWorm || p_213868_1_.getEntity() instanceof EntityVoidWormPart)) {
            boolean b = wormAttack(p_213868_1_.getEntity(), DamageSource.indirectMobAttack(this, (LivingEntity) entity).setProjectile(), (float) (AMConfig.voidWormDamageModifier * 4F));
            if(b && p_213868_1_.getEntity() instanceof PlayerEntity){
                PlayerEntity player = ((PlayerEntity)p_213868_1_.getEntity());
                if(player.getUseItem().isShield(player)){
                    player.disableShield(true);
                }
            }
        }

        this.remove();
    }

    private boolean wormAttack(Entity entity, DamageSource source, float dmg){
        return entity.hurt(source, dmg);
    }


    protected void onHitBlock(BlockRayTraceResult p_230299_1_) {
        BlockState blockstate = this.level.getBlockState(p_230299_1_.getBlockPos());
        if (!this.level.isClientSide) {
            this.remove();
        }
    }

    protected void defineSynchedData() {
        this.entityData.define(PORTALLING, false);
    }

    public boolean isPortalType(){
        return this.entityData.get(PORTALLING);
    }

    public void setPortalType(boolean portalType){
        this.entityData.set(PORTALLING, portalType);
    }

    public void setShooter(@Nullable Entity entityIn) {
        if (entityIn != null) {
            this.ownerUUID = entityIn.getUUID();
            this.ownerNetworkId = entityIn.getId();
        }

    }

    @Nullable
    public Entity getShooter() {
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
        Entity entity = this.getShooter();
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
        if (raytraceresult$type == RayTraceResult.Type.ENTITY) {
            this.onEntityHit((EntityRayTraceResult) result);
        } else if (raytraceresult$type == RayTraceResult.Type.BLOCK) {
            this.onHitBlock((BlockRayTraceResult) result);
        }
        this.playSound(SoundEvents.GLASS_BREAK, 1F, 0.5F);
        Entity entity = this.getShooter();
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
        if (!p_230298_1_.isSpectator() && p_230298_1_.isAlive() && p_230298_1_.isPickable()) {
            Entity entity = this.getShooter();
            return (entity == null || this.leftOwner || !entity.isPassengerOfSameVehicle(p_230298_1_)) && !(p_230298_1_ instanceof EntityVoidWormShot || p_230298_1_ instanceof EntityVoidWormPart);
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
