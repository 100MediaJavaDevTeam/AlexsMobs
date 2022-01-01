package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.message.MessageHurtMultipart;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityCentipedeBody extends MobEntity implements IHurtableMultipart {

    private static final DataParameter<Integer> BODYINDEX = EntityDataManager.defineId(EntityCentipedeBody.class, DataSerializers.INT);
    private static final DataParameter<Optional<UUID>> PARENT_UUID = EntityDataManager.defineId(EntityCentipedeBody.class, DataSerializers.OPTIONAL_UUID);
    public EntitySize multipartSize;
    protected float radius;
    protected float angleYaw;
    protected float offsetY;
    protected float damageMultiplier = 1;
    private float parentYaw = 0;
    protected EntityCentipedeBody(EntityType type, World worldIn) {
        super(type, worldIn);
        multipartSize = type.getDimensions();
    }

    public boolean requiresCustomPersistence() {
        return super.requiresCustomPersistence() || this.getParent() != null;
    }


    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return  source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || super.isInvulnerableTo(source);
    }

    public CreatureAttribute getMobType() {
        return CreatureAttribute.ARTHROPOD;
    }

    @Override
    public void tick() {
        super.tick();
        isInsidePortal = false;
        Entity parent = getParent();
        refreshDimensions();
        if (parent != null && !level.isClientSide) {
            float f = this.distanceTo(parent);
            this.setNoGravity(true);
            this.lookAt(parent, 1, 1);
            this.parentYaw = this.limitAngle(this.parentYaw, parent.yRotO, 5.0F);
            double yD1 = (parent.getY() - this.getY()) / (double) f;
            double ySet = parent.yo;
            if (!level.getBlockState(new BlockPos(this.getX(), ySet - 0.1, this.getZ())).canOcclude()) {
                ySet = parent.yo - 0.2F;
            }
            if (this.isInWall() || level.getBlockState(new BlockPos(this.getX(), ySet, this.getZ())).canOcclude()) {
                ySet = parent.yo + 0.2F;
            }
            double yaw = parentYaw;
            double x = parent.xo + this.radius * Math.cos(yaw * (Math.PI / 180.0F) + this.angleYaw);
            double z = parent.zo + this.radius * Math.sin(yaw * (Math.PI / 180.0F) + this.angleYaw);
            this.setPos(x, ySet, z);
            double d0 = parent.getX() - this.getX();
            double d1 = parent.getY() - this.getY();
            double d2 = parent.getZ() - this.getZ();
            double d3 = d0 * d0 + d1 * d1 + d2 * d2;
            float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt(d0 * d0 + d2 * d2)) * (double) (180F / (float) Math.PI)));
            this.xRot = this.limitAngle(this.xRot, f2, 5.0F);
            this.markHurt();
            this.yRot = parentYaw;
            this.yHeadRot = this.yRot;
            this.yBodyRot = this.yRotO;
            if (parent instanceof LivingEntity) {
                if(!level.isClientSide && (((LivingEntity) parent).hurtTime > 0 || ((LivingEntity) parent).deathTime > 0)){
                    AlexsMobs.sendMSGToAll(new MessageHurtMultipart(this.getId(), parent.getId(), 0));
                    this.hurtTime = ((LivingEntity) parent).hurtTime;
                    this.deathTime = ((LivingEntity) parent).deathTime;
                }
            }
            this.pushEntities();
            if ((parent.removed) && !level.isClientSide) {
                this.remove();
            }
        }
        if (parent == null && !level.isClientSide) {
            this.remove();
        }
    }

    protected float limitAngle(float sourceAngle, float targetAngle, float maximumChange) {
        float f = MathHelper.wrapDegrees(targetAngle - sourceAngle);
        if (f > maximumChange) {
            f = maximumChange;
        }

        if (f < -maximumChange) {
            f = -maximumChange;
        }

        float f1 = sourceAngle + f;
        if (f1 < 0.0F) {
            f1 += 360.0F;
        } else if (f1 > 360.0F) {
            f1 -= 360.0F;
        }

        return f1;
    }

    public void setInitialPartPos(LivingEntity parent, int index) {
        double radAdd = this.radius * index;
        this.yRot = parent.yRot;
        this.yBodyRot = parent.yBodyRot;
        this.parentYaw = parent.yRot;
        this.setPos(parent.xo +  radAdd * Math.cos(parent.yRot * (Math.PI / 180.0F) + this.angleYaw), parent.yo + this.offsetY, parent.zo + radAdd * Math.sin(parent.yRot * (Math.PI / 180.0F) + this.angleYaw));
    }

    public EntityCentipedeBody(EntityType t, LivingEntity parent, float radius, float angleYaw, float offsetY) {
        super(t, parent.level);
        this.setParent(parent);
        this.radius = radius;
        this.angleYaw = (angleYaw + 90.0F) * ((float) Math.PI / 180.0F);
        this.offsetY = offsetY;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        if (this.getParentId() != null) {
            compound.putUUID("ParentUUID", this.getParentId());
        }
        compound.putInt("BodyIndex", getBodyIndex());
        compound.putFloat("PartAngle", angleYaw);
        compound.putFloat("PartRadius", radius);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("ParentUUID")) {
            this.setParentId(compound.getUUID("ParentUUID"));
        }
        this.setBodyIndex(compound.getInt("BodyIndex"));
        this.angleYaw = compound.getFloat("PartAngle");
        this.radius = compound.getFloat("PartRadius");
    }
    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(BODYINDEX, 0);
    }

    public Entity getParent() {
        UUID id = getParentId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
    }

    public void setParent(Entity entity) {
        this.setParentId(entity.getUUID());
    }

    @Override
    public boolean is(net.minecraft.entity.Entity entity) {
        return this == entity || this.getParent() == entity;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        Entity parent = getParent();
        boolean prev = parent != null && parent.hurt(source, damage * this.damageMultiplier);
        if (prev && !level.isClientSide) {
            AlexsMobs.sendMSGToAll(new MessageHurtMultipart(this.getId(), parent.getId(), damage * this.damageMultiplier));
        }
        return prev;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public void pushEntities() {
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> entity != parent && !(entity instanceof EntityCentipedeBody) && entity.isPushable()).forEach(entity -> entity.push(parent));

        }
    }

    public boolean startRiding(Entity entityIn) {
        if(!(entityIn instanceof AbstractMinecartEntity || entityIn instanceof BoatEntity)){
            return super.startRiding(entityIn);
        }
        return false;
    }

    public int getBodyIndex() {
        return this.entityData.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.entityData.set(BODYINDEX, index);
    }

    @Nullable
    public UUID getParentId() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 10.0D).add(Attributes.FOLLOW_RANGE, 32.0D).add(Attributes.ARMOR, 6.0D).add(Attributes.ATTACK_DAMAGE, 8.0D).add(Attributes.KNOCKBACK_RESISTANCE, 0.5F).add(Attributes.MOVEMENT_SPEED, 0.25F);
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage) {
        if(parent.deathTime > 0){
            this.deathTime = parent.deathTime;
        }
        if(parent.hurtTime > 0){
            this.hurtTime = parent.hurtTime;
        }
    }
}