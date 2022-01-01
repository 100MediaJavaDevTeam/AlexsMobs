package com.github.alexthe666.alexsmobs.entity;

import com.github.alexthe666.alexsmobs.item.AMItemRegistry;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ILivingEntityData;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.projectile.ProjectileItemEntity;
import net.minecraft.item.Item;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.particles.ItemParticleData;
import net.minecraft.particles.ParticleTypes;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.FMLPlayMessages;
import net.minecraftforge.fml.network.NetworkHooks;

public class EntityCockroachEgg extends ProjectileItemEntity {

    public EntityCockroachEgg(EntityType p_i50154_1_, World p_i50154_2_) {
        super(p_i50154_1_, p_i50154_2_);
    }

    public EntityCockroachEgg(World worldIn, LivingEntity throwerIn) {
        super(AMEntityRegistry.COCKROACH_EGG, throwerIn, worldIn);
    }

    public EntityCockroachEgg(World worldIn, double x, double y, double z) {
        super(AMEntityRegistry.COCKROACH_EGG, x, y, z, worldIn);
    }

    public EntityCockroachEgg(FMLPlayMessages.SpawnEntity spawnEntity, World world) {
        this(AMEntityRegistry.COCKROACH_EGG, world);
    }

    @Override
    public IPacket<?> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @OnlyIn(Dist.CLIENT)
    public void handleEntityEvent(byte id) {
        if (id == 3) {
            double d0 = 0.08D;

            for(int i = 0; i < 8; ++i) {
                this.level.addParticle(new ItemParticleData(ParticleTypes.ITEM, this.getItem()), this.getX(), this.getY(), this.getZ(), ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D, ((double)this.random.nextFloat() - 0.5D) * 0.08D);
            }
        }

    }

    protected void onHit(RayTraceResult result) {
        super.onHit(result);
        if (!this.level.isClientSide) {
            this.level.broadcastEntityEvent(this, (byte)3);
            int i = random.nextInt(3);
            for(int j = 0; j < i; ++j) {
                EntityCockroach croc = AMEntityRegistry.COCKROACH.create(this.level);
                croc.setAge(-24000);
                croc.moveTo(this.getX(), this.getY(), this.getZ(), this.yRot, 0.0F);
                croc.finalizeSpawn((ServerWorld)level, level.getCurrentDifficultyAt(this.blockPosition()), SpawnReason.TRIGGERED, (ILivingEntityData)null, (CompoundNBT)null);
                croc.restrictTo(this.blockPosition(), 20);
                this.level.addFreshEntity(croc);
            }
            this.level.broadcastEntityEvent(this, (byte)3);
            this.remove();
        }

    }

    protected Item getDefaultItem() {
        return AMItemRegistry.COCKROACH_OOTHECA;
    }
}
