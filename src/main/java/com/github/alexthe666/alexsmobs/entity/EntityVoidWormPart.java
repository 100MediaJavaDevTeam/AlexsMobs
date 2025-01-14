package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.AlexsMobs;
import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.config.AMConfig;
import com.github.alexthe666.alexsmobs.message.MessageHurtMultipart;
import com.github.alexthe666.alexsmobs.misc.AMAdvancementTriggerRegistry;
import com.google.common.collect.ImmutableList;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifierMap;
import net.minecraft.entity.ai.attributes.Attributes;
import net.minecraft.entity.item.BoatEntity;
import net.minecraft.entity.item.minecart.AbstractMinecartEntity;
import net.minecraft.entity.monster.MonsterEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.EquipmentSlotType;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.ActionResultType;
import net.minecraft.util.DamageSource;
import net.minecraft.util.Hand;
import net.minecraft.util.HandSide;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityVoidWormPart extends LivingEntity implements IHurtableMultipart {

    protected static final EntitySize SIZE_BASE = EntitySize.scalable(1.2F, 1.95F);
    protected static final EntitySize TAIL_SIZE = EntitySize.scalable(1.6F, 2F);
    private static final DataParameter<Boolean> TAIL = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.BOOLEAN);
    private static final DataParameter<Integer> BODYINDEX = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.INT);
    private static final DataParameter<Float> WORM_SCALE = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> WORM_YAW = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.FLOAT);
    private static final DataParameter<Float> WORM_ANGLE = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.FLOAT);
    private static final DataParameter<Optional<UUID>> PARENT_UUID = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Optional<UUID>> CHILD_UUID = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.OPTIONAL_UUID);
    private static final DataParameter<Integer> PORTAL_TICKS = EntityDataManager.defineId(EntityVoidWormPart.class, DataSerializers.INT);
    public EntitySize multipartSize;
    public float prevWormAngle;
    protected float radius;
    protected float angleYaw;
    protected float offsetY;
    protected float damageMultiplier = 1;
    private float prevWormYaw = 0;
    private Vector3d teleportPos = null;
    private Vector3d enterPos = null;
    private boolean doesParentControlPos = false;

    public EntityVoidWormPart(EntityType t, World world) {
        super(t, world);
        multipartSize = t.getDimensions();
    }

    public EntityVoidWormPart(EntityType t, LivingEntity parent, float radius, float angleYaw, float offsetY) {
        super(t, parent.level);
        this.setParent(parent);
        this.radius = radius;
        this.angleYaw = (angleYaw + 90.0F) * ((float) Math.PI / 180.0F);
        this.offsetY = offsetY;
    }

    public static AttributeModifierMap.MutableAttribute bakeAttributes() {
        return MonsterEntity.createMonsterAttributes().add(Attributes.MAX_HEALTH, 30.0D).add(Attributes.MOVEMENT_SPEED, 0.15F);
    }

    public void push(Entity entityIn) {

    }

    public void kill() {
        this.remove();
    }

    public EntitySize getDimensions(Pose poseIn) {
        return this.isTail() ? TAIL_SIZE.scale(getScale()) : super.getDimensions(poseIn);
    }

    public float getWormScale() {
        return entityData.get(WORM_SCALE);
    }

    public void setWormScale(float scale) {
        this.entityData.set(WORM_SCALE, scale);
    }

    public float getScale() {
        return getWormScale() + 0.5F;
    }

    public boolean startRiding(Entity entityIn) {
        if (!(entityIn instanceof AbstractMinecartEntity || entityIn instanceof BoatEntity)) {
            return super.startRiding(entityIn);
        }
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return source == DamageSource.FALL || source == DamageSource.DROWN || source == DamageSource.OUT_OF_WORLD  || source == DamageSource.IN_WALL || source == DamageSource.FALLING_BLOCK || source == DamageSource.LAVA || source.isFire() || super.isInvulnerableTo(source);
    }

    @Override
    public net.minecraft.entity.Entity getEntity() {
        return this;
    }

    public void addAdditionalSaveData(CompoundNBT compound) {
        super.addAdditionalSaveData(compound);
        if (this.getParentId() != null) {
            compound.putUUID("ParentUUID", this.getParentId());
        }
        if (this.getChildId() != null) {
            compound.putUUID("ChildUUID", this.getChildId());
        }
        compound.putBoolean("TailPart", isTail());
        compound.putInt("BodyIndex", getBodyIndex());
        compound.putInt("PortalTicks", getPortalTicks());
        compound.putFloat("PartAngle", angleYaw);
        compound.putFloat("WormScale", this.getWormScale());
        compound.putFloat("PartRadius", radius);
        compound.putFloat("PartYOffset", offsetY);
    }

    public void readAdditionalSaveData(CompoundNBT compound) {
        super.readAdditionalSaveData(compound);
        if (compound.hasUUID("ParentUUID")) {
            this.setParentId(compound.getUUID("ParentUUID"));
        }
        if (compound.hasUUID("ChildUUID")) {
            this.setChildId(compound.getUUID("ChildUUID"));
        }
        this.setTail(compound.getBoolean("TailPart"));
        this.setBodyIndex(compound.getInt("BodyIndex"));
        this.setPortalTicks(compound.getInt("PortalTicks"));
        this.angleYaw = compound.getFloat("PartAngle");
        this.setWormScale(compound.getFloat("WormScale"));
        this.radius = compound.getFloat("PartRadius");
        this.offsetY = compound.getFloat("PartYOffset");
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PARENT_UUID, Optional.empty());
        this.entityData.define(CHILD_UUID, Optional.empty());
        this.entityData.define(TAIL, false);
        this.entityData.define(BODYINDEX, 0);
        this.entityData.define(WORM_SCALE, 1F);
        this.entityData.define(WORM_YAW, 0F);
        this.entityData.define(WORM_ANGLE, 0F);
        this.entityData.define(PORTAL_TICKS, 0);
    }

    @Nullable
    public UUID getParentId() {
        return this.entityData.get(PARENT_UUID).orElse(null);
    }

    public void setParentId(@Nullable UUID uniqueId) {
        this.entityData.set(PARENT_UUID, Optional.ofNullable(uniqueId));
    }

    @Nullable
    public UUID getChildId() {
        return this.entityData.get(CHILD_UUID).orElse(null);
    }

    public void setChildId(@Nullable UUID uniqueId) {
        this.entityData.set(CHILD_UUID, Optional.ofNullable(uniqueId));
    }

    public void setInitialPartPos(Entity parent) {
        this.setPos(parent.xo + this.radius * Math.cos(parent.yRot * (Math.PI / 180.0F) + this.angleYaw), parent.yo + this.offsetY, parent.zo + this.radius * Math.sin(parent.yRot * (Math.PI / 180.0F) + this.angleYaw));
    }

    public float getWormAngle() {
        return this.entityData.get(WORM_ANGLE);
    }

    public void setWormAngle(float progress) {
        this.entityData.set(WORM_ANGLE, progress);
    }

    public int getPortalTicks() {
        return this.entityData.get(PORTAL_TICKS).intValue();
    }

    public void setPortalTicks(int ticks) {
        this.entityData.set(PORTAL_TICKS, Integer.valueOf(ticks));
    }

    @Override
    public void tick() {
        isInsidePortal = false;
        prevWormAngle = this.getWormAngle();
        prevWormYaw = this.entityData.get(WORM_YAW);
        this.setDeltaMovement(Vector3d.ZERO);
        radius = 1.0F + (this.getWormScale() * (this.isTail() ? 0.65F : 0.3F)) + (this.getBodyIndex() == 0 ? 0.8F : 0);
        if (this.tickCount > 3) {
            Entity parent = getParent();
            refreshDimensions();
            if (parent != null && !level.isClientSide) {
                this.setNoGravity(true);
                Vector3d parentVec = parent.position().subtract(parent.xo, parent.yo, parent.zo);
                double restrictRadius = MathHelper.clamp(radius - parentVec.lengthSqr() * 0.25F, radius * 0.5F, radius);
                if (parent instanceof EntityVoidWorm) {
                    restrictRadius *= (isTail() ? 0.8F : 0.4F);
                }
                double x = parent.getX() + restrictRadius * Math.cos(parent.yRot * (Math.PI / 180.0F) + this.angleYaw);
                double yStretch = Math.abs(parent.getY() - parent.yo) > this.getBbWidth() ? parent.getY() : parent.yo;
                double y = yStretch + this.offsetY * getWormScale();
                double z = parent.getZ() + restrictRadius * Math.sin(parent.yRot * (Math.PI / 180.0F) + this.angleYaw);

                double d0 = parent.xo - this.getX();
                double d1 = parent.yo - this.getY();
                double d2 = parent.zo - this.getZ();
                float yaw = (float) (MathHelper.atan2(d2, d0) * (double) (180F / (float) Math.PI)) - 90.0F;
                float pitch = parent.xRot;
                if (this.getPortalTicks() <= 1 && !doesParentControlPos) {
                    double d3 = d0 * d0 + d1 * d1 + d2 * d2;
                    float f2 = -((float) (MathHelper.atan2(d1, MathHelper.sqrt(d0 * d0 + d2 * d2)) * (double) (180F / (float) Math.PI)));
                    this.setPos(x, y, z);
                    this.xRot = this.limitAngle(this.xRot, f2, 5.0F);
                    this.yRot = yaw;
                    this.entityData.set(WORM_YAW, yRot);
                }
                this.markHurt();
                this.yHeadRot = this.yRot;
                this.yBodyRot = pitch;
                if (parent instanceof LivingEntity) {
                    if (!level.isClientSide && (((LivingEntity) parent).hurtTime > 0 || ((LivingEntity) parent).deathTime > 0)) {
                        AlexsMobs.sendMSGToAll(new MessageHurtMultipart(this.getId(), parent.getId(), 0));
                        this.hurtTime = ((LivingEntity) parent).hurtTime;
                        this.deathTime = ((LivingEntity) parent).deathTime;
                    }
                }
                this.pushEntities();
                if (parent.removed && !level.isClientSide) {
                    this.remove();
                }
                if (parent instanceof EntityVoidWorm) {
                    this.setWormAngle(((EntityVoidWorm) parent).prevWormAngle);
                } else if (parent instanceof EntityVoidWormPart) {
                    this.setWormAngle(((EntityVoidWormPart) parent).prevWormAngle);
                }
            } else if (tickCount > 20 && !level.isClientSide) {
                remove();
            }
        }
        if (tickCount % 400 == 0) {
            this.heal(1);
        }
        super.tick();
        if(doesParentControlPos && enterPos != null){
            this.teleportTo(enterPos.x, enterPos.y, enterPos.z);
        }
        if (this.getPortalTicks() > 0) {
            this.setPortalTicks(this.getPortalTicks() - 1);
            if (this.getPortalTicks() <= 5 && teleportPos != null) {
                Vector3d vec = teleportPos;
                this.teleportTo(vec.x, vec.y, vec.z);
                xOld = vec.x;
                yOld = vec.y;
                zOld = vec.z;
                if (this.getPortalTicks() == 5 && this.getChild() instanceof EntityVoidWormPart) {
                    ((EntityVoidWormPart) this.getChild()).teleportTo(enterPos, teleportPos);
                }
                teleportPos = null;
            }else if(this.getPortalTicks() > 5 && enterPos != null){
                this.teleportTo(enterPos.x, enterPos.y, enterPos.z);
            }
            if(this.getPortalTicks() == 0){
                doesParentControlPos = false;
            }
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

    protected void tickDeath() {
        ++this.deathTime;
        if (this.deathTime == 20) {
            this.remove(false); //Forge keep data until we revive player
            for(int i = 0; i < 30; ++i) {
                double d0 = this.random.nextGaussian() * 0.02D;
                double d1 = this.random.nextGaussian() * 0.02D;
                double d2 = this.random.nextGaussian() * 0.02D;
                this.level.addParticle(AMParticleRegistry.WORM_PORTAL, this.getRandomX(1.0D), this.getRandomY(), this.getRandomZ(1.0D), d0, d1, d2);
            }
        }

    }

    public void die(DamageSource cause) {
        EntityVoidWorm worm = this.getWorm();
        if (worm != null) {
            int segments = Math.max(worm.getSegmentCount() / 2 - 1, 1);
            worm.setSegmentCount(segments);
            if (this.getChild() instanceof EntityVoidWormPart) {
                EntityVoidWormPart segment = (EntityVoidWormPart) this.getChild();
                EntityVoidWorm worm2 = AMEntityRegistry.VOID_WORM.create(level);
                worm2.copyPosition(this);
                segment.copyPosition(this);
                worm2.setChildId(segment.getUUID());
                worm2.setSegmentCount(segments);
                segment.setParent(worm2);
                if (!level.isClientSide) {
                    level.addFreshEntity(worm2);
                }
                worm2.setSplitter(true);
                worm2.setMaxHealth(worm.getMaxHealth() / 2F, true);
                worm2.setSplitFromUuid(worm.getUUID());
                worm2.setWormSpeed((float) MathHelper.clamp(worm.getWormSpeed() * 0.8, 0.4F, 1F));
                worm2.resetWormScales();
                if(!level.isClientSide) {
                    if (cause != null && cause.getEntity() instanceof ServerPlayerEntity) {
                        AMAdvancementTriggerRegistry.VOID_WORM_SPLIT.trigger((ServerPlayerEntity) cause.getEntity());
                    }
                }
            }
            worm.resetWormScales();
        }
    }

    public void remove() {
        this.remove(false);
    }


    public boolean isAlliedTo(Entity entityIn) {
        EntityVoidWorm worm = this.getWorm();
        return super.isAlliedTo(entityIn) || worm != null && worm.isAlliedTo(entityIn);
    }

    public EntityVoidWorm getWorm() {
        Entity parent = this.getParent();
        while (parent instanceof EntityVoidWormPart) {
            parent = ((EntityVoidWormPart) parent).getParent();
        }
        if (parent instanceof EntityVoidWorm) {
            return (EntityVoidWorm) parent;
        }
        return null;
    }

    public Entity getChild() {
        UUID id = getChildId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
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
    public boolean isPickable() {
        return true;
    }

    @Override
    public HandSide getMainArm() {
        return null;
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void pushEntities() {
        List<Entity> entities = this.level.getEntities(this, this.getBoundingBox().expandTowards(0.20000000298023224D, 0.0D, 0.20000000298023224D));
        Entity parent = this.getParent();
        if (parent != null) {
            entities.stream().filter(entity -> !entity.is(parent) && !(entity instanceof EntityVoidWormPart) && entity.isPushable()).forEach(entity -> entity.push(parent));

        }
    }

    public ActionResultType interact(PlayerEntity player, Hand hand) {
        Entity parent = getParent();

        return parent != null ? parent.interact(player, hand) : ActionResultType.PASS;
    }

    public boolean isHurt() {
        return this.getHealth() <= getHealthThreshold();
    }

    public double getHealthThreshold() {
        return 5D;
    }

    @Override
    public boolean hurt(DamageSource source, float damage) {
        if(super.hurt(source, damage)){
            EntityVoidWorm worm = this.getWorm();
            if(worm != null){
                worm.playHurtSoundWorm(source);
            }
            return true;
        }
        return false;
    }

    @Override
    public Iterable<ItemStack> getArmorSlots() {
        return ImmutableList.of();
    }

    @Override
    public ItemStack getItemBySlot(EquipmentSlotType slotIn) {
        return ItemStack.EMPTY;
    }

    @Override
    public void setItemSlot(EquipmentSlotType slotIn, ItemStack stack) {

    }

    public boolean isTail() {
        return this.entityData.get(TAIL).booleanValue();
    }

    public void setTail(boolean tail) {
        this.entityData.set(TAIL, Boolean.valueOf(tail));
    }

    public int getBodyIndex() {
        return this.entityData.get(BODYINDEX);
    }

    public void setBodyIndex(int index) {
        this.entityData.set(BODYINDEX, index);
    }

    public boolean shouldNotExist() {
        Entity parent = getParent();
        return !parent.isAlive();
    }

    @Override
    public void onAttackedFromServer(LivingEntity parent, float damage) {
        if (parent.deathTime > 0) {
            this.deathTime = parent.deathTime;
        }
        if (parent.hurtTime > 0) {
            this.hurtTime = parent.hurtTime;
        }
    }

    public boolean shouldContinuePersisting() {
        return isAddedToWorld() || this.removed;
    }

    public float getWormYaw(float partialTicks) {
        return partialTicks == 0 ? entityData.get(WORM_YAW) : prevWormYaw + (entityData.get(WORM_YAW) - prevWormYaw) * partialTicks;
    }

    public void teleportTo(Vector3d enterPos, Vector3d to) {
        this.setPortalTicks(10);
        teleportPos = to;
        this.enterPos = enterPos;
        EntityVoidWorm worm = this.getWorm();
        if(worm != null){
            if(this.getChild() == null){
                worm.fullyThrough = true;
            }
        }
    }

}
