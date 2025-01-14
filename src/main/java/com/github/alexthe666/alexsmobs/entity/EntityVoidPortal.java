package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.client.particle.AMParticleRegistry;
import com.github.alexthe666.alexsmobs.event.ServerEvents;
import com.github.alexthe666.alexsmobs.misc.AMSoundRegistry;
import com.github.alexthe666.alexsmobs.misc.AMTagRegistry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.nbt.NBTDynamicOps;
import net.minecraft.network.IPacket;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.network.datasync.EntityDataManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.ITag;
import net.minecraft.tags.Tag;
import net.minecraft.util.Direction;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;
import org.antlr.v4.runtime.misc.Triple;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class EntityVoidPortal extends Entity {

    protected static final DataParameter<Direction> ATTACHED_FACE = EntityDataManager.defineId(EntityVoidPortal.class, DataSerializers.DIRECTION);
    protected static final DataParameter<Integer> LIFESPAN = EntityDataManager.defineId(EntityVoidPortal.class, DataSerializers.INT);
    private static final DataParameter<Optional<BlockPos>> DESTINATION = EntityDataManager.defineId(EntityVoidPortal.class, DataSerializers.OPTIONAL_BLOCK_POS);
    private static final DataParameter<Optional<UUID>> SISTER_UUID = EntityDataManager.defineId(EntityVoidWorm.class, DataSerializers.OPTIONAL_UUID);
    public RegistryKey<World> exitDimension;
    private boolean madeOpenNoise = false;
    private boolean madeCloseNoise = false;
    private boolean isDummy = false;

    public EntityVoidPortal(EntityType<?> entityTypeIn, World worldIn) {
        super(entityTypeIn, worldIn);
    }

    public EntityVoidPortal(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.VOID_PORTAL, world);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public void tick() {
        super.tick();
        if (this.tickCount == 1) {
            if(this.getLifespan() == 0){
                this.setLifespan(100);
            }
        }
        if(!madeOpenNoise){
            this.playSound(AMSoundRegistry.VOID_PORTAL_OPEN, 1.0F, 1 + random.nextFloat() * 0.2F);
            madeOpenNoise = true;
        }
        Direction direction2 = this.getAttachmentFacing().getOpposite();
        float minX = -0.15F;
        float minY = -0.15F;
        float minZ = -0.15F;
        float maxX = 0.15F;
        float maxY = 0.15F;
        float maxZ = 0.15F;
        switch (direction2) {
            case NORTH:
            case SOUTH:
                minX = -1.5F;
                maxX = 1.5F;
                minY = -1.5F;
                maxY = 1.5F;
                break;
            case EAST:
            case WEST:
                minZ = -1.5F;
                maxZ = 1.5F;
                minY = -1.5F;
                maxY = 1.5F;
                break;
            case UP:
            case DOWN:
                minX = -1.5F;
                maxX = 1.5F;
                minZ = -1.5F;
                maxZ = 1.5F;
                break;
        }
        AxisAlignedBB bb = new AxisAlignedBB(this.getX() + minX, this.getY() + minY, this.getZ() + minZ, this.getX() + maxX, this.getY() + maxY, this.getZ() + maxZ);
        this.setBoundingBox(bb);
        if(random.nextFloat() < 0.5F && level.isClientSide && Math.min(tickCount, this.getLifespan()) >= 20){
            double particleX = this.getBoundingBox().minX + random.nextFloat() * (this.getBoundingBox().maxX - this.getBoundingBox().minX);
            double particleY = this.getBoundingBox().minY + random.nextFloat() * (this.getBoundingBox().maxY - this.getBoundingBox().minY);
            double particleZ = this.getBoundingBox().minZ + random.nextFloat() * (this.getBoundingBox().maxZ - this.getBoundingBox().minZ);
            level.addParticle(AMParticleRegistry.WORM_PORTAL, particleX, particleY, particleZ, 0.1 * random.nextGaussian(), 0.1 * random.nextGaussian(), 0.1 * random.nextGaussian());
        }
        ITag<EntityType<?>> tag = EntityTypeTags.getAllTags().getTag(AMTagRegistry.VOID_PORTAL_IGNORES);
        List<Entity> entities = this.level.getEntities(this, bb.deflate(0.2F));
        if (!level.isClientSide) {
            MinecraftServer server = level.getServer();
            if (this.getDestination() != null && this.getLifespan() > 20 && tickCount > 20) {
                BlockPos offsetPos = this.getDestination().relative(this.getAttachmentFacing().getOpposite(), 2);
                for (Entity e : entities) {
                    if(e.isOnPortalCooldown() || e.isShiftKeyDown() || e instanceof EntityVoidPortal || tag != null && tag.contains(e.getType())){
                        continue;
                    }
                    if (e instanceof EntityVoidWormPart) {
                        if (this.getLifespan() < 22) {
                            this.setLifespan(this.getLifespan() + 1);
                        }
                    } else if (e instanceof EntityVoidWorm) {
                        ((EntityVoidWorm) e).teleportTo(Vector3d.atCenterOf(this.getDestination()));
                        e.setPortalCooldown();
                        ((EntityVoidWorm) e).resetPortalLogic();
                    } else {
                        boolean flag = true;
                        if(exitDimension != null){
                            ServerWorld dimWorld = server.getLevel(exitDimension);
                            if (dimWorld != null && this.level.dimension() != exitDimension) {
                                teleportEntityFromDimension(e, dimWorld, offsetPos, true);
                                flag = false;
                            }
                        }
                        if(flag){
                            e.teleportToWithTicket(offsetPos.getX() + 0.5f, offsetPos.getY() + 0.5f, offsetPos.getZ() + 0.5f);
                            e.setPortalCooldown();
                        }
                    }
                }
            }
        }
        this.setLifespan(this.getLifespan() - 1);
        if(this.getLifespan() <= 20){
            if(!madeCloseNoise){
                this.playSound(AMSoundRegistry.VOID_PORTAL_CLOSE, 1.0F, 1 + random.nextFloat() * 0.2F);
                madeCloseNoise = true;
            }
        }
        if (this.getLifespan() <= 0) {
            this.remove();
        }
    }

    private void teleportEntityFromDimension(Entity entity, ServerWorld endpointWorld, BlockPos endpoint, boolean b) {
        if (entity instanceof ServerPlayerEntity) {
            ServerEvents.teleportPlayers.add(new Triple<>((ServerPlayerEntity)entity, endpointWorld, endpoint));
            if(this.getSisterId() == null){
                createAndSetSister(endpointWorld, Direction.DOWN);
            }
        } else {
            entity.unRide();
            entity.setLevel(endpointWorld);
            Entity teleportedEntity = entity.getType().create(endpointWorld);
            if (teleportedEntity != null) {
                teleportedEntity.restoreFrom(entity);
                teleportedEntity.moveTo(endpoint.getX() + 0.5D, endpoint.getY() + 0.5D, endpoint.getZ() + 0.5D, entity.yRot, entity.xRot);
                teleportedEntity.setYHeadRot(entity.yRot);
                teleportedEntity.setPortalCooldown();
                endpointWorld.addFromAnotherDimension(teleportedEntity);
            }
            entity.remove();
        }

    }

    public Direction getAttachmentFacing() {
        return this.entityData.get(ATTACHED_FACE);
    }

    public void setAttachmentFacing(Direction facing) {
        this.entityData.set(ATTACHED_FACE, facing);
    }

    public int getLifespan() {
        return this.entityData.get(LIFESPAN);
    }

    public void setLifespan(int i) {
        this.entityData.set(LIFESPAN, i);
    }

    public BlockPos getDestination() {
        return this.entityData.get(DESTINATION).orElse(null);
    }

    public void setDestination(BlockPos destination) {
        this.entityData.set(DESTINATION, Optional.ofNullable(destination));
        if (this.getSisterId() == null && (exitDimension == null || exitDimension == this.level.dimension())) {
            createAndSetSister(level, null);
        }
    }

    public void createAndSetSister(World world, Direction dir){
        EntityVoidPortal portal = AMEntityRegistry.VOID_PORTAL.create(world);
        portal.setAttachmentFacing(dir != null ? dir : this.getAttachmentFacing().getOpposite());
        portal.teleportToWithTicket(this.getDestination().getX() + 0.5f, this.getDestination().getY() + 0.5f, this.getDestination().getZ() + 0.5f);
        portal.link(this);
        portal.exitDimension = this.level.dimension();
        world.addFreshEntity(portal);
    }

    public void setDestination(BlockPos destination, Direction dir) {
        this.entityData.set(DESTINATION, Optional.ofNullable(destination));
        if (this.getSisterId() == null && (exitDimension == null || exitDimension == this.level.dimension())) {
            createAndSetSister(level, dir);
        }
    }

    public void link(EntityVoidPortal portal) {
        this.setSisterId(portal.getUUID());
        portal.setSisterId(this.getUUID());
        portal.setLifespan(this.getLifespan());
        this.setDestination(portal.blockPosition());
        portal.setDestination(this.blockPosition());
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(ATTACHED_FACE, Direction.DOWN);
        this.entityData.define(LIFESPAN, 300);
        this.entityData.define(SISTER_UUID, Optional.empty());
        this.entityData.define(DESTINATION, Optional.empty());
    }

    @Override
    protected void readAdditionalSaveData(CompoundNBT compound) {
        this.entityData.set(ATTACHED_FACE, Direction.from3DDataValue(compound.getByte("AttachFace")));
        this.setLifespan(compound.getInt("Lifespan"));
        if (compound.contains("DX")) {
            int i = compound.getInt("DX");
            int j = compound.getInt("DY");
            int k = compound.getInt("DZ");
            this.entityData.set(DESTINATION, Optional.of(new BlockPos(i, j, k)));
        } else {
            this.entityData.set(DESTINATION, Optional.empty());
        }
        if (compound.hasUUID("SisterUUID")) {
            this.setSisterId(compound.getUUID("SisterUUID"));
        }
        if (compound.contains("ExitDimension")) {
            this.exitDimension = World.RESOURCE_KEY_CODEC.parse(NBTDynamicOps.INSTANCE, compound.get("ExitDimension")).resultOrPartial(LOGGER::error).orElse(World.OVERWORLD);
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundNBT compound) {
        compound.putByte("AttachFace", (byte) this.entityData.get(ATTACHED_FACE).get3DDataValue());
        compound.putInt("Lifespan", getLifespan());
        BlockPos blockpos = this.getDestination();
        if (blockpos != null) {
            compound.putInt("DX", blockpos.getX());
            compound.putInt("DY", blockpos.getY());
            compound.putInt("DZ", blockpos.getZ());
        }
        if (this.getSisterId() != null) {
            compound.putUUID("SisterUUID", this.getSisterId());
        }
        if(this.exitDimension != null){
            ResourceLocation.CODEC.encodeStart(NBTDynamicOps.INSTANCE, this.exitDimension.location()).resultOrPartial(LOGGER::error).ifPresent((p_241148_1_) -> {
                compound.put("ExitDimension", p_241148_1_);
            });
        }

    }

    public Entity getSister() {
        UUID id = getSisterId();
        if (id != null && !level.isClientSide) {
            return ((ServerWorld) level).getEntity(id);
        }
        return null;
    }

    @Nullable
    public UUID getSisterId() {
        return this.entityData.get(SISTER_UUID).orElse(null);
    }

    public void setSisterId(@Nullable UUID uniqueId) {
        this.entityData.set(SISTER_UUID, Optional.ofNullable(uniqueId));
    }

}
